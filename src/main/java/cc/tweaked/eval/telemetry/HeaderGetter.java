package cc.tweaked.eval.telemetry;

import com.sun.net.httpserver.Headers;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class HeaderGetter implements TextMapGetter<Headers> {
    public static final HeaderGetter INSTANCE = new HeaderGetter();

    private HeaderGetter() {
    }

    @Nonnull
    @Override
    public Iterable<String> keys(@Nonnull Headers carrier) {
        return carrier.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Headers carrier, @Nonnull String key) {
        return carrier == null ? null : carrier.getFirst(key);
    }
}
