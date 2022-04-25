import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TCPreceiver extends TCPbase{
  FileOutputStream stream;

  public TCPreceiver(int port, int mtu, int sws, String fileName){
    super(port, fileName, mtu, sws);

    try{
      stream = new FileOutputStream(fileName);
    }catch(FileNotFoundException e){
      System.out.println("File could not be created");
      return;
    }
  }

  public void handlePacket(DatagramPacket packet){
    byte[] data = packet.getData();


    try{
      // Use FileChannel to write at position for out of order data
      // https://stackoverflow.com/questions/9558979/java-outputstream-skip-offset
      stream.write(data, headerSize, packet.getLength() - headerSize);
    }catch(IOException e){
      System.out.println(e);
    }
  }

  public void disconnect(){
    super.disconnect(); 

    try{
      stream.close();  
    }catch(IOException e){
      System.out.println(e);
    }
  }
}