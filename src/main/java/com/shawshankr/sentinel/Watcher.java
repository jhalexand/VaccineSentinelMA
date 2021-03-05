package com.shawshankr.sentinel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Watches for changes in vaccine availability and sends a few notices when
 * availability changes.
 */
public class Watcher extends TimerTask {
    public static final Logger logger = Logger.getLogger(Watcher.class.getName());

    private String appointmentsURL;

    private String notificationScript;

    private int maxNotices;
    private int noticeCount = 0;

    private URL proxyURL;

    private Set<String> previousAvailable = new HashSet<>();

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    public Watcher(String appointmentsURL, Integer maxNotices, String notificationScript, URL proxyURL) {
        this.appointmentsURL = appointmentsURL;
        this.maxNotices = maxNotices;
        this.notificationScript = notificationScript;
        this.proxyURL = proxyURL;
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        noticeCount = maxNotices;
    }

    /**
     * Gets the latest data, compares it to the previous data, determines
     * if a notification should be sent, and possibly sends ones.
     */
    @Override
    public void run() {
        //
        // Get the latest data
        HttpClient client;
        if (proxyURL == null) {
            client = HttpClient.newHttpClient();
        } else {
            client = HttpClient.newBuilder()
                    .proxy(ProxySelector.of(
                            new InetSocketAddress(
                                    proxyURL.getHost(), proxyURL.getPort())))
                    .build();
        }
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(appointmentsURL)).GET().build();
        try {
            //
            // Fetch the data and extract the results
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject respJ = new JSONObject(resp.body());
            JSONObject data = new JSONObject(respJ.getString("body"));

            JSONArray results = data.getJSONArray("results");
            Calendar now = Calendar.getInstance();
            // A lot of extra kludge here because of the loose typing of json data
            Set<String> available = StreamSupport.stream(results.spliterator(), false)
                    .map(result -> (JSONObject)result)
                    .filter(result -> result.getBoolean("hasAvailability"))
                    .filter(result -> isRecent(result.getString("timestamp"), now))
                    .map(result -> result.getString("name") + "[" + result.getString("city") + "]")
                    .collect(Collectors.toSet());

            boolean sendNotice = false;
            if (available.equals(previousAvailable)) {
                if (noticeCount++ < maxNotices) {
                    sendNotice = true;
                }
            } else {
                //
                // If now empty, send one notice
                if (available.isEmpty()) {
                    sendNotice = true;
                    noticeCount = maxNotices;
                } else {
                    // We have new data that includes appointments
                    noticeCount = 0;
                    sendNotice = true;
                }
            }
            if (sendNotice) {
                sendNotice(available.size(), String.join("," + System.lineSeparator(), available));
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Updated failed with error: " + e.getMessage(), e);
        }
    }

    /**
     * We'll use this to filter out data if it is too stale.  If we haven't had an update
     * in the past hour, we can probably assume those appointments are gone.
     *
     * @param timeToCheck the timestamp of the data we're looking at
     * @param currTime the time it is right now
     * @return whether the timestamp was recent or not
     */
    protected boolean isRecent(String timeToCheck, Calendar currTime) {
        try {
            Calendar check = Calendar.getInstance();
            check.setTime(dateFormat.parse(timeToCheck));
            if (currTime.getTimeInMillis() - check.getTimeInMillis() > 60 * 60 * 1000) {
                return false;
            }
        } catch (ParseException e) {
            logger.warning("Couldn't parse datetime string " + timeToCheck);
            return false;
        }
        return true;
    }


    /**
     * Invokes the notificationScript with NUM_SITES and LOCATIONS
     * available in the environment. You must provide the script
     * to send a notification of your choosing.
     *
     * @param numSites the number of sites with appointments available
     * @param locations the names and towns of all sites with appointments
     */
    protected void sendNotice(Integer numSites, String locations) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.environment().put("NUM_SITES", numSites.toString());
        pb.environment().put("LOCATIONS", locations);
        pb.command(notificationScript);
        try {
            Process p = pb.start();
            p.waitFor(30, TimeUnit.SECONDS);
            if (p.exitValue() != 0) {
                logger.warning("Notification exited with " + p.exitValue());
            }
        } catch (IOException e) {
            logger.severe("Notification failed! " + e.getMessage());
        } catch (InterruptedException e) {
            logger.severe("Timed out sending notification.");
        }
    }


}
