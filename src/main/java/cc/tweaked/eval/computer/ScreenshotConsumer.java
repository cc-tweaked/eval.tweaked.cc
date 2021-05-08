package cc.tweaked.eval.computer;

import java.awt.image.BufferedImage;

public interface ScreenshotConsumer {
    void consume(boolean cleanExit, BufferedImage image);
}
