package HTTP;

/**
 * Created by Eleanor on 15/03/2017.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class HTTPServer {

    private static String hostName = "localhost";
    private static boolean containsHostHeader = false;
    private static Socket connectionSocket;

    public static void main(String args[]) throws Exception {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        while(true)
        {
            connectionSocket = serverSocket.accept();
            BufferedReader requestFromClient = new BufferedReader(new InputStreamReader (connectionSocket.getInputStream()));
            serverAction(requestFromClient);
        }
    }
    //TODO multithreaded (assistent zei dat we eerst moeten zorgen dat het voor 1 client werkt)
    //TODO GET HEAD PUT POST (PUT & POST store received data in text file)
    //TODO persistent connection
    //TODO status codes implementeren

    public static void serverAction(BufferedReader requestFromClient) throws Exception {

        DataOutputStream responseToClient = new DataOutputStream(connectionSocket.getOutputStream());

        String requestLine = requestFromClient.readLine();


        String[] initialStrings = requestLine.split(" ");
        String command = initialStrings[0];
        String path = initialStrings[1];
        String version = initialStrings[2];
        boolean isBadRequest = false;

        if (version.contains("HTTP")){
            // read nextline
            if (version.equals("HTTP/1.1")){
                requestLine = requestFromClient.readLine();
                if(!requestLine.toLowerCase().contains("host:")){
                    isBadRequest = true;
                }
                String[] splits = requestLine.split(" ");
                String host = splits[1]; // host opslaan
            } else if (!version.equals("HTTP/1.0")){
                isBadRequest = true;
            }
        } else {
            isBadRequest = true;
        }

        String uri;
        if (Objects.equals(path, "/"))
            uri = "output.html";
        else if (path.substring(0,1).matches("\\/"))
            uri = path.substring(1);
        else
            uri=path;

        if (command.equals("POST") || command.equals("PUT")) {
            savePostPutText(requestFromClient);// TODO hier gaat het mis
        }
        String[] data = getHeadResponseData(uri, isBadRequest);

        //response
        responseToClient.writeBytes(version +" "+ data[0] +"\r\n");
        responseToClient.writeBytes("Date: " + data[1] + " GMT\r\n");
        if (!data[0].contains("404")){
            responseToClient.writeBytes("If-Modified-since: "+data [2] +" GMT\r\n"); //TODO hiermee bepalen of 304 Not Modified moet teruggegeven worden
            responseToClient.writeBytes("Content-type: " + data[3] +"\r\n"); //TODO
            responseToClient.writeBytes("Content-length: " + data[4] +"\r\n");
            responseToClient.writeBytes("\r\n");
            if (command.equals("GET") && !isBadRequest)
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
        int contentLength = 0;
        while (true) {
            if (t.toLowerCase().contains("content-length")) {
                String[] strings = t.split(": ");
                contentLength = Integer.parseInt(strings[1]);
                t = requestFromClient.readLine();
                while (t.equals("")) {
                    t = requestFromClient.readLine();
                }
                while(contentLength>0) {
                    writer.write(t);
                    writer.newLine();
                    contentLength -= t.length();
                    t = requestFromClient.readLine();
                }
                break;
            }
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

        Path pathOfBody = Paths.get(uri);
        if(!isBadRequest) {
            try {
                body = new String(Files.readAllBytes(pathOfBody));
                bodyLength = body.getBytes().length;
                statusCode =  "200 OK";
                SimpleDateFormat dateModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                dateModified.setTimeZone(TimeZone.getTimeZone("GMT"));
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
        String thisMoment = date.format(new Date());

//        int i = modifiedDate.compareTo(thisMoment);
//        if (i<0)
//            statusCode = "304 Not Modified";


        return new String[] {statusCode, thisMoment, modifiedDate, type, Integer.toString(bodyLength), body};

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