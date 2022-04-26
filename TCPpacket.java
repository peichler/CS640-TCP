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

  public byte[] packetToBytes(){
    return buff.array();
  }

  public boolean isChecksumValid(byte[] data){
    deserialize(data);

    short oldCheckSum = this.checksum;
    this.checksum = 0;

    serialize();

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

        int flagTotal = 0;
        if(this.flags[0]) {
          flagTotal += 32;
        }
        if(this.flags[1]) {
          flagTotal += 64;
        }
        if(this.flags[2]) {
          flagTotal += 128;
        }

        byte[] lengthFlags = new byte[] {
          (byte)((payloadLength & 0xFF000000) >> 24 + flagTotal),
          (byte)((payloadLength & 0x00FF0000) >> 16),
          (byte)((payloadLength & 0x0000FF00) >> 8),
          (byte)(payloadLength & 0x000000FF)
        };

        bb.put(lengthFlags);
        bb.putShort((short)0);
        bb.putShort(this.checksum)
        if(payloadLength != 0) {
          bb.put(this.payloadData)
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;
            for (int i = 0; i < this.headerLength / 2; i++) {
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
        byte[] lengthFlags = new byte[4];

        this.seqNum = bb.getInt();
        this.ackNum = bb.getInt();
        this.time = bb.getLong();

        bb.getInt(lengthFlags);

        int flags = (int)lengthFlags[0];

        if (flags >= 128) {
          flags -= 128;
          this.flags[2] = true;
        }
        if (flags >= 64) {
          flags -= 64;
          this.flags[1] = true;
        }
        if (flags >= 32) {
          flags -= 32;
          this.flags[0] = true;
        }

        int payloadLength = flags << 24 + (int)lengthFlags[1] << 16 + (int)lengthFlags[2] << 8 (int)lengthFlags[3];

        bb.getShort();
        this.checksum = bb.getShort();

        // just a check, delete later
        if(bb.remaining() != payloadLength) {
          System.out.println("Payload length is wrong")
        }

        this.payloadData = new byte[payloadLength];
        bb.get(payloadData);

        return this;
    }
}