package HTTP;
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
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HTTPClient {
    private static String body;

    /**
     * @param args {"HTTPCommand"	"URI Port"}
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String command;
        if (isImplementedCommand(args[0])) {
            command = args[0];

            String url = args[1];

            if (command.equals("POST") || command.equals("PUT")) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter body: ");
                body = scanner.nextLine();
            }

            if (args.length == 2) {
                try {
                    clientRequest(command, url);
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
                    clientRequest(command, url, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else
            System.out.println("501 not implemented");
    }

    /**
     * Creates a socket to the given host, and requests immediately the given command.
     *
     * @param command The request Command: GET, HEAD, POST, PUT
     * @param host    The host to connect to
     * @throws Exception
     */
    public static void clientRequest(String command, String host) throws Exception {
        clientRequest(command, host, null);
    }

    /**
     * Creates a socket to the given host to a specific given port, and requests immediately the given command.
     *
     * @param command The request Command: GET, HEAD, POST, PUT.
     * @param host    The host to connect to
     * @param port    Connects to this specific port
     * @throws Exception
     */
    public static void clientRequest(String command, String host, String port) throws Exception {
        clientRequest(command, host, null, port, false);
    }

    /**
     * Creates a socket to the given host with uri specification to a specific given port,
     * and requests immediately the given command.
     * Saves objects when necessary if askes
     *
     * @param command        The request Command: GET, HEAD, POST, PUT
     * @param host           The host to connect to
     * @param uri            A specification on the host adress
     * @param port           Connects to this specific port
     * @param retrieveObject When true, this will save the body as an object(image)
     * @throws Exception
     */
    public static void clientRequest(String command, String host, String uri, String port, boolean retrieveObject) throws Exception {

        ArrayList<String> requestHeader;

        if (port == null)
            port = "80";
        if (uri == null)
            uri = "";

        InetAddress addr = InetAddress.getByName(host);
        Socket clientSocket = new Socket(addr, Integer.parseInt(port));
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());


        //make the requestheader from the given arguments
        requestHeader = makeRequestHeader(command, host, uri);//url parameters voor Post en Put ergens definieren.

        //send requestheader to server
        for (String line : requestHeader)
            pw.println(line);
        pw.println("");
        pw.flush();

        //Save the response from server
        InputStream stream = clientSocket.getInputStream();
        try {
            saveResponse(stream);
        } catch (FileSystemException e) {
        }

        stream.close();
        clientSocket.close();

        //Analyze header, returns how long the body is (in bytes)
        int bytes = analyzeHeader();
        if (bytes>0) {
            if (command.equals("GET")) {
                saveBody(bytes, retrieveObject, uri);
                if (!retrieveObject)//object already retrieved two lines back
                    getImages(host, port);
            }
        }
    }

    /**
     * Checks the response of the server.
     *
     * @return The size of the body in bytes
     * @throws Exception
     */
    private static int analyzeHeader() throws Exception {

        FileInputStream fstream = new FileInputStream("output");
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        int bytes = 0;
        String t = br.readLine();

        if (t.contains("200")) { //200 OK is good
            while (!t.isEmpty()) { //read the header out, we only read the content-length out
                t = br.readLine();
                System.out.println(t);
                if (t.toLowerCase().contains("content-length")) {
                    String[] strings = t.split(": ");
                    bytes = Integer.parseInt(strings[1]);
                }
            }

        } else {
            while (t!=null) {
                System.out.println(t);
                t=br.readLine();
            }
        }
        fstream.close();
        br.close();
        return bytes;
    }

    /**
     * Saves the response to a local file.
     * @param stream The stream to be saved
     * @throws IOException
     */
    private static void saveResponse(InputStream stream) throws IOException {
        Path dst = Paths.get("output");
        Files.copy(stream, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Saves the body to a local file.
     * This can be a HTML file or a object(image file)
     *
     * @param bytesToSkip The amount of bytes to skip the header
     * @param object Is the file we want to save an object (image)
     * @param objName The name of the object, used to save the file
     * @throws IOException
     */
    private static void saveBody(int bytesToSkip, boolean object, String objName) throws IOException {
        FileInputStream fstream = new FileInputStream("output");
        fstream.skip(fstream.getChannel().size()-bytesToSkip);

        if (!object) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            Path dst = Paths.get("index.html");
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
     * This method creates a request header, every line is becomes a String.
     *
     * @param command The request command sent to the server
     *                This can be: GET, HEAD, PUT, POST
     * @param host The hostname of the location we connect to
     * @param uri The resource location
     * @return returns an ArrayList of Strings, every string is the next line.
     */
    private static ArrayList<String> makeRequestHeader(String command, String host, String uri) {
        ArrayList<String> request = new ArrayList<>();

        request.add(command + " /" + uri + " HTTP/1.1");
        if (Objects.equals(command, "GET"))
            request.add("If-Modified-Since: " + createDate(new Date()));
        request.add("Host: "+ host);
        if (Objects.equals(command, "POST") || Objects.equals(command, "PUT")) {
            int length = body.length();
            request.add("Content-type: text/html");
            request.add("Content-Length: " + length);
            request.add("\r\n");
            request.add(body);
        }
        return request;
    }

    /**
     * Checks if the given command does exists
     *
     * @param command The command to be checked.
     * @return
     */
    private static boolean isImplementedCommand(String command) {
        return (command.equals("GET") || command.equals("HEAD") ||command.equals("POST") ||command.equals("PUT"));
    }


    /**
     * Searches for images
     * @param host The host from where we will find the file
     * @throws Exception
     */
    private static void getImages(String host, String port) throws Exception { // retrieve images from the html file
        byte[] encoded = Files.readAllBytes(Paths.get("index.html"));
        String htmlAsString = new String(encoded, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(htmlAsString);
        Elements images = doc.select("img");
        for (Element el : images) {
            String imageURI = el.attr("src");
            clientRequest("GET", host, imageURI, port, true);
        }

    }

    static String createDate(Date dateTime) {
        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        return date.format(dateTime);
    }

}