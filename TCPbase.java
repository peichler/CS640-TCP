import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class TCPbase extends Thread{
  private boolean doStop = false;
  final int headerSize = 24;

  DatagramSocket socket;
  InetAddress ip;
  int remotePort;

  String fileName;
  int mtu, sws;
  int seqNum, ackNum;

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

  public void run(){
    while(running()){
      byte[] data = new byte[mtu];
      DatagramPacket packet = new DatagramPacket(data, data.length);

      try{
        this.socket.receive(packet);
        receivedPacket(packet);
      }catch(IOException e){ }
    }
  }

  // Receiving data
  private void receivedPacket(DatagramPacket packet){
    // Handle TCP here
    handlePacket(packet);
    System.out.println("Handled packet ... stopping thread");
    stopThread();
  }

  abstract void handlePacket(DatagramPacket packet);


  // Sending data
  void sendTCP(byte[] data, Boolean[] flags) throws IOException {
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

    buff.put(data);

    byte[] tcpData = buff.array();

    DatagramPacket packet = new DatagramPacket(tcpData, tcpData.length, ip, remotePort);
    this.socket.send(packet);
  }

  short getChecksum(byte[] data){
    return (short)0;
  }


  public synchronized void stopThread(){
    this.doStop = true;
    disconnect();
  }

  private synchronized boolean running(){
    return this.doStop == false;
  }

  public void disconnect(){
    socket.close();
  }
}