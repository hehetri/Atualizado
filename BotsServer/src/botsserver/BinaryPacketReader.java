package botsserver;

import java.nio.charset.Charset;

public class BinaryPacketReader implements PacketReader {
    private static final Charset PACKET_CHARSET = Charset.forName("windows-1252");
    private final byte[] payload;
    private int offset;

    public BinaryPacketReader(byte[] payload) {
        this.payload = payload == null ? new byte[0] : payload;
        this.offset = 0;
    }

    @Override
    public String readString() {
        if (offset >= payload.length)
            return "";
        int start = offset;
        while (offset < payload.length && payload[offset] != 0x00)
            offset++;
        String out = new String(payload, start, offset - start, PACKET_CHARSET);
        if (offset < payload.length && payload[offset] == 0x00)
            offset++;
        return out;
    }
}
