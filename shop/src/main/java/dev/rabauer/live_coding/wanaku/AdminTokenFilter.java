package dev.rabauer.live_coding.wanaku;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.regex.Pattern;

/**
 * Admin calls (customers, orders, refunds, prices) need "Authorization: Bearer shop.admin-token".
 * What a storefront visitor needs stays open: reading products and reviews, writing a review,
 * the order status list and the demo reset.
 */
@Provider
public class AdminTokenFilter implements ContainerRequestFilter {

    private static final Pattern NEW_REVIEW = Pattern.compile("api/products/\\d+/reviews");

    @ConfigProperty(name = "shop.admin-token")
    String adminToken;

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (isOpen(ctx.getMethod(), ctx.getUriInfo().getPath())) {
            return;
        }
        String header = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!("Bearer " + adminToken).equals(header)) {
            ctx.abortWith(Response.status(401).entity("admin token required").build());
        }
    }

    private static boolean isOpen(String method, String rawPath) {
        String path = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        if (!path.startsWith("api/")) {
            return true;
        }
        return switch (method) {
            case "GET" -> path.startsWith("api/products") || path.equals("api/orders/status");
            case "POST" -> NEW_REVIEW.matcher(path).matches() || path.equals("api/demo/reset");
            default -> false;
        };
    }
}
