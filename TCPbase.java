import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class TCPbase extends Thread{
  private boolean doStop = false;
  private boolean canSendData = false;

  DatagramSocket socket;
  InetAddress ip;
  int remotePort;

  String fileName;
  int mtu, sws;
  int seqNum;
  int ackNum;
  int lastRecAck;

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
    // We have an ip ... we are the sender so connect and send initial packet
    if(ip != null){
      this.socket.connect(ip, remotePort);
      sendTCP(ByteBuffer.allocate(4).putInt(this.socket.getPort()).array(), new Boolean[]{true,false,false});
    }

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
    TCPpacket tcpPacket = new TCPpacket(packet.getData());

    // Incorrect checksum ... drop packet
    if(tcpPacket.isChecksumValid() == false){
      System.out.println("Incorrect checksum ... dropping packet");
      return;
    }


    if(tcpPacket.isAck()){
      if(tcpPacket.ackNum > lastRecAck)
        lastRecAck = tcpPacket.ackNum;
    }

    if(tcpPacket.isSyn()){
      ackNum = tcpPacket.seqNum + 1;

      // No ACK flag ... we are client receiving syn for first time
      if(tcpPacket.isAck() == false){
        if(tcpPacket.data.length < 4){
          System.out.println("Invalid port number");
          return;
        }

        remotePort = tcpPacket.data[0];

        if(remotePort < 1 || remotePort > 65535){
          System.out.println("Invalid port number");
          return;
        }

        sendTCP(ByteBuffer.allocate(4).putInt(this.socket.getPort()).array(), new Boolean[]{true,true,false});
      }else{
        sendACK();
        canSendData = true;
      }
    }
    // Packet is not syn or ack ... handle packet by application
    else if(tcpPacket.isAck() == false){
      handlePacket(tcpPacket);

      // Send a packet back to acknoledge this packet
      ackNum = tcpPacket.seqNum + 1;
      sendACK();
    }

    // Sequence number has already been initialized
    // else{
    //   int prevSeq = tcpPacket.seqNum - tcpPacket.dataSize;
    //   if(prevSeq < ackNum){
    //     System.out.println("Duplicate packet ... dropping packet");
    //     return;
    //   }else if(prevSeq > ackNum){
    //     //TODO: Put packet in queue
    //     return;
    //   }
    // }

    
    // TODO: remove in future
    // if(tcpPacket.seqNum){
    //   System.out.println("Stopping thread");
    //   stopThread();
    // }
  }

  abstract void handlePacket(TCPpacket packet);


  // Sending data
  void sendACK(){
    seqNum += 1;
    sendTCP(new byte[0], new Boolean[]{false,true,false});
  }

  void sendTCP(byte[] data, Boolean[] flags) {
    try{
      long time = System.nanoTime();
      TCPpacket tcpPacket = new TCPpacket(seqNum, ackNum, time, flags, data);
      byte[] tcpData = tcpPacket.packetToBytes();

      DatagramPacket packet = new DatagramPacket(tcpData, tcpData.length, ip, remotePort);
      this.socket.send(packet);

      //Increment sequence
      seqNum += data.length;
    }catch(IOException e){
      System.out.println("Failure to send packet");
    }
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