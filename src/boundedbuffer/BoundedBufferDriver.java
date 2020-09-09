package boundedbuffer;

import java.lang.Thread;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.logging.Level;


/**
 * Run a bunch of threads to see
 * if we deadlock
 */
public class BoundedBufferDriver {
    // <num consumers> with consumption sizes of <consumption step size>, 2*<consumption step size>,
    // ..., <num consumers> * <consumption step size>. Each producer produces
    // <consumptions per consumer> times. We require <buffer size> is at least as big as
    // <num consumers> * <consumption step size>.
    //
    // We build producers who produce 1 at a time,
    // each who produce <consumption step size> times
    // until enough productions have been made for all the
    // consumers to consume 
    //
    // <consumption step size> <num consumers> <consumptions per consumer> <buffer size> <optional-debug level (default INFO)>
    public static void main(final String[] args) {
        // I hate the default logger setup from java:
        // https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        // Get arguments and set up logger
        assert(args.length == 4 || args.length == 5);
        int consumption_step_size = Integer.parseInt(args[0]);
        int num_consumers = Integer.parseInt(args[1]);
        int consumptions_per_consumer = Integer.parseInt(args[2]);
        int buf_size = Integer.parseInt(args[3]);
        Level log_level = Level.INFO;
        if(args.length == 5) {
            log_level = Level.parse(args[4].toUpperCase());
        }
        Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        LOGGER.setLevel(log_level);

        assert(num_consumers >= 1);
        assert(consumption_step_size * num_consumers <= buf_size);

        // build our bounded buffer monitor 
        BoundedBufferMonitor monitor = new BoundedBufferMonitor(buf_size);
        // Build and start our consumers 
        Consumer cnsmrs[] = new Consumer[num_consumers];
        int consumption_size = 0;
        int productions_todo = 0;
        for(int i = 0; i < num_consumers; ++i) {
            consumption_size += consumption_step_size;
            cnsmrs[i] = new Consumer(monitor,
                                     consumptions_per_consumer,
                                     consumption_size);
            productions_todo += consumption_size;
            cnsmrs[i].start();
            LOGGER.info(String.format("Started consumer %d on thread:%s", i, cnsmrs[i].getName()));
        }

        // Now start producers until we will have produced enough
        Producer[] prdcrs = new Producer[productions_todo];
        for(int i = 0; i < productions_todo; ++i) {
            prdcrs[i] = new Producer(monitor, consumptions_per_consumer, 1);
            prdcrs[i].start();
            LOGGER.info(String.format("Started producer %d on thread:%s",
                                      prdcrs.length, prdcrs[i].getName()));
        }

        // Join everyone up
        for(Consumer cnsmr : cnsmrs) {
            try {
                cnsmr.join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info(String.format("Joined consumer on thread:%s", cnsmr.getName()));
        }
        LOGGER.info("All consumers joined");
        for(Producer prdcr : prdcrs) {
            try {
                prdcr.join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info(String.format("Joined producer on thread:%s", prdcr.getName()));
        }
        LOGGER.info("All producers joined");
    }
}

/** * Write *num_productions* times onto the bounded buffer
 * of size *production_size*
 */
class Producer extends Thread {
    private final BoundedBufferMonitor monitor;
    private final int num_productions, production_size;
    private final static Logger
        LOGGER = Logger.getLogger(Producer.class.getName());

    public Producer(final BoundedBufferMonitor _monitor,
                    final int _num_productions,
                    final int _production_size) {
        monitor = _monitor;
        num_productions = _num_productions;
        production_size = _production_size;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Just try to write num_productions _production_size times
     */
    public void run() {
        int [] to_put = new int[production_size];
        for(int i = 0; i < num_productions; ++i) {
            LOGGER.info(String.format("Producer thread:%s trying to write %d %d's to buffer",
                                      this.getName(), production_size, production_size));
            Arrays.fill(to_put, production_size);
            monitor.put(to_put);
            LOGGER.info(String.format("Producer thread:%s finished writing", this.getName()));
        }
    }
}


/** 
 * Read *num_consume* times onto the bounded buffer
 * of size *consume_size*
 */
class Consumer extends Thread {
    private final BoundedBufferMonitor monitor;
    private final int num_consumptions, consumption_size;
    private final static Logger
        LOGGER = Logger.getLogger(Consumer.class.getName());

    public Consumer(final BoundedBufferMonitor _monitor,
                    final int _num_consumptions,
                    final int _consumption_size) {
        monitor = _monitor;
        num_consumptions = _num_consumptions;
        consumption_size = _consumption_size;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Just try to consume num_consumptions consumption_size times
     */
    public void run() {
        int [] to_consume;
        for(int i = 0; i < num_consumptions; ++i) {
            LOGGER.info(String.format("Consumer thread:%s trying to read %d values from buffer",
                                      this.getName(), consumption_size));
            to_consume = monitor.take(consumption_size);
            LOGGER.info(String.format("Consumer thread:%s read %s",
                                      this.getName(), Arrays.toString(to_consume)));
        }
    }
}