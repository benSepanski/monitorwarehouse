package roundrobin;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Manage n threads by allowing them to access the monitor
 * in round-robin order
 */
public class RoundRobinMonitor {
    private Logger LOGGER = Logger.getLogger(RoundRobinMonitor.class.getName());
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition my_turn = lock.newCondition();

    private int current_turn = 1, // threads labeled 1...n
        num_turns = 0;
    private final int num_threads_total;

    /*
    * Create a round robin monitor that manages exactly
    * *_num_threads_total* threads
    */
    public RoundRobinMonitor(final int _num_threads_total) {
        if(_num_threads_total <= 0) {
            throw new IllegalArgumentException("_num_threads_total must be positive");
        }
        num_threads_total = _num_threads_total;
        LOGGER.info("Monitor created managing " + num_threads_total + " threads.");
    }

    /**
     * Enter thread with given thread_id in 1,2...,num_threads_total
     * 
     * We trust that threads are honest
     */
    public void get_access(final int thread_id) {
        if(!(0 < thread_id && thread_id <= num_threads_total)) {
            String msg = "thread_id must be in range [1," + num_threads_total + "]";
            throw new IllegalArgumentException(msg);
        }
        lock.lock();
        try {
            // Wait until it's my turn
            if(current_turn != thread_id) {
                LOGGER.info("Thread " + thread_id + " waiting for its turn.");
            }
            while(current_turn != thread_id) {
                try {
                    my_turn.await();
                } catch(InterruptedException e) {}
            }
            // Record that we took our turn
            num_turns++;
            LOGGER.info("Thread " + thread_id + " takes its turn. This is turn " + num_turns);
            // Now it's somebody else's turn
            current_turn %= num_threads_total;
            current_turn++;
            // We have to signal everybody because everybody is waiting on the
            // same lock
            my_turn.signalAll();
        }
        finally {
            lock.unlock();
        }
    }
}