//WebSocketを使うためのライブラリー

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Base64.Encoder;

public class WS {
    public static void sendClose(OutputStream os){
        try{
            byte[] sendHead = new byte[2];
            sendHead[0]=(byte)128+8;
            sendHead[1]=(byte)0;
            os.write(sendHead);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void sendHeader(OutputStream os,int fin,int opecode, int len){
        try{
            byte[] sendHead = new byte[4];//送り返すヘッダーを用意
            sendHead[0]=(byte)(128*fin+opecode);
            if(len<=125){
                sendHead[1]=(byte)len;
                os.write(sendHead, 0, 2);
            }else if(len<=65535){
                sendHead[1]=126;
                sendHead[2]=(byte)((len>>>8));
                sendHead[3]=(byte)(len);
                os.write(sendHead,0,4);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void sendText(OutputStream os,String sendData){
        try{
            byte[] sendHead = new byte[10];//送り返すヘッダーを用意
            sendHead[0]=(byte)128+1;
            byte[] sendTextB=sendData.getBytes("UTF-8");
            int length=sendTextB.length;

            if(length<=65535){
                sendHeader(os,1,1,length);
                os.write(sendTextB);
            }else{
                int num=(length-1)/65535;
                for(int i=0;i<=num;i++){
                    int sendPayloadLen=Math.min(sendTextB.length-(i*65535), 65535);
                    if(i==0){
                        sendHeader(os, 0, 1, sendPayloadLen);
                    }else if(0<i&&i<num){
                        sendHeader(os, 0, 0, sendPayloadLen);
                    }else if(i==num){
                        sendHeader(os, 1, 0, sendPayloadLen);
                    }
                    os.write(sendTextB,i*65535,sendPayloadLen);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void decord(webSocketData wData){
        for(int i=0;i<wData.payloadLen;i++){
            wData.payload[i]=(byte)(wData.payload[i]^wData.maskingKey[i%4]);
        }
    }

    //closeの場合falseを返します。
    public static boolean recieve(InputStream is,webSocketData wData) {
        try{
            byte[] buff=new byte[2];
            is.read(buff);
            wData.fin=(((buff[0]>>>7)&1)==1);
            wData.opecode=(byte)(buff[0]&15);
            if(wData.opecode==8){
                return false;
            }
            wData.payloadLen=buff[1]&127;
            if(wData.payloadLen==126){
                buff=new byte[2];
                is.read(buff);
                wData.payloadLen=
                 (Byte.toUnsignedInt(buff[0])<<8)
                +(Byte.toUnsignedInt(buff[1]));
            }else if(wData.payloadLen==127){
                buff=new byte[8];
                is.read(buff);
                wData.payloadLen=
                 (Byte.toUnsignedInt(buff[0])<<56)
                +(Byte.toUnsignedInt(buff[1])<<48)
                +(Byte.toUnsignedInt(buff[2])<<40)
                +(Byte.toUnsignedInt(buff[3])<<32)
                +(Byte.toUnsignedInt(buff[4])<<24)
                +(Byte.toUnsignedInt(buff[5])<<16)
                +(Byte.toUnsignedInt(buff[6])<<8)
                +(Byte.toUnsignedInt(buff[7]));
            }
            System.out.println(wData.payloadLen);
            buff=new byte[4];
            is.read(wData.maskingKey);
            wData.payload=new byte[wData.payloadLen];
            is.read(wData.payload);
        }catch(IOException e){
            e.printStackTrace();
        }
        return true;
    }
    
    //closeの場合false
    public static String recieveText(InputStream is){
        String result="";
        webSocketData wData=new webSocketData();
        do{
            if(!recieve(is, wData)){
                return null;
            }
            decord(wData);
            try{
                result=result+new String(wData.payload,"UTF-8");
            }catch(Exception e){
                e.printStackTrace();
            }
        }while(!wData.fin);
        return result;
    }

    public static void handShake(BufferedReader bufferedReader,OutputStream outputStream){
        String header = "";//ヘッダーの変数宣言
        String key = "";//ウェブソケットキーの変数宣言
        try {
            while (!(header = bufferedReader.readLine()).equals("")) {//入力ストリームから得たヘッダーを文字列に代入し、全行ループ。
                System.out.println(header);//1行ごとにコンソールにヘッダーの内容を表示
                String[] spLine = header.split(":");//1行を「:」で分割して配列に入れ込む
                if (spLine[0].equals("Sec-WebSocket-Key")) {//Sec-WebSocket-Keyの行
                    key = spLine[1].trim();//空白をトリムし、ウェブソケットキーを入手
                }
            }
            key +="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";//キーに謎の文字列を追加する
            byte[] keyUtf8=key.getBytes("UTF-8");//キーを「UTF-8」のバイト配列に変換する
            MessageDigest md = MessageDigest.getInstance("SHA-1");//指定されたダイジェスト・アルゴリズムを実装するオブジェクトを返す
            byte[] keySha1=md.digest(keyUtf8);//キー(UTF-8)を使用してダイジェスト計算を行う
            Encoder encoder = Base64.getEncoder();//Base64のエンコーダーを用意
            byte[] keyBase64 = encoder.encode(keySha1);//キー(SHA-1)をBase64でエンコード
            String keyNext = new String(keyBase64);//キー(Base64)をStringへ変換
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + keyNext
                    + "\r\n\r\n")
                    .getBytes("UTF-8");//HTTP レスポンスを作成
            outputStream.write(response);//HTTP レスポンスを送信
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class webSocketData {
    boolean fin;
    byte opecode;
    boolean mask;
    int payloadLen;
    byte[] maskingKey;
    byte[] payload;
    public webSocketData(){
        this.fin=false;
        this.opecode=0;
        this.mask=false;
        this.payloadLen=0;
        this.maskingKey=new byte[4];
    }
}
