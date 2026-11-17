package io.jug.webshop;

import io.jug.webshop.Models.Customer;
import io.jug.webshop.Models.Product;
import io.jug.webshop.Models.Review;
import jakarta.inject.Inject;
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

/** The webshop's REST API. Every call needs the admin token (see AdminTokenFilter). */
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
