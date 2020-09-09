package roundrobin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Try a bunch of round robin accesses
 */
public class RoundRobinDriver {
    // <num threads> <num acceses>  <optional - log level>
    public static void main(String[] args) {
        // Get args and validate
        if(!(2 <= args.length && args.length <= 3)) {
            throw new IllegalArgumentException("Invalid args.length");
        }
        int num_threads = Integer.parseInt(args[0]),
            num_accesses = Integer.parseInt(args[1]);
       if(num_accesses <= 0 || num_threads <= 0) {
           throw new IllegalArgumentException("num threads and num accesses must be positive");
       } 
        
        // Set up logger
        Level log_level = Level.INFO;
        if(args.length == 3) {
            log_level = Level.parse(args[2]);
        }
        Logger LOGGER = Logger.getLogger("roundrobin");
        LOGGER.setLevel(log_level);
        // I hate the default logger setup from java:
        // https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        // Build monitor
        RoundRobinMonitor monitor = new RoundRobinMonitor(num_threads);
        // Build procs and start
        LOGGER.info("Building " + num_threads + " RRThread.instances");
        RRThread procs[] = new RRThread[num_threads];
        for(int i = 0; i < num_threads; ++i) {
            procs[i] = new RRThread(monitor, i+1, num_accesses);
        }
        LOGGER.info("RRThreads built, starting RRThreads");
        for(RRThread proc : procs) {
            proc.start();
        }
        LOGGER.info("All RRThreads started, waiting for them to join");
        for(RRThread proc : procs) {
            try {
                proc.join();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("All RRThreads joined");
    }
}


class RRThread extends Thread {
    private final RoundRobinMonitor monitor;
    private final int id, num_accesses;
    
    public RRThread(RoundRobinMonitor _monitor, int _id, int _num_accesses) {
        monitor = _monitor;
        id = _id;
        num_accesses = _num_accesses;
    }

    public void run() {
        for(int i = 0; i < num_accesses; ++i) {
            monitor.get_access(id);
        }
    }
}