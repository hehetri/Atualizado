package game.chatcmd.network;

import java.util.Arrays;
import java.util.Locale;

public class PacketDecoder {
    private static final int XOR_KEY = 0xED;

    public DecodedPacket decode(byte[] incoming) {
        if (incoming == null || incoming.length < 4) {
            throw new IllegalArgumentException("packet too small");
        }

        byte[] decrypted = Arrays.copyOf(incoming, incoming.length);
        for (int i = 0; i < decrypted.length; i++) {
            decrypted[i] = (byte) (decrypted[i] ^ XOR_KEY);
        }

        int opcodeValue = (decrypted[0] & 0xFF) | ((decrypted[1] & 0xFF) << 8);
        int payloadLength = (decrypted[2] & 0xFF) | ((decrypted[3] & 0xFF) << 8);
        if (payloadLength < 0 || 4 + payloadLength > decrypted.length) {
            throw new IllegalArgumentException("invalid payload length");
        }

        byte[] payload = Arrays.copyOfRange(decrypted, 4, 4 + payloadLength);
        String opcode = String.format(Locale.ROOT, "%04x", opcodeValue);
        return new DecodedPacket(opcode, payloadLength, payload);
    }
}
