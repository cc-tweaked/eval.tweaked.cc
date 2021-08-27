package cc.tweaked.eval.telemetry;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TracingHttpHandler implements HttpHandler {
    private static final Logger LOG = LogManager.getLogger(TracingHttpHandler.class);

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
            .setAttribute(SemanticAttributes.HTTP_METHOD, "GET")
            .setAttribute(SemanticAttributes.HTTP_SCHEME, "http")
            .setAttribute(SemanticAttributes.HTTP_HOST, exchange.getLocalAddress().toString())
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

    private static class WrappedExchange implements Exchange {
        private final HttpExchange exchange;
        private final Span span;

        private WrappedExchange(HttpExchange exchange, Span span) {
            this.exchange = exchange;
            this.span = span;
        }

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

