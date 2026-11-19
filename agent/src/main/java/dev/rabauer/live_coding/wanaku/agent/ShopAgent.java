package dev.rabauer.live_coding.wanaku.agent;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.AiServices;

/**
 * Two ways to run the agent module:
 *
 *   AGENT_MODE=moderator   the review moderation service (see Moderator), started by Docker Compose
 *   (default)              a command line assistant: mvn -q compile exec:java -Dexec.args="your question"
 *
 * Environment:
 *   MCP_URL            where the tools live (default http://localhost:9090/mcp, the Camel MCP server)
 *   MCP_AUTH           optional value for the Authorization header, e.g. "Bearer abc"
 *   AGENT_PROVIDER     anthropic (default) or openai
 *   ANTHROPIC_API_KEY / OPENAI_API_KEY
 *   AGENT_MODEL        model name, default claude-sonnet-5-5 or gpt-4o-mini
 */
public class ShopAgent {

    /** One id per run, so all tool calls of one conversation share it. */
    private static final String TRACKING_ID = java.util.UUID.randomUUID().toString();

    interface Assistant {
        String chat(String question);
    }

    /** Wanaku adds a required "x-request-id" argument to every tool and expects the id in the system prompt. */
    private static String systemPrompt(String trackingId) {
        return """
                You are the assistant of an online shop. Use the shop tools to answer the user's question.
                Be brief. Say which tools you called.
                Conversation tracking ID: %s. Some tools require it as the argument "x-request-id".
                """.formatted(trackingId);
    }

    public static void main(String[] args) throws Exception {
        if ("moderator".equals(Agents.env("AGENT_MODE", ""))) {
            Moderator.run();
            return;
        }
        String question = args.length > 0
                ? String.join(" ", args)
                : "Which products does the shop sell, and what do they cost?";

        McpClient mcp = Agents.mcpClient();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(Agents.model())
                .systemMessageProvider(memoryId -> systemPrompt(TRACKING_ID))
                .toolProvider(McpToolProvider.builder().mcpClients(mcp).build())
                .beforeToolExecution(e -> System.out.println(
                        ">> tool call: " + e.request().name() + " " + e.request().arguments()))
                .build();

        System.out.println("Question: " + question + "\n");
        System.out.println("\nAnswer:\n" + assistant.chat(question));
        mcp.close();
    }
}
