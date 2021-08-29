import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
    private static ServerSocket serverSocket;
    private static Socket socket;
    private static InputStream inputStream;
    private static InputStreamReader inputStreamReader;
    private static BufferedReader bufferedReader;
    private static OutputStream outputStream;

    public static boolean closeFlag=false;
    
    public static void main(String[] args) {
        try{
            serverSocket=new ServerSocket(config.PORT);
            socket=serverSocket.accept();
            inputStream=socket.getInputStream();
            inputStreamReader=new InputStreamReader(inputStream);
            bufferedReader=new BufferedReader(inputStreamReader);
            outputStream=socket.getOutputStream();
            WS.handShake(bufferedReader, outputStream);
            while(true){
                String data;
                if((data=WS.recieveText(inputStream))==null)break;
                WS.sendText(outputStream, data);
                WS.sendClose(outputStream);
                break;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}