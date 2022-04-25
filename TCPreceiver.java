import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TCPreceiver extends TCPbase{
  public TCPreceiver(int port, int mtu, int sws, String fileName){
    super(port, fileName, mtu, sws);
  }

  public void receiveFile(){
    FileOutputStream stream = null;

    try{
      stream = new FileOutputStream(fileName);
    }catch(FileNotFoundException e){
      System.out.println("File could not be created");
      return;
    }

    //TODO: check when file has been completly sent
    while(true){
      byte[] data = new byte[mtu];
      DatagramPacket packet = new DatagramPacket(data, data.length);
      try{
        this.socket.receive(packet);  

        //TODO: read through headers

        stream.write(data, headerSize, packet.getLength() - headerSize);
      }catch(IOException e){
        System.out.println(e);
        continue;
      }

      //TODO: make acknowledgment

      //TODO: check if end of file
      break;
    }
    
    try{
      stream.close();  
    }catch(IOException e){
      System.out.println(e);
    }
  }
}