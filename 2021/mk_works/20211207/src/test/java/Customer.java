public class Customer {
    private final int phone;
    private final String name;

    public Customer(int phone, String name) {
        this.phone = phone;
        this.name = name;
    }

    public int getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }
}
