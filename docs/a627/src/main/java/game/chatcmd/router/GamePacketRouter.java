package game.chatcmd.router;

import game.chatcmd.model.Session;
import game.chatcmd.packet.PacketReader;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GamePacketRouter {
    private static final Logger LOGGER = Logger.getLogger(GamePacketRouter.class.getName());

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public void register(String opcode, CommandHandler handler) {
        handlers.put(opcode, handler);
    }

    public void route(Session session, String opcode, int payloadLength, PacketReader packetReader) {
        CommandHandler handler = handlers.get(opcode);
        if (handler == null) {
            LOGGER.warning(() -> "unknown packet opcode=" + opcode + " size=" + payloadLength);
            return;
        }
        handler.handle(session, packetReader);
    }
}
