package cn.edu.whut.sept.zuul.game.item;

import cn.edu.whut.sept.zuul.game.Player;
import cn.edu.whut.sept.zuul.game.Room;
import cn.edu.whut.sept.zuul.game.combat.event.AttackEvent;
import cn.edu.whut.sept.zuul.game.combat.event.DeathEvent;
import cn.edu.whut.sept.zuul.game.combat.event.FightWinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoenixFeatherTest {

    private PhoenixFeather feather;
    private Player player;
    private Player killer;
    private Room room;

    @BeforeEach
    void setUp() {
        feather = new PhoenixFeather();
        room = new Room("arena", "arena");
        player = new Player(1, "Phoenix", room);
        killer = new Player(2, "Hunter", room);
    }

    @Test
    void shouldBePassiveEffect() {
        assertTrue(feather.isPassiveEffect());
        assertFalse(feather.isWeapon());
        assertFalse(feather.isConsumable());
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertEquals("PhoenixFeather", feather.getName());
        assertEquals(2, feather.getWeight());
    }

    @Test
    void shouldRegisterListenerWhenTaken() {
        feather.takenBy(player);
        assertTrue(player.getListeners().contains(feather));
    }

    @Test
    void shouldUnregisterListenerWhenDropped() {
        feather.takenBy(player);
        feather.droppedBy(player);
        assertFalse(player.getListeners().contains(feather));
    }

    @Test
    void shouldReviveWithHalfHP() {
        player.takeItem(feather);
        player.setCurrentHealth(0);
        int maxHp = player.getMaxHealth();

        DeathEvent event = new DeathEvent(killer, player);
        feather.onDeath(player, event);

        assertEquals(maxHp / 2, player.getCurrentHealth());
        assertFalse(player.isDead());
    }

    @Test
    void shouldRemoveFromBagOnUse() {
        player.takeItem(feather);
        assertTrue(player.getBag().contains(feather));

        DeathEvent event = new DeathEvent(killer, player);
        feather.onDeath(player, event);

        assertFalse(player.getBag().contains(feather));
    }

    @Test
    void shouldUnregisterListenerAfterUse() {
        player.takeItem(feather);

        DeathEvent event = new DeathEvent(killer, player);
        feather.onDeath(player, event);

        assertFalse(player.getListeners().contains(feather));
    }

    @Test
    void shouldOnlyTriggerOnce() {
        player.takeItem(feather);
        player.setCurrentHealth(0);

        DeathEvent event1 = new DeathEvent(killer, player);
        feather.onDeath(player, event1);
        assertEquals(player.getMaxHealth() / 2, player.getCurrentHealth());

        player.setCurrentHealth(0);
        DeathEvent event2 = new DeathEvent(killer, player);
        feather.onDeath(player, event2);
        assertEquals(0, player.getCurrentHealth());
    }

    @Test
    void shouldDiscardArmorWhenEquippedAsArmor() {
        player.takeItem(feather);
        player.equipArmor(feather);
        assertEquals(feather, player.getEquippedArmor());

        DeathEvent event = new DeathEvent(killer, player);
        feather.onDeath(player, event);

        assertNull(player.getEquippedArmor());
    }

    @Test
    void shouldIgnoreHurtEvent() {
        player.takeItem(feather);
        int hpBefore = player.getCurrentHealth();

        AttackEvent event = new AttackEvent(killer, player, 20);
        feather.onHurt(player, event);

        assertEquals(hpBefore, player.getCurrentHealth());
    }

    @Test
    void shouldIgnoreFightWinEvent() {
        player.takeItem(feather);
        int hpBefore = player.getCurrentHealth();

        FightWinEvent event = new FightWinEvent(player, killer);
        feather.onFightWin(player, event);

        assertEquals(hpBefore, player.getCurrentHealth());
    }

    @Test
    void usedByShouldNotThrow() {
        assertDoesNotThrow(() -> feather.usedBy(player));
    }
}