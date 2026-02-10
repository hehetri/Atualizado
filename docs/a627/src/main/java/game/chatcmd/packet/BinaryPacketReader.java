package game.chatcmd.packet;

import java.nio.charset.Charset;

public class BinaryPacketReader implements PacketReader {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final byte[] payload;
    private int cursor;

    public BinaryPacketReader(byte[] payload) {
        this.payload = payload == null ? new byte[0] : payload;
    }

    @Override
    public String readString() {
        int start = cursor;
        while (cursor < payload.length && payload[cursor] != 0x00) {
            cursor++;
        }
        String value = new String(payload, start, cursor - start, WINDOWS_1252);
        if (cursor < payload.length && payload[cursor] == 0x00) {
            cursor++;
        }
        return value;
    }
}
