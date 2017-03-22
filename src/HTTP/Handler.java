package HTTP;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;

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
            sendServerError();
        }

    }
    private void sendServerError()  {
        try {

            DataOutputStream responseToClient = new DataOutputStream(socket.getOutputStream());
            responseToClient.writeBytes(HTTPServer.HTTP_1_1 + " 500 Server Error\r\n");
            responseToClient.writeBytes("Date: " + HTTPServer.createDate(new Date())+ " GMT\r\n");
            responseToClient.close();
            socket.close();
        } catch (IOException e){
            // can't overwrite Runnable.run() method to pass along exception, so catch it here
        }
    }

}