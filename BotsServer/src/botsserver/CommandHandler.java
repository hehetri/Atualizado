package botsserver;

public interface CommandHandler {
    void handle(BotClass session, PacketReader packetReader);
}
