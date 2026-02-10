package botsserver;

public interface ChatService {
    void sendToPlayer(BotClass session, String message);
    void broadcastRoom(Room room, String message);
}
