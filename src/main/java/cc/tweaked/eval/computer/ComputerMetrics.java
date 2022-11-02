package cc.tweaked.eval.computer;

import cc.tweaked.eval.telemetry.TelemetryConfiguration;
import com.google.common.collect.ImmutableMap;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.metrics.MetricsObserver;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.util.Map;

/**
 * {@link MetricsObserver} implementation which writes to OpenTelemetry.
 * <p>
 * We also maintain a per-computer store of some metrics which are attached to the computer's trace.
 */
class ComputerMetrics implements MetricsObserver {
    private static final String NAMESPACE = TelemetryConfiguration.NAME + ".computers";
    private static final String PREFIX = "computers.";

    private static final AttributeKey<Long> TIME = AttributeKey.longKey("computer.time");
    private static final AttributeKey<Long> REQUESTS = AttributeKey.longKey("computer.http_requests");
    private static final AttributeKey<Long> UPLOAD = AttributeKey.longKey("computer.http_upload");
    private static final AttributeKey<Long> DOWNLOAD = AttributeKey.longKey("computer.http_download");

    private static final Map<Metric, LongCounter> counters;

    static {
        Metrics.init();
        Meter meter = GlobalMeterProvider.get().get(NAMESPACE);

        ImmutableMap.Builder<Metric, LongCounter> counterBuilder = new ImmutableMap.Builder<>();

        for (Metric metric : Metric.metrics().values()) {
            LongCounterBuilder builder = meter.counterBuilder(PREFIX + metric.name())
                .setDescription(metric.name()) // TODO: Can we localise this?
                .setUnit(metric.unit());
            counterBuilder.put(metric, builder.build());
        }

        counters = counterBuilder.build();
    }

    private final Context context;

    private volatile int time;
    private volatile int httpRequests;
    private volatile long httpUpload;
    private volatile long httpDownload;

    public ComputerMetrics(Context context) {
        this.context = context;
    }

    @Override
    public synchronized void observe(Metric.Counter counter) {
        counters.get(counter).add(1, Attributes.empty(), context);

        if (counter == Metrics.HTTP_REQUESTS) httpRequests += 1;
    }

    @Override
    public synchronized void observe(Metric.Event event, long value) {
        counters.get(event).add(value, Attributes.empty(), context);

        if (event == Metrics.COMPUTER_TASKS) time += value;
        if (event == Metrics.HTTP_DOWNLOAD || event == Metrics.WEBSOCKET_INCOMING) httpDownload += value;
        if (event == Metrics.HTTP_UPLOAD || event == Metrics.WEBSOCKET_OUTGOING) httpUpload += value;
    }

    public void report() {
        Span.fromContext(context)
            .setAttribute(TIME, time)
            .setAttribute(REQUESTS, httpRequests)
            .setAttribute(UPLOAD, httpUpload)
            .setAttribute(DOWNLOAD, httpDownload);
    }
}
