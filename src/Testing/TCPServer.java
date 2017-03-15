package Testing;

/**
 * Created by Eleanor on 15/03/2017.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class TCPServer {

    private static String statusCode;
    private static String body;
    private static int port = 8080;
    private static String hostName = ""; //TODO: wat moet hier?
    private static boolean containsHostHeader = false;
    private static Socket connectionSocket;

    public void main(String args[]) throws Exception { //listen on port 80 (or other)
        ServerSocket serverSocket = new ServerSocket(port);
        while(true)
        {
            connectionSocket = serverSocket.accept();
            BufferedReader requestFromClient = new BufferedReader(new InputStreamReader (connectionSocket.getInputStream()));
            DataOutputStream responseToClient = new DataOutputStream(connectionSocket.getOutputStream());
            TCPServer(requestFromClient, responseToClient);

        }
    }
    //TODO multithreaded (assistent zei dat we eerst moeten zorgen dat het voor 1 client werkt)
    //TODO GET HEAD PUT POST (PUT & POST store received data in text file)
    //TODO persistent connection
    //TODO status codes implementeren

    public void TCPServer(BufferedReader requestFromClient, DataOutputStream responseToClient) throws Exception {

        //request

        String requestLine = requestFromClient.readLine();
        String[] initialLine = requestLine.split(" ");
        String command = initialLine[0];

        while (requestLine!=null) {
            if (requestLine.contains("Host:"))
                containsHostHeader = true;
        }

        if (!containsHostHeader) {
            System.out.println("400 Bad Request");
            return;
        }

        //response
        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        responseToClient.writeBytes("HTTP/1.1" + statusCode); //TODO statusCode definieren
        responseToClient.writeBytes("Date: " + date.format(new Date()) + " GMT");
        responseToClient.writeBytes("If-Modified-since: "); //TODO hiermee bepalen of 304 Not Modified moet teruggegeven worden
        responseToClient.writeBytes("Content-type: "); //TODO
        responseToClient.writeBytes("Content-length: " + body.length());
        responseToClient.writeBytes("");
        if (!command.equals("HEAD"))
            responseToClient.writeBytes(body); //TODO body definieren

        responseToClient.close();
        requestFromClient.close();
        connectionSocket.close();



    }
}
