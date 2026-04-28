package cc.tweaked.eval.computer;

import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.filesystem.MemoryMount;
import dan200.computercraft.core.metrics.MetricsObserver;

/**
 * The environment in which a computer executes.
 */
final class Environment implements ComputerEnvironment {
    private final MetricsObserver metrics;

    Environment(MetricsObserver metrics) {
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
    public WritableMount createRootMount() {
        return new MemoryMount(1024 * 1024);
    }
}
