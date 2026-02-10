package game.chatcmd.model;

public class PlayerCharacter {
    private final String name;
    private final int position;
    private final PlayerSettings settings = new PlayerSettings();

    public PlayerCharacter(String name, int position) {
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public PlayerSettings getSettings() {
        return settings;
    }
}
