
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class FlashSaleInventory {
    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
    private final Map<String, Queue<Integer>> waitingList = new ConcurrentHashMap<>();
    public FlashSaleInventory() {
        inventory.put("IPHONE15_256GB", new AtomicInteger(100));
        inventory.put("MACBOOK_PRO_M3", new AtomicInteger(50));
        waitingList.put("IPHONE15_256GB", new LinkedList<>());
        waitingList.put("MACBOOK_PRO_M3", new LinkedList<>());
    }
    public int checkStock(String productId) {
        AtomicInteger stock = inventory.get(productId);
        return stock == null ? -1 : stock.get();
    }
    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = inventory.get(productId);
        if (stock == null) return "Product not found";
        int current;
        do {
            current = stock.get();
            if (current <= 0) {
                waitingList.get(productId).offer(userId);
                int position = waitingList.get(productId).size();
                return "Added to waiting list, position #" + position;
            }
        } while (!stock.compareAndSet(current, current - 1));
        return "Success, " + (current - 1) + " units remaining";
    }
    public void addProduct(String productId, int initialStock) {
        inventory.put(productId, new AtomicInteger(initialStock));
        waitingList.put(productId, new LinkedList<>());
    }
    public List<Integer> restock(String productId, int units) {
        AtomicInteger stock = inventory.get(productId);
        if (stock == null) return Collections.emptyList();
        List<Integer> notified = new ArrayList<>();
        Queue<Integer> waiting = waitingList.get(productId);
        while (units > 0 && !waiting.isEmpty()) {
            notified.add(waiting.poll());
            units--;
        }
        stock.addAndGet(units);
        return notified;
    }
    public static void main(String[] args) throws InterruptedException {
        FlashSaleInventory inventory = new FlashSaleInventory();
        System.out.println("=== Problem 2: Flash Sale Inventory Manager ===\n");
        System.out.println("checkStock(\"IPHONE15_256GB\") -> " + inventory.checkStock("IPHONE15_256GB") + " units available");
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 12345));
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 67890));
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (int i = 3; i <= 100; i++) {
            final int userId = i;
            executor.submit(() -> inventory.purchaseItem("IPHONE15_256GB", userId));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Stock after 100 purchases: " + inventory.checkStock("IPHONE15_256GB"));
        System.out.println(inventory.purchaseItem("IPHONE15_256GB", 99999));
        System.out.println("\nRestocking 5 units...");
        List<Integer> notified = inventory.restock("IPHONE15_256GB", 5);
        System.out.println("Notified waiting users: " + notified);
        System.out.println("Stock after restock: " + inventory.checkStock("IPHONE15_256GB"));
    }
}