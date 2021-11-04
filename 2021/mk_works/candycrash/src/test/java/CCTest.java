import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class CCTest {

    @Test
    void test1() {
        System.setIn(new ByteArrayInputStream("7 1 1 1 2 2 2 3 3".getBytes()));
        CandyCrush.main(new String[0]);
    }

    @Test
    void test2() {
        System.setIn(new ByteArrayInputStream(
                ("7 2 " +
                        "1 2 3 1 2 3 1 " +
                        "3 3 1 2 1 2 3 ").getBytes()));
        CandyCrush.main(new String[0]);
    }

    @Test
    void test3() {
        System.setIn(new ByteArrayInputStream(
                ("5 5 " +
                        "0 1 2 3 4 " +
                        "2 2 4 5 6 " +
                        "4 5 2 7 8 " +
                        "6 7 8 2 0 " +
                        "8 9 0 1 0 ").getBytes()));
        CandyCrush.main(new String[0]);
    }

    @Test
    void test1_kt() {
        System.setIn(new ByteArrayInputStream("7 1 1 1 2 2 2 3 3".getBytes()));
        CandyCrushKtKt.main();
    }

    @Test
    void test2_kt() {
        System.setIn(new ByteArrayInputStream(
                ("7 2 " +
                        "1 2 3 1 2 3 1 " +
                        "3 3 1 2 1 2 3 ").getBytes()));
        CandyCrushKtKt.main();
    }

    @Test
    void test3_kt() {
        System.setIn(new ByteArrayInputStream(
                ("5 5 " +
                        "0 1 2 3 4 " +
                        "2 2 4 5 6 " +
                        "4 5 2 7 8 " +
                        "6 7 8 2 0 " +
                        "8 9 0 1 0 ").getBytes()));
        CandyCrushKtKt.main();
    }

    @Test
    void convertNumberToString() {
        int i = 123;
        double f = 123.4;
        String s1 = String.valueOf(i);
        System.out.println(s1);
    }

    @Test
    void convertStringToNumber() {
        String s = "123";
        int i = Integer.parseInt(s);
        System.out.println(i);
    }

}
