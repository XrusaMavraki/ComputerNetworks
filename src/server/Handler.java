package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public abstract class Handler implements Runnable {

    private Socket socket;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Serializable incomingObject = (Serializable) ois.readObject();
            handleMessage(incomingObject, ois, oos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method is supposed to be overriden by the Tracker and Client server sockets so that they may handle
     * their corresponding incoming messages and act accordingly.
     * @param message The message received.
     * @param ois The object input stream of the socket.
     * @param oos The object output stream of the socket.
     * @throws Exception If any exception occurs.
     */
    public abstract void handleMessage(Serializable message, ObjectInputStream ois, ObjectOutputStream oos) throws Exception;
}
