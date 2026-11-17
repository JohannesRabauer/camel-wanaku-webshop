package io.jug.webshop;

public final class Models {
    private Models() {
    }

    public record Product(int id, String name, double price, String description) {
    }

    public record Review(int id, int productId, String author, String text) {
    }

    public record Customer(int id, String name, String email) {
    }

    public record Order(int id, int customerId, int productId, double amount, String status, String note) {
    }

    public record Refund(int orderId, double amount, String status) {
    }
}
