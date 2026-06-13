package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.game.Player;
import cn.edu.whut.sept.zuul.game.Room;
import cn.edu.whut.sept.zuul.game.item.Sword;
import cn.edu.whut.sept.zuul.game.item.DragonscaleBulwark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        Method initMethod = Game.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(game);
    }

    @Test
    void shouldHaveRoundDurationConstant() {
        assertEquals(600, Game.ROUND_DURATION_SECONDS);
    }

    @Test
    void shouldReturnRoundRemainingSeconds() {
        int remaining = game.getRoundRemainingSeconds();
        assertTrue(remaining >= 0);
        assertTrue(remaining <= 600);
    }

    @Test
    void shouldResetRoundTimer() {
        game.newRound();
        int remaining = game.getRoundRemainingSeconds();
        assertTrue(remaining >= 599);
    }

    @Test
    void shouldGetRemainingSecondsNotNegative() {
        game.newRound();
        int remaining = game.getRoundRemainingSeconds();
        assertTrue(remaining >= 0);
    }

    @Test
    void shouldGetStartingRoom() {
        Room starting = game.getStartingRoom();
        assertNotNull(starting);
        assertEquals("outside", starting.getName());
    }

    @Test
    void shouldGetAllRooms() {
        assertNotNull(game.getAllRooms());
        assertEquals(5, game.getAllRooms().size());
    }

    @Test
    void shouldGetOrCreatePlayer() {
        Player p = game.getOrCreatePlayer(42);
        assertNotNull(p);
        assertEquals(42, p.getUserId());
        assertEquals("Player42", p.getPlayerName());
    }

    @Test
    void shouldGetOrCreatePlayerReuseExisting() {
        Player p1 = game.getOrCreatePlayer(99);
        Player p2 = game.getOrCreatePlayer(99);
        assertSame(p1, p2);
    }

    @Test
    void shouldGetAllPlayers() {
        game.getOrCreatePlayer(1);
        game.getOrCreatePlayer(2);
        Map<Integer, Player> all = game.getAllPlayers();
        assertNotNull(all);
        assertEquals(3, all.size());
        assertTrue(all.containsKey(0));
        assertTrue(all.containsKey(1));
        assertTrue(all.containsKey(2));
    }

    @Test
    void shouldResetAllPlayersStats() {
        Player p = game.getOrCreatePlayer(1);
        p.setAttack(50);
        p.setDefense(30);
        p.setMaxHealth(200);
        p.setCurrentHealth(50);
        p.setMaxCapacity(30);
        p.setKills(10);

        Room startingRoom = game.getStartingRoom();
        int[] sp = startingRoom.getSpawnPoint();

        game.resetAllPlayers();

        assertEquals(10, p.getAttack());
        assertEquals(5, p.getDefense());
        assertEquals(100, p.getMaxHealth());
        assertEquals(100, p.getCurrentHealth());
        assertEquals(50, p.getMaxCapacity());
        assertNull(p.getEquippedWeapon());
        assertNull(p.getEquippedArmor());
        assertEquals(0, p.getBag().size());
        assertEquals(startingRoom, p.getCurrentRoom());
        assertEquals(sp[0], p.getPosX());
        assertEquals(sp[1], p.getPosY());
    }

    @Test
    void shouldResetAllPlayersUnequipsGear() {
        Player p = game.getOrCreatePlayer(1);
        Sword sword = new Sword();
        p.takeItem(sword);
        p.equipWeapon(sword);

        DragonscaleBulwark armor = new DragonscaleBulwark();
        p.takeItem(armor);
        p.equipArmor(armor);

        assertNotNull(p.getEquippedWeapon());
        assertNotNull(p.getEquippedArmor());

        game.resetAllPlayers();

        assertNull(p.getEquippedWeapon());
        assertNull(p.getEquippedArmor());
        assertEquals(0, p.getBag().size());
    }

    @Test
    void shouldRerollAllItems() {
        game.rerollAllItems();

        for (Room room : game.getAllRooms()) {
            assertNotNull(room.getItems());
            int itemCount = room.getItems().size();
            assertTrue(itemCount >= 1 && itemCount <= 3,
                    "Room " + room.getName() + " has " + itemCount + " items, expected 1-3");
        }
    }

    @Test
    void shouldGetRandomRoom() {
        Room room = game.getRandomRoom();
        assertNotNull(room);
        assertFalse(room.isPortal());
    }

    @Test
    void shouldRepeatedRerollProduceValidResults() {
        for (int i = 0; i < 10; i++) {
            game.rerollAllItems();
            game.resetAllPlayers();
        }
        assertNotNull(game.getStartingRoom());
        for (Room room : game.getAllRooms()) {
            assertTrue(room.getItems().size() >= 1);
        }
    }
}