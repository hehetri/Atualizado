package game.chatcmd;

import game.chatcmd.handler.GameChatCommandHandler;
import game.chatcmd.model.PlayerCharacter;
import game.chatcmd.model.Room;
import game.chatcmd.model.RoomMode;
import game.chatcmd.model.Session;
import game.chatcmd.packet.BinaryPacketReader;
import game.chatcmd.packet.PacketReader;
import game.chatcmd.router.GamePacketRouter;
import game.chatcmd.service.ChatService;
import game.chatcmd.service.PlayerStateService;
import game.chatcmd.service.RoomService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameChatCommandHandlerTest {
    private static final Charset CP1252 = Charset.forName("windows-1252");

    private FakeRoomService roomService;
    private FakeChatService chatService;
    private FakePlayerStateService playerStateService;
    private GameChatCommandHandler handler;
    private Session master;
    private Session other;
    private Room room;

    public static void main(String[] args) {
        GameChatCommandHandlerTest t = new GameChatCommandHandlerTest();
        t.testA627Dispatch();
        t.testIgnoreNonCommandMessage();
        t.testHelpCommand();
        t.testAutosellToggle();
        t.testSpeedValidationRange();
        t.testSpeedRequiresMaster();
        t.testKickSelfDenied();
        t.testUnknownCommand();
        t.testHandlerExceptionDoesNotDisconnect();
        System.out.println("All a627 tests passed.");
    }

    void setup() {
        roomService = new FakeRoomService();
        chatService = new FakeChatService();
        playerStateService = new FakePlayerStateService();
        handler = new GameChatCommandHandler(roomService, chatService, playerStateService);

        master = new Session("s1", new PlayerCharacter("Master", 1));
        other = new Session("s2", new PlayerCharacter("Other", 0));
        room = new Room("r1", RoomMode.BATTLE, master, 0);
        roomService.currentRoom = room;
        roomService.playersByName.put("Master", master);
        roomService.playersByName.put("Other", other);
    }

    void testA627Dispatch() {
        setup();
        GamePacketRouter router = new GamePacketRouter();
        router.register("a627", handler);
        router.route(master, "a627", 6, readerFor("@help"));
        expect(!chatService.playerMessages.get(master).isEmpty(), "testA627Dispatch");
    }

    void testIgnoreNonCommandMessage() {
        setup();
        handler.handle(master, readerFor("hello room"));
        expect(chatService.playerMessages.isEmpty(), "testIgnoreNonCommandMessage");
    }

    void testHelpCommand() {
        setup();
        handler.handle(master, readerFor("@help"));
        expect(chatService.playerMessages.get(master).size() >= 3, "testHelpCommand");
    }

    void testAutosellToggle() {
        setup();
        expect(!master.getCharacter().getSettings().isAutosell(), "testAutosellToggle initial");
        handler.handle(master, readerFor("@autosell"));
        expect(master.getCharacter().getSettings().isAutosell(), "testAutosellToggle on");
        handler.handle(master, readerFor("@autosell"));
        expect(!master.getCharacter().getSettings().isAutosell(), "testAutosellToggle off");
    }

    void testSpeedValidationRange() {
        setup();
        handler.handle(master, readerFor("@speed 100"));
        expect(room.getStatOverride().isEmpty(), "testSpeedValidationRange no override");
        expect(chatService.lastFor(master).contains("Valor inválido"), "testSpeedValidationRange message");
    }

    void testSpeedRequiresMaster() {
        setup();
        handler.handle(other, readerFor("@speed 500"));
        expect(room.getStatOverride().isEmpty(), "testSpeedRequiresMaster no override");
        expect(chatService.lastFor(other).contains("master"), "testSpeedRequiresMaster message");
    }

    void testKickSelfDenied() {
        setup();
        handler.handle(master, readerFor("@kick Master"));
        expect(roomService.lastRemovedTarget == null, "testKickSelfDenied no remove");
        expect(chatService.lastFor(master).contains("não pode se expulsar"), "testKickSelfDenied message");
    }

    void testUnknownCommand() {
        setup();
        handler.handle(master, readerFor("@abcxyz"));
        expect(chatService.lastFor(master).contains("Comando desconhecido"), "testUnknownCommand");
    }

    void testHandlerExceptionDoesNotDisconnect() {
        setup();
        roomService.throwOnFind = true;
        handler.handle(master, readerFor("@kick Other"));
        expect(chatService.lastFor(master).contains("Erro ao processar comando"), "testHandlerExceptionDoesNotDisconnect");
    }

    private PacketReader readerFor(String message) {
        return new BinaryPacketReader((message + "\0").getBytes(CP1252));
    }

    private static void expect(boolean ok, String name) {
        if (!ok) {
            throw new AssertionError("failed: " + name);
        }
    }

    private static class FakeRoomService implements RoomService {
        Room currentRoom;
        Map<String, Session> playersByName = new HashMap<>();
        Session lastRemovedTarget;
        boolean throwOnFind;

        @Override
        public Room getRoom(Session session) { return currentRoom; }
        @Override
        public Session findByName(Room room, String playerName) {
            if (throwOnFind) throw new RuntimeException("boom");
            return playersByName.get(playerName);
        }
        @Override
        public void removeSlot(Room room, Session target, int reason) { this.lastRemovedTarget = target; }
        @Override
        public void gameEnd(Room room, int status) { }
        @Override
        public boolean playerDeath(Room room, Session player) { return true; }
    }

    private static class FakeChatService implements ChatService {
        Map<Session, List<String>> playerMessages = new HashMap<>();
        List<String> broadcasts = new ArrayList<>();
        @Override
        public void sendToPlayer(Session session, String message) {
            playerMessages.computeIfAbsent(session, k -> new ArrayList<>()).add(message);
        }
        @Override
        public void broadcastRoom(Room room, String message) { broadcasts.add(message); }
        String lastFor(Session session) {
            List<String> messages = playerMessages.get(session);
            if (messages == null || messages.isEmpty()) return "";
            return messages.get(messages.size() - 1);
        }
    }

    private static class FakePlayerStateService implements PlayerStateService {
        @Override
        public boolean isAdmin(PlayerCharacter character) { return character.getPosition() == 1; }
        @Override
        public boolean isRoomMaster(Room room, Session session) { return room.getMaster() == session; }
        @Override
        public boolean toggleAutosell(PlayerCharacter character) {
            boolean next = !character.getSettings().isAutosell();
            character.getSettings().setAutosell(next);
            return next;
        }
    }
}
