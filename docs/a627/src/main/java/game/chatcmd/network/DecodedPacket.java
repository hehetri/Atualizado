package game.chatcmd.network;

public record DecodedPacket(String opcode, int payloadLength, byte[] payload) {
}
