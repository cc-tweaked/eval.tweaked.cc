package cc.tweaked.eval.computer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Renders a terminal to an image. This is largely copied from CCEmuX's renderer.
 */
public class Render {
    private static final Logger LOG = LogManager.getLogger(Render.class);

    private static final int MARGIN = 2;
    private static final int CELL_WIDTH = 6;
    private static final int CELL_HEIGHT = 9;
    private static final int FONT_MARGIN = 1;

    public static final int COLUMNS = 16;
    public static final int ROWS = 16;

    private static final Cache<CharImageRequest, BufferedImage> charImgCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .build();

    private static final BufferedImage font;

    static {
        BufferedImage aFont = null;
        try (InputStream stream = ComputerCraft.class.getResourceAsStream(
            "/assets/computercraft/textures/gui/term_font.png"
        )) {
            if (stream != null) aFont = ImageIO.read(stream);
        } catch (IOException e) {
            LOG.error("Exception loading font", e);
        }

        if (aFont == null) LOG.error("Cannot load terminal font");
        font = aFont;
    }

    private Render() {
    }

    public static boolean isValid() {
        return font != null;
    }

    public static BufferedImage screenshot(Terminal terminal, int width, int height) {
        BufferedImage image = new BufferedImage(
            MARGIN * 2 + width * CELL_WIDTH,
            MARGIN * 2 + height * CELL_HEIGHT,
            BufferedImage.TYPE_3BYTE_BGR
        );
        Graphics graphics = image.getGraphics();

        int dx = 0;
        int dy = 0;

        for (int y = 0; y < height; y++) {
            TextBuffer textLine = terminal.getLine(y);
            TextBuffer bgLine = terminal.getBackgroundColourLine(y);
            TextBuffer fgLine = terminal.getTextColourLine(y);

            int cellHeight = y == 0 || y == height - 1 ? CELL_HEIGHT + MARGIN : CELL_HEIGHT;

            for (int x = 0; x < width; x++) {
                int cellWidth = x == 0 || x == width - 1 ? CELL_HEIGHT + MARGIN : CELL_HEIGHT;

                graphics.setColor(getAwtColour(terminal.getPalette(), bgLine.charAt(x), Colour.BLACK));
                graphics.fillRect(dx, dy, cellWidth, cellHeight);
                drawChar(
                    graphics, textLine.charAt(x), x * CELL_WIDTH + MARGIN, y * CELL_HEIGHT + MARGIN,
                    getAwtColour(terminal.getPalette(), fgLine.charAt(x), Colour.WHITE)
                );

                dx += cellWidth;
            }

            dx = 0;
            dy += cellHeight;
        }

        graphics.dispose();

        return image;
    }

    private static void drawChar(Graphics g, char c, int x, int y, Color colour) {
        if (c == '\0' || Character.isSpaceChar(c)) return;

        Rectangle r = new Rectangle(
            FONT_MARGIN + c % COLUMNS * (CELL_WIDTH + FONT_MARGIN * 2), FONT_MARGIN + c / ROWS * (CELL_HEIGHT + FONT_MARGIN * 2),
            CELL_WIDTH, CELL_HEIGHT
        );
        BufferedImage charImg = null;

        float[] zero = new float[4];

        try {
            charImg = charImgCache.get(new CharImageRequest(c, colour), () -> {
                float[] rgb = new float[4];
                colour.getRGBComponents(rgb);

                RescaleOp rop = new RescaleOp(rgb, zero, null);

                GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();

                BufferedImage img = font.getSubimage(r.x, r.y, r.width, r.height);
                BufferedImage pixel = gc.createCompatibleImage(r.width, r.height, Transparency.TRANSLUCENT);

                Graphics ig = pixel.getGraphics();
                ig.drawImage(img, 0, 0, null);
                ig.dispose();

                rop.filter(pixel, pixel);
                return pixel;
            });
        } catch (ExecutionException e) {
            LOG.error("Could not retrieve char image from cache.", e);
        }

        g.drawImage(charImg, x, y, CELL_WIDTH, CELL_HEIGHT, null);
    }

    private static Color getAwtColour(Palette palette, char colour, Colour def) {
        double[] rgb = palette.getColour(15 - Terminal.getColour(colour, def));
        return new Color(constrainToRange(rgb[0]), constrainToRange(rgb[1]), constrainToRange(rgb[2]));
    }

    private static float constrainToRange(double val) {
        return Math.max(0f, Math.min(1f, (float) val));
    }

    private static class CharImageRequest {
        private final char character;
        private final Color color;

        private CharImageRequest(char character, Color color) {
            this.character = character;
            this.color = color;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CharImageRequest that = (CharImageRequest) o;
            return character == that.character && color.equals(that.color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(character, color);
        }
    }
}
