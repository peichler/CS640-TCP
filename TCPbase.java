import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.ByteBuffer;

// Base class for TCP sender and receiver
// Handles sending and receiving TCP packets
public abstract class TCPbase extends Thread{
  private boolean doStop = false;
  
  volatile boolean canSendData = false;

  DatagramSocket socket;
  InetAddress ip;
  int remotePort;

  String fileName;
  int mtu, sws;
  int seqNum;
  int ackNum;
  int lastRecAck;
  int duplicateAckCount = 0;

  boolean initiatedClose;
  boolean waitingForClose;

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
      connectSocket();
      sendTCP(new byte[0], new Boolean[]{true,false,false});
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

  void connectSocket(){
    this.socket.connect(ip, remotePort);
  }

  ///////// Receiving data /////////

  private void receivedPacket(DatagramPacket packet){
    TCPpacket tcpPacket = new TCPpacket();

    // Incorrect checksum ... drop packet
    if(tcpPacket.isChecksumValid(packet.getData()) == false){
      System.out.println("Incorrect checksum ... dropping packet");
      return;
    }

    printPacket(tcpPacket, "rcv");   

    // Packet has ACK flag ... update our latest received ACK
    if(tcpPacket.isAck()){
      handleAckPacket(tcpPacket);
    }

    // Packet has SYN flag ... follow handshake connection procedure
    if(tcpPacket.isSyn()){
      updateAckNum(tcpPacket);
      // Set up socket if it isn't connected  
      if(this.socket.isConnected() == false){
        remotePort = packet.getPort();
        ip = packet.getAddress();
        connectSocket();
      }

      handleSynPacket(tcpPacket, packet);
    }
    // Packet has FIN flag ... follow closing procedure
    else if(tcpPacket.isFin()){
      updateAckNum(tcpPacket);
      handleFinPacket(tcpPacket);
    }
    // Packet is data ... handle packet by application
    else if(tcpPacket.payloadData.length > 0){
      handlePacket(tcpPacket);
      sendACK(tcpPacket);
    }
    else{
      updateAckNum(tcpPacket);
    }
  }

  void updateAckNum(TCPpacket tcpPacket){
    if(tcpPacket.seqNum == ackNum)
      ackNum = tcpPacket.seqNum + Math.max(tcpPacket.payloadData.length, 1);
  }

  void handleSynPacket(TCPpacket tcpPacket, DatagramPacket packet){
    ackNum = tcpPacket.seqNum + 1;

    
    // No ACK flag ... we are receiver receiving syn for first time
    if(tcpPacket.isAck() == false){
      sendTCP(new byte[0], new Boolean[]{true,false,true});
    }
    // We are the origianl sender ... send a final ACK to complete handshake
    else{
      sendACK(tcpPacket);
      System.out.println("Established handshake connection ... can send data now");
      canSendData = true;
    }
  }

  void handleFinPacket(TCPpacket tcpPacket){
    // We did not initiate the FIN ... send FIN back and wait for last ACK
    if(initiatedClose == false){
      sendFIN();
      waitingForClose = true;
    }
    // We sent original FIN ... send ACK back and wait for wait time
    else{
      sendACK(tcpPacket);
      // TODO: wait for 16x average packet time
      DelayedClose delay = new DelayedClose(this, 5000);
      delay.start();
    }
  }

  void handleAckPacket(TCPpacket tcpPacket){
    if(waitingForClose){
      stopThread();
      return;
    }

    if(tcpPacket.ackNum > lastRecAck){
      // TODO: remove packets in holding list with seqNum less than received ackNum
      lastRecAck = tcpPacket.ackNum;
      duplicateAckCount = 0;
    }
    else if(tcpPacket.ackNum == lastRecAck){
      duplicateAckCount += 1;
      if(duplicateAckCount == 3)
        handleTripleAck();
    }
  }

  void handleTripleAck(){
    // TODO: implement triple ack retramission
    System.out.println("Triple ACK occured!");
  }

  abstract void handlePacket(TCPpacket packet);

  ///////// Sending data /////////
  void sendACK(TCPpacket origPacket){
    // seqNum += 1;
    sendTCP(new byte[0], new Boolean[]{false,false,true}, origPacket.time);
  }

  void sendFIN(){
    // seqNum += 1;
    sendTCP(new byte[0], new Boolean[]{false,true,true});
  }

  void sendTCP(byte[] data, Boolean[] flags) {
    sendTCP(data, flags, System.nanoTime());
  }

  void sendTCP(byte[] data, Boolean[] flags, long time) {
    TCPpacket tcpPacket = new TCPpacket(seqNum, ackNum, time, flags, data);
    byte[] tcpData = tcpPacket.serialize();

    DatagramPacket packet = new DatagramPacket(tcpData, tcpData.length, ip, remotePort);

    printPacket(tcpPacket, "snd");
    try{
      this.socket.send(packet);
    }catch(IOException e){
      System.out.println("Failure to send packet");
    }

    // Increment sequence after sent packet
    seqNum += Math.max(data.length, 1);

    // Check if packet is required to be ACK back ... if so add to list to make sure it gets transmitted
    if(tcpPacket.isSyn() || tcpPacket.isFin() || tcpPacket.payloadData.length > 0){
      // TODO: add to list
    }
  }

  void printPacket(TCPpacket tcpPacket, String tType){
    System.out.println(tType + " time " + (tcpPacket.isSyn() ? "S " : "- ") + (tcpPacket.isAck() ? "A " : "- ") + 
      (tcpPacket.isFin() ? "F " : "- ") + (tcpPacket.payloadData.length > 0 ? "D " : "- ") + tcpPacket.seqNum + 
      " " + tcpPacket.payloadData.length + " " + tcpPacket.ackNum);
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
