package io.jug.webshop.agent;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

/**
 * A small shop assistant: an LLM that can call the shop's tools over MCP.
 *
 * Usage: mvn -q compile exec:java -Dexec.args="your question"
 *
 * Environment:
 *   MCP_URL            where the tools live (default http://localhost:9090/mcp, the Camel MCP server)
 *   MCP_AUTH           optional value for the Authorization header, e.g. "Bearer abc" (for Wanaku later)
 *   AGENT_PROVIDER     anthropic (default) or openai
 *   ANTHROPIC_API_KEY / OPENAI_API_KEY
 *   AGENT_MODEL        model name, default claude-sonnet-5-5 or gpt-4o-mini
 */
public class ShopAgent {

    interface Assistant {
        @SystemMessage("""
                You are the assistant of an online shop. Use the shop tools to answer the user's question.
                Be brief. Say which tools you called.
                """)
        String chat(String question);
    }

    public static void main(String[] args) throws Exception {
        String question = args.length > 0
                ? String.join(" ", args)
                : "Look at the reviews of the Wireless Mouse and tell me in two sentences what customers think.";

        String mcpUrl = env("MCP_URL", "http://localhost:9090/mcp");
        var transport = StreamableHttpMcpTransport.builder().url(mcpUrl);
        String auth = System.getenv("MCP_AUTH");
        if (auth != null && !auth.isBlank()) {
            transport.customHeaders(java.util.Map.of("Authorization", auth));
        }
        McpClient mcp = DefaultMcpClient.builder().key("shop").transport(transport.build()).build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model())
                .toolProvider(McpToolProvider.builder().mcpClients(mcp).build())
                .beforeToolExecution(e -> System.out.println(
                        ">> tool call: " + e.request().name() + " " + e.request().arguments()))
                .build();

        System.out.println("Question: " + question + "\n");
        System.out.println("\nAnswer:\n" + assistant.chat(question));
        mcp.close();
    }

    private static ChatModel model() {
        String provider = env("AGENT_PROVIDER", "anthropic");
        if (provider.equals("openai")) {
            return OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .modelName(env("AGENT_MODEL", "gpt-4o-mini"))
                    .build();
        }
        return AnthropicChatModel.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .modelName(env("AGENT_MODEL", "claude-sonnet-5-5"))
                .maxTokens(1024)
                .build();
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }
}
