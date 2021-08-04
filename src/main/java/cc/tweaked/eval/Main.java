package cc.tweaked.eval;

import cc.tweaked.eval.computer.CC;
import cc.tweaked.eval.computer.Render;
import com.sun.net.httpserver.HttpServer;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.util.ThreadUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (System.getProperty("cct-eval.http", "true").equals("true")) {
            ComputerCraft.httpEnabled = ComputerCraft.httpWebsocketEnabled = true;
            // 128KB/s upload, 512KB/s download. Incredibly generous for CC, but should stop the most rampant of abuse.
            ComputerCraft.httpUploadBandwidth = getProperty("cct-eval.http.upload", 128 * 1024);
            ComputerCraft.httpDownloadBandwidth = getProperty("cct-eval.http.download", 512 * 1024);
        } else {
            ComputerCraft.httpEnabled = ComputerCraft.httpWebsocketEnabled = false;
        }

        ComputerCraft.computerThreads = getProperty("cct-eval.threads", 2);

        System.setProperty("java.awt.headless", "true");

        if (CC.getJar() == null || CC.getVersion() == null || !Render.isValid()) System.exit(1);

        int port = Integer.parseInt(args[0]);

        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 16);
        server.setExecutor(new ThreadPoolExecutor(
            0, 4, 5L, TimeUnit.MINUTES,
            new SynchronousQueue<>(), ThreadUtils.factory("Server")
        ));
        EvalRequestHandler handler = new EvalRequestHandler(server.getExecutor());
        server.createContext("/", handler);
        server.start();

        try {
            handler.run();
        } finally {
            server.stop(0);
        }
    }

    private static int getProperty(String key, int def) {
        String value = System.getProperty(key);
        if (value == null) return def;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.printf("Property %s invalid (cannot parse '%s').\n", key, value);
            System.exit(1);
            throw e;
        }
    }
}
