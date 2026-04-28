package cc.tweaked.eval.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A minimal implementation of {@link SLF4JServiceProvider}.
 */
public final class LoggingProvider implements SLF4JServiceProvider {
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void initialize() {
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.17";
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return name -> new LoggerImpl(name, lock);
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return new BasicMarkerFactory();
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return new NOPMDCAdapter();
    }

    private static final class LoggerImpl extends AbstractLogger {
        private final String loggerName;
        private final ReentrantLock lock;

        private LoggerImpl(String loggerName, ReentrantLock lock) {
            this.loggerName = loggerName;
            this.lock = lock;
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return loggerName;
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
            var thread = Thread.currentThread().getName();
            var message = MessageFormatter.arrayFormat(messagePattern, arguments, throwable);

            var span = Span.fromContext(Context.current()).getSpanContext();
            String traceId, spanId;
            if (span.isValid()) {
                traceId = span.getTraceId();
                spanId = span.getSpanId();
            } else {
                traceId = spanId = "-";
            }

            var formattedMessage = String.format(
                "thread=%s level=%s logger=%s trace_id=%s span_id=%s - %s\n", thread, level, loggerName, traceId, spanId, message.getMessage()
            );

            // Unfortunate double-locking here, but ensure that the two
            lock.lock();
            try {
                System.err.print(formattedMessage);
                if (message.getThrowable() != null) message.getThrowable().printStackTrace(System.err);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean isTraceEnabled() {
            return true;
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return true;
        }
    }
}
