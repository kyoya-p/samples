import org.junit.jupiter.api.Test;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class MK211207_2 {
    @Test
    void test() {
        Customer[] customers = new Customer[]{
                new Customer(123, "user1"),
                new Customer(456, "user2"),
        };
        System.out.printf("%s/%s %s : %s%n",
                formatLat(1d),
                formatLng(1d),
                getCustomerName(123, customers),
                formatPhoneNumber(1234567)
        );
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
