package monitorwarehouse;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/*
 * Annoted example from expresso paper
 * http://kferles.github.io/docs/publications/PLDI-18.pdf
 * 
 * Make sure locks are obtained if reading/writing
 * and that properly signal only as necessary to alert
 * threads waiting to read/write
 * 
 * This is a monitor: should be associated to some resource,
 * then any reader needs to call enterReader() when reading
 * the resource and exitReader() when leaving, similarly
 * for writers
 */
public class ReadersWriters {
    private int num_readers = 0;
    private boolean writer_in = false;

    private ReentrantLock lock = new ReentrantLock();
    private Condition readers_wanting_entry = lock.newCondition(),
        writers_wanting_entry = lock.newCondition();

    public void enterReader() throws InterruptedException {
        // First make sure no threads are currently asking for/relinquishing
        // access to the protected resource
        lock.lock();
        // Now wait until all writers are out
        while(writer_in) writers_wanting_entry.await();
        num_readers++;
        // now release the monitor to let it handle other threads
        // entering/exiting the resource
        lock.unlock();
        // and return control to the reader thread
    }

    public void exitReader() throws InterruptedException {
        // First make sure all other threads are in or out
        lock.lock();
        if(num_readers > 0) num_readers--;
        // A reader was in, so we know readers could freely enter
        // before this reader exited. This is also true after.
        // A writer might have been blocked if this was the
        // only reader, and only one writer can enter at a time,
        // so we signal to just one writer if there are no more readers
        if(num_readers == 0) writers_wanting_entry.signal();   
        // make sure to release the lock
        lock.unlock(); 
    }

    public void enterWriter() throws InterruptedException {
        // Block other threads from the monitor
        lock.lock();
        // wait until there are no readers & no writers
        while(num_readers > 0 || writer_in) writers_wanting_entry.await();
        writer_in = true;
        // release monitor resources
        lock.unlock();
    }

    public void exitWriter() throws InterruptedException {
        // Block other threads from the monitor
        lock.lock();
        // there is no longer a writer in
        writer_in = false;
        // Another writer can enter if there are no readers
        if(num_readers == 0) writers_wanting_entry.signal();
        // all readers can now enter
        readers_wanting_entry.signalAll();
        // release the monitor
        lock.unlock();
    }
}
