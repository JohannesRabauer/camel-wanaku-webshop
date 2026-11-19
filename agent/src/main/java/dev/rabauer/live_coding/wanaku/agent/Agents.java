package dev.rabauer.live_coding.wanaku.agent;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;

/** Shared setup for the CLI assistant and the moderation service: the MCP connection and the language model. */
final class Agents {

    private Agents() {
    }

    /**
     * Connects to the tools. Straight to the shop (http://shop:9090/mcp) or through Wanaku
     * (http://wanaku:8081/moderation/mcp): the only difference is this URL.
     */
    static McpClient mcpClient() {
        var transport = StreamableHttpMcpTransport.builder().url(env("MCP_URL", "http://localhost:9090/mcp"));
        String auth = System.getenv("MCP_AUTH");
        if (auth != null && !auth.isBlank()) {
            transport.customHeaders(Map.of("Authorization", auth));
        }
        // Pin the protocol version. Without it the client waits 30 seconds for a protocol detection
        // answer that Wanaku does not send ("method not supported"), and every moderation starts late.
        return DefaultMcpClient.builder()
                .key("shop")
                .protocolVersion("2025-11-25")
                .transport(transport.build())
                .build();
    }

    static ChatModel model() {
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

    static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }
}
