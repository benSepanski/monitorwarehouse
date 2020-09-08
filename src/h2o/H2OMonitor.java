package h2o;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implement h20: H, O particles cannot pass through barrier
 *  until they bond together to form an H2O.
 */
public class H2OMonitor {
    private ReentrantLock lock = new ReentrantLock();
    private Condition H_bonded = lock.newCondition(),
        available_H2 = lock.newCondition();
    private int 
        num_H = 0,
        num_H2 = 0,
        num_H_bonded = 0,
        num_H2O_bonded = 0;

    /**
     * Call to send hydrogen through barrier
     * 
     * Terminates once can bond with another hydrogen and another
     * oxygen.
     */
    public void enter_hydrogen() {
        lock.lock();
        try {
            // new H, join to make H2 if possible
            num_H++;
            if(num_H == 2) {
                num_H -= 2;
                num_H2++;
                available_H2.signal();
            }
            // Wait until bonded with HO
            while(num_H_bonded < 1) {
                try {
                    H_bonded.await();
                } catch(InterruptedException e) {}
            }
            num_H_bonded--;
            if(num_H_bonded > 0) {
                H_bonded.signal();
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
            // Wait for H2 to pair with
            while(num_H2 < 1) {
                try {
                    available_H2.await();
                } catch(InterruptedException e) {}
            }
            // Use the H2 and bond
            num_H2--;
            num_H_bonded += 2;
            num_H2O_bonded++;
            H_bonded.signal();
        }
        finally {
            lock.unlock();
        }
    }

    public int get_num_H2O() {
        return num_H2O_bonded;
    }
}