package game.chatcmd.model;

import java.util.HashMap;
import java.util.Map;

public class Room {
    public static final int STAT_SPEED = 0x01;
    public static final int STAT_ATT_TRANS_GAUGE = 0x02;

    private final String id;
    private final RoomMode mode;
    private final Session master;
    private int status;
    private final Map<Integer, Integer> statOverride = new HashMap<>();

    public Room(String id, RoomMode mode, Session master, int status) {
        this.id = id;
        this.mode = mode;
        this.master = master;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public RoomMode getMode() {
        return mode;
    }

    public Session getMaster() {
        return master;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<Integer, Integer> getStatOverride() {
        return statOverride;
    }
}
