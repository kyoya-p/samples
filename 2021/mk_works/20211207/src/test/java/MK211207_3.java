import org.junit.jupiter.api.Test;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class MK211207_3 {
    @Test
    void test() {
        Customer[] customers = new Customer[]{
                new Customer(123, "user1"),
                new Customer(456, "user2"),
        };
        int time1 = 1125;
        int time2 = 1203;
        double[] pos1 = new double[]{10, 78};
        double[] pos2 = new double[]{11, 79};
        System.out.println(
                possibleContact(time1, time2, pos1, pos2)
        );
    }

    private boolean possibleContact(int t1, int t2, double[] p1, double[] p2) {
        return diffTime(t1, t2) <= 1 && distance(p1, p2) <= 2.0;
    }

    private String getCustomerName(int phone, Customer[] customers) {
        for (Customer customer : customers) {
            if (customer.getPhone() == phone) return customer.getName();
        }
        return "";
    }

    private String formatLng(double lng) {
        if (lng >= 0) return "%sE".formatted(lng);
        else return "%sW".formatted(-lng);
    }

    private String formatLat(double lat) {
        if (lat >= 0) return "%sN".formatted(lat);
        else return "%sS".formatted(-lat);
    }

    private int diffTime(int time1, int time2) {
        return time1 / 100 - time2 / 100;
    }

    private double distance(double[] p1, double[] p2) {
        return sqrt(pow(p1[0] - p2[0], 2) + pow(p1[1] - p2[1], 2));
    }

    private String formatPhoneNumber(int num) {
        return "(902)%03d-%04d".formatted(num / 10000, num % 10000);
    }
}
