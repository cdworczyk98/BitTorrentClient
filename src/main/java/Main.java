import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {

    private static final byte test = "é".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte[] test2 = "é".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) {
        System.out.println(test);
        System.out.println(Arrays.toString(test2));
    }
}
