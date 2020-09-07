package h2o;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implement h20: H, O particles cannot pass through barrier
 *  until they bond together to form an H2O.
 */
public class H2OMonitor {
    private ReentrantLock lock = new ReentrantLock();
    private Condition new_H2O = lock.newCondition(),
        H2_pair = lock.newCondition(),
        second_H = lock.newCondition();
    
    private int num_free_first_H = 0,
        num_free_second_H = 0,
        num_H2_pairs = 0,
        num_H_in_H2O = 0;

    /**
     * Call to send hydrogen through barrier
     * 
     * Terminates once can bond with another hydrogen and another
     * oxygen.
     */
    public void enter_hydrogen() {
        lock.lock();
        try {
            // If there is no first H waiting for me, I am the first H
            boolean first;
            if(num_free_first_H == 0) {
                first = true;
                num_free_first_H++;
            } // If there is a first H waiting for me, I am the second H.
            // That first H is no longer free,
            else {
                first = false;
                num_free_second_H++;
                num_free_first_H--;
                second_H.signal();
            }
            // If I am the first H, wait for a second H
            while(num_free_second_H == 0) {
                try {
                    second_H.await();
                } catch(InterruptedException e) {}
            }
            // If I am the first H, mark that I found that second H
            // (it is no lnger free)
            // and record that there is a new H2 pair.
            if(first) {
                num_free_second_H--;
                num_H2_pairs++;
                H2_pair.signal();
            }
            // Now wait until H2 is bound with oxygen
            while(num_H_in_H2O == 0) {
                try {
                    new_H2O.await();
                } catch(InterruptedException e) {}
            }
            // That H2O passes out of the barrier
            num_H_in_H2O--;
            // If there is another H, signal
            if((num_H_in_H2O % 2) == 1) {
                new_H2O.signal();
            }
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Call to send oxygen through barrier
     * 
     * Terminates once can bond with two hydrogen, at which
     * point those three atoms bond into a water molecule and
     * pass through the barrier.
     */
    public void enter_oxygen() {
        lock.lock();
        try {
            // Wait for H^2 to pair with
            while(num_H2_pairs == 0) {
                try {
                    H2_pair.await();
                } catch(InterruptedException e) {}
            }
            // pair with the H2
            num_H2_pairs--;
            num_H_in_H2O += 2;
            new_H2O.signal();
        }
        finally {
            lock.unlock();
        }
    }
}