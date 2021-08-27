package cc.tweaked.eval.computer;

import cc.tweaked.eval.telemetry.TelemetryConfiguration;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.tracking.Tracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingField;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import static dan200.computercraft.core.tracking.TrackingField.*;

public class Metrics implements Tracker {
    private static final String NAMESPACE = TelemetryConfiguration.NAME + ".computers";
    private static final String PREFIX = "computers.";
    private static final Logger LOG = LogManager.getLogger(Metrics.class);

    private final Map<Computer, ComputerMetrics> activeMetrics = new WeakHashMap<>();

    private final LongCounter tasks;
    private final Map<TrackingField, LongCounter> counters = new HashMap<>();

    public Metrics() {
        Tracking.add(this);

        Meter meter = GlobalMeterProvider.get().get(NAMESPACE);
        tasks = meter.counterBuilder(PREFIX + "task_time").setDescription("Total task time of all computers").setUnit("ns").build();
        addCounter(meter, HTTP_REQUESTS, c -> c.setDescription("Number of HTTP requests").setUnit("requests"));
        addCounter(meter, HTTP_DOWNLOAD, c -> c.setDescription("Number of bytes downloaded in HTTP requests").setUnit("bytes"));
        addCounter(meter, HTTP_UPLOAD, c -> c.setDescription("Number of bytes uploaded in HTTP requests").setUnit("bytes"));
        addCounter(meter, WEBSOCKET_INCOMING, c -> c.setDescription("Number of bytes received from websockets").setUnit("bytes"));
        addCounter(meter, WEBSOCKET_OUTGOING, c -> c.setDescription("Number of sent from websockets").setUnit("bytes"));
    }

    private void addCounter(Meter meter, TrackingField field, Consumer<LongCounterBuilder> describe) {
        LongCounterBuilder builder = meter.counterBuilder(PREFIX + field.id());
        describe.accept(builder);
        counters.put(field, builder.build());
    }

    public synchronized ComputerMetrics add(Computer computer) {
        LOG.info("Adding {}", computer);
        ComputerMetrics metrics = new ComputerMetrics(Context.current());
        activeMetrics.put(computer, metrics);
        return metrics;
    }

    public synchronized void remove(Computer computer) {
        LOG.info("Removing {}", computer);
        activeMetrics.remove(computer);
    }

    public synchronized void addTaskTiming(Computer computer, long time) {
        ComputerMetrics metrics = activeMetrics.get(computer);
        tasks.add(time, Attributes.empty(), metrics == null ? Context.current() : metrics.context);
        if (metrics == null) return;

        metrics.time += time;
    }

    @Override
    public synchronized void addValue(Computer computer, TrackingField field, long change) {
        LongCounter counter = counters.get(field);
        ComputerMetrics metrics = activeMetrics.get(computer);

        if (counter != null) {
            counter.add(change, Attributes.empty(), metrics == null ? Context.current() : metrics.context);
        }
        if (metrics == null) return;

        if (field == HTTP_REQUESTS) metrics.httpRequests += change;
        if (field == HTTP_DOWNLOAD || field == WEBSOCKET_INCOMING) metrics.httpDownload += change;
        if (field == HTTP_UPLOAD || field == WEBSOCKET_OUTGOING) metrics.httpUpload += change;
    }

    public static class ComputerMetrics {
        private static final AttributeKey<Long> TIME = AttributeKey.longKey("computer.time");
        private static final AttributeKey<Long> REQUESTS = AttributeKey.longKey("computer.http_requests");
        private static final AttributeKey<Long> UPLOAD = AttributeKey.longKey("computer.http_upload");
        private static final AttributeKey<Long> DOWNLOAD = AttributeKey.longKey("computer.http_download");

        private final Context context;
        volatile int time;
        volatile int httpRequests;
        volatile long httpUpload;
        volatile long httpDownload;

        public ComputerMetrics(Context context) {
            this.context = context;
        }

        public void report() {
            Span.fromContext(context)
                .setAttribute(TIME, time)
                .setAttribute(REQUESTS, httpRequests)
                .setAttribute(UPLOAD, httpUpload)
                .setAttribute(DOWNLOAD, httpDownload);
        }
    }
}
