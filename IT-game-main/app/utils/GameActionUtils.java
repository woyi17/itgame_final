package utils;

import java.util.HashSet;
import java.util.Set;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;

/**
 * Core game mechanics: movement, attack, counter-attack, unit death, avatar damage.
 * SC9:  Adjacent Attack, SC10: Move-And-Attack, SC11: Move
 * SC12: Counter Attack, SC13: Unit Death, SC14: Avatar Damage, SC15: Game Win/Loss
 * SC22: Provoke, SC23: Rush, SC24: Flying
 */
public final class GameActionUtils {

  private GameActionUtils() {}

  public static final int HUMAN_PLAYER       = 1;
  public static final int AI_PLAYER          = 2;
  public static final int DEFAULT_MOVE_RANGE = 2;

  // SC11/SC24: valid move tiles; flying = anywhere; SC22: blocked if adjacent provoke enemy
  public static Set<int[]> getMoveTiles(GameState gs, int tilex, int tiley, int owner) {
    Set<int[]> result = new HashSet<>();
    Unit mover = gs.units[tilex][tiley];
    if (mover == null) return result;
    boolean flying = Keywords.hasKeyword(gs, mover.getId(), Keywords.FLYING);
    if (isAdjacentToProvoke(gs, tilex, tiley, owner)) return result;
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] != null) continue;
        int dist = Math.abs(x - tilex) + Math.abs(y - tiley);
        if (dist == 0) continue;
        if (flying || dist <= DEFAULT_MOVE_RANGE) result.add(new int[]{x, y});
      }
    }
    return result;
  }

  // SC9/SC22: adjacent enemies; if any has Provoke only those are valid targets
  public static Set<int[]> getAdjacentEnemyTiles(GameState gs, int tilex, int tiley, int attackerOwner) {
    int enemy = (attackerOwner == HUMAN_PLAYER) ? AI_PLAYER : HUMAN_PLAYER;
    Set<int[]> all = new HashSet<>(), prov = new HashSet<>();
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int[] d : dirs) {
      int ax = tilex + d[0], ay = tiley + d[1];
      if (ax < 1 || ax > 9 || ay < 1 || ay > 5) continue;
      if (gs.units[ax][ay] != null && gs.unitOwner[ax][ay] == enemy) {
        int[] p = {ax, ay};
        all.add(p);
        if (Keywords.hasKeyword(gs, gs.units[ax][ay].getId(), Keywords.PROVOKE)) prov.add(p);
      }
    }
    return prov.isEmpty() ? all : prov;
  }

  private static boolean isAdjacentToProvoke(GameState gs, int x, int y, int owner) {
    int enemy = (owner == HUMAN_PLAYER) ? AI_PLAYER : HUMAN_PLAYER;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int[] d : dirs) {
      int ax = x + d[0], ay = y + d[1];
      if (ax < 1 || ax > 9 || ay < 1 || ay > 5) continue;
      if (gs.units[ax][ay] != null && gs.unitOwner[ax][ay] == enemy
          && Keywords.hasKeyword(gs, gs.units[ax][ay].getId(), Keywords.PROVOKE)) return true;
    }
    return false;
  }

  public static void refreshActionHighlights(ActorRef out, GameState gs, int tilex, int tiley, int owner) {
    HighlightUtils.clearAll(out, gs);
    gs.moveTargets.clear();
    gs.attackTargets.clear();
    Unit unit = gs.units[tilex][tiley];
    if (unit == null) return;
    if (!gs.actedUnits.contains(unit.getId()) && !gs.stunnedUnits.contains(unit.getId())) {
      boolean cantAtk = Keywords.hasKeyword(gs, unit.getId(), Keywords.CANT_ATTACK_FIRST_TURN)
                     && gs.summonedThisTurn.contains(unit.getId());
      Set<int[]> moves = getMoveTiles(gs, tilex, tiley, owner);
      for (int[] xy : moves) gs.moveTargets.add(HighlightUtils.key(xy[0], xy[1]));
      HighlightUtils.highlightMove(out, gs, moves);
      if (!cantAtk) {
        Set<int[]> atks = getAdjacentEnemyTiles(gs, tilex, tiley, owner);
        for (int[] xy : atks) gs.attackTargets.add(HighlightUtils.key(xy[0], xy[1]));
        HighlightUtils.highlightAttack(out, gs, atks);
      }
    }
    BasicCommands.addPlayer1Notification(out,
        "Selected. Move=" + gs.moveTargets.size() + " Attack=" + gs.attackTargets.size()
        + (gs.stunnedUnits.contains(unit.getId()) ? " [STUNNED]" : ""), 2);
  }

  // SC9/SC12: attack with counter-attack
  public static void resolveAttack(ActorRef out, GameState gs, Unit attacker, int attackerX, int attackerY,
                                   Unit target, int targetX, int targetY) {
    int atkDmg      = gs.unitAttack.getOrDefault(attacker.getId(), 2);
    int atkOwner    = gs.unitOwner[attackerX][attackerY];
    int tgtOwner    = gs.unitOwner[targetX][targetY];

    BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.hit);
    sleep(400);

    boolean alive = applyDamageToUnit(out, gs, target, targetX, targetY, tgtOwner, atkDmg);
    TriggerUtils.onUnitDealsDamage(out, gs, attacker, atkOwner, target, tgtOwner, atkDmg);

    // SC12: counter-attack if target survived and still exists
    if (alive && gs.units[targetX][targetY] != null) {
      int ctrDmg = gs.unitAttack.getOrDefault(target.getId(), 2);
      BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
      BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.hit);
      sleep(400);
      applyDamageToUnit(out, gs, attacker, attackerX, attackerY, atkOwner, ctrDmg);
      TriggerUtils.onUnitDealsDamage(out, gs, target, tgtOwner, attacker, atkOwner, ctrDmg);
    }
    if (gs.units[attackerX][attackerY] == attacker) gs.actedUnits.add(attacker.getId());
    clearSelectionState(out, gs);
  }

  // SC13/SC14/SC15: apply damage; returns true if unit survived
  public static boolean applyDamageToUnit(ActorRef out, GameState gs, Unit target,
                                           int tx, int ty, int owner, int damage) {
    int newHp = gs.unitHealth.getOrDefault(target.getId(), 0) - damage;
    gs.unitHealth.put(target.getId(), newHp);
    BasicCommands.setUnitHealth(out, target, Math.max(newHp, 0));
    boolean isAvatar = (target.getId() == 100 || target.getId() == 200);
    if (isAvatar) {
      Player p = (owner == HUMAN_PLAYER) ? gs.player1 : gs.player2;
      p.setHealth(Math.max(newHp, 0));
      if (owner == HUMAN_PLAYER) BasicCommands.setPlayer1Health(out, p);
      else                       BasicCommands.setPlayer2Health(out, p);
      TriggerUtils.onAvatarDamaged(out, gs, owner, damage);
    }
    if (newHp <= 0) {
      if (isAvatar) handleGameOver(out, gs, owner);
      else          killUnit(out, gs, target, tx, ty);
      return false;
    }
    return true;
  }

  // SC13: kill unit, death anim, remove from board, fire Deathwatch
  public static void killUnit(ActorRef out, GameState gs, Unit unit, int x, int y) {
    int owner = gs.unitOwner[x][y];
    int ms = BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.death);
    sleep(Math.max(ms, 300));
    BasicCommands.deleteUnit(out, unit);
    gs.units[x][y] = null;
    gs.unitOwner[x][y] = 0;
    gs.unitHealth.remove(unit.getId());
    gs.unitAttack.remove(unit.getId());
    gs.unitMaxHealth.remove(unit.getId());
    gs.unitKeywords.remove(unit.getId());
    gs.actedUnits.remove(unit.getId());
    gs.stunnedUnits.remove(unit.getId());
    gs.summonedThisTurn.remove(unit.getId());
    TriggerUtils.onUnitDeath(out, gs, unit, owner);
  }

  // SC15: game over
  private static void handleGameOver(ActorRef out, GameState gs, int losingOwner) {
    gs.gameOver = true;
    int ax = (losingOwner == HUMAN_PLAYER) ? gs.humanAvatarX : gs.aiAvatarX;
    int ay = (losingOwner == HUMAN_PLAYER) ? gs.humanAvatarY : gs.aiAvatarY;
    Unit av = gs.units[ax][ay];
    if (av != null) {
      BasicCommands.playUnitAnimation(out, av, UnitAnimationType.death);
      sleep(800);
      BasicCommands.deleteUnit(out, av);
      gs.units[ax][ay] = null; gs.unitOwner[ax][ay] = 0;
    }
    sleep(400);
    boolean playerWon = (losingOwner == AI_PLAYER);
    BasicCommands.showGameOver(out, playerWon);
  }

  // UnitStopped callback
  public static void finishMoveAndMaybeAttack(ActorRef out, GameState gs, int unitId, int tilex, int tiley) {
    if (gs.selectedUnit == null || gs.selectedUnit.getId() != unitId) return;
    gs.selectedUnitX = tilex;
    gs.selectedUnitY = tiley;
    Tile tile = HighlightUtils.getTile(gs, tilex, tiley);
    if (tile != null) gs.selectedUnit.setPositionByTile(tile);
    if (!gs.pendingMoveAndAttack) return;

    boolean cantAtk = Keywords.hasKeyword(gs, unitId, Keywords.CANT_ATTACK_FIRST_TURN)
                   && gs.summonedThisTurn.contains(unitId);
    HighlightUtils.clearAll(out, gs);
    gs.moveTargets.clear();
    gs.attackTargets.clear();

    if (!cantAtk) {
      Set<int[]> atks = getAdjacentEnemyTiles(gs, tilex, tiley, HUMAN_PLAYER);
      for (int[] xy : atks) gs.attackTargets.add(HighlightUtils.key(xy[0], xy[1]));
      HighlightUtils.highlightAttack(out, gs, atks);
      if (atks.isEmpty()) {
        gs.actedUnits.add(unitId);
        BasicCommands.addPlayer1Notification(out, "Move complete. No adjacent enemy.", 2);
        clearSelectionState(out, gs);
      } else {
        BasicCommands.addPlayer1Notification(out, "Move complete. Click enemy to attack.", 2);
      }
    } else {
      gs.actedUnits.add(unitId);
      clearSelectionState(out, gs);
    }
  }

  public static void clearSelectionState(ActorRef out, GameState gs) {
    HighlightUtils.clearAll(out, gs);
    gs.selectedUnit = null; gs.selectedUnitX = -1; gs.selectedUnitY = -1;
    gs.moveTargets.clear(); gs.attackTargets.clear();
    gs.pendingMoveAndAttack = false;
  }

  private static void sleep(int ms) {
    if (ms <= 0) return;
    try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }
}
