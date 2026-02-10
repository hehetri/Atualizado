package botsserver;

public interface RoomService {
    Room getRoom(BotClass session);
    void removeSlot(Room room, int slot, int reason);
    void gameEnd(Room room, int status);
    boolean playerDeath(BotClass session, Room room);
}
