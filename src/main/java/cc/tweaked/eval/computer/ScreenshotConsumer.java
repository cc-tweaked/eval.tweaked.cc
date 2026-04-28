package cc.tweaked.eval.computer;

import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;

public interface ScreenshotConsumer {
    void consume(boolean cleanExit, @Nullable BufferedImage image);
}
