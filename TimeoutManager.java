import java.util.Queue;

// Manages timeout packets by adding and removing stored timeout packets
public class TimeoutManager{
	TCPbase base;
	double timeout;
  	double ertt;
  	double edev;

	public TimeoutManager(TCPbase base) {
		this.base = base;
		this.timeout = 5.0;
    	this.ertt = 0.0;
    	this.edev = 0.0;
	}

	public TCPbase getBase() {
		return base;
	}

	private void updateTimeout(TCPpacket packet) {
	    if (packet.ackNum == 0) {
	      ertt = (System.nanoTime() - packet.time).doubleValue();
	      edev = 0.0;
	      timeout = 2.0*ertt;
	    } else {
	      double srtt = (System.nanoTime() - packet.time).doubleValue();
	      double sdev = Math.abs(srtt - ertt);
	      ertt = .875*ertt + (1.0-.875)*srtt;
	      edev = .75*edev + (1.0-.75)*sdev;
	      timeout = ertt + 4*edev;
		 }
	  }

	public double getTimeout() {
		return timeout;
	}

	// Creates a timeout packet and starts thread
	// Adds timeout packet to queue
	public void startPacketTimer(byte[] data, int dataAck){
		TimeoutPacket toPacket = new TimeoutPacket(this, data, dataAck);
		toPacket.start();
	}

	public void resendPacket(byte[] data) {
		base.sendTCP(data, new boolean[]{false, false, false});
	}

	// // Removes all packets based on ack number received
	// public synchronized void removePacket(int ackNum){
	// 	// TODO: find all tcp packets with seqNum below or equal to ackNum
	// 	// Stop thread and remove from queue
	// }
}