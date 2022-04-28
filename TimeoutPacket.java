// Individual timeout thread for each packet sent
// Objects are destroyed when ACK is received for this packet
public class TimeoutPacket extends Thread{

	private int curRetrans;
	private TimeoutManager toMan;
	private byte[] data;
	private long startTime;
	private int dataAck;

	public TimeoutPacket(TimeoutManager toMan, byte[] data, int dataAck){
		this.toMan = toMan;
		this.curRetrans = 0;
		this.data = data;
		this.startTime = System.nanoTime();
		this.dataAck = dataAck;
	}

	// Thread for timeout wait
	public void run(){
		while(true) {

			double timeDiff = (double)(System.nanoTime() - this.startTime);

			if(timeDiff >= toMan.getTimeout()) {
				this.startTime = System.nanoTime();
				toMan.resendPacket(data);
				curRetrans += 1;
				if(curRetrans >= 16){
					// TODO: end program with error
					System.out.println("Reached maxiumum retransmits! Stop");
					return;
				}
			} else if (this.dataAck <= toMan.getBase().getLastRecAck()){
				return;
			}
		}	
	}
}