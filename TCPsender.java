import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TCPsender extends TCPbase{
  InetAddress ip;
  int remotePort;

  public TCPsender(int port, InetAddress ip, int remotePort, String fileName, int mtu, int sws){
    super(port, fileName, mtu, sws);

    this.ip = ip;
    this.remotePort = remotePort;
  }

  public void sendFile(){
    //TODO: catch connection errors?
    this.socket.connect(ip, remotePort);

    FileInputStream stream = null;
    
    try{
      stream = new FileInputStream(fileName);
    }catch(FileNotFoundException e){
      System.out.println("File not found");
    }

    try{
      while(stream.available() > 0){
        byte[] data = new byte [Math.min(getMaxDataSize(), stream.available()) + headerSize];
        stream.read(data, headerSize, data.length - headerSize);
        sendData(data);
      }

      stream.close();
    }catch(IOException e){
      System.out.println(e);
    }
  }

  void sendData(byte[] data) throws IOException{
    //TODO: add headers in data

    DatagramPacket packet = new DatagramPacket(data, data.length, ip, remotePort);
    this.socket.send(packet);
  }

  int getMaxDataSize(){
    // TODO: calculate real max size between both sender & reciever
    // TODO: cacluate real header size
    return this.mtu - headerSize;
  }
}