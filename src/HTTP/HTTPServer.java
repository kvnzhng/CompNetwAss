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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HTTPServer {

    public static final String HTTP_1_1 = "HTTP/1.1";
    private static String hostName = "localhost";
    private static boolean containsHostHeader = false;
    private static Socket connectionSocket;

    public static void main(String args[]) throws Exception {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        while(true)
        {
            connectionSocket = serverSocket.accept();
            if (connectionSocket != null)
            {
                Handler h = new Handler(connectionSocket);
                Thread thread = new Thread(h);
                thread.start();
            }
        }
    }
    //TODO status codes implementeren

    public static void serverAction(BufferedReader requestFromClient) throws Exception {

        //Analyze the request from the client.
        String uri;
        String ifModifiedDate = createDate(new Date(0));
        boolean isBadRequest = false;

        String requestLine = requestFromClient.readLine();
        String[] initialStrings = requestLine.split(" ");
        String command = initialStrings[0];
        String path = initialStrings[1];
        String version = initialStrings[2];
        if (version.equals(HTTP_1_1)){ //we only support HTTP/1.1
            requestLine = requestFromClient.readLine();
            if(command.equals("GET")) {
                if (requestLine.toLowerCase().contains("if-modified-since:" )) {
                    String[] modified = requestLine.split(": ");
                    ifModifiedDate = modified[1];
                    requestLine = requestFromClient.readLine();
                }

            }
            if(!requestLine.toLowerCase().contains("host:")){
                isBadRequest = true;
            }
            String[] splits = requestLine.split(" ");
            String host = splits[1]; // host opslaan
        } else {
            isBadRequest = true;
        }

        //check the path
        if (command.equals("POST") || command.equals("PUT")) {
            savePostPutText(requestFromClient);
            uri = "postput.txt";
        } else if (Objects.equals(path, "/"))
            uri = "output.html";
        else if (path.substring(0,1).matches("\\/")) // /path... case
            uri = path.substring(1);
        else //path equals uri immediately path without the "/"
            uri=path;



        String[] data = createHeaderData(uri, isBadRequest, ifModifiedDate);
        byte[] body = getBodyData(uri);


        //response
        DataOutputStream responseToClient = new DataOutputStream(connectionSocket.getOutputStream());
        responseToClient.writeBytes(version +" "+ data[0] +"\r\n");
        responseToClient.writeBytes("Date: " + data[1] + " GMT\r\n");
        if (data[0].contains("200")){
            responseToClient.writeBytes("Modified-since: "+data [2] +" GMT\r\n"); //TODO hiermee bepalen of 304 Not Modified moet teruggegeven worden
            responseToClient.writeBytes("Content-type: " + data[3] +"\r\n");
            responseToClient.writeBytes("Content-length: " + data[4] +"\r\n");
            if (command.equals("GET") && !isBadRequest){
                responseToClient.write(body);
            }
        }else{
            responseToClient.writeBytes("Connection: " + data[5]+ "\r\n");
        }
        responseToClient.writeBytes("\r\n");


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
                    contentLength -= t.length();
                    t = requestFromClient.readLine();
                }
                break;
            }
            t = requestFromClient.readLine();
        }
        writer.close();
    }

    private static byte[] getBodyData(String uri) throws IOException {
        Path pathOfBody = Paths.get(uri);
        byte[] data = null;
        try {
            data = Files.readAllBytes(pathOfBody);
        } catch (NoSuchFileException e){

        }
        return data;
    }

    private static String[] createHeaderData(String uri, boolean isBadRequest, String ifModifiedDate) throws IOException {
        String statusCode;
        long bodyLength = 0;
        String modifiedDate = null;
        String type = null;
        String connection = null;

        if(!isBadRequest) {
            try {
                Path pathOfBody = Paths.get(uri);
                bodyLength = Files.size(pathOfBody);
                statusCode =  "200 OK";
                SimpleDateFormat dateModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                dateModified.setTimeZone(TimeZone.getTimeZone("GMT"));
                modifiedDate = dateModified.format(Files.getLastModifiedTime(pathOfBody).toMillis());
                try {

                    Date simpleModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss").parse(modifiedDate);
                    Date simpleIfModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss").parse(ifModifiedDate);
                    int compare = simpleIfModifiedDate.compareTo(simpleModifiedDate);
                    if (compare >= 0) {
                        statusCode = "304 Not Modified";
                    }
                } catch (ParseException pexc) { }

                type = getType(uri);
            } catch (NoSuchFileException e) {
                statusCode = "404 Not Found";
                connection = "close";
            }
        } else {
            statusCode = "400 Bad Request";
            connection = "close";

        }


        String thisMoment = createDate(new Date());

        return new String[] {statusCode, thisMoment, modifiedDate, type, Long.toString(bodyLength), connection};

    }

    static String createDate(Date dateTime) {
        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        return date.format(dateTime);
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
