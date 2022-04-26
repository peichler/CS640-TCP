import java.nio.ByteBuffer;

public class TCPpacket{
  public static final int headerSize = 24;
  private ByteBuffer buff;

  public int seqNum, ackNum, dataSize;
  public long time;
  public Boolean[] flags = new Boolean[3];
  public byte[] data;

  short checksum;

  public TCPpacket(byte[] packet){
    if(packet.length < headerSize)
      return;

    buff = ByteBuffer.wrap(packet);

    seqNum = buff.getInt();
    ackNum = buff.getInt();
    time = buff.getLong();
    
    int sizeWithFlags = buff.getInt();
    
    dataSize = sizeWithFlags >> 3;
    if(dataSize > packet.length - headerSize){
      System.out.println("Incorrect data length");
      return;
    }

    for (int i = 0; i<3; i++) {
      flags[2-i] = sizeWithFlags % 2 == 1;
      sizeWithFlags = sizeWithFlags >> 1;
    }

    buff.getShort();
    checksum = buff.getShort();

    data = new byte[dataSize];
    buff.get(data, 0, data.length);
  }

  public TCPpacket(int seqNum, int ackNum, long time, Boolean[] flags, byte[] data){
    buff = ByteBuffer.allocate(data.length + headerSize);

    buff.putInt(seqNum);
    buff.putInt(ackNum);
    buff.putLong(time);
    
    int sizeWithFlags = data.length;
    for (int i=0; i<3; i++) {
      sizeWithFlags = sizeWithFlags << 1;
      sizeWithFlags += flags[i] ? 1 : 0;
    }

    buff.putInt(sizeWithFlags);
    buff.putShort((short)0);
    buff.putShort(getChecksum(data));

    buff.put(data);
  }

  public byte[] packetToBytes(){
    return buff.array();
  }

  public boolean isChecksumValid(){
    //TODO: complete checksum validation
    return true;
  }

  short getChecksum(byte[] data){
    //TODO: complete checksum
    return (short)0;
  }

  public boolean isSyn(){
    return flags[0];
  }

  public boolean isAck(){
    return flags[1];
  }

  public boolean isFin(){
    return flags[2];
  }
}