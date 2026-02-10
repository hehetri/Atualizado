package game.chatcmd;

import game.chatcmd.network.DecodedPacket;
import game.chatcmd.network.PacketDecoder;

import java.nio.charset.Charset;

public class PacketDecoderTest {
    private static final Charset CP1252 = Charset.forName("windows-1252");

    public static void main(String[] args) {
        byte[] payload = "@help\0".getBytes(CP1252);
        int len = payload.length;
        byte[] plain = new byte[4 + len];
        plain[0] = 0x27;
        plain[1] = (byte) 0xA6;
        plain[2] = (byte) (len & 0xFF);
        plain[3] = (byte) ((len >> 8) & 0xFF);
        System.arraycopy(payload, 0, plain, 4, len);

        byte[] encrypted = new byte[plain.length];
        for (int i = 0; i < plain.length; i++) encrypted[i] = (byte) (plain[i] ^ 0xED);

        DecodedPacket decoded = new PacketDecoder().decode(encrypted);
        if (!"a627".equals(decoded.opcode())) throw new AssertionError("opcode");
        if (decoded.payloadLength() != len) throw new AssertionError("length");
        System.out.println("PacketDecoderTest passed.");
    }
}
