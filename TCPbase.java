import java.net.DatagramSocket;
import java.net.SocketException;

public class TCPbase{
  final int headerSize = 24;

  DatagramSocket socket;
  String fileName;
  int mtu, sws;

  public TCPbase(int port, String fileName, int mtu, int sws){
    this.fileName = fileName;
    this.mtu = mtu;
    this.sws = sws;

    try{
      this.socket = new DatagramSocket(port);  
    }catch(SocketException e){
      System.out.println("Socket could not be created");
    }
  }

  public void disconnect(){
    socket.disconnect();
  }
}