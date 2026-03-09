import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
public class Problem8_ParkingLot {
    enum SpotStatus { EMPTY, OCCUPIED, DELETED }
    static class ParkingRecord {
        String licensePlate;
        LocalDateTime entryTime;
        SpotStatus status;
        ParkingRecord() { this.status = SpotStatus.EMPTY; }
        ParkingRecord(String licensePlate) {
            this.licensePlate = licensePlate;
            this.entryTime = LocalDateTime.now();
            this.status = SpotStatus.OCCUPIED;
        }
    }
    private final int capacity;
    private final ParkingRecord[] spots;
    private int occupiedCount = 0;
    private long totalProbes = 0;
    private long totalParkings = 0;
    private final Map<Integer, Integer> hourlyActivity = new HashMap<>(); 
    private static final double RATE_PER_HOUR = 5.00; 
    public Problem8_ParkingLot(int capacity) {
        this.capacity = capacity;
        this.spots = new ParkingRecord[capacity];
        for (int i = 0; i < capacity; i++) spots[i] = new ParkingRecord();
    }
    private int hash(String licensePlate) {
        int hash = 0;
        for (char c : licensePlate.toCharArray()) {
            hash = (hash * 31 + c) % capacity;
        }
        return Math.abs(hash);
    }
    public int parkVehicle(String licensePlate) {
        if (occupiedCount >= capacity) {
            System.out.println("parkVehicle(\"" + licensePlate + "\") -> Parking lot is FULL");
            return -1;
        }
        int preferred = hash(licensePlate);
        int probes = 0;
        int spot = preferred;
        while (spots[spot].status == SpotStatus.OCCUPIED) {
            probes++;
            spot = (spot + 1) % capacity;
            if (spot == preferred) {
                System.out.println("No spots available (full)");
                return -1;
            }
        }
        spots[spot] = new ParkingRecord(licensePlate);
        occupiedCount++;
        totalProbes += probes;
        totalParkings++;
        int hour = LocalDateTime.now().getHour();
        hourlyActivity.merge(hour, 1, Integer::sum);
        System.out.printf("parkVehicle(\"%s\") -> Assigned spot #%d (%d probe%s)%n",
                licensePlate, spot, probes, probes == 1 ? "" : "s");
        return spot;
    }
    public double exitVehicle(String licensePlate) {
        int spot = findSpot(licensePlate);
        if (spot == -1) {
            System.out.printf("exitVehicle(\"%s\") -> Vehicle not found%n", licensePlate);
            return 0;
        }
        ParkingRecord record = spots[spot];
        Duration duration = Duration.between(record.entryTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long remainderMinutes = minutes % 60;
        double fee = Math.ceil(duration.toMinutes() / 60.0) * RATE_PER_HOUR;
        spots[spot] = new ParkingRecord();
        spots[spot].status = SpotStatus.DELETED;
        occupiedCount--;
        System.out.printf("exitVehicle(\"%s\") -> Spot #%d freed, Duration: %dh %dm, Fee: $%.2f%n",
                licensePlate, spot, hours, remainderMinutes, fee);
        return fee;
    }
    private int findSpot(String licensePlate) {
        int preferred = hash(licensePlate);
        int spot = preferred;
        do {
            if (spots[spot].status == SpotStatus.EMPTY) return -1; 
            if (spots[spot].status == SpotStatus.OCCUPIED
                    && licensePlate.equals(spots[spot].licensePlate)) {
                return spot;
            }
            spot = (spot + 1) % capacity;
        } while (spot != preferred);
        return -1;
    }
    public boolean isParked(String licensePlate) {
        return findSpot(licensePlate) != -1;
    }
    public int findNearestAvailableSpot() {
        for (int i = 0; i < capacity; i++) {
            if (spots[i].status != SpotStatus.OCCUPIED) return i;
        }
        return -1;
    }
    public void getStatistics() {
        double occupancy = (occupiedCount * 100.0) / capacity;
        double avgProbes = totalParkings == 0 ? 0 : (totalProbes * 1.0 / totalParkings);
        Optional<Map.Entry<Integer, Integer>> peakHour = hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        String peakStr = peakHour.map(e -> e.getKey() + ":00-" + (e.getKey() + 1) + ":00")
                .orElse("N/A");
        System.out.printf("getStatistics() -> Occupancy: %.0f%%, Avg Probes: %.1f, Peak Hour: %s%n",
                occupancy, avgProbes, peakStr);
        System.out.printf("  Total spots: %d | Occupied: %d | Available: %d%n",
                capacity, occupiedCount, capacity - occupiedCount);
        System.out.printf("  Load factor: %.2f (recommend rehash at 0.7)%n", (double) occupiedCount / capacity);
    }
    public static void main(String[] args) throws InterruptedException {
        Problem8_ParkingLot lot = new Problem8_ParkingLot(500);
        System.out.println("=== Problem 8: Parking Lot with Open Addressing ===\n");
        lot.parkVehicle("ABC-1234");
        lot.parkVehicle("ABC-1235"); 
        lot.parkVehicle("XYZ-9999"); 
        lot.parkVehicle("DEF-5678");
        lot.parkVehicle("GHI-9012");
        System.out.println();
        Thread.sleep(100); 
        lot.exitVehicle("ABC-1234");
        System.out.println("\nIs 'XYZ-9999' parked? " + lot.isParked("XYZ-9999"));
        System.out.println("Is 'ABC-1234' parked? " + lot.isParked("ABC-1234"));
        System.out.println("Nearest available spot: #" + lot.findNearestAvailableSpot());
        System.out.println();
        Random rand = new Random(42);
        for (int i = 0; i < 300; i++) {
            String plate = String.format("%c%c%c-%04d",
                    (char)('A' + rand.nextInt(26)),
                    (char)('A' + rand.nextInt(26)),
                    (char)('A' + rand.nextInt(26)),
                    rand.nextInt(10000));
            lot.parkVehicle(plate);
        }
        System.out.println();
        lot.getStatistics();
    }
}