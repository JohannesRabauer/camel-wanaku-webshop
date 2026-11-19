package dev.rabauer.live_coding.wanaku;

public final class Models {
    private Models() {
    }

    public record Product(int id, String name, double price, String description) {
    }

    /** status is PENDING until the moderation agent approves or hides the review. The note is public. */
    public record Review(int id, int productId, String author, int rating, String text, String status,
                         String moderationNote) {
    }

    public record Customer(int id, String name, String email) {
    }

    public record Order(int id, int customerId, int productId, double amount, String status, String note) {
    }

    public record OrderStatus(int id, int productId, double amount, String status) {
    }

    public record NewReview(String author, int rating, String text) {
    }

    public record Refund(int orderId, double amount, String status) {
    }
}
