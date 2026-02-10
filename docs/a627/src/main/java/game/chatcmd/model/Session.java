package game.chatcmd.model;

public class Session {
    private final String id;
    private final PlayerCharacter character;

    public Session(String id, PlayerCharacter character) {
        this.id = id;
        this.character = character;
    }

    public String getId() {
        return id;
    }

    public PlayerCharacter getCharacter() {
        return character;
    }
}
