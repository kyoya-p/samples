import org.junit.jupiter.api.Test;

public class MK211207_1 {
    @Test
    void test() {
        Customer[] customers = new Customer[]{
                new Customer(123, "user1"),
                new Customer(456, "user2"),
        };
        System.out.printf("%s/%s %s%n",
                formatLat(1d),
                formatLng(1d),
                getCustomerName(123, customers)
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

    private static String formatLat(double lat) {
        if (lat >= 0) return "%sN".formatted(lat);
        else return "%sS".formatted(-lat);
    }

}
