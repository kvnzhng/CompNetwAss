package Testing;
/**
 * Created by KevinZh on 08/03/2017.
 */
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

class TCPClient {
    private static String body;

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

            if (command.equals("POST") || command.equals("PUT")) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter body: ");
                body = scanner.next();
            }

            if (args.length == 2) {
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

        ArrayList<String> requestHeader;

        if (port == null)
            port = "80";
        if (loc == null)
            loc = "";

        InetAddress addr = InetAddress.getByName(url);
        Socket clientSocket = new Socket(addr, Integer.parseInt(port));
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

        String urlParameters=null;

        //make the requestheader
         requestHeader = makeRequestHeader(command,url,loc);

        //send requestheader
        for (String line: requestHeader)
            pw.println(line);
        pw.println("");
        pw.flush();

        BufferedReader response = bufferResponse(clientSocket, command, port);

        saveFile(response);
        pw.close();

        clientSocket.close();
    }

    private static BufferedReader bufferResponse(Socket clientSocket,String command, String port) throws Exception {

        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        br.mark(1);
        String t = br.readLine();

        String domain;
        String location;

        if (t.contains("302"))
        //TODO: andere exceptions kunnen opvangen.
        {
            //redirection
            while (t != null) {
                if (t.contains("Location:")) {
                    String[] parts2 = t.split(" ");
                    String redirectLoc = parts2[1];
                    String[] parts3 = redirectLoc.split("/");
                    domain = parts3[2];
                    location = parts3[3];
                    TCPClient(command, domain, location, port);
                }
                t=br.readLine();
            }
        }
        br.reset();
        return br;
    }

    private static void saveFile(BufferedReader bufferResponse) throws IOException {

        BufferedWriter writer;
        Path dst = Paths.get("output.html");
        writer = Files.newBufferedWriter(dst, StandardCharsets.UTF_8);

        String t = bufferResponse.readLine();
        while(t!=null){
            System.out.println(t);
            writer.write(t);
            t = bufferResponse.readLine();
        }
        bufferResponse.close();

    }

    private static ArrayList<String> makeRequestHeader(String command, String url, String loc) {
        /*
        makes request header
         */
        ArrayList<String> request = new ArrayList<>();
        request.add(command + " /" + loc + " HTTP/1.1");
        request.add("Host: "+url);

        if (Objects.equals(command, "POST") || Objects.equals(command, "PUT")) {
            int length = body.length();
            request.add("Content-type: application/x-www-form-urlencoded");
            request.add("Content-Length: "+length);
            request.add("");
            request.add(body); // nog te definieren

        }

        return request;
    }

    private static boolean isImplementedCommand(String command) {
        return (command.equals("GET") || command.equals("HEAD") ||command.equals("POST") ||command.equals("PUT"));
    }


    private void head(String url) {

    }

    private void post(String url) {

    }

    private void put(String url) {

    }

    private void GET() throws IOException { // retrieve images from the html file
        byte[] encoded = Files.readAllBytes(Paths.get("output.html"));
        String htmlAsString = new String(encoded, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(htmlAsString);
        Elements images = doc.select("img");
        for (Element el : images) {
            String imageUrl = el.attr("src");
            try(InputStream in = new URL(imageUrl).openStream()){
                Files.copy(in, Paths.get("/"));
            }
        }

    }
}