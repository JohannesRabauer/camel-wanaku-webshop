package io.jug.webshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Every new review is handed to the moderation agent, no human involved:
 * ShopResource drops the review id on seda:reviewPosted, this route calls the agent's HTTP endpoint.
 * The agent answers 202 at once and moderates in the background through its MCP tools.
 */
@ApplicationScoped
public class ModerationTrigger extends RouteBuilder {

    @ConfigProperty(name = "shop.moderator-url")
    String moderatorUrl;

    @Override
    public void configure() {
        from("seda:reviewPosted")
                .onException(Exception.class)
                    .handled(true)
                    .log(LoggingLevel.WARN, "Moderation agent not reachable, review ${body} stays PENDING: ${exception.message}")
                .end()
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.HTTP_QUERY, simple("reviewId=${body}"))
                .setBody(constant(""))
                .to(moderatorUrl + "/moderate")
                .log("Moderation requested for review ${header.CamelHttpQuery}");
    }
}
