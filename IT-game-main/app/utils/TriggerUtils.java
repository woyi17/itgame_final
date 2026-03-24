package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;

/**
 * Handles ability triggers for all units on the board.
 * SC17: Ability Trigger: Unit Summoned (Opening Gambit)
 * SC18: Ability Trigger: Unit Death (Deathwatch)
 * SC19: Ability Trigger: Avatar Damaged
 * SC20: Ability Trigger: Unit Deals Damage
 */
public final class TriggerUtils {

  private TriggerUtils() {}

  /**
   * SC17: Called when a unit is summoned. Fires Opening Gambit effects.
   */
  public static void onUnitSummoned(ActorRef out, GameState gs, Unit unit, int x, int y, int owner) {
    Set<String> kw = gs.unitKeywords.get(unit.getId());
    if (kw == null) return;

    if (kw.contains(Keywords.OPENING_GAMBIT_WRAITHLING)) {
      // Gloom Chaser: summon a Wraithling on a random adjacent empty tile
      summonWraithlingNear(out, gs, x, y, owner);
    }

    if (kw.contains(Keywords.OPENING_GAMBIT_DESTROY)) {
      // Nightsorrow Assassin: destroy a nearby enemy unit with <=2 attack
      destroyNearbyWeakEnemy(out, gs, x, y, owner);
    }
  }

  /**
   * SC18: Called when any unit dies. Fires Deathwatch effects for all units on board.
   * @param deadUnit the unit that just died
   * @param deadOwner owner of the dead unit
   */
  public static void onUnitDeath(ActorRef out, GameState gs, Unit deadUnit, int deadOwner) {
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        Unit u = gs.units[x][y];
        if (u == null) continue;
        Set<String> kw = gs.unitKeywords.get(u.getId());
        if (kw == null) continue;
        int owner = gs.unitOwner[x][y];

        if (kw.contains(Keywords.DEATHWATCH_ATTACK_BUFF)) {
          // Bad Omen: +1 attack
          int atk = gs.unitAttack.getOrDefault(u.getId(), 0) + 1;
          gs.unitAttack.put(u.getId(), atk);
          BasicCommands.setUnitAttack(out, u, atk);
        }

        if (kw.contains(Keywords.DEATHWATCH_STAT_BUFF)) {
          // Shadow Watcher: +1/+1
          int atk = gs.unitAttack.getOrDefault(u.getId(), 0) + 1;
          int hp = gs.unitHealth.getOrDefault(u.getId(), 0) + 1;
          int maxHp = gs.unitMaxHealth.getOrDefault(u.getId(), hp) + 1;
          gs.unitAttack.put(u.getId(), atk);
          gs.unitHealth.put(u.getId(), hp);
          gs.unitMaxHealth.put(u.getId(), maxHp);
          BasicCommands.setUnitAttack(out, u, atk);
          BasicCommands.setUnitHealth(out, u, hp);
        }

        if (kw.contains(Keywords.DEATHWATCH_SUMMON_WRAITHLING)) {
          // Bloodmoon Priestess: summon Wraithling near this unit
          summonWraithlingNear(out, gs, x, y, owner);
        }

        if (kw.contains(Keywords.DEATHWATCH_SHADOW)) {
          // Shadowdancer: deal 1 to enemy avatar, heal 1 to friendly avatar
          int enemyOwner = (owner == GameActionUtils.HUMAN_PLAYER) ? GameActionUtils.AI_PLAYER : GameActionUtils.HUMAN_PLAYER;
          damageAvatar(out, gs, enemyOwner, 1);
          healAvatar(out, gs, owner, 1);
        }
      }
    }
  }

  /**
   * SC19: Called when an avatar takes damage. Fires reactive abilities.
   * @param avatarOwner owner whose avatar was damaged
   * @param damage amount of damage dealt
   */
  public static void onAvatarDamaged(ActorRef out, GameState gs, int avatarOwner, int damage) {
    // Silverguard Squire: whenever YOUR avatar takes damage, all friendly units +1/+1
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        Unit u = gs.units[x][y];
        if (u == null) continue;
        if (gs.unitOwner[x][y] != avatarOwner) continue;
        Set<String> kw = gs.unitKeywords.get(u.getId());
        if (kw == null) continue;

        if (kw.contains(Keywords.AVATAR_DAMAGE_BUFF)) {
          // Give +1/+1 to all friendly units (not including Squire itself? Actually give to all friendlies)
          buffAllFriendlyUnits(out, gs, avatarOwner, 1, 1);
          break; // only need to trigger once even if multiple squires
        }
      }
    }
  }

  /**
   * SC20: Called when a unit deals damage. Hook for future abilities.
   */
  public static void onUnitDealsDamage(ActorRef out, GameState gs, Unit attacker, int attackerOwner,
                                        Unit target, int targetOwner, int damage) {
    // Framework hook — no current cards use this trigger but the infrastructure is ready
  }

  // ---- Helper methods ----

  /**
   * Summons a Wraithling on a random adjacent empty tile near (cx, cy).
   */
  public static void summonWraithlingNear(ActorRef out, GameState gs, int cx, int cy, int owner) {
    List<int[]> empty = new ArrayList<>();
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    for (int[] d : dirs) {
      int nx = cx + d[0], ny = cy + d[1];
      if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
      if (gs.units[nx][ny] == null) empty.add(new int[]{nx, ny});
    }
    if (empty.isEmpty()) return;
    int[] pos = empty.get((int)(Math.random() * empty.size()));
    spawnWraithling(out, gs, pos[0], pos[1], owner);
  }

  /**
   * Spawns a Wraithling token at (x, y) owned by the given player.
   */
  public static void spawnWraithling(ActorRef out, GameState gs, int x, int y, int owner) {
    Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gs.nextUnitId++, Unit.class);
    if (wraithling == null) return;
    Tile tile = HighlightUtils.getTile(gs, x, y);
    if (tile == null) return;

    wraithling.setPositionByTile(tile);
    gs.units[x][y] = wraithling;
    gs.unitOwner[x][y] = owner;
    gs.unitHealth.put(wraithling.getId(), 1);
    gs.unitAttack.put(wraithling.getId(), 1);
    gs.unitMaxHealth.put(wraithling.getId(), 1);

    EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
    if (summonEffect != null) BasicCommands.playEffectAnimation(out, summonEffect, tile);

    BasicCommands.drawUnit(out, wraithling, tile);
    BasicCommands.setUnitHealth(out, wraithling, 1);
    BasicCommands.setUnitAttack(out, wraithling, 1);
  }

  /**
   * Nightsorrow Assassin: destroy an adjacent (8-dir) enemy unit with <= 2 attack.
   */
  private static void destroyNearbyWeakEnemy(ActorRef out, GameState gs, int cx, int cy, int owner) {
    int enemyOwner = (owner == GameActionUtils.HUMAN_PLAYER) ? GameActionUtils.AI_PLAYER : GameActionUtils.HUMAN_PLAYER;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    for (int[] d : dirs) {
      int nx = cx + d[0], ny = cy + d[1];
      if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
      Unit target = gs.units[nx][ny];
      if (target == null) continue;
      if (gs.unitOwner[nx][ny] != enemyOwner) continue;
      // Don't destroy avatars
      if (target.getId() == 100 || target.getId() == 200) continue;
      int atk = gs.unitAttack.getOrDefault(target.getId(), 0);
      if (atk <= 2) {
        GameActionUtils.killUnit(out, gs, target, nx, ny);
        BasicCommands.addPlayer1Notification(out, "Opening Gambit: destroyed " + atk + " attack enemy!", 2);
        return;
      }
    }
  }

  /**
   * Deals damage to an avatar (not through normal combat — direct damage).
   */
  public static void damageAvatar(ActorRef out, GameState gs, int avatarOwner, int damage) {
    Unit avatar = (avatarOwner == GameActionUtils.HUMAN_PLAYER) ? gs.humanAvatar : gs.aiAvatar;
    if (avatar == null) return;
    int ax = (avatarOwner == GameActionUtils.HUMAN_PLAYER) ? gs.humanAvatarX : gs.aiAvatarX;
    int ay = (avatarOwner == GameActionUtils.HUMAN_PLAYER) ? gs.humanAvatarY : gs.aiAvatarY;
    GameActionUtils.applyDamageToUnit(out, gs, avatar, ax, ay, avatarOwner, damage);
  }

  /**
   * Heals an avatar up to its max health.
   */
  public static void healAvatar(ActorRef out, GameState gs, int avatarOwner, int amount) {
    Unit avatar = (avatarOwner == GameActionUtils.HUMAN_PLAYER) ? gs.humanAvatar : gs.aiAvatar;
    if (avatar == null) return;
    Player p = (avatarOwner == GameActionUtils.HUMAN_PLAYER) ? gs.player1 : gs.player2;
    int cur = p.getHealth();
    int newHp = Math.min(cur + amount, 20);
    p.setHealth(newHp);
    gs.unitHealth.put(avatar.getId(), newHp);
    BasicCommands.setUnitHealth(out, avatar, newHp);
    if (avatarOwner == GameActionUtils.HUMAN_PLAYER) BasicCommands.setPlayer1Health(out, p);
    else                                              BasicCommands.setPlayer2Health(out, p);
  }

  /**
   * Gives +atk/+hp to all friendly units owned by the given player.
   */
  public static void buffAllFriendlyUnits(ActorRef out, GameState gs, int owner, int atkBuff, int hpBuff) {
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        Unit u = gs.units[x][y];
        if (u == null || gs.unitOwner[x][y] != owner) continue;
        int atk = gs.unitAttack.getOrDefault(u.getId(), 0) + atkBuff;
        int hp  = gs.unitHealth.getOrDefault(u.getId(), 0) + hpBuff;
        int maxHp = gs.unitMaxHealth.getOrDefault(u.getId(), hp) + hpBuff;
        gs.unitAttack.put(u.getId(), atk);
        gs.unitHealth.put(u.getId(), hp);
        gs.unitMaxHealth.put(u.getId(), maxHp);
        BasicCommands.setUnitAttack(out, u, atk);
        BasicCommands.setUnitHealth(out, u, hp);
      }
    }
  }
}
