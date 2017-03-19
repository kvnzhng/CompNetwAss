package Testing;
/**
 * Created by KevinZh on 08/03/2017.
 */
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

class TCPClient {
    private static String body;

    /**
     *
     * @param args
     * @throws Exception
     */
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

            if (command.equals("POST") || command.equals("PUT")) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter body: ");
                body = scanner.next();
            }

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

    /**
     *
     * @param command
     * @param url
     * @throws Exception
     */
    public static void TCPClient(String command, String url) throws Exception {
        TCPClient(command, url,null);
    }

    /**
     *
     * @param command
     * @param url
     * @param port
     * @throws Exception
     */
    public static void TCPClient(String command, String url, String port) throws Exception {
        TCPClient(command, url,null, port,false);
    }

    /**
     *
     * @param command
     * @param url
     * @param uri
     * @param port
     * @param retrieveObject
     * @throws Exception
     */
    public static void TCPClient(String command, String url, String uri, String port, boolean retrieveObject) throws Exception {

        ArrayList<String> requestHeader;

        if (port == null)
            port = "80";
        if (uri == null)
            uri = "";

        InetAddress addr = InetAddress.getByName(url);
        Socket clientSocket = new Socket(addr, Integer.parseInt(port));
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

        String urlParameters=null;

        //make the requestheader
        requestHeader = makeRequestHeader(command,url,uri);//url parameters voor Post en Put ergens definieren.


        //send requestheader
        for (String line: requestHeader)
            pw.println(line);
        pw.println("");
        pw.flush();

        //Save the response from server
        InputStream stream = clientSocket.getInputStream();
        saveResponse(stream);
        stream.close();
        clientSocket.close();

        //Analyze header, returns how long the body is (in bytes)
        int bytes = analyzeHeader();

        saveBody(bytes, retrieveObject, uri);


        if (!retrieveObject)//object already retrieved two lines back
            getImages(url);

    }

    /**
     *
     * @return
     * @throws Exception
     */
    private static int analyzeHeader() throws Exception {

        FileInputStream fstream = new FileInputStream("output");
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        int bytes = 0;
        String t = br.readLine();
        if (t.contains("404")){
            throw new Exception("Server not found");
        } else if (t.contains("500")){
            throw new Exception("Server error");
        } else if (t.contains("304")){
            throw new Exception("Server modified");
        } else if (t.contains("200")){
            while(!t.isEmpty()){
                t=br.readLine();
                if (t.contains("Content-Length")){
                    String[] strings = t.split(": ");
                    bytes = Integer.parseInt(strings[1]);
                }
            }
            br.close();
            return bytes;
        } else{
            throw new Exception("Other error");
        }
    }

    /**
     *
     * @param stream
     * @throws IOException
     */
    private static void saveResponse(InputStream stream) throws IOException {
        Path dst = Paths.get("output");
        Files.copy(stream,dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     *
     * @param bytesToSkip
     * @param object
     * @param objName
     * @throws IOException
     */
    private static void saveBody(int bytesToSkip, boolean object, String objName) throws IOException {
        FileInputStream fstream = new FileInputStream("output");
        fstream.skip(fstream.getChannel().size()-bytesToSkip);

        if (!object) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            Path dst = Paths.get("body.html");
            BufferedWriter writer = Files.newBufferedWriter(dst, StandardCharsets.UTF_8);

            String t;

            t = br.readLine();
            while(t!=null){
                writer.write(t);
                writer.newLine();
                t = br.readLine();
            }
            writer.close();
            br.close();

        } else{
            String[] strings = objName.split("\\.");
            ImageInputStream iis = ImageIO.createImageInputStream(fstream);
            BufferedImage image = ImageIO.read(iis);
            File outputFile = new File(objName);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(image, strings[1], outputFile);
        }

        fstream.close();

    }


    /**
     *
     * @param command
     * @param url
     * @param loc
     * @return
     */
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
            request.add("Content-Length: " + length);
            request.add("");
            request.add(body); // nog te definieren
        }
        return request;
    }

    /**
     *
     * @param command
     * @return
     */
    private static boolean isImplementedCommand(String command) {
        return (command.equals("GET") || command.equals("HEAD") ||command.equals("POST") ||command.equals("PUT"));
    }


    /**
     *
     * @param url
     * @throws Exception
     */
    private static void getImages(String url) throws Exception { // retrieve images from the html file
        byte[] encoded = Files.readAllBytes(Paths.get("body.html"));
        String htmlAsString = new String(encoded, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(htmlAsString);
        Elements images = doc.select("img");
        for (Element el : images) {
            String imageURI = el.attr("src");
            try {
                TCPClient("GET", url, imageURI, null, true);
            } catch (Exception e){

            }
        }

    }
}