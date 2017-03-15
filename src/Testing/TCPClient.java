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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

class TCPClient {

    public static void main(String[] args) throws Exception {

        String command;
        if (isImplementedCommand(args[0])) {
            command = args[0];

            try {
                InetAddress.getByName(args[1]);
            } catch (UnknownHostException uhe) {
                System.out.println("404 Not Found");
                return;
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
                    return;
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
        requestHeader = makeRequestHeader(command,url,loc,urlParameters);//url parameters voor Post en Put ergens definieren.


        //send requestheader
        for (String line: requestHeader)
            pw.println(line);
        pw.println("");
        pw.flush();

        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        br.mark(1);
        String t = br.readLine();
        if (t.contains("302")){
            br.reset();
            String[] strings= redirect(br);
            pw.close();
            br.close();
            clientSocket.close();
            TCPClient(command, strings[0], strings[1], port);
        }else{
            br.reset();
            saveFile(br);
            br.close();
            clientSocket.close();
            GET(url);
        }
        clientSocket.close();
    }

    private static String[] redirect(BufferedReader br) throws Exception {

        String domain = null;
        String location = null;
        boolean found = false;
        String t=br.readLine();
        while (t != null && !found) {
            if (t.contains("Location:")) {
                found = true;
                String[] parts2 = t.split(" ");
                String redirectLoc = parts2[1];
                String[] parts3 = redirectLoc.split("/");
                domain = parts3[2];
                location = parts3[3];}
            t=br.readLine();
        }
        return new String[] {domain,location};
    }

    private static void saveFile(BufferedReader bufferResponse) throws IOException {

        BufferedWriter writer;
        Path dst = Paths.get("output.html");
        writer = Files.newBufferedWriter(dst, StandardCharsets.UTF_8);

        String t = bufferResponse.readLine();
        while(t!=null){
            writer.write(t);
            writer.newLine();
            t = bufferResponse.readLine();
        }
        writer.close();
    }

    private static ArrayList<String> makeRequestHeader(String command, String url, String loc,String urlParameters) {
        /*
        makes request header
         */
        ArrayList<String> request = new ArrayList<>();
        request.add(command + " /" + loc + " HTTP/1.1");
        request.add("Host: "+url);
        if (Objects.equals(command, "POST") || Objects.equals(command, "PUT"))
            request.add(urlParameters); // nog te definieren

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

    private static void GET(String url) throws IOException { // retrieve images from the html file
        byte[] encoded = Files.readAllBytes(Paths.get("output.html"));
        String htmlAsString = new String(encoded, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(htmlAsString);
        Elements images = doc.select("img");
        for (Element el : images) {
            String imageUrl = "http://"+url+"/"+el.attr("src");
            String[] strings = el.attr("src").split("/");
            try(InputStream in = new URL(imageUrl).openStream()){
                Files.copy(in, Paths.get(strings[strings.length-1]));
            }catch (FileAlreadyExistsException e){

            }catch (FileNotFoundException e){

            }
        }

    }
}