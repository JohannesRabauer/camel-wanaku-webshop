package io.jug.webshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Wraps the shop's REST endpoints as AI tools. Each ai-tool route becomes an MCP tool (tag "shop").
 * Declared parameters arrive as headers; the shared route direct:shopApi calls the REST API
 * with the admin token.
 */
@ApplicationScoped
public class ShopTools extends RouteBuilder {

    @ConfigProperty(name = "shop.admin-token")
    String adminToken;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    @Override
    public void configure() {
        from("ai-tool:searchProducts?tags=shop&description=Search products by name or description"
                + "&parameter.query=string&parameter.query.description=Search text&parameter.query.required=true")
                .process(e -> e.getMessage().setHeader(Exchange.HTTP_QUERY,
                        "query=" + URLEncoder.encode(e.getMessage().getHeader("query", String.class), StandardCharsets.UTF_8)))
                .setHeader("shopPath", constant("products"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from("ai-tool:getReviews?tags=shop&description=Get the customer reviews of a product"
                + "&parameter.productId=integer&parameter.productId.description=Product id&parameter.productId.required=true")
                .setHeader("shopPath", simple("products/${header.productId}/reviews"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from("ai-tool:getOrder?tags=shop&description=Get an order by id"
                + "&parameter.orderId=integer&parameter.orderId.description=Order id&parameter.orderId.required=true")
                .setHeader("shopPath", simple("orders/${header.orderId}"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from("ai-tool:refundOrder?tags=shop&description=Refund an order, fully or partly"
                + "&parameter.orderId=integer&parameter.orderId.description=Order id&parameter.orderId.required=true"
                + "&parameter.amount=number&parameter.amount.description=Amount in EUR&parameter.amount.required=true")
                .setHeader("shopPath", simple("orders/${header.orderId}/refund"))
                .setHeader(Exchange.HTTP_QUERY, simple("amount=${header.amount}"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("direct:shopApi");

        from("ai-tool:listCustomers?tags=shop&description=List all customers with their email addresses")
                .setHeader("shopPath", constant("customers"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("direct:shopApi");

        from("ai-tool:updatePrice?tags=shop&description=Change the price of a product"
                + "&parameter.productId=integer&parameter.productId.description=Product id&parameter.productId.required=true"
                + "&parameter.price=number&parameter.price.description=New price in EUR&parameter.price.required=true")
                .setHeader("shopPath", simple("products/${header.productId}/price"))
                .setHeader(Exchange.HTTP_QUERY, simple("price=${header.price}"))
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .to("direct:shopApi");

        // The only place that knows the admin token. In the Wanaku part of the session this moves out.
        from("direct:shopApi")
                .removeHeaders("*", "CamelHttp*", "shopPath")
                .setHeader("Authorization", constant("Bearer " + adminToken))
                .toD("http://localhost:" + port + "/api/${header.shopPath}")
                .convertBodyTo(String.class);
    }
}
