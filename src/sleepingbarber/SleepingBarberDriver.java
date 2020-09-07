package sleepingbarber;

import java.lang.Thread;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Run a barber who cuts a bunch of customers hair
 */
public class SleepingBarberDriver{
    // <num cust threads> <custs/thread> <num chairs> <optional-debug level (default INFO)>
    public static void main(final String[] args) {
        // I hate the default logger setup from java:
        // https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        // Get arguments and set up logger
        assert(3 <= args.length && args.length <= 4);
        int num_cust_threads = Integer.parseInt(args[0]);
        int num_cust_per_thread = Integer.parseInt(args[1]);
        int num_chairs = Integer.parseInt(args[2]);
        Level log_level = Level.INFO;
        if(args.length == 4) {
            log_level = Level.parse(args[3].toUpperCase());
        }
        Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        LOGGER.setLevel(log_level);

        // build our barbershop monitor
        assert(num_chairs > 0 && num_cust_threads > 0);
        SleepingBarberMonitor monitor = new SleepingBarberMonitor(num_chairs);
        // Build and start our barber
        Barber brbr = new Barber(monitor);
        brbr.start();
        LOGGER.info(String.format("Starting barber on thread:%s", brbr.getName()));
        // Build and start our customers
        Customer cstmrs[] = new Customer[num_cust_threads];
        for(int i = 0; i < num_cust_threads; ++i) {
            cstmrs[i] = new Customer(monitor, num_cust_per_thread);
            cstmrs[i].start();
            String name = cstmrs[i].getName();
            LOGGER.info(String.format("Starting customer on thread:%s", name));
        }
        // Now join all our customers
        for(int i = 0; i < num_cust_threads; ++i) {
            String name = cstmrs[i].getName();
            LOGGER.info(String.format("Trying to join customer thread:%s", name));
            try {
                cstmrs[i].join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info(String.format("Customer thread:%s joined", name));
        }
        LOGGER.info("All customer threads joined");
        assert(brbr.isAlive());
        LOGGER.info("Barber is still at work... end-of-day so interrupting");
        brbr.interrupt();
        LOGGER.info("barber back home, day complete.");
    }
}

class Barber extends Thread {
    private final SleepingBarberMonitor monitor;
    private final static Logger
        LOGGER = Logger.getLogger(Barber.class.getName());

    public Barber(final SleepingBarberMonitor _monitor) {
        monitor = _monitor;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Try to cut hair until interrupted by spouse (time to come home)
     */
    public void run() {
        String name = this.getName();
        String base_msg = String.format("Barber thread:%s", name);
        boolean done = false;
        int num_cuts = 0;
        while(!done) {
            LOGGER.info(base_msg + " checking for customers");
            done = !monitor.cut_hair();
            if(!done) {
                ++num_cuts;
                LOGGER.info(base_msg + " cut someone's hair");
            }
        }
        LOGGER.info(base_msg + String.format("gave %d haircuts today", num_cuts));
        LOGGER.info(base_msg + "interrupted, spouse says its end-of-day");
    }
}

class Customer extends Thread {
    private final SleepingBarberMonitor monitor;
    private final int num_customers;
    private final static Logger
        LOGGER = Logger.getLogger(Customer.class.getName());

    public Customer(final SleepingBarberMonitor _monitor, int _num_customers) {
        monitor = _monitor;
        num_customers = _num_customers;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Have num_customers many customers try to get a haircut
     */
    public void run() {
        String name = this.getName();
        String msg;
        for(int i = 0; i < num_customers; ++i) {
            msg = String.format("Customer:%d on thread:%s", i, name);
            boolean got_haircut = monitor.get_haircut();        
            if(got_haircut) {
                msg += " received haircut";
            }
            else {
                msg += " left without haircut";
            }
            LOGGER.info(msg);
        }
    }
}