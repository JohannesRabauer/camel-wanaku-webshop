package io.jug.webshop;

import io.jug.webshop.Models.Customer;
import io.jug.webshop.Models.NewReview;
import io.jug.webshop.Models.OrderStatus;
import io.jug.webshop.Models.Product;
import io.jug.webshop.Models.Review;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** The webshop's REST API. Storefront calls are open, everything else needs the admin token (see AdminTokenFilter). */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ShopResource {

    @Inject
    ShopData data;

    @GET
    @Path("/products")
    public List<Product> products(@QueryParam("query") String query) {
        return data.searchProducts(query);
    }

    @PUT
    @Path("/products/{id}/price")
    public Response updatePrice(@PathParam("id") int id, @QueryParam("price") double price) {
        return orNotFound(data.updatePrice(id, price));
    }

    @GET
    @Path("/products/{id}/reviews")
    public List<Review> reviews(@PathParam("id") int id) {
        return data.reviews(id);
    }

    /** Open to everyone, like on a real shop: any visitor can write a review. */
    @POST
    @Path("/products/{id}/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReview(@PathParam("id") int id, NewReview body) {
        if (body == null || body.text() == null || body.text().isBlank()) {
            return Response.status(400).entity("text is required").build();
        }
        Review created = data.addReview(id, body.author(), body.text());
        return created == null ? Response.status(404).build() : Response.status(201).entity(created).build();
    }

    /** Order id and status only, for the storefront's "shop owner view". */
    @GET
    @Path("/orders/status")
    public List<OrderStatus> orderStatuses() {
        return data.orderStatuses();
    }

    /** Demo helper: back to the start state. */
    @POST
    @Path("/demo/reset")
    public Response reset() {
        data.reset();
        return Response.noContent().build();
    }

    @GET
    @Path("/customers")
    public List<Customer> customers() {
        return data.customers();
    }

    @GET
    @Path("/orders/{id}")
    public Response order(@PathParam("id") int id) {
        return orNotFound(data.order(id));
    }

    @POST
    @Path("/orders/{id}/refund")
    public Response refund(@PathParam("id") int id, @QueryParam("amount") double amount) {
        return orNotFound(data.refund(id, amount));
    }

    private static Response orNotFound(Object body) {
        return body == null ? Response.status(404).build() : Response.ok(body).build();
    }
}
