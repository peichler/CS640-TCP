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

  double timeout;
  double ertt;
  double edev;

  public TCPsender(int port, InetAddress ip, int remotePort, String fileName, int mtu, int sws){
    super(port, fileName, mtu, sws);

    this.ip = ip;
    this.remotePort = remotePort;
    this.timeout = 5.0;
    this.ertt = 0.0;
    this.edev = 0.0;
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
    super.stopThread();
  }

  public void handlePacket(TCPpacket packet){
    System.out.println("Handling packet in sender");



  }

  private void checkTimeout(TCPpacket packet) {
    if (packet.ackNum == 0) {
      ertt = (double)(System.nanoTime() - packet.time);
      edev = 0.0;
      timeout = 2.0*ertt;
    } else {
      srtt = (double)(System.nanoTime() - packet.time);
      sdev = Math.abs(srtt - ertt);
      ertt = .875*ertt + (1.0-.875)*srtt;
      edev = .75*edev + (1.0-.75)*sdev;
      timeout = ertt + 4*edev;
    }
  }

  int getMaxDataSize(){
    // TODO: calculate real max size between both sender & reciever
    // TODO: cacluate real header size
    return this.mtu - TCPpacket.headerSize - 28;
  }
}