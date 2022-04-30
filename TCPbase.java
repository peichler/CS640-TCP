import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.IOException;
import java.nio.ByteBuffer;
// import java.util.Random;
import java.text.DecimalFormat;

// Base class for TCP sender and receiver
// Handles sending and receiving TCP packets
public abstract class TCPbase extends Thread{
  private boolean doStop = false;
  
  DatagramSocket socket;
  InetAddress ip;
  int remotePort;

  String fileName;
  int mtu, sws;
  int seqNum;
  int ackNum;
  int lastRecAck;
  int duplicateAckCount = 0;
  long startTime;

  volatile boolean established = false;
  boolean initiatedClose;
  boolean waitingForClose;
  boolean syn_rec;
  boolean timeWait;

  TimeoutManager toMan;

  // Data for stats
  int dataTransfered, packetsSent, packetsReceived, outOfSeqPackets, incorrectChecksumNum, retransNum, duplicateAcksNum;

  public TCPbase(int port, String fileName, int mtu, int sws){
    this.fileName = fileName;
    this.mtu = mtu;
    this.sws = sws;
    this.startTime = System.nanoTime();
    toMan = new TimeoutManager(this);

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
    packetsReceived += 1;
    TCPpacket tcpPacket = new TCPpacket();

    // Incorrect checksum ... drop packet
    if(tcpPacket.isChecksumValid(packet.getData()) == false){
      System.out.println("Incorrect checksum ... dropping packet");
      incorrectChecksumNum += 1;
      return;
    }

    printPacket(tcpPacket, "rcv");   

    // Packet has ACK flag ... update our latest received ACK
    if(tcpPacket.isAck()){
      handleAckPacket(tcpPacket);
    }

    // Packet has SYN flag ... follow handshake connection procedure
    if(tcpPacket.isSyn()){
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
    // Assumed to be only an ACK flag ... update our ACK number if any number bigger regardless
    else{
      updateAckNumForAck(tcpPacket);
    }
  }

  void updateAckNum(TCPpacket tcpPacket){
    if(tcpPacket.seqNum == ackNum)
      ackNum = tcpPacket.seqNum + Math.max(tcpPacket.payloadData.length, 1);
  }

  void updateAckNumForAck(TCPpacket tcpPacket){
    if(tcpPacket.seqNum >= ackNum)
      ackNum = tcpPacket.seqNum + Math.max(tcpPacket.payloadData.length, 1);
  }

  void handleSynPacket(TCPpacket tcpPacket, DatagramPacket packet){
    // No ACK flag ... we are receiver ... check if we are receiving syn for first time
    if(tcpPacket.isAck() == false){
      if(syn_rec == false){
        ackNum = tcpPacket.seqNum + 1;
        sendTCP(new byte[0], new Boolean[]{true,false,true});
        syn_rec = true;
      }
    }
    // We are the origianl sender ... send a final ACK to complete handshake
    else{
      ackNum = tcpPacket.seqNum + 1;
      if(established)
        resendACKtoSYN();
      else{
        sendACK(tcpPacket);
        established = true;
      }
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
      if(timeWait){
        sendACK(tcpPacket);
        DelayedClose delay = new DelayedClose(this, (int)toMan.getTimeout()/1000000 * 16);
        delay.start();
        timeWait = true;
      }
    }
  }

  void handleAckPacket(TCPpacket tcpPacket){
    if(tcpPacket.isFin() == false && waitingForClose){
      stopThread();
      return;
    }

    if(tcpPacket.ackNum > lastRecAck){
      lastRecAck = tcpPacket.ackNum;
      duplicateAckCount = 0;
      toMan.updateTimeout(tcpPacket);
      toMan.removePacket(tcpPacket.ackNum);
    }
    else if(tcpPacket.ackNum == lastRecAck){
      duplicateAckCount += 1;
      duplicateAcksNum += 1;
      if(duplicateAckCount == 3)
        handleTripleAck(tcpPacket);
    }
  }

  void handleTripleAck(TCPpacket tcpPacket){
    toMan.resendPacket(tcpPacket);
  }

  abstract void handlePacket(TCPpacket packet);

  ///////// Sending data /////////
  void sendACK(TCPpacket origPacket){
    sendTCP(new byte[0], new Boolean[]{false,false,true}, origPacket.time);
  }

  void resendACKtoSYN(){
    long time = System.nanoTime();
    TCPpacket tcpPacket = new TCPpacket(1, ackNum, time, new Boolean[]{false,false,true}, new byte[0]);
    sendTCP(tcpPacket);
  }

  void sendFIN(){
    sendTCP(new byte[0], new Boolean[]{false,true,true});
  }

  void sendTCP(byte[] data, Boolean[] flags) {
    sendTCP(data, flags, System.nanoTime());
  }

  void sendTCP(byte[] data, Boolean[] flags, long time) {
    TCPpacket tcpPacket = new TCPpacket(seqNum, ackNum, time, flags, data);
    sendTCP(tcpPacket);

    // Check if packet is required to be ACK back ... if so add to list to make sure it gets transmitted
    if(tcpPacket.isSyn() || tcpPacket.isFin() || tcpPacket.payloadData.length > 0){
      // toMan.startPacketTimer(tcpPacket, seqNum + Math.max(tcpPacket.payloadData.length, 1));
      toMan.startPacketTimer(tcpPacket);
    }

    // Increment sequence after sent packet
    seqNum += Math.max(data.length, 1);
  }

  synchronized void sendTCP(TCPpacket tcpPacket){
    byte[] tcpData = tcpPacket.serialize();

    DatagramPacket packet = new DatagramPacket(tcpData, tcpData.length, ip, remotePort);

    printPacket(tcpPacket, "snd");

    try{
      this.socket.send(packet);
      packetsSent += 1;
    }catch(IOException e){
      System.out.println("Failure to send packet");
    }
  }

  public void resendTCP(TCPpacket tcpPacket){
    tcpPacket.ackNum = ackNum;
    tcpPacket.time = System.nanoTime();
    retransNum += 1;

    sendTCP(tcpPacket);
  }

  void printPacket(TCPpacket tcpPacket, String tType){
    long time = (System.nanoTime() - startTime) /((long)1e6);
    DecimalFormat df = new DecimalFormat("##.###");

    System.out.println(tType + " " + df.format((double)time/1e3) + " " + (tcpPacket.isSyn() ? "S " : "- ") + (tcpPacket.isAck() ? "A " : "- ") + 
      (tcpPacket.isFin() ? "F " : "- ") + (tcpPacket.payloadData.length > 0 ? "D " : "- ") + tcpPacket.seqNum + 
      " " + tcpPacket.payloadData.length + " " + tcpPacket.ackNum);
  }

  public synchronized void stopThread(){
    this.doStop = true;
    disconnect();
    established = true;
    printStats();
  }

  protected synchronized boolean running(){
    return this.doStop == false;
  }

  public void disconnect(){
    socket.close();
    toMan.clearAllPackets();
  }

  public int getLastRecAck(){
    return lastRecAck;
  }

  void printStats(){
    System.out.println("======================================");
    System.out.println("Data transfered: " + dataTransfered);
    System.out.println("Packets sent/received: " + packetsSent + "/" + packetsReceived);
    System.out.println("Out of sequence packets: " + outOfSeqPackets);
    System.out.println("Packets discarded due to incorrect checksum: " + incorrectChecksumNum);
    System.out.println("Packets retransmited: " + retransNum);
    System.out.println("Duplicate ACKs: " + duplicateAcksNum);
    System.out.println("======================================");

  }
}
