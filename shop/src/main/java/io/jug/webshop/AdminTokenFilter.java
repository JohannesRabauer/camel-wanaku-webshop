package io.jug.webshop;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Rejects every API call without "Authorization: Bearer shop.admin-token". */
@Provider
public class AdminTokenFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "shop.admin-token")
    String adminToken;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String header = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!("Bearer " + adminToken).equals(header)) {
            ctx.abortWith(Response.status(401).entity("admin token required").build());
        }
    }
}
