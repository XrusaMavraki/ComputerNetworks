package Util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by xrusa on 3/5/2017.
 */
public class SocketMagic {
    public static <RES> RES doWithSocket(String host, int port, ExceptionalBiFunction<ObjectInputStream, ObjectOutputStream, RES> function) throws RuntimeException {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {


            return function.apply(ois, oos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    private SocketMagic(){}


}
