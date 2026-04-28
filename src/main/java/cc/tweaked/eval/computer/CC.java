package cc.tweaked.eval.computer;

import dan200.computercraft.core.ComputerContext;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.jar.JarFile;

public class CC {
    private static final Logger LOG = LoggerFactory.getLogger(CC.class);

    private static final @Nullable Path source;
    private static final @Nullable String version;

    static {
        source = findCC();

        String knownVersion = null;
        if (source != null) {
            LOG.debug("Loaded ComputerCraft from {}", source);

            try (JarFile jar = new JarFile(source.toFile())) {
                knownVersion = jar.getManifest().getMainAttributes().getValue("Implementation-Version");
            } catch (IOException e) {
                LOG.error("Cannot find ComputerCraft version", e);
            }
        }
        version = knownVersion;

        if (knownVersion == null) {
            LOG.error("Cannot find ComputerCraft version");
        } else {
            LOG.info("Running ComputerCraft {}.", knownVersion);
        }
    }

    private CC() {
    }

    private static @Nullable Path findCC() {
        CodeSource source = ComputerContext.class.getProtectionDomain().getCodeSource();
        if (source == null) return null;

        URI uri;
        try {
            uri = source.getLocation().toURI();
        } catch (URISyntaxException e) {
            LOG.error("Invalid URI for ComputerCraft jar", e);
            return null;
        }

        return Paths.get(uri);
    }

    public static @Nullable String getVersion() {
        return version;
    }

    public static @Nullable Path getJar() {
        return source;
    }
}
