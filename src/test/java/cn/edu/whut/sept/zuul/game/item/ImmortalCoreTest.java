package cn.edu.whut.sept.zuul.game.item;

import cn.edu.whut.sept.zuul.game.Player;
import cn.edu.whut.sept.zuul.game.Room;
import cn.edu.whut.sept.zuul.game.combat.event.AttackEvent;
import cn.edu.whut.sept.zuul.game.combat.event.DeathEvent;
import cn.edu.whut.sept.zuul.game.combat.event.FightWinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImmortalCoreTest {

    private ImmortalCore core;
    private Player player;
    private Player killer;
    private Room room;

    @BeforeEach
    void setUp() {
        core = new ImmortalCore();
        room = new Room("arena", "arena");
        player = new Player(1, "Hero", room);
        killer = new Player(2, "Villain", room);
    }

    @Test
    void shouldBePassiveEffect() {
        assertTrue(core.isPassiveEffect());
        assertFalse(core.isWeapon());
        assertFalse(core.isConsumable());
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertEquals("ImmortalCore", core.getName());
        assertEquals(2, core.getWeight());
    }

    @Test
    void shouldRegisterListenerWhenTaken() {
        core.takenBy(player);
        assertTrue(player.getListeners().contains(core));
    }

    @Test
    void shouldUnregisterListenerWhenDropped() {
        core.takenBy(player);
        core.droppedBy(player);
        assertFalse(player.getListeners().contains(core));
    }

    @Test
    void shouldPreventDeathByKeepingOneHP() {
        player.takeItem(core);
        player.setCurrentHealth(0);

        DeathEvent event = new DeathEvent(killer, player);
        core.onDeath(player, event);

        assertEquals(1, player.getCurrentHealth());
        assertFalse(player.isDead());
    }

    @Test
    void shouldRemoveFromBagOnUse() {
        player.takeItem(core);
        assertTrue(player.getBag().contains(core));

        DeathEvent event = new DeathEvent(killer, player);
        core.onDeath(player, event);

        assertFalse(player.getBag().contains(core));
    }

    @Test
    void shouldUnregisterListenerAfterUse() {
        player.takeItem(core);

        DeathEvent event = new DeathEvent(killer, player);
        core.onDeath(player, event);

        assertFalse(player.getListeners().contains(core));
    }

    @Test
    void shouldOnlyTriggerOnce() {
        player.takeItem(core);
        player.setCurrentHealth(0);

        DeathEvent event1 = new DeathEvent(killer, player);
        core.onDeath(player, event1);
        assertEquals(1, player.getCurrentHealth());

        player.setCurrentHealth(0);
        DeathEvent event2 = new DeathEvent(killer, player);
        core.onDeath(player, event2);
        assertEquals(0, player.getCurrentHealth());
    }

    @Test
    void shouldDiscardArmorWhenEquippedAsArmor() {
        player.takeItem(core);
        player.equipArmor(core);
        assertEquals(core, player.getEquippedArmor());

        DeathEvent event = new DeathEvent(killer, player);
        core.onDeath(player, event);

        assertNull(player.getEquippedArmor());
    }

    @Test
    void shouldIgnoreHurtEvent() {
        player.takeItem(core);
        int hpBefore = player.getCurrentHealth();

        AttackEvent event = new AttackEvent(killer, player, 20);
        core.onHurt(player, event);

        assertEquals(hpBefore, player.getCurrentHealth());
    }

    @Test
    void shouldIgnoreFightWinEvent() {
        player.takeItem(core);
        int hpBefore = player.getCurrentHealth();

        FightWinEvent event = new FightWinEvent(player, killer);
        core.onFightWin(player, event);

        assertEquals(hpBefore, player.getCurrentHealth());
    }

    @Test
    void usedByShouldNotThrow() {
        assertDoesNotThrow(() -> core.usedBy(player));
    }
}