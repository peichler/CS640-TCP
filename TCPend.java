import java.net.InetAddress;

public class TCPend {
  public static void main(String[] args){
    // java TCPend -p <port> -s <remote IP> -a <remote port> –f <file name> -m <mtu> -c <sws>
    // java TCPend -p 6000 -s x.x.x.x -a 5000 -f original.txt -m 200 -c 4

    // java TCPend -p <port> -m <mtu> -c <sws> -f <file name>
    // java TCPend -p 5000 -m 200 -c 4 -f test.txt

    if(args.length == 12 && args[2].equals("-s")) {
      createSender(args);
    } else if(args.length == 8) {
      createReceiver(args);
    }else {
      System.out.println("Error: missing or additional arguments");
    }
  }

  static void createSender(String[] args){
    int port = 0;
    try{
      port = stringToPort(args[1]);
    }
    catch (NumberFormatException ex){
      System.out.println("Error: port is not a number");
      return;
    }

    InetAddress ip = null;
    try {
      ip = InetAddress.getByName(args[3]);
    }catch(Exception e) {
      System.out.println("Error: incorrect IP format");
    }

    int remotePort = 0;
    try{
      remotePort = stringToPort(args[5]);
    }
    catch (NumberFormatException ex){
      System.out.println("Error: port is not a number");
      return;
    }

    String fileName = args[7];

    int mtu = 0;
    try{
      mtu = Integer.parseInt(args[9]);
      if(mtu <= 0) {
        System.out.println("Error: mtu can't be negative");
        return;
      }
    }
    catch (NumberFormatException ex){
      System.out.println("Error: mtu is not a valid number");
      return;
    }

    int sws = 0;
    try{
      sws = Integer.parseInt(args[11]);
      if(sws <= 0) {
        System.out.println("Error: sws can't be negative");
        return;
      }
    }
    catch (NumberFormatException ex){
      System.out.println("Error: sws is not a valid number");
      return;
    }

    // Create tcp sender and start it
    TCPsender sender = new TCPsender(port, ip, remotePort, fileName, mtu, sws);
    sender.start();
    sender.sendFile();
  }

  static void createReceiver(String[] args){
    int port = 0;
    try{
      port = stringToPort(args[1]);
    }
    catch (NumberFormatException ex){
      System.out.println("Error: port is not a number");
      return;
    }
    
    int mtu = 0;
    try{
      mtu = Integer.parseInt(args[3]);
      if(mtu <= 0) {
        System.out.println("Error: mtu can't be negative");
        return;
      }
    }
    catch (NumberFormatException ex){
      System.out.println("Error: mtu is not a valid number");
      return;
    }

    int sws = 0;
    try{
      sws = Integer.parseInt(args[5]);
      if(sws <= 0) {
        System.out.println("Error: sws can't be negative");
        return;
      }
    }
    catch (NumberFormatException ex){
      System.out.println("Error: sws is not a valid number");
      return;
    }

    String fileName = args[7];
    
    // Create tcp receiver and start it
    TCPreceiver receiver = new TCPreceiver(port, mtu, sws, fileName);
    receiver.start();
  }

  public static int stringToPort(String p) {
    int port = Integer.parseInt(p);
    if(port < 1 || port > 65535) {
      throw new NumberFormatException();
    }
    return port;
  }
}