package cc.tweaked.eval;

import cc.tweaked.eval.computer.Metrics;
import cc.tweaked.eval.computer.RunRequest;
import cc.tweaked.eval.telemetry.TelemetryConfiguration;
import cc.tweaked.eval.telemetry.TracingHttpHandler;
import com.google.common.io.ByteStreams;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Handles the main entrypoint.
 * <p>
 * This reads the code to execute from the request body and returns an image in the response body.
 */
public class EvalRequestHandler implements TracingHttpHandler.Handler {
    private static final Logger LOG = LogManager.getLogger(EvalRequestHandler.class);

    private final Executor executor;
    private final List<RunRequest> requests = new ArrayList<>();
    private final BlockingQueue<RunRequest> pendingRequests = new LinkedBlockingDeque<>();
    private final Metrics metricsStore = new Metrics();

    private final LongCounter computers;

    public EvalRequestHandler(Executor executor) {
        this.executor = executor;

        Meter meter = GlobalMeterProvider.get().get(TelemetryConfiguration.NAME);
        meter.upDownCounterBuilder(TelemetryConfiguration.NAME + ".running_computers")
            .setDescription("Number of running computers")
            .setUnit("computers")
            .buildWithCallback(o -> o.observe(requests.size() + pendingRequests.size()));

        computers = meter.counterBuilder(TelemetryConfiguration.NAME + ".computers")
            .setDescription("Number of computers started")
            .setUnit("computers")
            .build();
    }

    @Override
    public void handle(TracingHttpHandler.Exchange exchange) throws IOException {
        Span span = Span.current();
        byte[] body = ByteStreams.toByteArray(exchange.getRequestBody());

        Span child = TelemetryConfiguration.tracer().spanBuilder("computer")
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();

        RunRequest request;
        try (Scope ignored = child.makeCurrent()) {
            LOG.info("Starting new computer");
            request = new RunRequest(body, metricsStore, (ok, image) -> executor.execute(() -> sendResponse(exchange, span, ok, image)));
        } catch (RuntimeException e) {
            LOG.error("Failed to create computer", e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            child.end();

            exchange.sendResponseHeaders(500, 0);
            exchange.close();
            return;
        }

        pendingRequests.offer(request);
        computers.add(1);
    }

    private void sendResponse(TracingHttpHandler.Exchange exchange, Span span, boolean ok, BufferedImage image) {
        try (Scope ignored = span.makeCurrent()) {
            exchange.getResponseHeaders().set("X-Clean-Exit", ok ? "True" : "False");
            if (image != null) {
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, 0);

                ImageIO.write(image, "png", exchange.getResponseBody());
                span.setStatus(StatusCode.OK);
            } else {
                exchange.sendResponseHeaders(204, 0);
                span.setStatus(StatusCode.ERROR, "No image provided");
            }
        } catch (IOException e) {
            LOG.error("Failed to send body", e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
        }

        exchange.close();
    }

    /**
     * Tick all computers in a loop.
     *
     * @throws InterruptedException If the thread is terminated
     */
    public void run() throws InterruptedException {
        while (true) {
            long started = System.nanoTime();

            RunRequest toQueue;
            while ((toQueue = pendingRequests.poll()) != null) {
                requests.add(toQueue);
            }

            Iterator<RunRequest> iterator = requests.iterator();
            while (iterator.hasNext()) {
                RunRequest request = iterator.next();
                if (!request.tick()) {
                    request.cleanup();
                    iterator.remove();
                }
            }

            if (requests.isEmpty()) {
                requests.add(pendingRequests.take());
            } else {
                long took = System.nanoTime() - started;
                long remaining = (50_000_000L - took) / 1_000_000;
                if (remaining > 0) Thread.sleep(remaining);
            }
        }
    }
}
