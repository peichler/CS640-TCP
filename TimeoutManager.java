import java.util.PriorityQueue;
import java.util.Timer;

// Manages timeout packets by adding and removing stored timeout packets
public class TimeoutManager{
	PriorityQueue<TimeoutPacket> packetBuffer = new PriorityQueue<TimeoutPacket>((p1, p2) -> p1.tcpPacket.seqNum - p2.tcpPacket.seqNum);
	final Timer timer = new Timer();

	TCPbase base;
	long timeout;
  	double ertt;
  	double edev;

	public TimeoutManager(TCPbase base) {
		this.base = base;
		this.timeout = (long)1e9;
    	this.ertt = 0.0;
    	this.edev = 0.0;
	}

	public void updateTimeout(TCPpacket packet) {  
	    if (packet.ackNum == 0) {
	      ertt = (double)(System.nanoTime() - packet.time);
	      edev = 0.0;
	      timeout = (long)(2.0*ertt);
	    } else {
	      double srtt = (double)(System.nanoTime() - packet.time);
	      double sdev = Math.abs(srtt - ertt);
	      ertt = .875*ertt + (1.0-.875)*srtt;
	      edev = .75*edev + (1.0-.75)*sdev;
	      timeout = (long)(ertt + 4*edev);
		 }

		 // System.out.println("errt: ," + (ertt/1e6) + "edev: " + (edev/1e6) + ",new timeout: " + (timeout/1e6));
	 }

	public long getTimeout() {
		return Math.max(timeout, 10000000);
	}

	public void startPacketTimer(TCPpacket tcpPacket){
		startPacketTimer(tcpPacket, 0);
	}

	// Creates a timeout packet and starts thread
	// Adds timeout packet to queue
	public void startPacketTimer(TCPpacket tcpPacket, int curRetrans){
		synchronized(packetBuffer){
			TimeoutPacket toPacket = new TimeoutPacket(this, tcpPacket, curRetrans);
			packetBuffer.add(toPacket);
			timer.schedule(toPacket, (int)(getTimeout()/(long)1e6));
		}
	}
	
	public void resendPacket(TimeoutPacket packet) {
		if(packet.curRetrans >= 16){
			// System.out.println("Over max retransmissions ... quitting program");
			base.stopThread();
			return;
		}

		synchronized(packetBuffer){
			packetBuffer.remove(packet);
			startPacketTimer(packet.tcpPacket, packet.curRetrans);
			base.resendTCP(packet.tcpPacket);
		}
	}

	public void resendPacket(TCPpacket ackPacket){
		synchronized(packetBuffer){
			for(TimeoutPacket packet : packetBuffer){
				if(packet.tcpPacket.seqNum == ackPacket.ackNum){
					resendPacket(packet);
					return;
				}
			}
		}
	}

	// // Removes all packets based on ack number received
	public void removePacket(int ackNum){
		synchronized(packetBuffer){
			while(packetBuffer.size() > 0 && packetBuffer.peek().tcpPacket.getReturnAck() <= ackNum){
				TimeoutPacket toPacket = packetBuffer.poll();
				toPacket.cancel();
			}
			timer.purge();
			packetBuffer.notify();
		}
	}

	public void clearAllPackets(){
		synchronized(packetBuffer){
			timer.cancel();
			packetBuffer.clear();
			packetBuffer.notify();
		}
	}

	public void waitTillPacketsLessThanNum(int num){
		synchronized(packetBuffer){
			try{
				while(packetBuffer.size() > num)
					packetBuffer.wait();
			}catch(InterruptedException ex){
				Thread.currentThread().interrupt();
			}
		}
	}
}