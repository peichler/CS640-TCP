import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TCPpacket{
  public static final int headerSize = 24;

  public int seqNum, ackNum;
  public long time;
  public Boolean[] flags = new Boolean[3];
  public byte[] payloadData;

  short checksum;

  // Create packet from variables
  public TCPpacket(int seqNum, int ackNum, long time, Boolean[] flags, byte[] payloadData){
    this.seqNum = seqNum;
    this.ackNum = ackNum;
    this.time = time;
    this.flags = flags;
    this.payloadData = payloadData;
    this.checksum = 0;
  }

  public TCPpacket(){}

  public boolean isChecksumValid(byte[] data){
    this.deserialize(data);

    short oldCheckSum = this.checksum;
    this.checksum = 0;

    this.serialize();

    return oldCheckSum == this.checksum;
  }

  short getChecksum(byte[] data){
    //TODO: complete checksum
    return (short)0;
  }

  public boolean isSyn(){
    return flags[0];
  }

  public boolean isFin(){
    return flags[1];
  }

  public boolean isAck(){
    return flags[2];
  }

  public byte[] serialize() {
        int payloadLength = 0;
        if(payloadData != null) {
          payloadLength = this.payloadData.length;
        }
        int totalLength = headerSize + payloadLength;

        byte[] data = new byte[totalLength];
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(this.seqNum);
        bb.putInt(this.ackNum);
        bb.putLong(this.time);

        int sizeWithFlags = payloadLength;
        for (int i=0; i<3; i++) {
          sizeWithFlags = sizeWithFlags << 1;
          sizeWithFlags += flags[i] ? 1 : 0;
        }

        bb.putInt(sizeWithFlags);
        bb.putShort((short)0);
        bb.putShort(this.checksum);
        if(payloadLength != 0) {
          bb.put(this.payloadData);
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;
            // TODO: check if changing to headerSize is correct ... was headerLength before
            for (int i = 0; i < this.headerSize / 2; i++) {
                accumulation += 0xffff & bb.getShort();
            }
            accumulation = ((accumulation >> 16) & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(22, this.checksum);
        }

        return data;
    }

    public TCPpacket deserialize(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);

        this.seqNum = bb.getInt();
        this.ackNum = bb.getInt();
        this.time = bb.getLong();

        int sizeWithFlags = bb.getInt();
    
        int payloadLength = sizeWithFlags >> 3;
        if(payloadLength > data.length - headerSize){
          System.out.println("Incorrect data length");
          return null;
        }

        for (int i = 0; i<3; i++) {
          flags[2-i] = sizeWithFlags % 2 == 1;
          sizeWithFlags = sizeWithFlags >> 1;
        }

        bb.getShort();
        this.checksum = bb.getShort();

        // just a check, delete later
        // if(bb.remaining() != payloadLength) {
        //   System.out.println("Payload length is wrong");
        // }

        this.payloadData = new byte[payloadLength];
        bb.get(payloadData);

        return this;
    }
}