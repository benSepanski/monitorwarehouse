package sleepingbarber;

import java.lang.Thread;
import java.util.logging.Logger;

import java.util.logging.Level;

/**
 * Run a barber who cuts a bunch of customers hair
 */
public class SleepingBarberDriver{
    // <num cust threads> <num chairs> <optional-debug level (default INFO)>
    public static void main(final String[] args) {
        // I hate the default logger setup from java:
        // https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        // Get arguments and set up logger
        assert(2 <= args.length && args.length <= 3);
        int num_custs = Integer.parseInt(args[0]);
        int num_chairs = Integer.parseInt(args[1]);
        Level log_level = Level.INFO;
        if(args.length == 3) {
            log_level = Level.parse(args[2].toUpperCase());
        }
        Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        LOGGER.setLevel(log_level);

        // build our barbershop monitor
        assert(num_chairs > 0 && num_custs > 0);
        SleepingBarberMonitor monitor = new SleepingBarberMonitor(num_chairs);
        // Build and start our barber
        Barber brbr = new Barber(monitor, num_custs);
        brbr.start();
        LOGGER.info(String.format("barber started on thread:%s", brbr.getName()));
        // Build and start our customers
        LOGGER.info("Building and starting our customers");
        Customer cstmrs[] = new Customer[num_custs];
        for(int i = 0; i < num_custs; ++i) {
            cstmrs[i] = new Customer(monitor);
            cstmrs[i].start();
        }
        LOGGER.info("All customers started");

        // Now try to join our barber
        LOGGER.info("Trying to join barber thread");
        try {
            brbr.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info("Barber joined");
        // Now join all our customers
        LOGGER.info("Trying to join customer threads");
        for(int i = 0; i < num_custs; ++i) {
            try {
                cstmrs[i].join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("All customer threads joined");
    }
}

class Barber extends Thread {
    private final SleepingBarberMonitor monitor;
    private final static Logger
        LOGGER = Logger.getLogger(Barber.class.getName());
    private final int num_cuts;

    public Barber(final SleepingBarberMonitor _monitor, int _num_cuts) {
        monitor = _monitor;
        num_cuts = _num_cuts;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Try to cut hair num_cuts times
     */
    public void run() {
        String name = this.getName();
        String base_msg = String.format("Barber thread:%s", name);
        for(int i = 0; i < num_cuts; ++i) {
            LOGGER.info(base_msg + " checking for customers");
            monitor.cut_hair();
            LOGGER.info(base_msg + " gave haircut " + (i+1) + " of day");
        }
    }
}

class Customer extends Thread {
    private final SleepingBarberMonitor monitor;
    private final static Logger
        LOGGER = Logger.getLogger(Customer.class.getName());

    public Customer(final SleepingBarberMonitor _monitor) {
        monitor = _monitor;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Try to get a haircut until successful
     */
    public void run() {
        String name = this.getName();
        String msg = String.format("Customer on thread:%s", name);
        LOGGER.info(msg + " starting haircut attempts");
        while(!monitor.get_haircut()) { }
        LOGGER.info(msg + " received haircut");
    }
}