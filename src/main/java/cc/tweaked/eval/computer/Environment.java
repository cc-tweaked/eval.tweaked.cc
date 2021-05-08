package cc.tweaked.eval.computer;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.JarMount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * The environment in which a computer executes.
 */
final class Environment implements IComputerEnvironment {
    private static final Logger LOG = LogManager.getLogger(Render.class);

    private final Path root;

    Environment(Path root) {
        this.root = root;
    }

    @Override
    public int getDay() {
        return 0;
    }

    @Override
    public double getTimeOfDay() {
        return 0;
    }

    @Override
    public boolean isColour() {
        return true;
    }

    @Override
    public long getComputerSpaceLimit() {
        return 1024 * 1024;
    }

    @Nonnull
    @Override
    public String getHostString() {
        return String.format("ComputerCraft %s (eval.tweaked.cc)", CC.getVersion());
    }

    @Nonnull
    @Override
    public String getUserAgent() {
        return "computercraft/" + CC.getVersion();
    }

    @Override
    public int assignNewID() {
        return 0;
    }

    @Override
    public IWritableMount createSaveDirMount(String s, long l) {
        return new FileMount(root.toFile(), getComputerSpaceLimit());
    }

    @Override
    public IMount createResourceMount(String domain, String subPath) {
        try {
            return new JarMount(CC.getJar().toFile(), "data/" + domain + "/" + subPath);
        } catch (IOException e) {
            LOG.error("Could not create ROM mount", e);
            return null;
        }
    }

    @Nullable
    @Override
    public InputStream createResourceFile(String domain, String subPath) {
        return getClass().getClassLoader().getResourceAsStream("data/" + domain + "/" + subPath);
    }
}
