package botsserver;

import java.util.Locale;

public class GameChatCommandHandler implements CommandHandler {
    private static final int ROOM_STATUS_READY = 0;
    private static final int ROOM_STATUS_STARTED = 3;
    private static final int ROOM_MODE_PVP = 0;
    private static final int ROOM_MODE_TEAM_PVP = 1;
    private static final int ROOM_MODE_PLANET = 2;
    private static final int ROOM_MODE_BVB = 3;

    public static final int STAT_SPEED = 3;
    public static final int STAT_ATT_TRANS_GAUGE = 1;

    private final RoomService roomService;
    private final ChatService chatService;

    public GameChatCommandHandler() {
        this(new DefaultRoomService(), new DefaultChatService());
    }

    public GameChatCommandHandler(RoomService roomService, ChatService chatService) {
        this.roomService = roomService;
        this.chatService = chatService;
    }

    @Override
    public void handle(BotClass session, PacketReader packetReader) {
        Room room = roomService.getRoom(session);
        if (room == null)
            return;
        String message = packetReader.readString();
        Main.debug("DEBUG a627 payload text=" + message);
        if (message == null || message.trim().isEmpty() || !message.trim().startsWith("@"))
            return;

        String commandLine = message.trim().substring(1).trim();
        if (commandLine.isEmpty())
            return;

        String[] split = commandLine.split("\\s+", 2);
        String command = split[0].toLowerCase(Locale.ROOT);
        String args = split.length > 1 ? split[1].trim() : "";

        try {
            if ("exit".equals(command)) {
                Main.debug("INFO a627 exit player=" + session.botname + " room=" + room.roomnum);
                roomService.removeSlot(room, session.roomnum, 0);
                return;
            }
            if ("win".equals(command) || "lose".equals(command) || "timeout".equals(command) || "timeoutdm".equals(command)) {
                if (session.gm != 1) {
                    Main.debug("WARN a627 unauthorized gameEnd player=" + session.botname);
                    return;
                }
                int status = "win".equals(command) ? 1 : ("lose".equals(command) ? 0 : ("timeout".equals(command) ? 2 : 3));
                Main.debug("INFO a627 gameEnd status=" + status + " player=" + session.botname + " room=" + room.roomnum);
                roomService.gameEnd(room, status);
                return;
            }
            if ("speed".equals(command) || "gauge".equals(command)) {
                if (session.roomnum != room.roomowner) {
                    Main.debug("WARN a627 stat non-master player=" + session.botname);
                    chatService.sendToPlayer(session, "Somente o room master pode usar esse comando.");
                    return;
                }
                if (room.roommode != ROOM_MODE_PVP && room.roommode != ROOM_MODE_TEAM_PVP && room.roommode != ROOM_MODE_BVB) {
                    chatService.sendToPlayer(session, "Comando permitido apenas em PVP, TEAM_PVP e BVB.");
                    return;
                }
                if (room.status != ROOM_STATUS_READY) {
                    chatService.sendToPlayer(session, "Só é possível alterar antes da partida iniciar.");
                    return;
                }
                Integer value = parseIntRange(args, 200, 8000);
                if (value == null) {
                    chatService.sendToPlayer(session, "Valor inválido. Use um número entre 200 e 8000.");
                    return;
                }
                int stat = "speed".equals(command) ? STAT_SPEED : STAT_ATT_TRANS_GAUGE;
                room.setStatOverride(stat, value);
                chatService.broadcastRoom(room, "[Room] " + session.botname + " definiu " + command + "=" + value);
                Main.debug("INFO a627 stat cmd=" + command + " value=" + value + " player=" + session.botname + " room=" + room.roomnum);
                return;
            }
            if ("reset".equals(command)) {
                if (session.roomnum != room.roomowner) {
                    Main.debug("WARN a627 reset non-master player=" + session.botname);
                    chatService.sendToPlayer(session, "Somente o room master pode usar esse comando.");
                    return;
                }
                if (room.status != ROOM_STATUS_READY) {
                    chatService.sendToPlayer(session, "Só é possível resetar antes da partida iniciar.");
                    return;
                }
                room.clearStatOverride();
                chatService.broadcastRoom(room, "[Room] " + session.botname + " resetou as estatísticas da sala.");
                return;
            }
            if ("suicide".equals(command)) {
                if (room.status != ROOM_STATUS_STARTED)
                    return;
                if (room.roommode != ROOM_MODE_PLANET) {
                    chatService.sendToPlayer(session, "@suicide só funciona no modo planeta.");
                    return;
                }
                if (roomService.playerDeath(session, room))
                    chatService.sendToPlayer(session, "Você se suicidou.");
                return;
            }
            if ("kick".equals(command)) {
                if (session.roomnum != room.roomowner) {
                    Main.debug("WARN a627 kick non-master player=" + session.botname);
                    chatService.sendToPlayer(session, "Somente o room master pode expulsar jogadores.");
                    return;
                }
                if (args.isEmpty()) {
                    chatService.sendToPlayer(session, "Uso: @kick <nome>");
                    return;
                }
                if (session.botname.equalsIgnoreCase(args)) {
                    chatService.sendToPlayer(session, "Você não pode expulsar a si mesmo.");
                    return;
                }
                BotClass target = findByName(room, args);
                if (target == null) {
                    chatService.sendToPlayer(session, "Jogador não encontrado: " + args);
                    return;
                }
                roomService.removeSlot(room, target.roomnum, 2);
                Main.debug("INFO a627 kick actor=" + session.botname + " target=" + target.botname + " room=" + room.roomnum);
                return;
            }
            if ("help".equals(command)) {
                chatService.sendToPlayer(session, "@exit @win @lose @timeout @timeoutdm");
                chatService.sendToPlayer(session, "@speed <n> @gauge <n> @reset");
                chatService.sendToPlayer(session, "@suicide @kick <nome> @autosell @stat-help");
                return;
            }
            if ("stat-help".equals(command)) {
                chatService.sendToPlayer(session, "@speed <200-8000> altera speed");
                chatService.sendToPlayer(session, "@gauge <200-8000> altera gauge");
                chatService.sendToPlayer(session, "@reset restaura estatísticas padrões");
                return;
            }
            if ("autosell".equals(command)) {
                session.autosell = !session.autosell;
                chatService.sendToPlayer(session, "Autosell " + (session.autosell ? "ON" : "OFF"));
                return;
            }
            chatService.sendToPlayer(session, "Comando desconhecido. Escreva @help para ajuda.");
        } catch (Exception ex) {
            Main.debug("ERROR a627 exception player=" + session.botname + " room=" + (room != null ? room.roomnum : -1) + " cmd=" + commandLine + " err=" + ex);
            chatService.sendToPlayer(session, "Erro ao processar comando. Tente novamente.");
        }
    }

    private BotClass findByName(Room room, String name) {
        for (int i = 0; i < 8; i++)
            if (room.bot[i] != null && room.bot[i].botname.equalsIgnoreCase(name))
                return room.bot[i];
        return null;
    }

    private Integer parseIntRange(String value, int min, int max) {
        try {
            int n = Integer.parseInt(value.trim());
            return (n < min || n > max) ? null : n;
        } catch (Exception e) {
            return null;
        }
    }

    static class DefaultRoomService implements RoomService {
        @Override
        public Room getRoom(BotClass session) {
            return session.room;
        }

        @Override
        public void removeSlot(Room room, int slot, int reason) {
            room.Exit(slot, reason == 2);
        }

        @Override
        public void gameEnd(Room room, int status) {
            int[] winner = new int[8];
            for (int i = 0; i < 8; i++)
                winner[i] = status == 1 ? 1 : 0;
            room.EndRoom(new int[8], new int[8], 0, winner, status == 2 || status == 3);
        }

        @Override
        public boolean playerDeath(BotClass session, Room room) {
            room.Dead(session.roomnum, 10);
            return true;
        }
    }

    static class DefaultChatService implements ChatService {
        @Override
        public void sendToPlayer(BotClass session, String message) {
            if (session.room != null)
                session.room.SendMessage(false, session.roomnum, message, 2);
            else
                session.sendChatMsg(message, 2, false, -1);
        }

        @Override
        public void broadcastRoom(Room room, String message) {
            room.SendMessage(true, 0, message, 2);
        }
    }
}
