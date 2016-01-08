package logserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String [] args) throws Exception{
        for(int i = 0; i < 100; i++){
            new Thread(){
                public void run(){
                    try {
                        DatagramSocket socket = new DatagramSocket();

                        for(int i = 0; i < 1000; i++){
                            String test = "test"+ + i + "@abc,123,keep it simple and stupid" + i;
                            if (new Random().nextInt(100) < 3){
                                test = test + "\r\nexception use multi line";
                            }
                            byte[] bytes = test.getBytes();
                            DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length, InetAddress.getByName("127.0.0.1"), 10010);
                            socket.send(dp);

                            TimeUnit.MILLISECONDS.sleep(1);
                        }
                        TimeUnit.MINUTES.sleep(3);
                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        TimeUnit.MINUTES.sleep(3);
    }

}
