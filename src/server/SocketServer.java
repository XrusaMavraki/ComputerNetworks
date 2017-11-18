package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class SocketServer implements Runnable {

    private int port;
    private final ExecutorService executorService; //service ths java poy ektelei ta threads. einai pio efficient apo new thread.
    private final Supplier<Handler> handlerSupplier; //o suplier einai tropos na pernis new instances apo handler.

    public SocketServer(int port, int numThreads, Supplier<Handler> handlerSupplier) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(numThreads);
        this.handlerSupplier = handlerSupplier;
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            port = serverSocket.getLocalPort();
            System.out.println("Listening on " + serverSocket.getLocalPort());
            while(true) {
                Socket clientSocket = serverSocket.accept();
                Handler handler = handlerSupplier.get(); //gia kathe thread neos handler.
                handler.setSocket(clientSocket);
                executorService.execute(handler);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }
}
