import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

// Sends file to receiver using TCP from base class
public class TCPsender extends TCPbase{

  public TCPsender(int port, InetAddress ip, int remotePort, String fileName, int mtu, int sws){
    super(port, fileName, mtu, sws);

    this.ip = ip;
    this.remotePort = remotePort;
  }

  public void sendFile(){
    // Possibly change this into the future with a thread synchronized lock
    while(canSendData == false);
    System.out.println("Sending file");

    FileInputStream stream = null;
    
    try{
      stream = new FileInputStream(fileName);
    }catch(FileNotFoundException e){
      System.out.println("File not found");
    }

    try{
      // TODO: Add support for resending unacked packets
      while(stream.available() > 0){
        // Create empty data with maximum size
        byte[] data = new byte [Math.min(getMaxDataSize(), stream.available())];

        // Read data into byte array
        stream.read(data, 0, data.length);

        System.out.println("Sending data with size: "+ data.length);
        sendTCP(data, new Boolean[]{false, false, false});
      }

      stream.close();
    }catch(IOException e){
      System.out.println(e);
    }

    //TODO: remove this in future
    // super.stopThread();
    this.sendFIN();
  }

  public void handlePacket(TCPpacket packet){
    System.out.println("Handling packet in sender");
  }

  int getMaxDataSize(){
    // TODO: calculate real max size between both sender & reciever
    // TODO: cacluate real header size
    return this.mtu - TCPpacket.headerSize - 28;
  }
}