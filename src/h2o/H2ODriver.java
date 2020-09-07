
package h2o;

import java.lang.Thread;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Send a bunch of hydrogen and oxygen into a barrier
 */
public class H2ODriver {
    // Each hydrogen spawner spawns 1 hydrogen atom, there
    // will be exactly the required hydrogen
    //
    // <num oxygen spawners> <num spawn/spawner> <optional-debug level (default INFO)>
    public static void main(final String[] args) {
        // I hate the default logger setup from java:
        // https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        // Get arguments and set up logger
        assert(args.length == 2 || args.length == 3);
        int num_spawners = Integer.parseInt(args[0]);
        int num_spawn = Integer.parseInt(args[1]);
        Level log_level = Level.INFO;
        if(args.length == 3) {
            log_level = Level.parse(args[2].toUpperCase());
        }
        Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        LOGGER.setLevel(log_level);

        // build our resource and monitor
        H2OMonitor monitor = new H2OMonitor();
        // Build our oxygen and hydrogen spawners
        OxygenSpawner oxygen[] = new OxygenSpawner[num_spawners];
        HydrogenSpawner hydrogen[] = new HydrogenSpawner[2 * num_spawners * num_spawn];
        // Start our spawners
        for(int i = 0; i < num_spawners; ++i) {
            oxygen[i] = new OxygenSpawner(monitor, num_spawn);
            oxygen[i].start();
            LOGGER.info(String.format("Started oxygen spawner on thread %s", oxygen[i].getName()));
        }
        for(int i = 0; i < hydrogen.length; ++i) {
            hydrogen[i] = new HydrogenSpawner(monitor, 1);
            hydrogen[i].start();
            LOGGER.info(String.format("Started hydrogen spawner on thread %s", hydrogen[i].getName()));
        }
        // Join our spawners
        for(int i = 0; i < num_spawners; ++i) {
            try {
                oxygen[i].join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info(String.format("Joined oxygen spawner on thread %s", oxygen[i].getName()));
        }
        for(int i = 0; i < hydrogen.length; ++i) {
            try {
                hydrogen[i].join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info(String.format("Joined hydrogen spawner on thread %s", hydrogen[i].getName()));
        }
        LOGGER.info("All spawners joined");
    }
}

class OxygenSpawner extends Thread {
    private final H2OMonitor monitor;
    private final int num_spawn;
    private final static Logger
        LOGGER = Logger.getLogger(OxygenSpawner.class.getName());

    public OxygenSpawner(final H2OMonitor _monitor, final int _num_spawn) {
        monitor = _monitor;
        num_spawn = _num_spawn;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Spawn num_spawn oxys
     */
    public void run() {
        String base_msg = String.format("Oxygen thread:%s", this.getName());
        for(int i = 0; i < num_spawn; ++i) {
            LOGGER.info(base_msg + " entering barrier");
            monitor.enter_oxygen();
            LOGGER.info(base_msg + " exiting barrier as H20");
        }
    }
}


class HydrogenSpawner extends Thread {
    private final H2OMonitor monitor;
    private final int num_spawn;
    private final static Logger
        LOGGER = Logger.getLogger(HydrogenSpawner.class.getName());

    public HydrogenSpawner(final H2OMonitor _monitor, final int _num_spawn) {
        monitor = _monitor;
        num_spawn = _num_spawn;
        LOGGER.setParent(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    /**
     * Spawn num_spawn hydrogens
     */
    public void run() {
        String base_msg = String.format("Hydrogen thread:%s", this.getName());
        for(int i = 0; i < num_spawn; ++i) {
            LOGGER.info(base_msg + " entering barrier");
            monitor.enter_hydrogen();
            LOGGER.info(base_msg + " exiting barrier as H20");
        }
    }
}