package cc.tweaked.eval.computer;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.filesystem.JarMount;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public final class GlobalContext implements GlobalEnvironment, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalContext.class);

    private final ComputerContext context;

    public GlobalContext(int threads) {
        this.context = ComputerContext.builder(this).computerThreads(threads).build();
    }

    public ComputerContext context() {
        return context;
    }

    @Override
    public String getHostString() {
        return String.format("ComputerCraft %s (eval.tweaked.cc)", CC.getVersion());
    }

    @Override
    public String getUserAgent() {
        return "computercraft/" + CC.getVersion();
    }

    @Override
    public @Nullable Mount createResourceMount(String domain, String subPath) {
        try {
            return new JarMount(CC.getJar().toFile(), "data/" + domain + "/" + subPath);
        } catch (IOException e) {
            LOGGER.error("Could not create ROM mount", e);
            return null;
        }
    }

    @Override
    public InputStream createResourceFile(String domain, String subPath) {
        return getClass().getClassLoader().getResourceAsStream("data/" + domain + "/" + subPath);
    }

    @Override
    public void close() throws InterruptedException {
        context.ensureClosed(5, TimeUnit.SECONDS);
    }
}
