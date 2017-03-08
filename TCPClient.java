/**
 * Created by KevinZh on 08/03/2017.
 */
import java.io.*;
import java.net.*;
class TCPClient
{
    public static void main(String argv[]) throws Exception
    {
        //System.out.println("enter ");
        BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
        InetAddress addr = InetAddress.getByName("http://www.google.com");
        Socket clientSocket = new Socket(addr, 80);
        DataOutputStream outToServer = new DataOutputStream
                (clientSocket.getOutputStream());

        BufferedReader inFromServer = new BufferedReader(new
                InputStreamReader(clientSocket.getInputStream()));


        //user sends
        String sentence = inFromUser.readLine();

        outToServer.writeBytes(sentence + '\n');

        //String modifiedSentence = inFromServer.readLine();

        //ser receives
        String inputline;
        StringBuffer response = new StringBuffer();

        while ((inputline = inFromServer.readLine()) != null){
            response.append(inputline);
        }

        System.out.println("FROM SERVER: " + response.toString());
        inFromServer.close();
        clientSocket.close();
    }
}