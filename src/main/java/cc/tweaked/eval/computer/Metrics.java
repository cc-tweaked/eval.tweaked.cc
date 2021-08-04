package cc.tweaked.eval.computer;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.tracking.Tracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;

import static dan200.computercraft.core.tracking.TrackingField.*;

public class Metrics implements Tracker {
    private static final Logger LOG = LogManager.getLogger(Metrics.class);

    private final Map<Computer, ComputerMetrics> activeMetrics = new WeakHashMap<>();

    public Metrics() {
        Tracking.add(this);
    }

    public synchronized ComputerMetrics add(Computer computer) {
        LOG.info("Adding {}", computer);
        ComputerMetrics metrics = new ComputerMetrics();
        activeMetrics.put(computer, metrics);
        return metrics;
    }

    public synchronized void remove(Computer computer) {
        LOG.info("Removing {}", computer);
        activeMetrics.remove(computer);
    }

    public synchronized void addTaskTiming(Computer computer, long time) {
        ComputerMetrics metrics = activeMetrics.get(computer);
        if (metrics == null) return;

        metrics.time += time;
    }

    @Override
    public synchronized void addValue(Computer computer, TrackingField field, long change) {
        ComputerMetrics metrics = activeMetrics.get(computer);
        if (metrics == null) return;

        if (field == HTTP_REQUESTS) metrics.httpRequests += change;
        if (field == HTTP_DOWNLOAD || field == WEBSOCKET_INCOMING) metrics.httpDownload += change;
        if (field == HTTP_UPLOAD || field == WEBSOCKET_OUTGOING) metrics.httpUpload += change;
    }

    public static class ComputerMetrics {
        volatile int time;
        volatile int httpRequests;
        volatile long httpUpload;
        volatile long httpDownload;

        public String toString() {
            return String.format("time=%d http_requests=%d http_upload=%d http_download=%d", time, httpRequests, httpUpload, httpDownload);
        }
    }
}
