package cn.edu.whut.sept.zuul.game.item;

import cn.edu.whut.sept.zuul.game.Player;
import cn.edu.whut.sept.zuul.game.Room;
import cn.edu.whut.sept.zuul.game.combat.event.AttackEvent;
import cn.edu.whut.sept.zuul.game.combat.event.DeathEvent;
import cn.edu.whut.sept.zuul.game.combat.event.FightWinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BerserkerTotemTest {

    private BerserkerTotem totem;
    private Player player;
    private Player attacker;
    private Room room;

    @BeforeEach
    void setUp() {
        totem = new BerserkerTotem();
        room = new Room("arena", "arena");
        player = new Player(1, "Warrior", room);
        attacker = new Player(2, "Enemy", room);
    }

    @Test
    void shouldBePassiveEffect() {
        assertTrue(totem.isPassiveEffect());
        assertFalse(totem.isWeapon());
        assertFalse(totem.isConsumable());
    }

    @Test
    void shouldHaveCorrectProperties() {
        assertEquals("BerserkerTotem", totem.getName());
        assertEquals(5, totem.getWeight());
    }

    @Test
    void shouldRegisterListenerWhenTaken() {
        totem.takenBy(player);
        assertTrue(player.getListeners().contains(totem));
    }

    @Test
    void shouldUnregisterListenerWhenDropped() {
        totem.takenBy(player);
        totem.droppedBy(player);
        assertFalse(player.getListeners().contains(totem));
    }

    @Test
    void shouldActivateRageOnHurt() {
        int baseAttack = player.getAttack();
        player.takeItem(totem);

        AttackEvent event = new AttackEvent(attacker, player, 20);
        totem.onHurt(player, event);

        assertEquals(baseAttack + 8, player.getAttack());
    }

    @Test
    void shouldNotDoubleActivateRage() {
        player.takeItem(totem);
        int baseAttack = player.getAttack();

        AttackEvent event1 = new AttackEvent(attacker, player, 10);
        totem.onHurt(player, event1);

        AttackEvent event2 = new AttackEvent(attacker, player, 10);
        totem.onHurt(player, event2);

        assertEquals(baseAttack + 8, player.getAttack());
    }

    @Test
    void shouldDeactivateRageWhenDroppedWhileActive() {
        int baseAttack = player.getAttack();
        player.takeItem(totem);

        AttackEvent event = new AttackEvent(attacker, player, 20);
        totem.onHurt(player, event);
        assertEquals(baseAttack + 8, player.getAttack());

        totem.droppedBy(player);
        assertEquals(baseAttack, player.getAttack());
    }

    @Test
    void shouldNotActivateForNonOwner() {
        int baseAttack = player.getAttack();
        Player otherOwner = new Player(3, "OtherOwner", room);
        otherOwner.takeItem(totem);

        AttackEvent event = new AttackEvent(attacker, player, 20);
        totem.onHurt(player, event);

        assertEquals(baseAttack, player.getAttack());
    }

    @Test
    void shouldIgnoreDeathEvent() {
        player.takeItem(totem);
        int baseAttack = player.getAttack();

        DeathEvent event = new DeathEvent(attacker, player);
        totem.onDeath(player, event);

        assertEquals(baseAttack, player.getAttack());
    }

    @Test
    void shouldIgnoreFightWinEvent() {
        player.takeItem(totem);
        int baseAttack = player.getAttack();

        FightWinEvent event = new FightWinEvent(player, attacker);
        totem.onFightWin(player, event);

        assertEquals(baseAttack, player.getAttack());
    }

    @Test
    void usedByShouldNotThrow() {
        assertDoesNotThrow(() -> totem.usedBy(player));
    }
}