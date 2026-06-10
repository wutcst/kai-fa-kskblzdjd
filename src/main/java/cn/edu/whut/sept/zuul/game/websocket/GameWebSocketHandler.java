package cn.edu.whut.sept.zuul.game.websocket;

import cn.edu.whut.sept.zuul.game.combat.event.AttackEvent;
import cn.edu.whut.sept.zuul.game.combat.event.DeathEvent;
import cn.edu.whut.sept.zuul.game.combat.event.FightWinEvent;
import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.game.Direction;
import cn.edu.whut.sept.zuul.game.Player;
import cn.edu.whut.sept.zuul.game.Room;
import cn.edu.whut.sept.zuul.game.item.AbstractItem;
import cn.edu.whut.sept.zuul.game.message.GameMessageBridge;
import cn.edu.whut.sept.zuul.game.user.security.JwtUtil;
import cn.edu.whut.sept.zuul.game.websocket.vo.PlayerVO;
import cn.edu.whut.sept.zuul.game.websocket.vo.RoomPlayerVO;
import cn.edu.whut.sept.zuul.game.websocket.vo.RoomVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final long HEARTBEAT_TIMEOUT = 60000;

    private final Game game;
    private final JwtUtil jwtUtil;
    private final GameMessageBridge messageBridge;
    private final ObjectMapper objectMapper;

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, GameSession> playerSessions = new ConcurrentHashMap<>();

    private final RedisSessionManager redisSessionManager;
    private final RedisPubSubService redisPubSubService;

    public GameWebSocketHandler(Game game, JwtUtil jwtUtil, GameMessageBridge messageBridge,
                                RedisSessionManager redisSessionManager, RedisPubSubService redisPubSubService) {
        this.game = game;
        this.jwtUtil = jwtUtil;
        this.messageBridge = messageBridge;
        this.objectMapper = new ObjectMapper();
        this.redisSessionManager = redisSessionManager;
        this.redisPubSubService = redisPubSubService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getTokenParam(session);
        if (token == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        int userId;
        try {
            userId = jwtUtil.validateToken(token);
        } catch (Exception e) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Player player = game.getOrCreatePlayer(userId);
        player.setOnline(true);

        int[] sp = player.getCurrentRoom().getSpawnPoint();
        player.setPosX(sp[0]);
        player.setPosY(sp[1]);

        GameSession existing = playerSessions.remove(userId);
        if (existing != null) {
            sessions.remove(existing.getWebSocketSession().getId());
            existing.getPlayer().setOnline(false);
            try { existing.getWebSocketSession().close(); } catch (Exception ignored) {}
        }

        GameSession gameSession = new GameSession(player, session);
        sessions.put(session.getId(), gameSession);
        playerSessions.put(userId, gameSession);

        if (redisSessionManager != null) {
            redisSessionManager.playerOnline(player);
        }

        sendToSession(session, new WebSocketOutgoingPayload("playerPush", PlayerVO.from(player)));
        roomPush(player.getCurrentRoom());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        GameSession gameSession = sessions.get(session.getId());
        if (gameSession == null) {
            return;
        }

        try {
            WebSocketIncomingPayload payload = objectMapper.readValue(message.getPayload(), WebSocketIncomingPayload.class);
            dispatchAction(gameSession, payload);
        } catch (Exception ignored) {
        }
    }

    private void dispatchAction(GameSession gameSession, WebSocketIncomingPayload payload) {
        String action = payload.getAction();

        if ("heartbeat".equals(action)) {
            gameSession.updateHeartbeat();
            if (redisSessionManager != null) {
                redisSessionManager.updateHeartbeat(gameSession.getPlayer());
            }
            return;
        }

        if ("command".equals(action)) {
            String cmd = payload.getData();
            Player player = gameSession.getPlayer();
            game.processCommand(player, cmd);
            playerPush(player);
            roomPush(player.getCurrentRoom());
            messagePush(player, game.getLastCommandOutput());
        }

        if ("move".equals(action)) {
            handleMove(gameSession.getPlayer(), payload);
        }

        if ("interact".equals(action)) {
            handleInteract(gameSession.getPlayer());
        }

        if ("drop".equals(action)) {
            handleDrop(gameSession.getPlayer(), payload.getData());
        }

        if ("use".equals(action)) {
            handleUse(gameSession.getPlayer(), payload.getData());
        }

        if ("attack".equals(action)) {
            try {
                com.fasterxml.jackson.databind.JsonNode n = objectMapper.readTree(payload.getData());
                handleAttack(gameSession.getPlayer(), n.get("dx").asInt(), n.get("dy").asInt());
            } catch (Exception e) {
                handleAttack(gameSession.getPlayer(), payload.getData());
            }
        }

        if ("equip".equals(action)) {
            handleEquip(gameSession.getPlayer(), payload.getData());
        }

        if ("unequipW".equals(action)) {
            gameSession.getPlayer().unequipWeapon();
            playerPush(gameSession.getPlayer());
        }

        if ("unequipA".equals(action)) {
            gameSession.getPlayer().unequipArmor();
            playerPush(gameSession.getPlayer());
        }
    }

    private void handleEquip(Player player, String itemName) {
        if (itemName == null) return;
        for (AbstractItem item : player.getBag()) {
            if (item.getName().equals(itemName)) {
                if (item.isWeapon()) {
                    player.equipWeapon(item);
                    messagePush(player, "装备了 " + itemName + "。");
                } else {
                    player.equipArmor(item);
                    messagePush(player, "装备了 " + itemName + " 防具。");
                }
                playerPush(player);
                return;
            }
        }
    }

        private void handleMove(Player player, WebSocketIncomingPayload payload) {
        String dataStr = payload.getData();
        if (dataStr == null) return;
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(dataStr);
            int dx = node.has("dx") ? node.get("dx").asInt() : 0;
            int dy = node.has("dy") ? node.get("dy").asInt() : 0;
            if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || (dx == 0 && dy == 0)) return;

            int oldX = player.getPosX();
            int oldY = player.getPosY();
            int newX = oldX + dx;
            int newY = oldY + dy;
            Room currentRoom = player.getCurrentRoom();

            // self-correct: if player is stuck in unwalkable tile, reset to spawn
            if (!currentRoom.isWalkable(oldX, oldY)) {
                int[] sp = currentRoom.getSpawnPoint();
                if (sp != null) {
                    player.setPosX(sp[0]);
                    player.setPosY(sp[1]);
                    oldX = sp[0];
                    oldY = sp[1];
                    newX = oldX + dx;
                    newY = oldY + dy;
                }
            }

            // diagonal: check corner tiles too
            if (dx != 0 && dy != 0) {
                if (!currentRoom.isWalkable(newX, newY)
                        || !currentRoom.isWalkable(newX, oldY)
                        || !currentRoom.isWalkable(oldX, newY)) {
                    playerPush(player);
                    return;
                }
            } else if (!currentRoom.isWalkable(newX, newY)) {
                playerPush(player);
                return;
            }

            Direction doorDir = currentRoom.getDoorDirection(newX, newY);
            if (doorDir != null) {
                Room nextRoom = currentRoom.getExitMap().get(doorDir.toLower());
                if (nextRoom == null) {
                    playerPush(player);
                    return;
                }
                if (nextRoom.isPortal()) {
                    Room randomRoom = game.getRandomRoom(currentRoom);
                    if (randomRoom != null) {
                        nextRoom = randomRoom;
                    }
                }
                Room oldRoom = currentRoom;
                String oldRoomName = oldRoom.getName();
                player.moveTo(nextRoom);
                int[] sp = nextRoom.getSpawnPoint();
                // safeguard: if target spawn is unwalkable, go to starting room
                if (!nextRoom.isWalkable(sp[0], sp[1])) {
                    Room startRoom = game.getStartingRoom();
                    if (startRoom != null) {
                        nextRoom = startRoom;
                        sp = nextRoom.getSpawnPoint();
                    }
                }
                player.setPosX(sp[0]);
                player.setPosY(sp[1]);
                onPlayerMoved(player, oldRoomName);
                playerPush(player);
                roomPush(oldRoom);
                roomPush(player.getCurrentRoom());
                return;
            } else {
                player.setPosX(newX);
                player.setPosY(newY);
            }

            playerPush(player);
            roomPush(player.getCurrentRoom());
        } catch (Exception ignored) {
        }
    }

private void handleInteract(Player player) {
        Room currentRoom = player.getCurrentRoom();
        AbstractItem item = currentRoom.takeItemAt(player.getPosX(), player.getPosY());
        if (item != null && player.takeItem(item)) {
            messagePush(player, "你拾取了 " + item.getName() + "。");
        }
        playerPush(player);
        roomPush(player.getCurrentRoom());
    }

    private void handleDrop(Player player, String itemName) {
        if (itemName == null) return;
        for (AbstractItem item : player.getBag()) {
            if (item.getName().equals(itemName)) {
                player.dropItem(item);
                Room room = player.getCurrentRoom();
                int x = player.getPosX(), y = player.getPosY();
                int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
                boolean placed = false;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (room.isWalkable(nx, ny)) {
                        room.placeItem(item, nx, ny);
                        placed = true;
                        break;
                    }
                }
                if (!placed) room.placeItem(item, x, y);
                messagePush(player, "你丢掉了 " + itemName + "。");
                break;
            }
        }
        playerPush(player);
        roomPush(player.getCurrentRoom());
    }

    private void handleUse(Player player, String itemName) {
        if (itemName == null) return;
        for (AbstractItem item : player.getBag()) {
            if (item.getName().equals(itemName)) {
                if (item.isWeapon()) {
                    messagePush(player, itemName + " 是武器，请拾取后装备。");
                    return;
                }
                player.useItem(item);
                messagePush(player, "使用了 " + itemName + "。");
                break;
            }
        }
        playerPush(player);
        roomPush(player.getCurrentRoom());
    }

    private void handleAttack(Player player, String targetName) {
        game.processCommand(player, "attack " + targetName);
        playerPush(player);
        roomPush(player.getCurrentRoom());
    }

    private void handleAttack(Player attacker, int dx, int dy) {
        long now = System.currentTimeMillis();

        AbstractItem weapon = attacker.getEquippedWeapon();
        int maxRange = weapon != null ? weapon.getAttackRange() : 1;
        int cooldown = weapon != null ? weapon.getAttackCooldown() : 500;
        String atkType = weapon != null ? weapon.getAttackType() : "melee";

        if (now - attacker.getLastAttackTime() < cooldown) return;
        attacker.setLastAttackTime(now);

        int totalDamage = attacker.getAttack();
        Room room = attacker.getCurrentRoom();

        if ("melee".equals(atkType)) {
            int tx = attacker.getPosX() + dx, ty = attacker.getPosY() + dy;
            hitPlayer(attacker, room, tx, ty, totalDamage);
        } else if ("ranged".equals(atkType)) {
            for (int i = 1; i <= maxRange; i++) {
                int tx = attacker.getPosX() + dx * i, ty = attacker.getPosY() + dy * i;
                if (!room.isWalkable(tx, ty)) break;
                if (hitPlayer(attacker, room, tx, ty, totalDamage)) break;
            }
        } else if ("aoe".equals(atkType)) {
            for (int ox = -1; ox <= 1; ox++)
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox == 0 && oy == 0) continue;
                    hitPlayer(attacker, room, attacker.getPosX() + ox, attacker.getPosY() + oy, totalDamage / 2);
                }
        }
        String atkBC = "ATK:" + attacker.getPosX() + "," + attacker.getPosY() + "," + dx + "," + dy + "," + atkType;
        broadcastToRoom(room, atkBC);
    }

    private boolean hitPlayer(Player attacker, Room room, int tx, int ty, int damage) {
        for (Player p : getPlayersInRoom(room, attacker)) {
            if (p.getPosX() == tx && p.getPosY() == ty) {
                int dmg = Math.max(1, damage - p.getDefense());

                int targetAtkBefore = p.getAttack();
                int attackerDefBefore = attacker.getDefense();

                p.hurtBy(dmg);

                AttackEvent attackEvent = new AttackEvent(attacker, p, dmg);
                attacker.notifyHurt(attackEvent);
                p.notifyHurt(attackEvent);

                int counterDamage = Math.max(0, (int)(targetAtkBefore * 0.25) - attackerDefBefore);
                if (counterDamage > 0) {
                    attacker.hurtBy(counterDamage);
                    messagePush(attacker, p.getPlayerName() + " 反击造成 " + counterDamage + " 点伤害！");
                }

                messagePush(attacker, "命中 " + p.getPlayerName() + " 造成 " + dmg + " 点伤害！");
                messagePush(p, attacker.getPlayerName() + " 命中你造成 " + dmg + " 点伤害！");

                boolean victimDead = p.isDead();
                boolean attackerDead = attacker.isDead();

                if (victimDead) {
                    handlePlayerDeath(p, room);
                    roomPush(room);
                    Room spawnRoomP = game.getStartingRoom();
                    roomPush(spawnRoomP);
                    DeathEvent deathEvent = new DeathEvent(attacker, p);
                    p.notifyDeath(deathEvent);
                    if (!p.isDead()) {
                        p.setCurrentHealth(p.getCurrentHealth());
                    } else {
                        respawnDeadPlayer(p);
                    }
                    FightWinEvent winEvent = new FightWinEvent(attacker, p);
                    attacker.notifyFightWin(winEvent);
                    attacker.setMaxHealth(attacker.getMaxHealth() + 10);
                    attacker.setCurrentHealth(Math.min(attacker.getCurrentHealth() + 10, attacker.getMaxHealth()));
                    attacker.setAttack(attacker.getAttack() + 2);
                    messagePush(attacker, "你击败了 " + p.getPlayerName() + "！+2攻击 +10生命");
                    playerPush(attacker);
                    playerPush(p);
                } else {
                    playerPush(attacker);
                    playerPush(p);
                    roomPush(room);
                }

                if (attackerDead) {
                    handlePlayerDeath(attacker, room);
                    roomPush(room);
                    Room spawnRoomA = game.getStartingRoom();
                    roomPush(spawnRoomA);
                    DeathEvent deathEventA = new DeathEvent(p, attacker);
                    attacker.notifyDeath(deathEventA);
                    if (!attacker.isDead()) {
                        attacker.setCurrentHealth(attacker.getCurrentHealth());
                    } else {
                        respawnDeadPlayer(attacker);
                    }
                    FightWinEvent winEventA = new FightWinEvent(p, attacker);
                    p.notifyFightWin(winEventA);
                    p.setMaxHealth(p.getMaxHealth() + 10);
                    p.setCurrentHealth(Math.min(p.getCurrentHealth() + 10, p.getMaxHealth()));
                    p.setAttack(p.getAttack() + 2);
                    messagePush(p, "你击败了 " + attacker.getPlayerName() + "！+2攻击 +10生命");
                    playerPush(attacker);
                    playerPush(p);
                }

                return true;
            }
        }
        return false;
    }

    private void handlePlayerDeath(Player dead, Room room) {
        if (dead.getEquippedWeapon() != null) dead.unequipWeapon();
        if (dead.getEquippedArmor() != null) dead.unequipArmor();
        for (AbstractItem item : new ArrayList<>(dead.getBag())) {
            dead.dropItem(item);
            room.placeItem(item, dead.getPosX(), dead.getPosY());
        }
    }

    private void respawnDeadPlayer(Player dead) {
        dead.setCurrentHealth(dead.getMaxHealth());
        dead.setAttack(10);
        dead.setDefense(5);
        Room oldRoom = dead.getCurrentRoom();
        Room spawnRoom = game.getStartingRoom();
        dead.getPreviousRooms().clear();
        dead.moveTo(spawnRoom);
        int[] sp = spawnRoom.getSpawnPoint();
        dead.setPosX(sp[0]);
        dead.setPosY(sp[1]);
        roomPush(oldRoom);
        roomPush(spawnRoom);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        GameSession gameSession = sessions.remove(session.getId());
        if (gameSession != null) {
            Player player = gameSession.getPlayer();
            player.setOnline(false);
            playerSessions.remove(player.getUserId());
            if (redisSessionManager != null) {
                redisSessionManager.playerOffline(player);
            }
            roomPush(player.getCurrentRoom());
        }
    }

    public void playerPush(Player player) {
        GameSession session = playerSessions.get(player.getUserId());
        if (session != null) {
            sendToSession(session.getWebSocketSession(),
                    new WebSocketOutgoingPayload("playerPush", PlayerVO.from(player)));
        }
    }

    public void roomPush(Room room) {
        List<Player> playersInRoom = getPlayersInRoom(room, null);
        List<RoomPlayerVO> playerVOs = RoomPlayerVO.fromList(playersInRoom);
        WebSocketOutgoingPayload payload = new WebSocketOutgoingPayload("roomPush", RoomVO.from(room, playerVOs));

        for (Player p : playersInRoom) {
            GameSession session = playerSessions.get(p.getUserId());
            if (session != null) {
                sendToSession(session.getWebSocketSession(), payload);
            }
        }

        if (redisPubSubService != null) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                redisPubSubService.publish(room.getName(), json);
            } catch (Exception ignored) {
            }
        }
    }

    public void handlePubSubMessage(String roomName, String jsonPayload) {
        try {
            WebSocketOutgoingPayload payload = objectMapper.readValue(jsonPayload, WebSocketOutgoingPayload.class);
            for (GameSession session : sessions.values()) {
                Player p = session.getPlayer();
                if (p.isOnline() && p.getCurrentRoom().getName().equals(roomName)) {
                    sendToSession(session.getWebSocketSession(), payload);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void messagePush(Player player, String msg) {
        GameSession session = playerSessions.get(player.getUserId());
        if (session != null) {
            sendToSession(session.getWebSocketSession(),
                    new WebSocketOutgoingPayload("messagePush", msg));
        }
    }

    public void broadcastToRoom(Room room, String msg) {
        List<Player> players = getPlayersInRoom(room, null);
        for (Player p : players) {
            messagePush(p, msg);
        }
    }

    public void broadcastLocal(WebSocketOutgoingPayload payload) {
        for (GameSession session : sessions.values()) {
            sendToSession(session.getWebSocketSession(), payload);
        }
    }

    public List<Player> getOnlinePlayers() {
        List<Player> online = new ArrayList<>();
        if (redisSessionManager != null) {
            List<Integer> ids = redisSessionManager.getOnlinePlayerIds();
            for (int id : ids) {
                GameSession session = playerSessions.get(id);
                if (session != null && session.getWebSocketSession().isOpen()) {
                    Player p = session.getPlayer();
                    if (p.isOnline()) {
                        online.add(p);
                    }
                }
            }
        } else {
            for (GameSession session : sessions.values()) {
                Player p = session.getPlayer();
                if (p.isOnline() && session.getWebSocketSession().isOpen()) {
                    online.add(p);
                }
            }
        }
        return online;
    }

    public List<Player> getPlayersInRoom(Room room, Player exclude) {
        List<Player> result = new ArrayList<>();
        if (redisSessionManager != null) {
            List<Integer> ids = redisSessionManager.getPlayersInRoom(room);
            for (int id : ids) {
                GameSession session = playerSessions.get(id);
                if (session != null && session.getWebSocketSession().isOpen()) {
                    Player p = session.getPlayer();
                    if (p.isOnline() && p != exclude) {
                        result.add(p);
                    }
                } else if (session != null) {
                    session.getPlayer().setOnline(false);
                }
            }
        } else {
            for (GameSession session : sessions.values()) {
                Player p = session.getPlayer();
                if (p.isOnline() && p.getCurrentRoom() == room && p != exclude
                        && session.getWebSocketSession().isOpen()) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    public void checkHeartbeats() {
        if (redisSessionManager != null) {
            redisSessionManager.checkHeartbeats(HEARTBEAT_TIMEOUT);
        }

        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
            if (now - entry.getValue().getLastHeartbeat() > HEARTBEAT_TIMEOUT) {
                toRemove.add(entry.getKey());
            }
        }
        for (String id : toRemove) {
            GameSession session = sessions.remove(id);
            if (session != null) {
                session.getPlayer().setOnline(false);
                playerSessions.remove(session.getPlayer().getUserId());
                try {
                    session.getWebSocketSession().close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public GameMessageBridge getMessageBridge() {
        return messageBridge;
    }

    private void sendToSession(WebSocketSession session, WebSocketOutgoingPayload payload) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (IOException ignored) {
        }
    }

    public void onPlayerMoved(Player player, String oldRoomName) {
        if (redisSessionManager != null) {
            redisSessionManager.playerMoved(player, oldRoomName);
        }
    }

    private String getTokenParam(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equals(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }
}