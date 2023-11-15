package cc.tweaked.eval.computer;

import com.google.common.io.ByteStreams;
import com.google.common.io.MoreFiles;
import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.FileSystemWrapper;
import dan200.computercraft.core.terminal.Terminal;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A request to run code on a computer.
 * <p>
 * This allocates a new computer and injects a {@code cct_eval.screenshot} method into the global environment. We inject
 * the user provided code and a custom startup file into the environment, before starting the computer and letting it
 * run.
 * <p>
 * This lets computers run for 10 seconds, or until they call the screenshot function. At that point, we take a
 * screenshot, send it to the client, and then shutdown the computer.
 */
public class RunRequest implements ILuaAPI {
    private static final Logger LOG = LoggerFactory.getLogger(RunRequest.class);

    public static final int TICK = 50;
    public static final int MAX_TIME = 10_000 / TICK;

    private final Context context;

    private final byte[] startup;
    private final Computer computer;
    private final Path root;
    private final ComputerMetrics metrics;

    private boolean everOn = false;
    private int aliveFor = 0;

    private boolean sentScreenshot = false;
    private final ScreenshotConsumer consumer;

    public RunRequest(ComputerContext computerContext, byte[] startup, ScreenshotConsumer consumer) throws IOException {
        this.context = Context.current();
        this.startup = startup;
        this.consumer = consumer;
        this.root = Files.createTempDirectory("cct_eval-");
        this.metrics = new ComputerMetrics(context);
        this.computer = new Computer(
            computerContext,
            new Environment(root, metrics),
            new Terminal(51, 19, true),
            0
        );
        computer.addApi(this);
        computer.turnOn();
    }

    @Override
    public String[] getNames() {
        return new String[]{"cct_eval"};
    }

    public boolean tick() {
        try (Scope ignored = context.makeCurrent()) {
            return tickImpl();
        }
    }

    private boolean tickImpl() {
        LOG.info("Ticking computer (alive for {} ticks)", aliveFor);

        // If we've a screenshot available, abort immediately.
        if (sentScreenshot) return false;

        // If we've run for more than 10 seconds, take a screenshot and kill the computer.
        if (aliveFor++ > MAX_TIME) {
            sendScreenshot(false);
            return false;
        }

        // If this computer has shutdown prematurely, then just give up at this point.
        everOn |= computer.isOn();
        if (!computer.isOn() && everOn) return false;

        computer.tick();
        return true;
    }

    public void cleanup() {
        try (Scope ignored = context.makeCurrent()) {
            cleanupImpl();
        }
    }

    private synchronized void cleanupImpl() {
        Span span = Span.current();
        computer.unload();

        LOG.info("Computer finished.");
        metrics.report();

        try {
            MoreFiles.deleteRecursively(root);
        } catch (IOException e) {
            LOG.error("Failed to clean up filesystem", e);
            span.recordException(e);
        }

        if (!sentScreenshot) {
            sentScreenshot = true;
            consumer.consume(false, null);
        }

        span.end();
    }

    @Override
    public void startup() {
        try (Scope ignored = context.makeCurrent()) {
            startupImpl();
        }
    }

    private void startupImpl() {
        try (
            FileSystemWrapper<SeekableByteChannel> startupWriter = computer
                .getAPIEnvironment()
                .getFileSystem()
                .openForWrite("startup.lua", MountConstants.WRITE_OPTIONS);
            InputStream startupReader = getClass().getResourceAsStream("/startup.lua");
            ReadableByteChannel startupChannel = Channels.newChannel(startupReader);
        ) {
            ByteStreams.copy(startupChannel, startupWriter.get());
        } catch (FileSystemException | IOException e) {
            LOG.error("Cannot create startup file", e);
            Span.current().recordException(e);
            computer.unload();
            return;
        }

        try (
            FileSystemWrapper<SeekableByteChannel> code = computer.getAPIEnvironment().getFileSystem().openForWrite("code.lua", MountConstants.WRITE_OPTIONS);
        ) {
            code.get().write(ByteBuffer.wrap(startup));
        } catch (FileSystemException | IOException e) {
            LOG.error("Cannot create startup file", e);
            Span.current().recordException(e);
            computer.unload();
        }
    }

    @LuaFunction("shutdown")
    public final void doShutdown() throws LuaException {
        try (Scope ignored = context.makeCurrent()) {
            shutdownImpl();
        }
    }

    private void shutdownImpl() throws LuaException {
        if (sentScreenshot) throw new LuaException("Cannot take multiple screenshots");

        sendScreenshot(true);
        computer.unload();
    }

    private synchronized void sendScreenshot(boolean manual) {
        if (sentScreenshot) return;

        sentScreenshot = true;
        Terminal terminal = computer.getAPIEnvironment().getTerminal();
        consumer.consume(manual, Render.screenshot(terminal, terminal.getWidth(), terminal.getHeight()));
    }
}
