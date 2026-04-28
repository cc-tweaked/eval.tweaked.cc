package cc.tweaked.eval.telemetry;

import com.sun.net.httpserver.Headers;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.jspecify.annotations.Nullable;

final class HeaderGetter implements TextMapGetter<Headers> {
    public static final HeaderGetter INSTANCE = new HeaderGetter();

    private HeaderGetter() {
    }

    @Override
    public Iterable<String> keys(Headers carrier) {
        return carrier.keySet();
    }


    @Override
    public @Nullable String get(@Nullable Headers carrier, String key) {
        return carrier == null ? null : carrier.getFirst(key);
    }
}
