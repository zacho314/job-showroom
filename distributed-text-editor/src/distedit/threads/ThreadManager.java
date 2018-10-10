package distedit.threads;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fuve on 31/05/2017.
 */
public class ThreadManager {
    private List<Thread> threads = new ArrayList<>();

    public void add(Thread t) {
        threads.add(t);
    }

    public void stop() {
        for (Thread t : threads) {
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
            System.out.println("Successfully stopped all threads!");
    }
}
