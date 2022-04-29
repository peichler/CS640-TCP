import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.lang.Math;

// Sends file to receiver using TCP from base class
public class TCPsender extends TCPbase{

  public TCPsender(int port, InetAddress ip, int remotePort, String fileName, int mtu, int sws){
    super(port, fileName, mtu, sws);

    this.ip = ip;
    this.remotePort = remotePort;
  }

  public void sendFile(){
    while(established == false);
    if(running() == false)
      return;

    System.out.println("Sending file");

    FileInputStream stream = null;
    
    try{
      stream = new FileInputStream(fileName);
    }catch(FileNotFoundException e){
      System.out.println("File not found");
    }

    try{
      while(stream.available() > 0){
        // Make sure we are in sliding window ... if not wait
        toMan.waitTillPacketsLessThanNum(sws-1);

        // Create empty data with maximum size
        byte[] data = new byte [Math.min(getMaxDataSize(), stream.available())];

        // Read data into byte array
        stream.read(data, 0, data.length);

        if(running() == false)
          return;
        dataTransfered += data.length;
        sendTCP(data, new Boolean[]{false, false, false});
      }

      stream.close();
    }catch(IOException e){
      System.out.println(e);
    }

    toMan.waitTillPacketsLessThanNum(0);
    if(running() == false)
      return;

    System.out.println("Initiating close");
    initiatedClose = true;
    this.sendFIN();
  }

  public void handlePacket(TCPpacket packet){
    
  }

  int getMaxDataSize(){
    return this.mtu - TCPpacket.headerSize - 28;
  }
}
