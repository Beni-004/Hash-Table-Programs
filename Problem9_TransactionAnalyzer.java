import java.util.*;
import java.util.stream.Collectors;
public class Problem9_TransactionAnalyzer {
    static class Transaction {
        int id;
        double amount;
        String merchant;
        String accountId;
        String time; 
        Transaction(int id, double amount, String merchant, String accountId, String time) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.accountId = accountId;
            this.time = time;
        }
        int timeInMinutes() {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }
        @Override
        public String toString() {
            return String.format("(id:%d, $%.0f, %s, acc:%s, %s)", id, amount, merchant, accountId, time);
        }
    }
    private final List<Transaction> transactions;
    public Problem9_TransactionAnalyzer(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    public List<int[]> findTwoSum(double target) {
        List<int[]> result = new ArrayList<>();
        Map<Double, List<Transaction>> complementMap = new HashMap<>();
        for (Transaction tx : transactions) {
            double complement = Math.round((target - tx.amount) * 100.0) / 100.0;
            if (complementMap.containsKey(tx.amount)) {
                for (Transaction match : complementMap.get(tx.amount)) {
                    result.add(new int[]{match.id, tx.id});
                }
            }
            complementMap.computeIfAbsent(complement, k -> new ArrayList<>()).add(tx);
        }
        System.out.printf("findTwoSum(target=$%.0f) -> %s%n", target,
                result.stream().map(p -> "(id:" + p[0] + ", id:" + p[1] + ")")
                        .collect(Collectors.joining(", ", "[", "]")));
        return result;
    }
    public List<int[]> findTwoSumWithTimeWindow(double target, int windowMinutes) {
        List<int[]> result = new ArrayList<>();
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparingInt(Transaction::timeInMinutes));
        Map<Double, Transaction> window = new LinkedHashMap<>();
        for (Transaction tx : sorted) {
            window.entrySet().removeIf(e ->
                    tx.timeInMinutes() - e.getValue().timeInMinutes() > windowMinutes);
            double complement = Math.round((target - tx.amount) * 100.0) / 100.0;
            if (window.containsKey(tx.amount)) {
                Transaction match = window.get(tx.amount);
                result.add(new int[]{match.id, tx.id});
            }
            window.put(complement, tx);
        }
        System.out.printf("findTwoSumWithTimeWindow(target=$%.0f, window=%dmin) -> %s%n",
                target, windowMinutes,
                result.stream().map(p -> "(id:" + p[0] + ", id:" + p[1] + ")")
                        .collect(Collectors.joining(", ", "[", "]")));
        return result;
    }
    public List<List<Integer>> findKSum(int k, double target) {
        List<List<Integer>> result = new ArrayList<>();
        kSumHelper(transactions, 0, k, target, new ArrayList<>(), result);
        System.out.printf("findKSum(k=%d, target=$%.0f) -> %s%n", k, target,
                result.stream()
                        .map(ids -> ids.stream().map(id -> "id:" + id)
                                .collect(Collectors.joining(", ", "(", ")")))
                        .collect(Collectors.joining(", ", "[", "]")));
        return result;
    }
    private void kSumHelper(List<Transaction> txList, int start, int k, double remaining,
                             List<Integer> current, List<List<Integer>> result) {
        if (k == 1) {
            Map<Double, Transaction> map = new HashMap<>();
            for (int i = start; i < txList.size(); i++) {
                Transaction tx = txList.get(i);
                if (Math.abs(tx.amount - remaining) < 0.001) {
                    List<Integer> combo = new ArrayList<>(current);
                    combo.add(tx.id);
                    result.add(combo);
                    return; 
                }
            }
            return;
        }
        for (int i = start; i < txList.size(); i++) {
            Transaction tx = txList.get(i);
            if (tx.amount <= remaining) {
                current.add(tx.id);
                kSumHelper(txList, i + 1, k - 1, remaining - tx.amount, current, result);
                current.remove(current.size() - 1);
            }
        }
    }
    public void detectDuplicates() {
        Map<String, List<Transaction>> grouped = new HashMap<>();
        for (Transaction tx : transactions) {
            String key = tx.amount + "_" + tx.merchant;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }
        System.out.println("detectDuplicates() ->");
        boolean found = false;
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            List<Transaction> group = entry.getValue();
            Set<String> accounts = new HashSet<>();
            for (Transaction tx : group) accounts.add(tx.accountId);
            if (accounts.size() > 1) {
                found = true;
                double amount = group.get(0).amount;
                String merchant = group.get(0).merchant;
                System.out.printf("  [{amount:$%.0f, merchant:\"%s\", accounts:%s}]%n",
                        amount, merchant, accounts);
            }
        }
        if (!found) System.out.println("  No duplicates found.");
    }
    public static void main(String[] args) {
        System.out.println("=== Problem 9: Two-Sum Variants for Financial Transactions ===\n");
        List<Transaction> txList = Arrays.asList(
                new Transaction(1, 500, "Store A", "acc1", "10:00"),
                new Transaction(2, 300, "Store B", "acc2", "10:15"),
                new Transaction(3, 200, "Store C", "acc3", "10:30"),
                new Transaction(4, 150, "Store D", "acc4", "10:40"),
                new Transaction(5, 350, "Store E", "acc5", "11:00"),
                new Transaction(6, 500, "Store A", "acc6", "10:05"), 
                new Transaction(7, 100, "Store F", "acc7", "14:00"),
                new Transaction(8, 400, "Store G", "acc8", "14:10")
        );
        Problem9_TransactionAnalyzer analyzer = new Problem9_TransactionAnalyzer(txList);
        analyzer.findTwoSum(500);
        System.out.println();
        analyzer.findTwoSumWithTimeWindow(500, 60); 
        System.out.println();
        analyzer.findKSum(3, 1000); 
        System.out.println();
        analyzer.detectDuplicates();
    }
}