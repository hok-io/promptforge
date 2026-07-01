package promptforge;

public final class SmokeTest {
    public static void main(String[] args) {
        if (Runtime.version().feature() < 15) {
            throw new AssertionError("Java 15 or newer is required");
        }
        System.out.println("PASS SmokeTest");
    }
}
