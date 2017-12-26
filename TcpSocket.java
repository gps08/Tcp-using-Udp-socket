import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class TcpSocket {
    private DatagramSocket udpSocket;
    private void setTimeout(int mls){ try { udpSocket.setSoTimeout(mls); } catch (Exception e){ e.printStackTrace(); }}

    public TcpSocket(){ try{ udpSocket=new DatagramSocket(); } catch (Exception e){e.printStackTrace();} }
    public TcpSocket(int port){ try{ udpSocket=new DatagramSocket(port); } catch (Exception e){e.printStackTrace();} }

    public TcpPacket receive()
    {
        byte[] recvBuffer=new byte[14];
        TcpPacket pkt=new TcpPacket(new DatagramPacket(recvBuffer,recvBuffer.length));
        try{ udpSocket.receive(pkt.getUdpPacket()); }
        catch (Exception e) { e.printStackTrace(); }
        pkt.unPack(); return pkt;
    }

    public void send(TcpPacket pkt)
    {
        try{ udpSocket.send(pkt.getUdpPacket()); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public TcpPacket receiveWithoutWait()
    {
        setTimeout(10);
        byte[] recvBuffer=new byte[14];
        TcpPacket pkt=new TcpPacket(new DatagramPacket(recvBuffer,recvBuffer.length));
        try{ udpSocket.receive(pkt.getUdpPacket()); }
        catch (Exception e) { setTimeout(0); return null; }
        pkt.unPack(); setTimeout(0);
        return pkt;
    }

    public void close(){ udpSocket.close(); }
}