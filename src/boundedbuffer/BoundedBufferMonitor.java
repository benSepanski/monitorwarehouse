package boundedbuffer;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/*
* A monitor which holds a shared fixed-size buffer.
* producers must wait if the buffer is full and
* consumers must wait if the buffer is empty
*/
public class BoundedBufferMonitor {
    private Object[] shared_buffer;
    // front back allow us to "wrap around" the buffer
    // buf_size is capacity, count is current number of objects on the
    // buffer.
    private int front, back, buf_size, count;

    private ReentrantLock lock = new ReentrantLock();
    private Condition space_on_buffer = lock.newCondition(),
        items_on_buffer = lock.newCondition();

    /*
    * Create a buffer size
    */
    public BoundedBufferMonitor(int buf_size) {
        shared_buffer = new Object[buf_size];
        this.buf_size = buf_size;
        count = front = back = 0;
    }

    /*
    * Add something to the buffer
    */
    public void put(Object[] items) {
        lock.lock();
        try {
            // wait for space to drop in items 
            // (count is available space, so need count+0, count+1,...
            //  count+items.length-1, i.e. count+items.length -1<buf_size
            //  i.e. count+items.length < buf_size+1
            while(items.length + count > buf_size) {
                try{
                    space_on_buffer.await();
                }
                catch(InterruptedException e) {
                    throw new RuntimeException();
                }
            } 
            for(int i = 0; i < items.length; i++, back = (back+1) % buf_size) {
                shared_buffer[back] = items[i];
            }
            count += items.length;
            // now threads waiting for items might want to be notified
            items_on_buffer.notifyAll();
        }
        finally {
            lock.unlock();
        }
    }

    /*
     * Take num things off the buffer
     */
    public Object[] take(int num) {
        Object[] items = new Object[num];
        lock.lock();
        try {
            // Wait until there are at least *num* items 
            while(count < num) {
                try{
                    items_on_buffer.await();
                }
                catch(InterruptedException e) {
                    throw new RuntimeException();
                }
            } 
            // Grab the *num* items off of the array
            for(int i = 0; i < num; ++i, front = (front+1) % buf_size) {
                items[i] = items[front];
            }
            count -= num;
            // Now wake any threads waiting for space
            space_on_buffer.notifyAll();
        }
        finally {
            lock.unlock();
        }
        return items;
    }
}