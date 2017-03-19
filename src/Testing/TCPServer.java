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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;

public class TCPServer {

    private static String hostName = "localhost"; //TODO: wat moet hier?
    private static boolean containsHostHeader = false;
    private static Socket connectionSocket;

    public static void main(String args[]) throws Exception { //listen on port 80 (or other)
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        while(true)
        {
            connectionSocket = serverSocket.accept();
            BufferedReader requestFromClient = new BufferedReader(new InputStreamReader (connectionSocket.getInputStream()));
            TCPServer(requestFromClient);
        }
    }
    //TODO multithreaded (assistent zei dat we eerst moeten zorgen dat het voor 1 client werkt)
    //TODO GET HEAD PUT POST (PUT & POST store received data in text file)
    //TODO persistent connection
    //TODO status codes implementeren

    public static void TCPServer(BufferedReader requestFromClient) throws Exception {

        //request

        DataOutputStream responseToClient = new DataOutputStream(connectionSocket.getOutputStream());

        String requestLine = requestFromClient.readLine();
//        System.out.println("Received: " + requestLine);
//        String capitalizedSentence = requestLine.toUpperCase() + '\n';
//        responseToClient.writeBytes(capitalizedSentence);



        String[] initialStrings = requestLine.split(" ");
        String command = initialStrings[0];
        String path = initialStrings[1];
        String version = initialStrings[2];

        // read nextline
        requestLine = requestFromClient.readLine();
        if(!requestLine.toLowerCase().contains("host:")){ //hoofdletter insensitive maken?
            throw new UnknownHostException();
        }
        String[] splits = requestLine.split(" ");
        String host = splits[1]; // host opslaan
//
//        if (!containsHostHeader) {
//            System.out.println("400 Bad Request");
//            return;
//        }

        String uri;
        if (Objects.equals(path, "/"))
            uri = "index.html";
        else if (path.substring(0,1).matches("\\/"))
            uri = path.substring(1);
        else
            uri=path;


        Path pathOfBody = Paths.get(uri); // <-- deze lijn werkt enkel indien TCPClient al eens is uitgevoergd geweest
        String body = new String(Files.readAllBytes(pathOfBody));
        String statusCode = "200 OK";
        FileTime modifiedDate = Files.getLastModifiedTime(pathOfBody);
        String type = getType(uri);
        int bodyLength = body.getBytes().length;

        //response
        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        responseToClient.writeBytes("HTTP/1.1" + statusCode +"\n"); //TODO statusCode definieren
        responseToClient.writeBytes("Date: " + date.format(new Date()) + " GMT \n");
        responseToClient.writeBytes("If-Modified-since: "+modifiedDate.toString()+"\n"); //TODO hiermee bepalen of 304 Not Modified moet teruggegeven worden
        responseToClient.writeBytes("Content-type: "+ type +"\n"); //TODO
        responseToClient.writeBytes("Content-length: " + bodyLength +"\n");
        responseToClient.writeBytes("\n");
        if (!command.equals("HEAD"))
            responseToClient.writeBytes(body +"\n");

        responseToClient.close();
        requestFromClient.close();
        connectionSocket.close();

    }

    private static String getType(String uri) {
        String extension = "";

        int i = uri.lastIndexOf('.');
        if (i > 0) {
            extension = uri.substring(i+1);
        }

        String partOne;
        if (extension.equals("html"))
            partOne = "text/";
        else
            partOne = "image";

        return partOne+extension;
    }


}
