package rwlock;

import java.lang.Thread;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Run a bunch of read-writes on several threads tosee
 * if we deadlock
 */
public class ReadersWritersDriver {
    // <num readers> <num writers> <num_ops> <optional-debug level (default INFO)>
    public static void main(final String[] args) {
        // I hate the default logger setup from java:
        // https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        // Get arguments and set up logger
        assert(args.length == 3 || args.length == 4);
        int num_readers = Integer.parseInt(args[0]);
        int num_writers = Integer.parseInt(args[1]);
        int num_ops = Integer.parseInt(args[2]);
        Level log_level = Level.INFO;
        if(args.length == 4) {
            log_level = Level.parse(args[3].toUpperCase());
        }
        Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        LOGGER.setLevel(log_level);

        // build our resource and monitor
        int resource[] = new int[100];
        ReadersWritersMonitor monitor = new ReadersWritersMonitor();
        // Build our readers and writers
        Reader rdrs[] = new Reader[num_readers];
        Writer wrtrs[] = new Writer[num_writers];
        // Start our readers and writers
        for(int i = 0; i < num_readers; ++i) {
            rdrs[i] = new Reader(monitor, resource, num_ops);
            rdrs[i].start();
            LOGGER.info(String.format("Started reader %d on thread %d", i, rdrs[i].getId()));
        }
        for(int i = 0; i < num_writers; ++i) {
            wrtrs[i] = new Writer(monitor, resource, num_ops);
            wrtrs[i].start();
            LOGGER.info(String.format("Started writer %d on thread %d", i, wrtrs[i].getId()));
        }

        // Join readers back together then writers back together
        for(int i = 0; i < num_readers; ++i) {
            LOGGER.info(String.format("Joining reader %d on thread %d", i, rdrs[i].getId()));
            try {
                rdrs[i].join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("All readers joined");
        for(int i = 0; i < num_writers; ++i) {
            LOGGER.info(String.format("Joining writer %d on thread %d", i, wrtrs[i].getId()));
            try {
                wrtrs[i].join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("All writers joined");
    }
}

class Reader extends Thread {
    private final ReadersWritersMonitor monitor;
    private final int[] resource;
    private final int num_reads;
    private final static Logger
        LOGGER = Logger.getLogger(Reader.class.getName());

    public Reader(final ReadersWritersMonitor _monitor,
                  final int[] _resource,
                  final int _num_reads) {
        monitor = _monitor;
        resource = _resource;
        num_reads = _num_reads;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Just add together all the entries in the resource to test that we gain
     * access, etc.
     */
    public void run() {
        int total_sum = 0;
        for(int i = 0; i < num_reads; ++i) {
            LOGGER.info(String.format("Reader thread %d entering monitor", this.getId()));
            monitor.enterReader();
            LOGGER.info(String.format("Reader thread %d inside of monitor", this.getId()));
            // Read from the resource
            int sum = 0;
            for(int j = 0; j < resource.length; ++j) {
                sum += resource[j];
            }
            LOGGER.info(String.format("Reader thread %d exiting monitor with total sum %d", this.getId(), total_sum));
            monitor.exitReader();
            LOGGER.info(String.format("Reader thread %d exited monitor", this.getId()));
            total_sum += sum;
        }
    }
}


class Writer extends Thread {
    private final ReadersWritersMonitor monitor;
    private final int[] resource;
    private final int num_writes;
    private final static Logger
        LOGGER = Logger.getLogger(Reader.class.getName());

    public Writer(final ReadersWritersMonitor _monitor,
                  final int[] _resource,
                  final int _num_writes) {
        monitor = _monitor;
        resource = _resource;
        num_writes = _num_writes;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Increment all the entries in the resource num_writes times
     */
    public void run() {
        int total_sum = 0;
        for(int i = 0; i < num_writes; ++i) {
            LOGGER.info(String.format("Writer thread %d entering monitor", this.getId()));
            monitor.enterWriter();
            LOGGER.info(String.format("Writer thread %d inside of monitor", this.getId()));
            // Increment resources
            for(int j = 0; j < resource.length; ++j) {
                resource[j]++;
            }
            LOGGER.info(String.format("Writer thread %d exiting monitor", this.getId(), total_sum));
            monitor.exitWriter();
            LOGGER.info(String.format("Writer thread %d exited monitor", this.getId()));
        }
    }
}
