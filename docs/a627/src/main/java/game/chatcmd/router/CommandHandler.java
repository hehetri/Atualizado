package game.chatcmd.router;

import game.chatcmd.model.Session;
import game.chatcmd.packet.PacketReader;

public interface CommandHandler {
    void handle(Session session, PacketReader packet);
}
