package game.chatcmd.service;

import game.chatcmd.model.Room;
import game.chatcmd.model.Session;

public interface RoomService {
    Room getRoom(Session session);

    Session findByName(Room room, String playerName);

    void removeSlot(Room room, Session target, int reason);

    void gameEnd(Room room, int status);

    boolean playerDeath(Room room, Session player);
}
