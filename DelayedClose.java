public class DelayedClose extends Thread{
  private TCPbase tcp;
  private long time;

  public DelayedClose(TCPbase tcp, long time){
    this.tcp = tcp;
    this.time = time;
  }

  public void run(){
    // System.out.println("Closing in " + time + " ms");
    try{
        Thread.sleep(time);
    }catch(InterruptedException ex){
        Thread.currentThread().interrupt();
    }

    tcp.stopThread();
  }
}