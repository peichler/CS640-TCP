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
      this.socket.connect(ip, remotePort);
      sendTCP(ByteBuffer.allocate(4).putInt(this.socket.getLocalPort()).array(), new Boolean[]{true,false,false});
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

  ///////// Receiving data /////////

  private void receivedPacket(DatagramPacket packet){
    TCPpacket tcpPacket = new TCPpacket(packet.getData());

    printPacket(tcpPacket, "rcv");

    // Incorrect checksum ... drop packet
    if(tcpPacket.isChecksumValid() == false){
      System.out.println("Incorrect checksum ... dropping packet");
      return;
    }

    // Update our ACK num if it is the next packet
    int prevSeq = tcpPacket.seqNum - tcpPacket.dataSize;
    if(prevSeq == ackNum)
      ackNum = tcpPacket.seqNum + 1;

    // Packet has ACK flag ... update our latest received ACK
    if(tcpPacket.isAck()){
      handleAckPacket(tcpPacket);
    }

    // Packet has SYN flag ... follow handshake connection procedure
    if(tcpPacket.isSyn()){
      handleSynPacket(tcpPacket, packet.getAddress());
    }
    // Packet has FIN flag ... follow closing procedure
    else if(tcpPacket.isFin()){
      handleFinPacket(tcpPacket);
    }
    // Packet is data ... handle packet by application
    else if(tcpPacket.payloadData.length > 0){
      handlePacket(tcpPacket);
      sendACK();
    }
  }

  void handleSynPacket(TCPpacket tcpPacket, InetAddress address){
    ackNum = tcpPacket.seqNum + 1;

    // Set up socket if it isn't connected
    if(this.socket.isConnected() == false){
      // No ACK flag ... we are receiver receiving syn for first time
      if(tcpPacket.isAck() == false){
        if(tcpPacket.payloadData.length < 4){
          System.out.println("Invalid port number ... no int sent");
          return;
        }

        remotePort = ByteBuffer.wrap(tcpPacket.payloadData).getInt();

        if(remotePort < 1 || remotePort > 65535){
          System.out.println("Invalid port number");
          return;
        }

        //Connect socket and send SYN + ACK back to sender
        
        this.socket.connect(address, remotePort);
      }

      sendTCP(ByteBuffer.allocate(4).putInt(this.socket.getLocalPort()).array(), new Boolean[]{true,true,false});
    }
    // We are the origianl sender ... send a final ACK to complete handshake
    else{
      sendACK();
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
      sendACK();
      // TODO: wait for 16x average packet time
      // TODO: create a new thread that waits for time and closes this thread and itself
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
  void sendACK(){
    seqNum += 1;
    sendTCP(new byte[0], new Boolean[]{false,true,false});
  }

  void sendFIN(){
    seqNum += 1;
    sendTCP(new byte[0], new Boolean[]{false,true,true});
  }

  void sendTCP(byte[] data, Boolean[] flags) {
    long time = System.nanoTime();
    TCPpacket tcpPacket = new TCPpacket(seqNum, ackNum, time, flags, data);
    byte[] tcpData = tcpPacket.packetToBytes();

    DatagramPacket packet = new DatagramPacket(tcpData, tcpData.length, ip, remotePort);

    try{
      this.socket.send(packet);
    }catch(IOException e){
      System.out.println("Failure to send packet");
    }
    printPacket(tcpPacket, "snd");

    // Increment sequence after sent packet
    seqNum += data.length;

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