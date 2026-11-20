package dev.rabauer.live_coding.wanaku;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Wraps the shop's REST endpoints as AI tools. Each ai-tool route becomes an MCP tool (tag "shop").
 * Declared parameters arrive as headers; the shared route direct:shopApi calls the REST API
 * with the admin token.
 */
@ApplicationScoped
public class ShopTools extends EndpointRouteBuilder {

    @ConfigProperty(name = "shop.admin-token")
    String adminToken;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    @Override
    public void configure() {
        from(aiTool("searchProducts")
                .tags("shop")
                .description("Search products by name or description")
                .parameters("query", "string")
                .parameters("query.description", "Search text")
                .parameters("query.required", "true"))
                .process(e -> e.getMessage().setHeader(Exchange.HTTP_QUERY,
                        "query=" + URLEncoder.encode(e.getMessage().getHeader("query", String.class), StandardCharsets.UTF_8)))
                .setHeader("shopPath", constant("products"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from(aiTool("getReviews")
                .tags("shop")
                .description("Get the customer reviews of a product")
                .parameters("productId", "integer")
                .parameters("productId.description", "Product id")
                .parameters("productId.required", "true"))
                .setHeader("shopPath", simple("products/${header.productId}/reviews"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from(aiTool("getOrder")
                .tags("shop")
                .description("Get an order by id")
                .parameters("orderId", "integer")
                .parameters("orderId.description", "Order id")
                .parameters("orderId.required", "true"))
                .setHeader("shopPath", simple("orders/${header.orderId}"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from(aiTool("refundOrder")
                .tags("shop")
                .description("Refund an order, fully or partly")
                .parameters("orderId", "integer")
                .parameters("orderId.description", "Order id")
                .parameters("orderId.required", "true")
                .parameters("amount", "number")
                .parameters("amount.description", "Amount in EUR")
                .parameters("amount.required", "true"))
                .setHeader("shopPath", simple("orders/${header.orderId}/refund"))
                .setHeader(Exchange.HTTP_QUERY, simple("amount=${header.amount}"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("direct:shopApi");

        from(aiTool("listCustomers")
                .tags("shop")
                .description("List all customers with their email addresses"))
                .setHeader("shopPath", constant("customers"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from(aiTool("updatePrice")
                .tags("shop")
                .description("Change the price of a product")
                .parameters("productId", "integer")
                .parameters("productId.description", "Product id")
                .parameters("productId.required", "true")
                .parameters("price", "number")
                .parameters("price.description", "New price in EUR")
                .parameters("price.required", "true"))
                .setHeader("shopPath", simple("products/${header.productId}/price"))
                .setHeader(Exchange.HTTP_QUERY, simple("price=${header.price}"))
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .to("direct:shopApi");

        // Moderation tools: what the review moderation agent is meant to use.
        from(aiTool("getReview")
                .tags("shop")
                .description("Get one customer review by id, with its star rating and status")
                .parameters("reviewId", "integer")
                .parameters("reviewId.description", "Review id")
                .parameters("reviewId.required", "true"))
                .setHeader("shopPath", simple("reviews/${header.reviewId}"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        // LIVE: setReviewRating goes here (PUT reviews/{reviewId}/rating?rating=N, parameters reviewId and rating)

        from(aiTool("approveReview")
                .tags("shop")
                .description("Approve a review so it is shown in the shop, with a short public moderation note")
                .parameters("reviewId", "integer")
                .parameters("reviewId.description", "Review id")
                .parameters("reviewId.required", "true")
                .parameters("note", "string")
                .parameters("note.description", "Short public note shown with the review")
                .parameters("note.required", "true"))
                .setHeader("shopPath", simple("reviews/${header.reviewId}/approve"))
                .process(ShopTools::noteQuery)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("direct:shopApi");

        // LIVE: hideReview goes here (POST reviews/{reviewId}/hide?note=..., like approveReview above)

        // The only place that knows the admin token. In the Wanaku part of the session this moves out.
        from("direct:shopApi")
                .removeHeaders("*", "CamelHttp*", "shopPath")
                .setHeader("Authorization", constant("Bearer " + adminToken))
                .toD("http://localhost:" + port + "/api/${header.shopPath}")
                .convertBodyTo(String.class);
    }

    private static void noteQuery(Exchange e) {
        String note = e.getMessage().getHeader("note", "", String.class);
        e.getMessage().setHeader(Exchange.HTTP_QUERY, "note=" + URLEncoder.encode(note, StandardCharsets.UTF_8));
    }
}
