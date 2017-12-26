import java.net.DatagramPacket;
import java.util.Arrays;

// fin syn ack-->lsb  seqNo ackNo rcwnd
public class TcpPacket {
    private DatagramPacket udpPacket;
    private boolean ack,syn,fin;
    private int seqNo,ackNo,rcwnd;

    public TcpPacket(DatagramPacket udpPacket){ this.udpPacket=udpPacket; }
    public TcpPacket(byte flags,int seqNo,int ackNo,int rcwnd,DatagramPacket udpPacket) // packs the packet
    {
        byte[] result = new byte[13+udpPacket.getData().length]; result[0]=flags;
        byte[] a=new byte[]{(byte)(seqNo>>24),(byte)(seqNo>>16),(byte)(seqNo>>8),(byte)seqNo};
        byte[] b=new byte[]{(byte)(ackNo>>24),(byte)(ackNo>>16),(byte)(ackNo>>8),(byte)ackNo};
        byte[] c=new byte[]{(byte)(rcwnd>>24),(byte)(rcwnd>>16),(byte)(rcwnd>>8),(byte)rcwnd};

        System.arraycopy(a, 0, result, 1, 4);
        System.arraycopy(b, 0, result, 5, 4);
        System.arraycopy(c, 0, result, 9, 4);
        System.arraycopy(udpPacket.getData(), 0, result, 13, udpPacket.getData().length);

        ack=((flags&0x01)==0x01); syn=(((flags>>1)&0x01)==0x01); fin=(((flags>>2)&0x01)==0x01);
        this.ackNo=ackNo; this.seqNo=seqNo; this.rcwnd=rcwnd;
        this.udpPacket=new DatagramPacket(result,result.length,udpPacket.getAddress(),udpPacket.getPort());
    }

    public void unPack()
    {
        byte[] byteArr=udpPacket.getData();

        ack=((byteArr[0]&0x01)==0x01);
        syn=(((byteArr[0]>>1)&0x01)==0x01);
        fin=(((byteArr[0]>>2)&0x01)==0x01);

        seqNo=(byteArr[1]<<24)&0xff000000|(byteArr[2]<<16)&0x00ff0000|(byteArr[3]<<8)&0x0000ff00|(byteArr[4]<<0)&0x000000ff;
        ackNo=(byteArr[5]<<24)&0xff000000|(byteArr[6]<<16)&0x00ff0000|(byteArr[7]<<8)&0x0000ff00|(byteArr[8]<<0)&0x000000ff;
        rcwnd=(byteArr[9]<<24)&0xff000000|(byteArr[10]<<16)&0x00ff0000|(byteArr[11]<<8)&0x0000ff00|(byteArr[12]<<0)&0x000000ff;

        byte[] newByteArr = new byte[1]; newByteArr[0]=byteArr[13];
        if(udpPacket.getPort()!=-1) this.udpPacket=new DatagramPacket(newByteArr,newByteArr.length,udpPacket.getAddress(),udpPacket.getPort());
        else this.udpPacket=new DatagramPacket(newByteArr,newByteArr.length);
    }

    public DatagramPacket getUdpPacket() { return udpPacket; }
    public String getAddress(){ return udpPacket.getAddress().toString(); }
    public boolean isAck(){ return ack; }
    public boolean isSyn(){ return syn; }
    public boolean isFin(){ return fin; }
    public int getSeqNo(){ return seqNo; }
    public int getAckNo(){ return ackNo; }
    public int getRcwnd(){ return rcwnd; }
}