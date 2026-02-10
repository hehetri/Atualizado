package game.chatcmd.service;

import game.chatcmd.model.Room;
import game.chatcmd.model.Session;

public interface ChatService {
    void sendToPlayer(Session session, String message);

    void broadcastRoom(Room room, String message);
}
