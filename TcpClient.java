import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TcpClient
{
    private TcpSocket client;
    private InetAddress serverAdd;
    private int serverPort,seqNo,buffStart, buffSize,rcwnd;
    private DatagramPacket toServer;
    private TcpPacket pkt;
    private byte[] buff;
    private String out;

    TcpClient()
    {
        boolean singleAck=false;
        client=new TcpSocket(); serverPort=8888; seqNo=0; buffSize=5; rcwnd=5; out=new String("");
        try { serverAdd=InetAddress.getByName("localhost"); } catch (UnknownHostException e) { e.printStackTrace(); }
        toServer=new DatagramPacket(new byte[0],0,serverAdd,serverPort); buff=new byte[buffSize];

        while(true) {
            // 3 way handshake
            client.send(new TcpPacket((byte)0x02,seqNo,0,buffSize,toServer)); // syn bit
            System.out.println("sent syn");
            pkt=client.receiveWithoutWait(); if(pkt==null) continue;
            if(pkt.isSyn() && pkt.isAck() && pkt.getAckNo()==seqNo+1)
            {
                System.out.println("got syn");
                buffStart=pkt.getSeqNo(); sendAck(pkt.getSeqNo());
                System.out.println("Connection established\n"); break;
            }
        }
        if(singleAck) recieveDataSingleAck(); else recieveData();
        System.out.println("\n"+out+"\n\nConnection Closed"); client.close();
    }

    private void sendAck(int ackNo){ seqNo++; client.send(new TcpPacket((byte)0x01,seqNo,ackNo+1,rcwnd,toServer)); System.out.println("sent ack "+(ackNo+1)); }
    private void deliver(char a){ out+=a; }

    private void recieveData()
    {
        boolean[] isRecvd=new boolean[buffSize]; for(int i=0;i<buffSize;i++) isRecvd[i]=false; int off=buffStart;
        while(true)
        {
            pkt=client.receive();
            if(pkt.isFin()){ client.send(new TcpPacket((byte)0x05,seqNo,pkt.getSeqNo()+1,0,toServer)); System.out.println("sent fin"); break; }
            else if(pkt.isSyn()){ sendAck(pkt.getSeqNo()); continue; }

            if(pkt.getSeqNo()>buffStart && pkt.getSeqNo()<buffStart+buffSize) // if packet seq no. in buffer
            {
                int ind=pkt.getSeqNo()-buffStart-1; buff[ind]=pkt.getUdpPacket().getData()[0];
                System.out.println("recvd "+(pkt.getSeqNo()-off)); if(!isRecvd[ind]) rcwnd--; isRecvd[ind]=true;

                while(isRecvd[0]){
                    deliver((char)buff[0]); buffStart++; rcwnd++;
                    System.out.println("buffer forwarded to "+(buffStart-off+1)+"-"+(buffStart+ buffSize -off));
                    for(int i=0;i<buffSize-1;i++){ buff[i]=buff[i+1]; isRecvd[i]=isRecvd[i+1]; }
                    isRecvd[buffSize-1]=false;
                }

                if(buffStart==off) sendAck(off); else sendAck(buffStart);
            }
            else if(pkt.getSeqNo()>=buffStart+buffSize){
                System.out.println("advance "+(pkt.getSeqNo()-off)); sendAck(buffStart);
            }
            else if(pkt.getSeqNo()<=buffStart){
                System.out.println("duplicate "+(pkt.getSeqNo()-off));
                sendAck(buffStart);
            }
        }
    }

    private void recieveDataSingleAck()
    {
        boolean[] isRecvd=new boolean[buffSize]; for(int i=0;i<buffSize;i++) isRecvd[i]=false; int off=buffStart;
        while(true)
        {
            pkt=client.receive();
            if(pkt.isFin()){ client.send(new TcpPacket((byte)0x05,seqNo,pkt.getSeqNo()+1,0,toServer)); System.out.println("sent fin"); break; }
            else if(pkt.isSyn()){ sendAck(pkt.getSeqNo()); continue; }

            if(pkt.getSeqNo()>buffStart && pkt.getSeqNo()<buffStart+buffSize) // if packet seq no. in buffer
            {
                int ind=pkt.getSeqNo()-buffStart-1; buff[ind]=pkt.getUdpPacket().getData()[0]; rcwnd--;
                System.out.println("recvd "+(pkt.getSeqNo()-off)); isRecvd[ind]=true; sendAck(pkt.getSeqNo()-1);

                while(isRecvd[0]){
                    deliver((char)buff[0]); buffStart++; rcwnd++;
                    System.out.println("buffer forwarded to "+(buffStart-off+1)+"-"+(buffStart+ buffSize -off));
                    for(int i=0;i<buffSize-1;i++){ buff[i]=buff[i+1]; isRecvd[i]=isRecvd[i+1]; }
                    isRecvd[buffSize -1]=false;
                }
            }
            else if(pkt.getSeqNo()<=buffStart){
                System.out.println("duplicate "+(pkt.getSeqNo()-off));
                sendAck(pkt.getSeqNo()-1);
            }
        }
    }

    public static void main(String[] args) { new TcpClient(); }
}