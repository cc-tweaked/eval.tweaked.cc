package cc.tweaked.eval.computer;

import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.metrics.MetricsObserver;

import java.nio.file.Path;

/**
 * The environment in which a computer executes.
 */
final class Environment implements ComputerEnvironment {
    private final Path root;
    private final MetricsObserver metrics;

    Environment(Path root, MetricsObserver metrics) {
        this.root = root;
        this.metrics = metrics;
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
    public MetricsObserver getMetrics() {
        return metrics;
    }

    @Override
    public IWritableMount createRootMount() {
        return new FileMount(root.toFile(), 1024 * 1024);
    }
}
