package distedit;

import java.util.Random;

public class IDs {

    private static Random r = new Random();

    public static int getNextId() {
        return r.nextInt(100000);
    }
}
