import org.junit.jupiter.api.Test;

public class mk211127 {
    @Test
    void d1_genHist() {
        int[] e = new int[]{1, 3, 5, 2, 5, 5, 0, 0, 3};
        int[] r = new int[6];
        for (int i : e) r[i]++;
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < r[i]; ++j) {
                System.out.print("x");
            }
            System.out.println();
        }
    }

    @Test
    void d2_sumsPosit() {
        int[][] m = new int[][]{new int[]{1, 4, 3}, new int[]{2, -3, 4}};
        int[] r = new int[m.length];
        for (int i = 0; i < m.length; i++) {
            r[i] = 0;
            for (int j = 0; j < m[i].length; ++j) {
                if (m[i][j] > 0) r[i] += m[i][j];
            }
        }
        for (int i = 0; i < r.length; ++i) {
            System.out.printf("%s: %s\n", i, r[i]);
        }
    }

    @Test
    void d3_StringFormat() {
        String[] people = new String[]{"pa", "pb"};
        String[] verbs = new String[]{"v1", "v2"};
        String[] nouns = new String[]{"n1", "n2"};

        String r = String.format("%s %s %s.", people[0], verbs[0], nouns[0]);
        System.out.println(r);
    }

    @Test
    void d4_split1() {
        String[] r = "a==b=c".split("==");
        for (String e : r) {
            System.out.println(e);
        }
    }

    @Test
    void d5_split2() {
        String[] r = ("0z" + "X1X2X3").split("[^\\d]+");
        int s = 0;
        for (String e : r) {
            s += Integer.parseInt(e);
        }
        System.out.println(s);
    }
}
