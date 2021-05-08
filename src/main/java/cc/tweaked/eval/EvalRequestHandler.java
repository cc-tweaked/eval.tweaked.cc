package cc.tweaked.eval;

import cc.tweaked.eval.computer.RunRequest;
import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dan200.computercraft.ComputerCraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Handles the main entrypoint.
 * <p>
 * This reads the code to execute from the request body and returns an image in the response body.
 */
public class EvalRequestHandler implements HttpHandler {
    private static final Logger LOG = LogManager.getLogger(EvalRequestHandler.class);

    private final Executor executor;
    private final Object lock = new Object();
    private final List<RunRequest> requests = new ArrayList<>();

    public EvalRequestHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] body = ByteStreams.toByteArray(exchange.getRequestBody());

        LOG.info("Starting new computer");

        RunRequest request;
        try {
            request = new RunRequest(body, (ok, image) -> executor.execute(() -> sendResponse(exchange, ok, image)));
        } catch (Exception e) {
            LOG.error("Failed to create computer", e);

            exchange.sendResponseHeaders(500, 0);
            exchange.close();
            return;
        }

        synchronized (lock) {
            requests.add(request);
        }
    }

    private void sendResponse(HttpExchange exchange, boolean ok, BufferedImage image) {
        try {
            exchange.getResponseHeaders().set("X-Clean-Exit", ok ? "True" : "False");
            if (image != null) {
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, 0);

                ImageIO.write(image, "png", exchange.getResponseBody());
            } else {
                exchange.sendResponseHeaders(204, 0);
            }
        } catch (IOException e) {
            ComputerCraft.log.error("Failed to send body", e);
        }

        exchange.close();
    }

    /**
     * Tick all computers in a loop.
     *
     * @throws InterruptedException If the thread is terminated
     */
    public void run() throws InterruptedException {
        while (true) {
            long started = System.nanoTime();

            synchronized (lock) {
                Iterator<RunRequest> iterator = requests.iterator();
                while (iterator.hasNext()) {
                    RunRequest request = iterator.next();
                    if (!request.tick()) {
                        request.cleanup();
                        iterator.remove();
                    }
                }
            }

            long took = System.nanoTime() - started;
            long remaining = (50_000_000L - took) / 1_000_000;
            if (remaining > 0) Thread.sleep(remaining);
        }
    }
}
