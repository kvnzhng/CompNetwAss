package HTTP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by KevinZh on 21/03/2017.
 */
public class Handler implements Runnable {
    private Socket socket;
    Handler(Socket socket) {
        this.socket = socket;
        run();
    }

    @Override
    public void run() {
        try {
            BufferedReader requestFromClient = new BufferedReader(new InputStreamReader (socket.getInputStream()));
            HTTPServer.serverAction(requestFromClient);
        } catch (Exception e){
            //System.out.println(e + " Serious problems");
        }

    }

}