package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.game.Player;
import cn.edu.whut.sept.zuul.game.Room;
import cn.edu.whut.sept.zuul.game.message.AbsMessageBridge;
import cn.edu.whut.sept.zuul.game.message.ConsoleMessageBridge;
import cn.edu.whut.sept.zuul.game.message.GlobalMessage;
import cn.edu.whut.sept.zuul.game.message.SinglePlayerMessage;
import cn.edu.whut.sept.zuul.game.store.StoreManager;
import cn.edu.whut.sept.zuul.game.websocket.GameWebSocketHandler;
import cn.edu.whut.sept.zuul.game.item.Items;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Game {

    private Parser parser;
    private Player player;
    private List<Room> allRooms;
    private AbsMessageBridge messageBridge;
    private Map<Integer, Player> playerMap;
    private Room startingRoom;
    private GameWebSocketHandler webSocketHandler;

    @Autowired
    private StoreManager storeManager;

    private static final Random RANDOM = new Random();

    public Game() {
        messageBridge = new ConsoleMessageBridge();
    }

    @PostConstruct
    private void init() {
        playerMap = new ConcurrentHashMap<>();
        createRooms();
        parser = new Parser(storeManager);
    }

    private void createRooms() {
        allRooms = new ArrayList<>();

        Room outside = new Room("outside", "大学正门外");
        Room theater = new Room("theater", "演讲厅内");
        Room pub = new Room("pub", "校园酒吧");
        Room lab = new Room("lab", "计算机实验室");
        Room office = new Room("office", "行政办公室");

        outside.setExit("east", theater);
        outside.setExit("south", lab);
        outside.setExit("west", pub);

        theater.setExit("west", outside);

        pub.setExit("east", outside);

        lab.setExit("north", outside);
        lab.setExit("east", office);

        office.setExit("west", lab);

        lab.setPortal(true);

        allRooms.add(outside);
        allRooms.add(theater);
        allRooms.add(pub);
        allRooms.add(lab);
        allRooms.add(office);

        setupTileMaps(outside, theater, pub, lab, office);

        for (Room room : allRooms) {
            room.addRandomItems();
        }

        startingRoom = outside;
        player = getOrCreatePlayer(0);
    }

    private void setupTileMaps(Room outside, Room theater, Room pub, Room lab, Room office) {
        outside.setWidth(15); outside.setHeight(10);
        outside.setTiles(new int[][]{
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,1,1,1,0,0,0,0,0,0,1},
            {1,0,0,0,0,1,1,1,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,4,0,0,0,0,0,0,0,0,0,0,0,5,1},
            {1,1,1,1,1,3,1,1,1,1,1,1,1,1,1}
        });
        outside.setSpawnPoint(4, 5);

        theater.setWidth(15); theater.setHeight(10);
        theater.setTiles(new int[][]{
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,1,1,1,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,4,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        });
        theater.setSpawnPoint(12, 5);

        pub.setWidth(15); pub.setHeight(10);
        pub.setTiles(new int[][]{
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,1,0,1,0,0,0,1,0,0,0,1},
            {1,0,0,0,1,0,1,0,0,0,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,1,0,0,0,1,0,0,0,1,0,1},
            {1,0,0,0,1,0,0,0,1,0,0,0,1,0,1},
            {1,0,0,0,1,0,0,0,1,0,0,0,1,5,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        });
        pub.setSpawnPoint(2, 5);

        lab.setWidth(15); lab.setHeight(10);
        lab.setTiles(new int[][]{
            {1,1,1,2,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,1,1,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,1,1,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,5,1}
        });
        lab.setSpawnPoint(7, 5);

        office.setWidth(15); office.setHeight(10);
        office.setTiles(new int[][]{
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,4,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        });
        office.setSpawnPoint(7, 5);
    }

    public Player getOrCreatePlayer(int userId) {
        return playerMap.computeIfAbsent(userId, id -> new Player(id, "Player" + id, startingRoom));
    }

    public void processCommand(Player p, String cmd) {
        Command command = parser.parseCommand(cmd);
        if (command == null) {
            messageBridge.send(new SinglePlayerMessage("I don't understand..."), p);
        } else {
            command.execute(this, p);
        }
    }

    public void play() {
        printWelcome();

        boolean finished = false;
        while (!finished) {
            Command command = parser.getCommand();
            if (command == null) {
                messageBridge.send(new SinglePlayerMessage("无法理解..."), player);
            } else {
                finished = command.execute(this, player);
            }
        }

        messageBridge.send(new GlobalMessage("感谢游玩，再见！"));
    }

    private void printWelcome() {
        messageBridge.send(new GlobalMessage(""));
        messageBridge.send(new GlobalMessage("欢迎来到 World of Zuul！"));
        messageBridge.send(new GlobalMessage("一个全新的多人冒险游戏。"));
        messageBridge.send(new GlobalMessage("输入 'help' 查看帮助。"));
        messageBridge.send(new GlobalMessage(""));
        messageBridge.send(new SinglePlayerMessage(player.getCurrentRoom().getLongDescription()), player);
    }

    public Room getCurrentRoom() {
        return player.getCurrentRoom();
    }

    public void setCurrentRoom(Room room) {
        player.moveTo(room);
    }

    public Player getPlayer() {
        return player;
    }

    public AbsMessageBridge getMessageBridge() {
        return messageBridge;
    }

    public String getLastCommandOutput() {
        if (messageBridge instanceof ConsoleMessageBridge) {
            return ((ConsoleMessageBridge) messageBridge).getLastMessage();
        }
        return "";
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(allRooms);
    }

    public Room getStartingRoom() {
        return startingRoom;
    }

    public void respawnItems() {
        boolean added = false;
        for (Room room : allRooms) {
            if (room.getItems().size() < 2) {
                room.addItem(Items.generateRandomItem());
                added = true;
            }
        }
        if (added && webSocketHandler != null) {
            for (Room room : allRooms) {
                webSocketHandler.roomPush(room);
            }
        }
    }

    public Map<Integer, Player> getAllPlayers() {
        return playerMap;
    }

    public GameWebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }

    public void setWebSocketHandler(GameWebSocketHandler handler) {
        this.webSocketHandler = handler;
    }

    public Room getRandomRoom(Room... exclude) {
        List<Room> nonPortal = new ArrayList<>();
        java.util.Set<Room> excludeSet = new java.util.HashSet<>();
        if (exclude != null) {
            for (Room r : exclude) {
                if (r != null) excludeSet.add(r);
            }
        }
        for (Room room : allRooms) {
            if (!room.isPortal() && !excludeSet.contains(room)) {
                nonPortal.add(room);
            }
        }
        if (nonPortal.isEmpty()) {
            return null;
        }
        return nonPortal.get(RANDOM.nextInt(nonPortal.size()));
    }
}
