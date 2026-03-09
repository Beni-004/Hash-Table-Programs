import java.util.*;
public class usernameChecker {
    private final Map<String, Integer> registeredUsers = new HashMap<>();
    private final Map<String, Integer> attemptFrequency = new HashMap<>();
    public usernameChecker() {
        registeredUsers.put("john_doe", 1001);
        registeredUsers.put("admin", 1002);
        registeredUsers.put("jane", 1003);
    }
    public boolean checkAvailability(String username) {
        attemptFrequency.merge(username, 1, Integer::sum);
        return !registeredUsers.containsKey(username);
    }
    public boolean register(String username, int userId) {
        if (!checkAvailability(username)) {
            return false;
        }
        registeredUsers.put(username, userId);
        return true;
    }
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String candidate = username + i;
            if (!registeredUsers.containsKey(candidate)) {
                suggestions.add(candidate);
            }
        }
        String dotVersion = username.replace("_", ".");
        if (!registeredUsers.containsKey(dotVersion)) {
            suggestions.add(dotVersion);
        }
        String underscoreNum = username + "_2";
        if (!registeredUsers.containsKey(underscoreNum)) {
            suggestions.add(underscoreNum);
        }
        return suggestions;
    }
    public String getMostAttempted() {
        return attemptFrequency.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + " attempts)")
                .orElse("No attempts yet");
    }
    public static void main(String[] args) {
        usernameChecker checker = new usernameChecker();
        System.out.println("=== Problem 1: Username Availability Checker ===\n");
        System.out.println("checkAvailability(\"john_doe\") -> " + checker.checkAvailability("john_doe"));
        System.out.println("checkAvailability(\"jane_smith\") -> " + checker.checkAvailability("jane_smith"));
        System.out.println("suggestAlternatives(\"john_doe\") -> " + checker.suggestAlternatives("john_doe"));
        for (int i = 0; i < 10543; i++) checker.checkAvailability("admin");
        System.out.println("getMostAttempted() -> " + checker.getMostAttempted());
        System.out.println("\nRegister 'jane_smith' -> " + checker.register("jane_smith", 5001));
        System.out.println("checkAvailability(\"jane_smith\") after register -> " + checker.checkAvailability("jane_smith"));
    }
}