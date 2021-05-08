package cc.tweaked.eval.computer;

import dan200.computercraft.ComputerCraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.jar.JarFile;

public class CC {
    private static final Logger LOG = LogManager.getLogger(Render.class);

    private static final Path source;
    private static final String version;

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

    private static Path findCC() {
        CodeSource source = ComputerCraft.class.getProtectionDomain().getCodeSource();
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

    public static String getVersion() {
        return version;
    }

    public static Path getJar() {
        return source;
    }
}
