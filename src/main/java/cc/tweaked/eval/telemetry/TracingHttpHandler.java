package cc.tweaked.eval.telemetry;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TracingHttpHandler implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TracingHttpHandler.class);

    private final Handler handler;

    public TracingHttpHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Span span = TelemetryConfiguration.tracer()
            .spanBuilder(exchange.getHttpContext().getPath())
            .setParent(
                GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
                    .extract(Context.current(), exchange.getRequestHeaders(), HeaderGetter.INSTANCE)
            )
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(SemanticAttributes.NET_HOST_NAME, exchange.getLocalAddress().getHostName())
            .setAttribute(SemanticAttributes.NET_HOST_PORT, (long) exchange.getLocalAddress().getPort())
            .setAttribute(SemanticAttributes.HTTP_METHOD, exchange.getRequestMethod())
            .setAttribute(SemanticAttributes.HTTP_SCHEME, "http")
            .setAttribute(SemanticAttributes.HTTP_TARGET, "/")
            .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            handler.handle(new WrappedExchange(exchange, span));
        } catch (Throwable e) {
            LOG.error("Error processing request", e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.toString());
            span.end();

            exchange.close();
            throw e;
        }
    }

    public interface Handler {
        void handle(Exchange exchange) throws IOException;
    }

    public interface Exchange {
        InputStream getRequestBody();

        OutputStream getResponseBody();

        void close();

        void sendResponseHeaders(int status, int length) throws IOException;

        Headers getRequestHeaders();

        Headers getResponseHeaders();
    }

    private record WrappedExchange(HttpExchange exchange, Span span) implements Exchange {
        @Override
        public InputStream getRequestBody() {
            return exchange.getRequestBody();
        }

        @Override
        public OutputStream getResponseBody() {
            return exchange.getResponseBody();
        }

        @Override
        public void close() {
            try {
                exchange.close();
            } finally {
                span.end();
            }
        }

        @Override
        public void sendResponseHeaders(int status, int length) throws IOException {
            span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, status);
            exchange.sendResponseHeaders(status, length);
        }

        @Override
        public Headers getRequestHeaders() {
            return exchange.getRequestHeaders();
        }

        @Override
        public Headers getResponseHeaders() {
            return exchange.getResponseHeaders();
        }
    }
}

