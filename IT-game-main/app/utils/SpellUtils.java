package utils;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;

/**
 * Handles all spell card effects.
 * SC21: Play Spell Card
 * SC26: Spell Ability: Direct Damage
 * SC27: Spell Ability: Heal
 * SC28: Spell Ability: Destroy Unit
 * SC29: Spell Ability: Stun
 * SC30: Spell Ability: Summon Unit
 */
public final class SpellUtils {

  private SpellUtils() {}

  /**
   * Applies the spell effect of the given card on the target tile.
   * Called after the player clicks a valid target tile.
   */
  public static void applySpell(ActorRef out, GameState gs, String cardName, int targetX, int targetY) {
    switch (cardName) {
      case "Truestrike":
        spellDirectDamage(out, gs, targetX, targetY, 2);
        break;
      case "Sundrop Elixir":
        spellHeal(out, gs, targetX, targetY, 5);
        break;
      case "Beamshock":
        spellStun(out, gs, targetX, targetY);
        break;
      case "Dark Terminus":
        spellDestroyUnit(out, gs, targetX, targetY);
        break;
      case "Wraithling Swarm":
        spellSummonWraithlings(out, gs, 3);
        break;
      case "Horn of the Forsaken":
        spellAvatarBuff(out, gs, 2);
        break;
      default:
        BasicCommands.addPlayer1Notification(out, "Unknown spell: " + cardName, 2);
        break;
    }
  }

  /**
   * SC26: Deal direct damage to target unit/avatar.
   */
  public static void spellDirectDamage(ActorRef out, GameState gs, int tx, int ty, int damage) {
    Unit target = gs.units[tx][ty];
    if (target == null) return;

    Tile tile = HighlightUtils.getTile(gs, tx, ty);
    EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_projectiles);
    if (effect != null && tile != null) BasicCommands.playEffectAnimation(out, effect, tile);

    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.hit);
    int owner = gs.unitOwner[tx][ty];
    GameActionUtils.applyDamageToUnit(out, gs, target, tx, ty, owner, damage);

    BasicCommands.addPlayer1Notification(out, "Truestrike: dealt " + damage + " damage.", 2);
  }

  /**
   * SC27: Heal a target unit (up to its max health).
   */
  public static void spellHeal(ActorRef out, GameState gs, int tx, int ty, int amount) {
    Unit target = gs.units[tx][ty];
    if (target == null) return;

    Tile tile = HighlightUtils.getTile(gs, tx, ty);
    EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
    if (effect != null && tile != null) BasicCommands.playEffectAnimation(out, effect, tile);

    int cur = gs.unitHealth.getOrDefault(target.getId(), 1);
    int max = gs.unitMaxHealth.getOrDefault(target.getId(), cur);
    int newHp = Math.min(cur + amount, max);
    gs.unitHealth.put(target.getId(), newHp);
    BasicCommands.setUnitHealth(out, target, newHp);

    // If it's an avatar, also update player health
    int owner = gs.unitOwner[tx][ty];
    if (target.getId() == 100 || target.getId() == 200) {
      Player p = (owner == GameActionUtils.HUMAN_PLAYER) ? gs.player1 : gs.player2;
      p.setHealth(newHp);
      if (owner == GameActionUtils.HUMAN_PLAYER) BasicCommands.setPlayer1Health(out, p);
      else                                        BasicCommands.setPlayer2Health(out, p);
    }

    BasicCommands.addPlayer1Notification(out, "Sundrop Elixir: healed " + (newHp - cur) + " HP.", 2);
  }

  /**
   * SC28: Destroy a non-avatar enemy unit. Dark Terminus also summons a Wraithling.
   */
  public static void spellDestroyUnit(ActorRef out, GameState gs, int tx, int ty) {
    Unit target = gs.units[tx][ty];
    if (target == null) return;

    Tile tile = HighlightUtils.getTile(gs, tx, ty);
    EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_martyrdom);
    if (effect != null && tile != null) {
      int millis = BasicCommands.playEffectAnimation(out, effect, tile);
      sleep(millis);
    }

    int owner = gs.unitOwner[tx][ty];
    GameActionUtils.killUnit(out, gs, target, tx, ty);

    // Dark Terminus: summon Wraithling on that tile (if now empty)
    if (gs.units[tx][ty] == null) {
      TriggerUtils.spawnWraithling(out, gs, tx, ty, GameActionUtils.HUMAN_PLAYER);
    }

    BasicCommands.addPlayer1Notification(out, "Dark Terminus: unit destroyed, Wraithling summoned!", 2);
  }

  /**
   * SC29: Stun a target enemy unit (cannot move or attack next turn).
   */
  public static void spellStun(ActorRef out, GameState gs, int tx, int ty) {
    Unit target = gs.units[tx][ty];
    if (target == null) return;

    Tile tile = HighlightUtils.getTile(gs, tx, ty);
    EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_martyrdom);
    if (effect != null && tile != null) BasicCommands.playEffectAnimation(out, effect, tile);

    gs.stunnedUnits.add(target.getId());
    BasicCommands.addPlayer1Notification(out, "Beamshock: unit stunned!", 2);
  }

  /**
   * SC30: Summon multiple Wraithlings adjacent to the friendly avatar.
   */
  public static void spellSummonWraithlings(ActorRef out, GameState gs, int count) {
    int summoned = 0;
    int ax = gs.humanAvatarX;
    int ay = gs.humanAvatarY;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    java.util.List<int[]> available = new java.util.ArrayList<>();
    for (int[] d : dirs) {
      int nx = ax + d[0], ny = ay + d[1];
      if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
      if (gs.units[nx][ny] == null) available.add(new int[]{nx, ny});
    }
    java.util.Collections.shuffle(available);
    for (int[] pos : available) {
      if (summoned >= count) break;
      TriggerUtils.spawnWraithling(out, gs, pos[0], pos[1], GameActionUtils.HUMAN_PLAYER);
      summoned++;
    }
    BasicCommands.addPlayer1Notification(out, "Wraithling Swarm: summoned " + summoned + " Wraithlings!", 2);
  }

  /**
   * Horn of the Forsaken: give human avatar +2 attack.
   */
  public static void spellAvatarBuff(ActorRef out, GameState gs, int atkBonus) {
    Unit avatar = gs.humanAvatar;
    if (avatar == null) return;
    Tile tile = HighlightUtils.getTile(gs, gs.humanAvatarX, gs.humanAvatarY);
    EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
    if (effect != null && tile != null) BasicCommands.playEffectAnimation(out, effect, tile);

    int atk = gs.unitAttack.getOrDefault(avatar.getId(), 2) + atkBonus;
    gs.unitAttack.put(avatar.getId(), atk);
    BasicCommands.setUnitAttack(out, avatar, atk);
    BasicCommands.addPlayer1Notification(out, "Horn of the Forsaken: avatar gets +" + atkBonus + " attack!", 2);
  }

  /**
   * Returns the spell target type for a given card name.
   * Used by HighlightUtils to determine which tiles to highlight.
   * Types: "ANY_ENEMY", "ANY_FRIENDLY", "NON_AVATAR_ENEMY", "NO_TARGET"
   */
  public static String getSpellTargetType(String cardName) {
    switch (cardName) {
      case "Truestrike":   return "ANY_ENEMY";
      case "Beamshock":    return "ANY_ENEMY";
      case "Dark Terminus": return "NON_AVATAR_ENEMY";
      case "Sundrop Elixir": return "ANY_FRIENDLY";
      case "Wraithling Swarm": return "NO_TARGET";
      case "Horn of the Forsaken": return "NO_TARGET";
      default: return "NO_TARGET";
    }
  }

  private static void sleep(int millis) {
    if (millis <= 0) return;
    try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }
}
