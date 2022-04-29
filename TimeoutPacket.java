import java.util.TimerTask;

// Individual timeout thread for each packet sent
// Objects are destroyed when ACK is received for this packet
public class TimeoutPacket extends TimerTask{

	public int curRetrans;
	public TCPpacket tcpPacket;
	private TimeoutManager toMan;

	public TimeoutPacket(TimeoutManager toMan, TCPpacket tcpPacket, int curRetrans){
		this.toMan = toMan;
		this.tcpPacket = tcpPacket;
		this.curRetrans = curRetrans;
	}

	// Thread for timeout wait
	public void run(){
		curRetrans += 1;
		toMan.resendPacket(this);
	}
}