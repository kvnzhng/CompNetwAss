package Testing;
/**
 * Created by KevinZh on 08/03/2017.
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TCPClient {

    public static void main(String[] args) throws Exception {

        String command;
        if (isImplementedCommand(args[0])) {
            command = args[0];

            try {
                InetAddress.getByName(args[1]);
            } catch (UnknownHostException uhe) {
                System.out.println("Unknown Host");
            }
            String url = args[1];


            if (args.length == 2) {//zo moet het denk ik voor de cmd line
                try {
                    TCPClient(command, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (args.length == 3) {
                try {
                    Integer.parseInt(args[2]);
                } catch (NumberFormatException nfe) {
                    System.out.println("Port is not a number");
                }
                String port = args[2];

                try {
                    TCPClient(command, url, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else
            System.out.println("501 not implemented");
    }

    public static void TCPClient(String command, String url) throws Exception {
        TCPClient(command, url,null);
    }

    public static void TCPClient(String command, String url, String port) throws Exception {
        TCPClient(command, url,null,port);
    }

    public static void TCPClient(String command, String url, String loc, String port) throws Exception {

        InetAddress addr = InetAddress.getByName(url);
        if (port == null)
            port = "80";
        Socket clientSocket = new Socket(addr, Integer.parseInt(port));

        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

        if (loc == null)
            loc = "";

        String requestLine = command + " /" + loc + " HTTP/1.1";
        String requestURI = "Host: "+url;

        pw.println(requestLine);
        pw.println(requestURI);
        pw.println("");
        pw.flush();

        /*switch (command) {  TODO implementeren van methodes
            case "HEAD":
                head(url); //TODO
                break;
            case "GET":
                get(url); //TODO
                break;
            case "PUT":
                put(url); //TODO
                break;
            case "POST":
                post(url); //TODO
                break;
        }*/

        boolean redirect = false;
        final BufferedWriter writer;

        Path dst = Paths.get("C:", "output.html");
        writer = Files.newBufferedWriter(dst, StandardCharsets.UTF_8);

        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String t;
        while((((t = br.readLine())) != null) || (t.equals("") && command.equals("HEAD"))) { // zou dit goed genoeg zijn voor de head?
            String domain=null;
            String location=null;
            if (t.contains("302"))
            //TODO: andere exceptions kunnen opvangen.
            {
                //redirection
                while ((t=br.readLine()) != null){
                    if (t.contains("Location:")){
                        String[] parts2 = t.split(" ");
                        String redirectLoc = parts2[1];
                        String[] parts3 = redirectLoc.split("/");
                        redirect = true;
                        domain = parts3[2];
                        location = parts3[3];
                        break;
                    }
                }
            }
            if (redirect)
                TCPClient(command, domain,location, port);
            else {
                writer.write(t); //save the text
                System.out.println(t);
            }
        }


        pw.close();
        br.close();
        clientSocket.close();
    }

    private static boolean isImplementedCommand(String command) {
        return (command.equals("GET") || command.equals("HEAD") ||command.equals("POST") ||command.equals("PUT"));
    }

    /*private static void get(Socket clientSocket) {
        boolean redirect = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String t;
        while((t = br.readLine()) != null) {
            String domain=null;
            String location=null;
            if (t.contains("302"))
            //TODO: andere exceptions kunnen opvangen.
            {
                //redirection
                while ((t=br.readLine()) != null){
                    if (t.contains("Location:")){
                        String[] parts2 = t.split(" ");
                        String redirectLoc = parts2[1];
                        String[] parts3 = redirectLoc.split("/");
                        redirect = true;
                        domain = parts3[2];
                        location = parts3[3];
                        break;
                    }
                }
            }
            if (redirect)
                TCPClient(command, domain,location, port);
            else
                System.out.println(t);
        }
    }*/

    private void head(String url) {

    }

    private void post(String url) {

    }

    private void put(String url) {

    }

    private void GET(){ // retrieve images from the html file

    }
}