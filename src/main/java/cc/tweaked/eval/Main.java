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
        ComputerCraft.httpEnabled = false;
        ComputerCraft.httpWebsocketEnabled = false;
        ComputerCraft.computerThreads = 2;

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
}
