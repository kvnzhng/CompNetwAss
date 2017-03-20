package Testing;

/**
 * Created by Eleanor on 15/03/2017.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
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


        String[] initialStrings = requestLine.split(" ");
        String command = initialStrings[0];
        String path = initialStrings[1];
        String version = initialStrings[2];
        boolean isBadRequest = false;

        if (version.equals("HTTP/1.1")){
            // read nextline
            requestLine = requestFromClient.readLine();
            if(!requestLine.toLowerCase().contains("host:")){ //hoofdletter insensitive maken?
                isBadRequest = true;
            }
            String[] splits = requestLine.split(" ");
            String host = splits[1]; // host opslaan
        }

        String uri;
        if (Objects.equals(path, "/"))
            uri = "index.html";
        else if (path.substring(0,1).matches("\\/"))
            uri = path.substring(1);
        else
            uri=path;

        /*if (command.equals("POST") || command.equals("PUT")) {
            savePostPutText(requestFromClient);
        }*/
        String[] data = getHeadResponseData(uri, isBadRequest);

        //response
        responseToClient.writeBytes("\r\n");
        responseToClient.writeBytes(version +" "+ data[0] +"\r\n"); //TODO statusCode definieren
        responseToClient.writeBytes("Date: " + data[1] + " GMT\r\n");
        if (!data[0].contains("404")){
            responseToClient.writeBytes("If-Modified-since: "+data [2] +"\r\n"); //TODO hiermee bepalen of 304 Not Modified moet teruggegeven worden
            responseToClient.writeBytes("Content-type: "+ data[3] +"\r\n"); //TODO
            responseToClient.writeBytes("Content-length: " + data[4] +"\r\n");
            responseToClient.writeBytes("\r\n");
                if (!command.equals("HEAD"))
                    responseToClient.writeBytes(data[5] +"\r\n");
        }



        responseToClient.close();
        requestFromClient.close();
        connectionSocket.close();

    }

    private static void savePostPutText(BufferedReader requestFromClient) throws IOException {
        Path dst = Paths.get("postput.txt");
        BufferedWriter writer = Files.newBufferedWriter(dst, StandardCharsets.UTF_8);

        String t;

        t = requestFromClient.readLine();
        while(t!=null){
            writer.write(t);
            writer.newLine();
            t = requestFromClient.readLine();
        }
        writer.close();
    }

    private static String[] getHeadResponseData(String uri, boolean isBadRequest) throws IOException {

        String body = null;
        String statusCode;
        int bodyLength = 0;
        String modifiedDate = null;
        String type = null;

        Path pathOfBody = Paths.get(uri); // <-- deze lijn werkt enkel indien TCPClient al eens is uitgevoergd geweest
        if(!isBadRequest) {
            try {
                body = new String(Files.readAllBytes(pathOfBody));
                bodyLength = body.getBytes().length;
                statusCode = "200 OK";
                SimpleDateFormat dateModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                modifiedDate = dateModified.format(Files.getLastModifiedTime(pathOfBody).toMillis());
                type = getType(uri);
            } catch (NoSuchFileException e) {
                statusCode = "404 Not Found";
            }
        } else {
            statusCode = "400 Bad Request";
        }
        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        return new String[] {statusCode, date.format(new Date()), modifiedDate, type, Integer.toString(bodyLength), body};

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
            partOne = "image/";

        return partOne+extension;
    }
}
