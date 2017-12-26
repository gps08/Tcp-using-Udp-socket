import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Data {

    private byte[] data;
    public Data()
    {
        try { data = Files.readAllBytes(Paths.get("data")); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public byte getByte(int i) { return data[i]; }
    public int getSize(){ return data.length; }
}
