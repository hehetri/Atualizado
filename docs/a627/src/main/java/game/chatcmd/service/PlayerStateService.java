package game.chatcmd.service;

import game.chatcmd.model.PlayerCharacter;
import game.chatcmd.model.Room;
import game.chatcmd.model.Session;

public interface PlayerStateService {
    boolean isAdmin(PlayerCharacter character);

    boolean isRoomMaster(Room room, Session session);

    boolean toggleAutosell(PlayerCharacter character);
}
