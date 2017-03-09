package Testing;
/**
 * Created by KevinZh on 08/03/2017.
 */
import java.io.*;
import java.net.*;
class TCPClient {

    public static void main(String[] args) throws Exception {

        try {
            TCPClient("www.google.com");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void TCPClient(String url) throws Exception {
            TCPClient(url,null);
        }

    public static void TCPClient(String url, String loc) throws Exception {

        InetAddress addr = InetAddress.getByName(url);
        Socket clientSocket = new Socket(addr, 80);

        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());

        if (loc == null)
            loc = "";

        String header = "GET /" + loc + " HTTP/1.1";
        //TODO: laten werken met HTTP 1.0 indien 1.1 niet ondersteunt is.

        pw.println(header);
        pw.println("");
        pw.flush();


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
                TCPClient(domain,location);
            else
                System.out.println(t);
        }

        pw.close();
        br.close();
        clientSocket.close();
    }
}