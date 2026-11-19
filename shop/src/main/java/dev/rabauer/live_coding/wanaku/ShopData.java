package dev.rabauer.live_coding.wanaku;

import dev.rabauer.live_coding.wanaku.Models.Customer;
import dev.rabauer.live_coding.wanaku.Models.Order;
import dev.rabauer.live_coding.wanaku.Models.OrderStatus;
import dev.rabauer.live_coding.wanaku.Models.Product;
import dev.rabauer.live_coding.wanaku.Models.Refund;
import dev.rabauer.live_coding.wanaku.Models.Review;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * In-memory shop data. New reviews are untrusted text from anyone on the internet. They start as PENDING
 * and the moderation agent reads them: that is where an injected instruction gets in.
 */
@ApplicationScoped
public class ShopData {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String HIDDEN = "HIDDEN";

    private final Map<Integer, Product> products = new ConcurrentHashMap<>();
    private final Map<Integer, Customer> customers = new ConcurrentHashMap<>();
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final Map<Integer, Review> reviews = new LinkedHashMap<>();   // guarded by "this"
    private int nextReviewId;

    public ShopData() {
        reset();
    }

    /** Back to the start state of the demo: seed data, no refunds, no extra reviews. */
    public final synchronized void reset() {
        products.clear();
        customers.clear();
        orders.clear();
        reviews.clear();
        Stream.of(
                new Product(1, "Mechanical Keyboard", 89.00, "Tenkeyless, brown switches"),
                new Product(2, "Wireless Mouse", 39.90, "Ergonomic, 2.4 GHz"),
                new Product(3, "HD Webcam", 59.50, "1080p with microphone"))
                .forEach(p -> products.put(p.id(), p));
        Stream.of(
                new Customer(1, "Anna Berger", "anna.berger@example.com"),
                new Customer(2, "Ben Fischer", "ben.fischer@example.com"),
                new Customer(3, "Carol Winter", "carol.winter@example.com"))
                .forEach(c -> customers.put(c.id(), c));
        Stream.of(
                new Order(1234, 2, 1, 89.00, "PAID", ""),
                new Order(1235, 1, 2, 39.90, "PAID", ""),
                new Order(1236, 3, 3, 59.50, "PAID", "Please deliver to the neighbour."))
                .forEach(o -> orders.put(o.id(), o));
        seed(1, 1, "anna", 5, "Solid keyboard, the switches feel great.");
        seed(2, 1, "ben", 4, "Good value, a bit loud at night.");
        seed(3, 2, "dave", 4, "Fits my hand, the battery lasts for weeks.");
        seed(4, 3, "carol", 3, "The webcam picture is sharp, the microphone is average.");
        nextReviewId = 5;
    }

    private void seed(int id, int productId, String author, int rating, String text) {
        reviews.put(id, new Review(id, productId, author, rating, text, APPROVED, ""));
    }

    public List<Product> searchProducts(String query) {
        String q = query == null ? "" : query.toLowerCase();
        return products.values().stream()
                .filter(p -> p.name().toLowerCase().contains(q) || p.description().toLowerCase().contains(q))
                .sorted(Comparator.comparingInt(Product::id))
                .toList();
    }

    public Product updatePrice(int id, double price) {
        return products.computeIfPresent(id, (k, p) -> new Product(p.id(), p.name(), price, p.description()));
    }

    public synchronized List<Review> reviews(int productId) {
        return reviews.values().stream().filter(r -> r.productId() == productId).toList();
    }

    public synchronized Review review(int id) {
        return reviews.get(id);
    }

    /** Returns null for an unknown product. New reviews are PENDING. Author, rating and text are sanitised. */
    public synchronized Review addReview(int productId, String author, int rating, String text) {
        if (!products.containsKey(productId)) {
            return null;
        }
        int id = nextReviewId++;
        Review review = new Review(id, productId, clean(author, 30, "anonymous"), clampRating(rating),
                clean(text, 1500, ""), PENDING, "");
        reviews.put(id, review);
        return review;
    }

    /** Returns null for an unknown review. */
    public synchronized Review setRating(int id, int rating) {
        Review r = reviews.get(id);
        if (r == null) {
            return null;
        }
        Review changed = new Review(r.id(), r.productId(), r.author(), clampRating(rating), r.text(), r.status(),
                r.moderationNote());
        reviews.put(id, changed);
        return changed;
    }

    /** Sets APPROVED or HIDDEN with a public note. Returns null for an unknown review. */
    public synchronized Review moderate(int id, String status, String note) {
        Review r = reviews.get(id);
        if (r == null) {
            return null;
        }
        Review changed = new Review(r.id(), r.productId(), r.author(), r.rating(), r.text(), status,
                clean(note, 600, ""));
        reviews.put(id, changed);
        return changed;
    }

    private static int clampRating(int rating) {
        return Math.max(1, Math.min(5, rating == 0 ? 5 : rating));
    }

    private static String clean(String value, int max, String fallback) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            return fallback;
        }
        return v.length() > max ? v.substring(0, max) : v;
    }

    public List<Customer> customers() {
        return customers.values().stream().sorted(Comparator.comparingInt(Customer::id)).toList();
    }

    public Order order(int id) {
        return orders.get(id);
    }

    /** Only id and status, no customer data: safe to show on the public storefront. */
    public List<OrderStatus> orderStatuses() {
        return orders.values().stream()
                .sorted(Comparator.comparingInt(Order::id))
                .map(o -> new OrderStatus(o.id(), o.productId(), o.amount(), o.status()))
                .toList();
    }

    /** No limit on purpose: the shop trusts whoever has the admin token. */
    public Refund refund(int orderId, double amount) {
        Order o = orders.get(orderId);
        if (o == null) {
            return null;
        }
        orders.put(orderId, new Order(o.id(), o.customerId(), o.productId(), o.amount(), "REFUNDED", o.note()));
        return new Refund(orderId, amount, "REFUNDED");
    }
}
