import java.net.DatagramPacket;
import java.net.InetAddress;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class TcpServer
{
    private TcpSocket server; private TcpPacket pkt;
    private int seqNo,clientPort,winSize,thresh; private Data toSend;
    private InetAddress clientAdd; private DatagramPacket toClient;
    private long RTT,timeout; private long[] keepTime; private boolean[] hasReached;

    TcpServer()
    {
        boolean singleAck=false;
        System.out.println("Server Started"); RTT=500;
        server=new TcpSocket(8888); seqNo=0;

        // 3 way handshake
        do{ pkt = server.receive(); } while(!pkt.isSyn());
        System.out.println("Connection request by "+pkt.getAddress()); thresh=pkt.getRcwnd();
        clientAdd=pkt.getUdpPacket().getAddress(); clientPort=pkt.getUdpPacket().getPort();
        toClient=new DatagramPacket(new byte[0],0,clientAdd,clientPort);
        int temp=pkt.getSeqNo();
        do {
            server.send(new TcpPacket((byte) 0x03,seqNo, temp+1,0,toClient));
            System.out.println("sent syn to "+clientAdd);
            pkt=server.receiveWithoutWait();
        }while(pkt==null||(!pkt.isAck())||(pkt.getAckNo()!=seqNo+1));

        toSend=new Data(); hasReached=new boolean[toSend.getSize()];
        keepTime=new long[toSend.getSize()]; timeout=3*RTT; winSize=pkt.getRcwnd();
        for(int i=0;i<toSend.getSize();i++){ hasReached[i]=false; keepTime[i]=0; }

        System.out.println("Connection Established");
        if(pkt.isAck() && pkt.getAckNo()==seqNo+1){
            if(singleAck) startConnSingleAck(); else startConn();
        }
    }

    private void sendByte(int i,boolean fl){
        byte[] temp=new byte[1]; temp[0]=toSend.getByte(i);
        if(fl) System.out.println("resent "+(i+1)); else System.out.println("sent "+(i+1));
        server.send(new TcpPacket((byte)0x00, seqNo+i+1, 0,0, new DatagramPacket(temp,temp.length,clientAdd,clientPort)));
    }

    private void closeConn()
    {
        System.out.println();
        while(true)
        {
            server.send(new TcpPacket((byte) 0x04,seqNo+toSend.getSize()+1, 0,0,toClient));
            System.out.println("sent fin"); pkt=server.receiveWithoutWait();
            if(pkt==null) continue;

            //System.out.println(pkt.isAck()+" "+pkt.isFin()+" "+(pkt.getAckNo()-off-toSend.getSize()-2));
            if(pkt.isFin() && pkt.isAck() && pkt.getAckNo()==seqNo+toSend.getSize()+2){
                System.out.println("Connection Closed");
                server.close(); break;
            }
        }
    }

    private void startConnSingleAck()
    {
        int winStart=0,sent=0,off=seqNo+1; winSize=5;
        while(sent!=toSend.getSize())
        {
            for(int i=winStart;i<winStart+winSize && i<toSend.getSize();i++)
                if((!hasReached[i])||(keepTime[i]!=0 && System.currentTimeMillis()-keepTime[i]>timeout)) {
                    sendByte(i,keepTime[i]!=0); keepTime[i]=System.currentTimeMillis();
                }

            while(true)
            {
                pkt=server.receiveWithoutWait();
                if(pkt!=null && pkt.isAck())
                {
                    if(hasReached[pkt.getAckNo()-off]==false) sent++;
                    hasReached[pkt.getAckNo()-off]=true;
                    System.out.println("ack "+(pkt.getAckNo()-off+1));
                    while (winStart+winSize<toSend.getSize() && hasReached[winStart])
                    {
                        winStart++; System.out.println("Window forwarded to "+(winStart+1)+"-"+(winStart+winSize));
                        if (winStart+winSize<toSend.getSize()) { // advances window and send the next pkt
                            sendByte(winStart+winSize-1,false);
                            keepTime[winStart+winSize-1]=System.currentTimeMillis();
                        }
                    }
                }else break;
            }
        }
        closeConn();
    }

    private void startConn()
    {
        int lastUnacked=0,winStart=0,off=seqNo+1,lastAck=-1,sameAcks=0; winSize=1;
        for(int i=winStart;i<winStart+winSize&&i<toSend.getSize();i++) {
            sendByte(i,false); keepTime[i]=System.currentTimeMillis();
        }

        while(true)
        {
            pkt=server.receiveWithoutWait();
            if(pkt!=null && pkt.isAck())
            {
                if(pkt.getAckNo()-off==toSend.getSize()) break; // data sending complete
                if(pkt.getAckNo()-off<1) continue;

                hasReached[pkt.getAckNo()-off-1]=true;

                // congestion control
                if(lastAck==pkt.getAckNo()) sameAcks++; else sameAcks=0;
                if(sameAcks==3){
                    thresh=winSize/2; winSize=max(1,winSize/2);
                    System.out.println("fast retransmission after 3 acks");
                    for(int i=winStart;i<winStart+winSize && i<toSend.getSize();i++) {
                        sendByte(i,true); keepTime[i]=System.currentTimeMillis();
                    }
                }
                else if(winSize*2<thresh) winSize=min(winSize*2,pkt.getRcwnd());
                else winSize=min(winSize+1,pkt.getRcwnd());
                lastAck=pkt.getAckNo();

                System.out.println("ack "+(pkt.getAckNo()-off+1)+" rcwnd "+pkt.getRcwnd());
                System.out.println("window size "+winSize+" thresh "+thresh);

                lastUnacked=max(lastUnacked,pkt.getAckNo()-off);

                while(winStart+winSize<toSend.getSize() && winStart<pkt.getAckNo()-off)
                {
                    hasReached[winStart]=true; winStart++;
                    System.out.println("Window forwarded to "+(winStart+1)+"-"+(winStart+winSize));
                }

                for(int i=winStart;i<winStart+winSize && i<toSend.getSize();i++) if(keepTime[i]==0){
                    sendByte(i,false); keepTime[i]=System.currentTimeMillis();
                }
            }
            else if(!hasReached[lastUnacked] && System.currentTimeMillis()-keepTime[lastUnacked]>timeout){ //resend previous window
                System.out.println("timeout on "+(lastUnacked+1)); thresh=winSize/2; winSize=1;
                for(int i=winStart;i<winStart+winSize && i<toSend.getSize();i++) {
                    sendByte(i,true); keepTime[i]=System.currentTimeMillis();
                }
            }
        }

        closeConn();
    }

    public static void main(String[] args) { new TcpServer(); }
}