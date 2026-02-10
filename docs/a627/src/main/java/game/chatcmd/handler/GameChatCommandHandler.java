package game.chatcmd.handler;

import game.chatcmd.model.PlayerCharacter;
import game.chatcmd.model.Room;
import game.chatcmd.model.RoomMode;
import game.chatcmd.model.Session;
import game.chatcmd.packet.PacketReader;
import game.chatcmd.router.CommandHandler;
import game.chatcmd.service.ChatService;
import game.chatcmd.service.PlayerStateService;
import game.chatcmd.service.RoomService;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameChatCommandHandler implements CommandHandler {
    private static final Logger LOGGER = Logger.getLogger(GameChatCommandHandler.class.getName());

    private static final Set<RoomMode> STAT_MODES = Set.of(RoomMode.DEATHMATCH, RoomMode.BATTLE, RoomMode.TEAM_BATTLE);

    private final RoomService roomService;
    private final ChatService chatService;
    private final PlayerStateService playerStateService;

    public GameChatCommandHandler(RoomService roomService, ChatService chatService, PlayerStateService playerStateService) {
        this.roomService = roomService;
        this.chatService = chatService;
        this.playerStateService = playerStateService;
    }

    @Override
    public void handle(Session session, PacketReader packet) {
        Room room = roomService.getRoom(session);
        if (room == null) {
            return;
        }

        String message = packet.readString();
        LOGGER.fine(() -> "opcode=a627 payloadText='" + message + "'");
        if (message == null || message.isBlank() || !message.startsWith("@")) {
            return;
        }

        String trimmed = message.substring(1).trim();
        if (trimmed.isEmpty()) {
            return;
        }

        String[] split = trimmed.split("\\s+", 2);
        String command = split[0].toLowerCase(Locale.ROOT);
        String args = split.length > 1 ? split[1].trim() : "";

        try {
            processCommand(session, room, command, args);
            LOGGER.info(() -> "command=" + command + " player=" + session.getCharacter().getName() + " room=" + room.getId());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "handler error player=" + session.getCharacter().getName() + " room=" + room.getId() + " cmd=" + command, ex);
            chatService.sendToPlayer(session, "Erro ao processar comando. Tente novamente.");
        }
    }

    private void processCommand(Session session, Room room, String command, String args) {
        PlayerCharacter character = session.getCharacter();
        switch (command) {
            case "exit" -> roomService.removeSlot(room, session, 0);
            case "win" -> executeAdminGameEnd(session, room, 1);
            case "lose" -> executeAdminGameEnd(session, room, 0);
            case "timeout" -> executeAdminGameEnd(session, room, 2);
            case "timeoutdm" -> executeAdminGameEnd(session, room, 3);
            case "speed" -> executeStatOverride(session, room, args, Room.STAT_SPEED, "speed");
            case "gauge" -> executeStatOverride(session, room, args, Room.STAT_ATT_TRANS_GAUGE, "gauge");
            case "reset" -> executeReset(session, room);
            case "suicide" -> executeSuicide(session, room);
            case "kick" -> executeKick(session, room, args);
            case "help" -> sendHelp(session);
            case "stat-help" -> sendStatHelp(session);
            case "autosell" -> {
                boolean enabled = playerStateService.toggleAutosell(character);
                chatService.sendToPlayer(session, "Autosell " + (enabled ? "ON" : "OFF"));
            }
            default -> chatService.sendToPlayer(session, "Comando desconhecido. Escreva @help para ver os comandos.");
        }
    }

    private void executeAdminGameEnd(Session session, Room room, int status) {
        if (!playerStateService.isAdmin(session.getCharacter())) {
            LOGGER.warning("admin command denied");
            return;
        }
        roomService.gameEnd(room, status);
    }

    private void executeStatOverride(Session session, Room room, String arg, int statType, String label) {
        if (!playerStateService.isRoomMaster(room, session)) {
            chatService.sendToPlayer(session, "Apenas o master da sala pode alterar stats.");
            LOGGER.warning("stat command denied non-master");
            return;
        }
        if (!STAT_MODES.contains(room.getMode())) {
            chatService.sendToPlayer(session, "Comando disponível apenas em DEATHMATCH/BATTLE/TEAM_BATTLE.");
            return;
        }
        if (room.getStatus() != 0) {
            chatService.sendToPlayer(session, "Comando disponível apenas antes da partida iniciar.");
            return;
        }
        Integer value = parseRange(arg, 200, 8000);
        if (value == null) {
            chatService.sendToPlayer(session, "Valor inválido. Use um número entre 200 e 8000.");
            return;
        }
        room.getStatOverride().put(statType, value);
        chatService.broadcastRoom(room, session.getCharacter().getName() + " definiu @" + label + "=" + value);
    }

    private void executeReset(Session session, Room room) {
        if (!playerStateService.isRoomMaster(room, session)) {
            chatService.sendToPlayer(session, "Apenas o master da sala pode resetar stats.");
            return;
        }
        if (room.getStatus() != 0) {
            chatService.sendToPlayer(session, "Comando disponível apenas antes da partida iniciar.");
            return;
        }
        room.getStatOverride().clear();
        chatService.broadcastRoom(room, session.getCharacter().getName() + " resetou os stats da sala.");
    }

    private void executeSuicide(Session session, Room room) {
        if (room.getStatus() != 3) {
            return;
        }
        if (room.getMode() != RoomMode.PLANET) {
            chatService.sendToPlayer(session, "@suicide funciona apenas no modo planeta.");
            return;
        }
        if (roomService.playerDeath(room, session)) {
            chatService.sendToPlayer(session, "Suicide executado com sucesso.");
        }
    }

    private void executeKick(Session session, Room room, String targetName) {
        if (!playerStateService.isRoomMaster(room, session)) {
            chatService.sendToPlayer(session, "Apenas o master pode expulsar jogadores.");
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            chatService.sendToPlayer(session, "Informe um nome. Uso: @kick <nome>");
            return;
        }
        if (session.getCharacter().getName().equalsIgnoreCase(targetName)) {
            chatService.sendToPlayer(session, "Você não pode se expulsar.");
            return;
        }
        Session target = roomService.findByName(room, targetName);
        if (target == null) {
            chatService.sendToPlayer(session, "Jogador não encontrado na sala: " + targetName);
            return;
        }
        roomService.removeSlot(room, target, 2);
    }

    private void sendHelp(Session session) {
        chatService.sendToPlayer(session, "@help, @exit, @win, @lose, @timeout, @timeoutdm");
        chatService.sendToPlayer(session, "@speed <n>, @gauge <n>, @reset, @stat-help");
        chatService.sendToPlayer(session, "@suicide, @kick <nome>, @autosell");
    }

    private void sendStatHelp(Session session) {
        chatService.sendToPlayer(session, "@speed <200-8000> altera STAT_SPEED");
        chatService.sendToPlayer(session, "@gauge <200-8000> altera STAT_ATT_TRANS_GAUGE");
        chatService.sendToPlayer(session, "@reset limpa todos os overrides de stat");
    }

    private Integer parseRange(String raw, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
