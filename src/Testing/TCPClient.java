package Testing;
/**
 * Created by KevinZh on 08/03/2017.
 */
import java.io.*;
import java.net.*;
class TCPClient {

    public static void main(String[] args) throws Exception {

        /*try {
            TCPClient("www.google.com");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        if (args.length == 2) {//zo moet het denk ik voor de cmd line
            try {
                TCPClient(args[0], args[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (args.length == 3) {
            try {
                TCPClient(args[0], args[1], args[2]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void TCPClient(String command, String url) throws Exception {
        TCPClient(command, url,null, Integer.toString(80));
    }

    public static void TCPClient(String command, String url, String port) throws Exception {
        TCPClient(command, url,null,port);
    }

    public static void TCPClient(String command, String url, String loc, String port) throws Exception {

        InetAddress addr = InetAddress.getByName(url);
        Socket clientSocket = new Socket(addr, 80);

        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

        if (loc == null)
            loc = "";

        String initialLine = command + " /" + loc + " HTTP/1.1";
        String header = "Host: "+url;

        pw.println(initialLine);
        pw.println(header);
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

        pw.close();
        br.close();
        clientSocket.close();
    }

    private void get(String url) {

    }

    private void head(String url) {

    }

    private void post(String url) {

    }

    private void put(String url) {

    }
}