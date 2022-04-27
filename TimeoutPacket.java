// Individual timeout thread for each packet sent
// Objects are destroyed when ACK is received for this packet
public class TimeoutPacket extends Thread{
	// Number of retransmits
	public int curRetrans = 0;
	public TCPpacket tcpPacket;

	float timeoutTime

	public TimeoutPacket(TCPpacket tcpPacket, float timeoutTime){
		this.tcpPacket = tcpPacket;
		this.timeoutTime = timeoutTime;
	}

	// Thread for timeout wait
	public void run(){
		// TODO: wait amount of time
		// Thread.wait(time);

		
		curRetrans += 1;
		// Check if retransmit count is greater than 16 ... if so we have reached
		// maximum number of times ... so end program with error
		if(curRetrans >= 16){
			// TODO: end program with error
			System.out.println("Reached maxiumum retransmits! Stop");
			return;
		}

		// TODO: send retrasmit

		// Start thread again
	}

	// Stops thread object, might need to use interupt
	public void stopThread(){
		// TODO: stop thread
	}
}