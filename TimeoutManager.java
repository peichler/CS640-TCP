import java.util.Queue;

// Manages timeout packets by adding and removing stored timeout packets
public class TimeoutManager{
	// Could be proirity queue based on sequence number
	private Queue<TimeoutPacket> packetQueue = new Queue<TimeoutPacket>();

	// Creates a timeout packet and starts thread
	// Adds timeout packet to queue
	public synchronized void addPacket(TCPpacket tcpPacket){
		// TODO: get timeout time for packet
		TimeoutPacket packet = new TimeoutPacket(tcpPacket, 10);
		packetQueue.enqueue(packet);
		packet.start();
	}

	// Removes all packets based on ack number received
	public synchronized void removePacket(int ackNum){
		// TODO: find all tcp packets with seqNum below or equal to ackNum
		// Stop thread and remove from queue
	}
}