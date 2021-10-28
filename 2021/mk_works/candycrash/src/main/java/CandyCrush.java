import java.util.Scanner;

public class CandyCrush {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt();
        int height = in.nextInt();
        int[][] field = new int[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                field[y][x] = in.nextInt();
            }
        }
        int max = 1;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int candyColor = field[y][x];
                int i = 0;

                // To the right
                for (i = 1; x + i < width && field[y][x + i] == candyColor; ++i) ;
                if (i > max) max = i;

                // Downward
                for (i = 1; y + i < height && field[y + i][x] == candyColor; ++i) ;
                if (i > max) max = i;

                // To lower left
                // ...

                // To lower right
                // ...

                System.out.printf("(%s,%s)%s\n", x, y, max);
            }
        }
    }
}
