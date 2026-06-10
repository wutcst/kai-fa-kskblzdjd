package cn.edu.whut.sept.zuul.game;

import cn.edu.whut.sept.zuul.game.item.AbstractItem;
import cn.edu.whut.sept.zuul.game.item.Items;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间类 — 表示游戏地图中的一个房间，支持物品存放、传送和多人游戏。
 *
 * <p>对外接口供 #5（GoCommand/BackCommand）、#6（Take/Drop）、#7（传送）调用。</p>
 *
 * @author lfk
 * @since 1.0
 */
public class Room {

    private final String name;
    private final String description;
    private boolean portal;
    private final HashMap<String, Room> exits;
    private final List<AbstractItem> items;

    // 2D 瓦片地图 (#25)
    private int width;
    private int height;
    private int[][] tiles;
    private final Map<AbstractItem, int[]> itemSpawns;
    private int[] spawnPoint;

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.portal = false;
        this.exits = new HashMap<>();
        this.items = Collections.synchronizedList(new ArrayList<>());
        this.itemSpawns = new ConcurrentHashMap<>();
        this.width = 15;
        this.height = 10;
        this.spawnPoint = new int[]{1, 1};
    }

    // ========== 基本信息 — #5 #6 调用 ==========

    /** @return 房间名称 */
    public String getName() {
        return name;
    }

    /** @return 房间简短描述 */
    public String getShortDescription() {
        return description;
    }

    /**
     * 获取房间完整描述，拼接名称、描述、物品列表和出口。
     *
     * <pre>
     * 物品列表格式：物品: Sword(8kg), BloodVial(2kg)
     * 出口格式：  出口: east, west
     * </pre>
     *
     * @return 完整描述字符串
     */
    public String getLongDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n").append(description);

        if (!items.isEmpty()) {
            sb.append("\n物品: ");
            List<String> itemDescs = new ArrayList<>();
            for (AbstractItem item : items) {
                itemDescs.add(item.getName() + "(" + item.getWeight() + "kg)");
            }
            sb.append(String.join(", ", itemDescs));
        }

        Set<String> exitSet = exits.keySet();
        if (!exitSet.isEmpty()) {
            sb.append("\n出口: ").append(String.join(", ", exitSet));
        }

        return sb.toString();
    }

    // ========== 出口 — #5 #7 调用 ==========

    /**
     * 设置房间出口方向。
     *
     * @param direction 方向（如 "east", "west"）
     * @param neighbor  相邻房间
     */
    public void setExit(String direction, Room neighbor) {
        exits.put(direction, neighbor);
    }

    /**
     * 获取指定方向的相邻房间。
     *
     * @param direction 方向名称
     * @return 相邻房间，无则返回 null
     */
    public Room getExit(String direction) {
        if (portal) {
            // 传送房间：忽略方向，随机返回一个出口
            List<Room> values = new ArrayList<>(exits.values());
            if (values.isEmpty()) {
                return null;
            }
            return values.get((int) (Math.random() * values.size()));
        }
        return exits.get(direction);
    }

    /** @return 所有出口方向集合 */
    public Set<String> getExits() {
        return exits.keySet();
    }

    /** @return 出口 Map（方向 → 房间）的副本 */
    public HashMap<String, Room> getExitMap() {
        return new HashMap<>(exits);
    }

    // ========== 物品 — #6 调用 ==========

    /** @return 房间内物品列表（不可变副本） */
    public List<AbstractItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * 按名称查找物品。
     *
     * @param name 物品名称
     * @return 物品对象，未找到返回 null
     */
    public AbstractItem getItem(String name) {
        for (AbstractItem item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按名称移除物品。
     *
     * @param name 物品名称
     * @return 移除的物品，未找到返回 null
     */
    public AbstractItem removeItem(String name) {
        synchronized (items) {
            for (AbstractItem item : items) {
                if (item.getName().equals(name)) {
                    items.remove(item);
                    itemSpawns.remove(item);
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * 随机生成 0~2 件物品到房间。
     */
    public void addRandomItems() {
        int count = (int) (Math.random() * 3);
        for (int i = 0; i < count; i++) {
            AbstractItem item = Items.generateRandomItem();
            if (item != null) {
                this.addItem(item);
            }
        }
    }

    /** @return 房间内所有物品的总重量 */
    public int getItemsTotalWeight() {
        int total = 0;
        for (AbstractItem item : items) {
            total += item.getWeight();
        }
        return total;
    }

    // ========== 传送 — #7 调用 ==========

    /** @return 是否为传送房间 */
    public boolean isPortal() {
        return portal;
    }

    /** @param portal 设为传送房间 */
    public void setPortal(boolean portal) {
        this.portal = portal;
    }

    /**
     * 从所有房间中随机返回一个非自身、非传送房间的目标。
     *
     * @param allRooms 所有房间列表
     * @return 随机目标房间，无可选则返回 null
     */
    public Room getRandomExit(List<Room> allRooms) {
        List<Room> candidates = new ArrayList<>();
        for (Room r : allRooms) {
            if (r != this && !r.isPortal()) {
                candidates.add(r);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get((int) (Math.random() * candidates.size()));
    }

    // ========== 2D 瓦片地图 (#25) ==========

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int[][] getTiles() { return tiles; }

    public void setTiles(int[][] tiles) { this.tiles = tiles; }

    public int[] getSpawnPoint() { return spawnPoint; }
    public void setSpawnPoint(int x, int y) { this.spawnPoint = new int[]{x, y}; }

    public boolean isWalkable(int x, int y) {
        if (tiles == null) return true;
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        return tiles[y][x] != TileType.WALL;
    }

    public Direction getDoorDirection(int x, int y) {
        if (tiles == null) return null;
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return TileType.toDirection(tiles[y][x]);
    }

    public boolean hasItemAt(int x, int y) {
        synchronized (items) {
            for (int[] pos : itemSpawns.values()) {
                if (pos[0] == x && pos[1] == y) return true;
            }
        }
        return false;
    }

    public AbstractItem takeItemAt(int x, int y) {
        synchronized (items) {
            for (Map.Entry<AbstractItem, int[]> entry : itemSpawns.entrySet()) {
                int[] pos = entry.getValue();
                if (pos[0] == x && pos[1] == y) {
                    AbstractItem item = entry.getKey();
                    itemSpawns.remove(item);
                    items.remove(item);
                    return item;
                }
            }
        }
        return null;
    }

    public void placeItem(AbstractItem item, int x, int y) {
        items.add(item);
        itemSpawns.put(item, new int[]{x, y});
    }

    public int[] getItemPosition(String itemName) {
        synchronized (items) {
            for (Map.Entry<AbstractItem, int[]> entry : itemSpawns.entrySet()) {
                if (entry.getKey().getName().equals(itemName)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public void addItem(AbstractItem item) {
        items.add(item);
        int maxAttempts = 100;
        int x = 1, y = 1;
        boolean found = false;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            x = 1 + (int) (Math.random() * (width - 2));
            y = 1 + (int) (Math.random() * (height - 2));
            if (tiles == null || isWalkable(x, y)) {
                found = true;
                break;
            }
        }
        if (!found && tiles != null) {
            for (int ty = 1; ty < height - 1 && !found; ty++)
                for (int tx = 1; tx < width - 1 && !found; tx++)
                    if (isWalkable(tx, ty)) { x = tx; y = ty; found = true; }
        }
        itemSpawns.put(item, new int[]{x, y});
    }
}
