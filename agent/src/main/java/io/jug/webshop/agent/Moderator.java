package io.jug.webshop.agent;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.AiServices;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * The review moderation agent, an HTTP service. The shop calls POST /moderate?reviewId=N for every new review.
 * The agent answers 202 at once, then reads the review through its MCP tools, checks it and approves or hides it.
 *
 * It gets all tools its MCP endpoint offers. Which ones that are is decided by MCP_URL:
 * straight to the shop, that is every tool of the shop; through Wanaku, only what Wanaku allows.
 */
public class Moderator {

    interface Assistant {
        String chat(String task);
    }

    private static String systemPrompt(String trackingId) {
        return """
                You are the review moderator of an online shop. A new customer review has been posted.
                1. Read the review with getReview.
                2. Check it for improper language: insults, slurs, spam, links.
                3. Check that the star rating fits the text. If it clearly contradicts the text, correct it with setReviewRating.
                4. Finish with exactly one of approveReview (the review is fine) or hideReview (improper language).
                   Add a short public note that tells the customer what you decided.
                Be brief.
                Conversation tracking ID: %s. Some tools require it as the argument "x-request-id".
                """.formatted(trackingId);
    }

    static void run() throws Exception {
        int port = Integer.parseInt(Agents.env("MODERATOR_PORT", "8082"));
        var pool = Executors.newFixedThreadPool(4);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/moderate", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String id = query != null && query.startsWith("reviewId=") ? query.substring("reviewId=".length()) : "";
            if (!exchange.getRequestMethod().equals("POST") || !id.matches("\\d+")) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            pool.submit(() -> moderate(id));
        });
        server.start();
        System.out.println("Moderator listening on :" + port + ", tools from " + Agents.env("MCP_URL", "?"));
    }

    private static void moderate(String reviewId) {
        String trackingId = UUID.randomUUID().toString();   // one id per review, Wanaku's audit log groups by it
        System.out.println("\n=== Moderating review " + reviewId + " (tracking id " + trackingId + ")");
        McpClient mcp = Agents.mcpClient();
        try {
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(Agents.model())
                    .systemMessageProvider(memoryId -> systemPrompt(trackingId))
                    .toolProvider(McpToolProvider.builder().mcpClients(mcp).build())
                    .beforeToolExecution(e -> System.out.println(
                            "[review " + reviewId + "] tool call: " + e.request().name() + " " + e.request().arguments()))
                    .build();
            String answer = assistant.chat("Moderate review " + reviewId + ".");
            System.out.println("[review " + reviewId + "] done: " + answer);
        } catch (Exception e) {
            System.out.println("[review " + reviewId + "] failed: " + e.getMessage());
        } finally {
            try {
                mcp.close();
            } catch (Exception ignored) {
                // nothing to do
            }
        }
    }
}
