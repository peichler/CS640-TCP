public class DelayedClose extends Thread{
  private TCPbase tcp;
  private long time;

  public DelayedClose(TCPbase tcp, long time){
    this.tcp = tcp;
    this.time = time;
  }

  public void run(){
    try{
        Thread.sleep(time);
    }catch(InterruptedException ex){
        Thread.currentThread().interrupt();
    }

    tcp.stopThread();
  }
}