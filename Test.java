public class Test {
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SUCCESS: Driver found!");
        } catch (ClassNotFoundException e) {
            System.out.println("FAILURE: Driver NOT found.");
            e.printStackTrace();
        }
    }
}
