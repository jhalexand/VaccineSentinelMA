package com.shawshankr.sentinel;

import com.oracle.labs.mlrg.olcut.config.Config;
import com.oracle.labs.mlrg.olcut.config.Configurable;
import com.oracle.labs.mlrg.olcut.config.ConfigurationManager;
import com.oracle.labs.mlrg.olcut.config.PropertyException;

import java.io.File;
import java.net.URL;
import java.util.Timer;
import java.util.logging.Logger;

/**
 * A program to watch for vaccine availability. When availability changes,
 * it will invoke the notificationScript as many as maxNotices times
 * (once every 5 minutes) until the next change occurs. The notificationScript
 * must be readable and executable. The number of sites available and the full
 * list of available sites are sent in the environment variables NUM_SITES
 * and LOCATIONS respectively.
 */
public class Sentinel implements Configurable {
    public static final Logger logger = Logger.getLogger(Sentinel.class.getName());

    @Config(description="URL of the appointment data")
    private String appointmentsURL = null;

    @Config(description = "Path to script to run to send notice")
    private String notificationScript = null;

    @Config(description="How many times to send notice unless there's a change")
    private int maxNotices = 3;

    @Config(description="URL of a proxy if you need one")
    private URL proxyURL = null;

    /**
     * This runs after the Sentinal is configured to check for a notification script.
     */
    @Override
    public void postConfig() {
        //
        // Verify that our notificiation script is good
        File script = new File(notificationScript);
        if (!script.exists()) {
            throw new PropertyException("notificationScript", "Couldn't find notification script: " + notificationScript);
        }
        if (!script.canRead() || !script.canExecute()) {
            throw new PropertyException("notificationScript", "Notification script (" + notificationScript + ") must be readable and executable");
        }
    }

    /**
     * Gets a watcher, as configured by the Sentinel.
     *
     * @return a watcher ready to run
     */
    public Watcher getWatcher() {
        return new Watcher(appointmentsURL, maxNotices, notificationScript, proxyURL);
    }

    /**
     * Start it all up
     * @param args the configuration manager which watch for a -c with a config file
     */
    public static void main(String[] args) {
        ConfigurationManager cm = new ConfigurationManager(args, "/config.xml");
        Sentinel sentinel = cm.lookup(Sentinel.class, null);

        logger.info("Starting watcher");
        Timer t = new Timer();
        t.scheduleAtFixedRate(sentinel.getWatcher(), 0, 1000 * 60 * 5);
    }
}
