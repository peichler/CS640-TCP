import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TCPsender extends TCPbase{
  InetAddress ip;
  int remotePort;

  int seqNum, ackNum;

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
        //TODO: set flags
        Boolean[] flags = new Boolean[]{false, false, false};

        byte[] data = new byte [Math.min(getMaxDataSize(), stream.available())];
        stream.read(data, 0, data.length);

        byte[] tcpData = makeTCPPacket(data, flags);
        sendTCP(tcpData);

        //Increment sequence
        seqNum += data.length;
      }

      stream.close();
    }catch(IOException e){
      System.out.println(e);
    }
  }

  short getChecksum(byte[] data){
    return (short)0;
  }

  byte[] makeTCPPacket(byte[] data, Boolean[] flags){
    ByteBuffer buff = ByteBuffer.allocate(data.length + headerSize);

    buff.putInt(seqNum);
    buff.putInt(ackNum);
    buff.putLong(System.nanoTime());
    
    int sizeWithFlags = data.length;
    for (int i=0; i<3; i++) {
      sizeWithFlags = sizeWithFlags << 1;
      sizeWithFlags += flags[i] ? 1 : 0;
    }

    buff.putInt(sizeWithFlags);
    buff.putShort((short)0);
    buff.putShort(getChecksum(data));
    System.out.println("Header position: " + buff.position());

    buff.put(data);

    return buff.array();
  }

  void sendTCP(byte[] data) throws IOException {
    DatagramPacket packet = new DatagramPacket(data, data.length, ip, remotePort);
    this.socket.send(packet);
  }

  int getMaxDataSize(){
    // TODO: calculate real max size between both sender & reciever
    // TODO: cacluate real header size
    return this.mtu - headerSize - 28;
  }
}