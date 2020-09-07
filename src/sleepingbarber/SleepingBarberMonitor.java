package sleepingbarber;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements sleeping barber problem
 * 
 * A barber thread is calling cut_hair() over and over
 * again, customer threads are calling get_haircut()
 * 
 * There must be N > 0 chairs in the waiting room
 * 
 */
public class SleepingBarberMonitor {
    private int max_free_seats, num_free_seats;
    private boolean barber_cutting_hair;

    private ReentrantLock lock = new ReentrantLock();
    private Condition customer_ready = lock.newCondition(),
        barber_cuts_hair = lock.newCondition();

    public SleepingBarberMonitor(int nchairs) {
        max_free_seats = Math.max(1, nchairs);
        num_free_seats = max_free_seats;  // start with empty shop
        barber_cutting_hair = false;
    }

    /**
     * If there are no customers, the barber sleeps in the
     * cutting chair until they are awoken. They then
     * take a customer from the waiting room and put them
     * in the cutting chair and become busy (cutting their hair).
     * 
     * On an interrupt, throw a runtime error if interrupted with a nonempty waiting room,
     * otherwise gracefully return without altering state.
     * Returns true iff was not interrupted.
     */
    public boolean cut_hair() {
        lock.lock();
        try {
            // While waiting room is empty, wait for a customer to
            // wake me up (signal me)
            while(num_free_seats >= max_free_seats) {
                try{
                    customer_ready.await();
                }
                catch(InterruptedException e) { 
                    if(num_free_seats < max_free_seats) {
                        throw new RuntimeException("Interrupted with customers in waiting room");
                    }
                    return false;
                }
            }
            // Now I have a customer, take them from waiting room and
            // begin to give them a haircut
            num_free_seats++;
            barber_cutting_hair = true;
            barber_cuts_hair.signal();
        }
        finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * If a customer enters a waiting room with room to sit
     * and the barber is sleeping, they wake the barber up. 
     * 
     * If there is room in the waiting room, the customer
     * waits in a chair until the barber is cutting their hair. 
     * They then receive a haircut and release the barber to no
     * longer be busy
     */
    public boolean get_haircut() {
        lock.lock();
        boolean got_haircut = false;
        try {
            // Now if there is room in the waiting room, wait
            if(num_free_seats > 0) {
                num_free_seats--;
                // Let barber know to wake up!
                customer_ready.signal();
                try {
                    while(!barber_cutting_hair) {
                        barber_cuts_hair.await();
                    }
                }
                catch(InterruptedException e) { }
                // We got our haircut, let's go
                barber_cutting_hair = false;
                got_haircut = true;
            }
        }
        finally {
            lock.unlock();
        }
        return got_haircut;
    }
}