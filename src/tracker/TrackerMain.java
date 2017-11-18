package tracker;

import server.SocketServer;

public class TrackerMain {

    public static void main(String[] args) {
        System.out.println("Starting Tracker Server...");
        Tracker.getInstance();
        SocketServer socketServer = new SocketServer(12345,8, TrackerHandler::new);
        Thread thread = new Thread(socketServer);
        thread.start();
        try {
            // Tracker only waits for incoming connections, so block forever.
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
