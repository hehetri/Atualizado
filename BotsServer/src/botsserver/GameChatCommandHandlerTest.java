package botsserver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GameChatCommandHandlerTest {
    public static void main(String[] args) throws Exception {
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
        System.out.println("All tests passed");
    }

    public void testA627Dispatch() throws Exception {
        Ctx ctx = newCtx();
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@help\0")));
        if (ctx.chat.messages.isEmpty()) throw new RuntimeException("a627 did not dispatch to handler");
    }

    public void testIgnoreNonCommandMessage() throws Exception {
        Ctx ctx = newCtx();
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("hello\0")));
        if (!ctx.chat.messages.isEmpty()) throw new RuntimeException("non command should be ignored");
    }

    public void testHelpCommand() throws Exception {
        Ctx ctx = newCtx();
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@help\0")));
        if (ctx.chat.messages.size() < 3) throw new RuntimeException("help should send command list");
    }

    public void testAutosellToggle() throws Exception {
        Ctx ctx = newCtx();
        boolean before = ctx.session.autosell;
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@autosell\0")));
        if (ctx.session.autosell == before) throw new RuntimeException("autosell not toggled");
    }

    public void testSpeedValidationRange() throws Exception {
        Ctx ctx = newCtx();
        ctx.session.roomnum = 0;
        ctx.room.roomowner = 0;
        ctx.room.roommode = 0;
        ctx.room.status = 0;
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@speed 100\0")));
        if (!ctx.chat.contains("Valor inválido")) throw new RuntimeException("speed range not validated");
    }

    public void testSpeedRequiresMaster() throws Exception {
        Ctx ctx = newCtx();
        ctx.session.roomnum = 1;
        ctx.room.roomowner = 0;
        ctx.room.roommode = 0;
        ctx.room.status = 0;
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@speed 500\0")));
        if (!ctx.chat.contains("room master")) throw new RuntimeException("speed should require master");
    }

    public void testKickSelfDenied() throws Exception {
        Ctx ctx = newCtx();
        ctx.session.roomnum = 0;
        ctx.room.roomowner = 0;
        ctx.session.botname = "TestPlayer";
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@kick TestPlayer\0")));
        if (!ctx.chat.contains("não pode expulsar a si mesmo")) throw new RuntimeException("self kick should be denied");
    }

    public void testUnknownCommand() throws Exception {
        Ctx ctx = newCtx();
        new GameChatCommandHandler(ctx.roomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@idontexist\0")));
        if (!ctx.chat.contains("Comando desconhecido")) throw new RuntimeException("unknown command feedback missing");
    }

    public void testHandlerExceptionDoesNotDisconnect() throws Exception {
        Ctx ctx = newCtx();
        RoomService brokenRoomService = new RoomService() {
            public Room getRoom(BotClass session) { return ctx.room; }
            public void removeSlot(Room room, int slot, int reason) { throw new RuntimeException("boom"); }
            public void gameEnd(Room room, int status) {}
            public boolean playerDeath(BotClass session, Room room) { return false; }
        };
        new GameChatCommandHandler(brokenRoomService, ctx.chatService).handle(ctx.session, new BinaryPacketReader(bytes("@exit\0")));
        if (!ctx.chat.contains("Erro ao processar comando")) throw new RuntimeException("exception fallback message missing");
    }

    private static byte[] bytes(String value) {
        try { return value.getBytes("windows-1252"); } catch (Exception e) { return value.getBytes(); }
    }

    private Ctx newCtx() throws Exception {
        BotClass session = (BotClass) unsafe().allocateInstance(BotClass.class);
        Room room = (Room) unsafe().allocateInstance(Room.class);
        set(room, "bot", new BotClass[8]);
        set(room, "statmod", new int[]{1,1,1,1,1});
        set(room, "statOverride", new java.util.HashMap<Integer, Integer>());
        session.botname = "TestPlayer";
        session.roomnum = 0;
        session.room = room;
        room.bot[0] = session;
        StubChat chat = new StubChat();
        RoomService rs = new RoomService() {
            public Room getRoom(BotClass s) { return room; }
            public void removeSlot(Room r, int slot, int reason) {}
            public void gameEnd(Room r, int status) {}
            public boolean playerDeath(BotClass s, Room r) { return true; }
        };
        Ctx ctx = new Ctx();
        ctx.session = session;
        ctx.room = room;
        ctx.chat = chat;
        ctx.roomService = rs;
        ctx.chatService = chat;
        return ctx;
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    private static void set(Object obj, String field, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }

    static class Ctx {
        BotClass session;
        Room room;
        StubChat chat;
        RoomService roomService;
        ChatService chatService;
    }

    static class StubChat implements ChatService {
        List<String> messages = new ArrayList<String>();
        public void sendToPlayer(BotClass session, String message) { messages.add(message); }
        public void broadcastRoom(Room room, String message) { messages.add(message); }
        boolean contains(String text) {
            for (String m : messages)
                if (m.contains(text))
                    return true;
            return false;
        }
    }
}
