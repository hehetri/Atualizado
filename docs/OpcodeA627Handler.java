package docs;

import java.util.Locale;
import java.util.Map;

/**
 * Exemplo de parser/handler Java equivalente ao opcode a627 (chat/comandos dentro do jogo).
 * Ajuste as interfaces e integrações conforme a sua base.
 */
public final class OpcodeA627Handler {
    private static final int MODE_PLANET = 2;
    private static final int MODE_BATTLE = 0;
    private static final int MODE_TEAM_BATTLE = 1;
    private static final int MODE_DEATHMATCH = 3;

    private static final int STAT_SPEED = 0x01;
    private static final int STAT_ATT_TRANS_GAUGE = 0x02;

    private static final int COLOR_SYSTEM = 2;
    private static final int COLOR_BROADCAST = 3;

    public void handle(GameContext ctx, PacketReader packet) {
        Room room = ctx.getRoom();
        if (room == null) {
            return;
        }

        String message = packet.readString();
        if (message == null || message.isBlank() || !message.startsWith("@")) {
            return;
        }

        Client client = ctx.getClient();
        String commandLine = message.substring(1).trim();
        if (commandLine.isEmpty()) {
            return;
        }

        String[] parts = commandLine.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {
            case "exit" -> ctx.removeFromRoom(client, room, 0);
            case "win" -> {
                if (client.getCharacterPosition() == 1) {
                    ctx.gameEnd(room, 1);
                }
            }
            case "lose" -> {
                if (client.getCharacterPosition() == 1) {
                    ctx.gameEnd(room, 0);
                }
            }
            case "timeout" -> {
                if (client.getCharacterPosition() == 1) {
                    ctx.gameEnd(room, 2);
                }
            }
            case "timeoutdm" -> {
                if (client.getCharacterPosition() == 1) {
                    ctx.gameEnd(room, 3);
                }
            }
            case "speed", "gauge" -> {
                if (!room.isMaster(client)) {
                    ctx.sendChat(client, "Solo los maestros de la sala pueden cambiar las estadísticas", COLOR_SYSTEM);
                    return;
                }
                if (!room.isMode(MODE_DEATHMATCH, MODE_BATTLE, MODE_TEAM_BATTLE)) {
                    ctx.sendChat(client, "Solo disponible en Deathmatch/Battle/Team Battle", COLOR_SYSTEM);
                    return;
                }
                if (room.getStatus() != 0) {
                    ctx.sendChat(client, "No se puede cambiar después de iniciar la partida", COLOR_SYSTEM);
                    return;
                }
                Integer value = parseRange(arg, 200, 8000);
                if (value == null) {
                    ctx.sendChat(client, "Argumento inválido. Use @speed/@gauge <200-8000>", COLOR_SYSTEM);
                    return;
                }
                int statKey = command.equals("speed") ? STAT_SPEED : STAT_ATT_TRANS_GAUGE;
                room.getStatOverride().put(statKey, value);
                ctx.broadcastRoom(room, String.format("%s cambió %s a %d",
                    client.getName(), command, value), COLOR_BROADCAST);
            }
            case "reset" -> {
                if (!room.isMaster(client)) {
                    ctx.sendChat(client, "Solo los maestros de la sala pueden cambiar las estadísticas", COLOR_SYSTEM);
                    return;
                }
                if (room.getStatus() != 0) {
                    ctx.sendChat(client, "No se puede cambiar después de iniciar la partida", COLOR_SYSTEM);
                    return;
                }
                room.getStatOverride().clear();
                ctx.broadcastRoom(room, String.format("%s restableció todas las estadísticas",
                    client.getName()), COLOR_BROADCAST);
            }
            case "suicide" -> {
                if (room.getStatus() != 3) {
                    return;
                }
                if (room.getGameType() == MODE_PLANET) {
                    boolean ok = ctx.playerDeath(client, room);
                    if (ok) {
                        ctx.sendChat(client, "Has matado a tu personaje", COLOR_SYSTEM);
                    }
                } else {
                    ctx.sendChat(client, "Este comando solo funciona en modo planeta", COLOR_SYSTEM);
                }
            }
            case "kick" -> {
                if (!room.isMaster(client)) {
                    ctx.sendChat(client, "Solo los maestros de la sala pueden expulsar jugadores", COLOR_SYSTEM);
                    return;
                }
                if (arg.isBlank()) {
                    return;
                }
                if (arg.equalsIgnoreCase(client.getName())) {
                    ctx.sendChat(client, "No puedes expulsarte a ti mismo", COLOR_SYSTEM);
                    return;
                }
                Client target = room.findByName(arg);
                if (target == null) {
                    ctx.sendChat(client, "Jugador no encontrado: " + arg, COLOR_SYSTEM);
                    return;
                }
                ctx.removeFromRoom(target, room, 2);
            }
            case "help" -> {
                ctx.sendChat(client, "@help - Lista de comandos", COLOR_SYSTEM);
                ctx.sendChat(client, "@exit, @win, @lose, @timeout, @timeoutdm", COLOR_SYSTEM);
                ctx.sendChat(client, "@speed <n>, @gauge <n>, @reset", COLOR_SYSTEM);
                ctx.sendChat(client, "@kick <nombre>, @suicide, @autosell", COLOR_SYSTEM);
            }
            case "stat-help" -> {
                ctx.sendChat(client, "@speed <n> cambia velocidad", COLOR_SYSTEM);
                ctx.sendChat(client, "@gauge <n> cambia barra", COLOR_SYSTEM);
                ctx.sendChat(client, "@reset restablece stats", COLOR_SYSTEM);
            }
            case "autosell" -> {
                boolean newState = !client.isAutosellEnabled();
                client.setAutosellEnabled(newState);
                ctx.sendChat(client, "Autosell " + (newState ? "activado" : "desactivado"), COLOR_SYSTEM);
            }
            default -> ctx.sendChat(client, "Comando desconocido. Usa @help", COLOR_SYSTEM);
        }
    }

    private Integer parseRange(String value, int min, int max) {
        try {
            int number = Integer.parseInt(value);
            return number >= min && number <= max ? number : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public interface PacketReader {
        String readString();
    }

    public interface GameContext {
        Client getClient();
        Room getRoom();
        void sendChat(Client client, String message, int color);
        void broadcastRoom(Room room, String message, int color);
        void removeFromRoom(Client client, Room room, int reason);
        void gameEnd(Room room, int status);
        boolean playerDeath(Client client, Room room);
    }

    public interface Room {
        int getStatus();
        int getGameType();
        boolean isMaster(Client client);
        boolean isMode(int... modes);
        Map<Integer, Integer> getStatOverride();
        Client findByName(String name);
    }

    public interface Client {
        String getName();
        int getCharacterPosition();
        boolean isAutosellEnabled();
        void setAutosellEnabled(boolean enabled);
    }
}
