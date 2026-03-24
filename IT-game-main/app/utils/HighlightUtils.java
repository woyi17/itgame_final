package utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;

/**
 * Utility helpers for all board highlighting.
 * SC6:  Board Highlighting, Move (mode=1)
 * SC7:  Board Highlighting, Attack (mode=2)
 * SC8:  Board Highlighting, Clear (mode=0)
 * SC31: Board Highlighting, Spell Targets
 * SC32: Board Highlighting, Summon Unit positions
 */
public final class HighlightUtils {

  private HighlightUtils() {}

  public static int key(int x, int y)  { return (x << 8) | (y & 0xFF); }
  public static int keyX(int key)       { return (key >> 8) & 0xFF; }
  public static int keyY(int key)       { return key & 0xFF; }

  /** SC8: Clear all highlighted tiles back to normal. */
  public static void clearAll(ActorRef out, GameState gs) {
    for (int k : gs.highlighted) {
      Tile t = getTile(gs, keyX(k), keyY(k));
      if (t != null) BasicCommands.drawTile(out, t, 0);
    }
    gs.highlighted.clear();
    gs.moveHighlighted.clear();
    gs.attackHighlighted.clear();
    for (int k : gs.summonTargets) {
      Tile t = getTile(gs, keyX(k), keyY(k));
      if (t != null) BasicCommands.drawTile(out, t, 0);
    }
    for (int k : gs.spellTargets) {
      Tile t = getTile(gs, keyX(k), keyY(k));
      if (t != null) BasicCommands.drawTile(out, t, 0);
    }
    gs.summonTargets.clear();
    gs.spellTargets.clear();
  }

  /** SC6: Highlight move tiles (mode 1). */
  public static void highlightMove(ActorRef out, GameState gs, Set<int[]> coords) {
    highlight(out, gs, coords, 1, gs.moveHighlighted);
  }

  /** SC7: Highlight attack tiles (mode 2). */
  public static void highlightAttack(ActorRef out, GameState gs, Set<int[]> coords) {
    highlight(out, gs, coords, 2, gs.attackHighlighted);
  }

  private static void highlight(ActorRef out, GameState gs, Set<int[]> coords, int mode, Set<Integer> bucket) {
    for (int[] xy : coords) {
      Tile t = getTile(gs, xy[0], xy[1]);
      if (t == null) continue;
      BasicCommands.drawTile(out, t, mode);
      int k = key(xy[0], xy[1]);
      gs.highlighted.add(k);
      bucket.add(k);
    }
  }

  /**
   * SC32: Highlight valid summon positions (mode 1, blue).
   * Empty tiles adjacent (8-dir) to any friendly unit.
   */
  public static void highlightSummonPositions(ActorRef out, GameState gs, int owner) {
    Set<int[]> positions = getSummonPositions(gs, owner);
    for (int[] xy : positions) {
      Tile t = getTile(gs, xy[0], xy[1]);
      if (t == null) continue;
      BasicCommands.drawTile(out, t, 1);
      gs.summonTargets.add(key(xy[0], xy[1]));
    }
  }

  public static Set<int[]> getSummonPositions(GameState gs, int owner) {
    Set<Integer> seen = new HashSet<>();
    List<int[]> result = new ArrayList<>();
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] == null || gs.unitOwner[x][y] != owner) continue;
        for (int[] d : dirs) {
          int nx = x + d[0], ny = y + d[1];
          if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
          if (gs.units[nx][ny] != null) continue;
          int k = key(nx, ny);
          if (!seen.contains(k)) { seen.add(k); result.add(new int[]{nx, ny}); }
        }
      }
    }
    return new HashSet<>(result);
  }

  /**
   * SC31: Highlight valid spell targets.
   * targetType: "ANY_ENEMY", "NON_AVATAR_ENEMY", "ANY_FRIENDLY"
   */
  public static void highlightSpellTargets(ActorRef out, GameState gs, int casterOwner, String targetType) {
    if ("NO_TARGET".equals(targetType)) return;
    int mode = "ANY_FRIENDLY".equals(targetType) ? 1 : 2;
    int enemyOwner = (casterOwner == GameActionUtils.HUMAN_PLAYER)
        ? GameActionUtils.AI_PLAYER : GameActionUtils.HUMAN_PLAYER;
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] == null) continue;
        int unitOwner = gs.unitOwner[x][y];
        int unitId    = gs.units[x][y].getId();
        boolean valid = false;
        switch (targetType) {
          case "ANY_ENEMY":       valid = (unitOwner == enemyOwner); break;
          case "NON_AVATAR_ENEMY": valid = (unitOwner == enemyOwner) && (unitId != 100 && unitId != 200); break;
          case "ANY_FRIENDLY":    valid = (unitOwner == casterOwner); break;
        }
        if (valid) {
          Tile t = getTile(gs, x, y);
          if (t != null) BasicCommands.drawTile(out, t, mode);
          gs.spellTargets.add(key(x, y));
        }
      }
    }
  }

  public static Tile getTile(GameState gs, int x, int y) {
    if (gs.board == null) return null;
    if (x < 1 || x >= gs.board.length) return null;
    if (y < 1 || y >= gs.board[0].length) return null;
    return gs.board[x][y];
  }

  public static Set<int[]> manhattanRange(int x, int y, int range, int maxX, int maxY) {
    Set<int[]> out = new HashSet<>();
    for (int dx = -range; dx <= range; dx++) {
      for (int dy = -range; dy <= range; dy++) {
        int dist = Math.abs(dx) + Math.abs(dy);
        if (dist == 0 || dist > range) continue;
        int nx = x + dx, ny = y + dy;
        if (nx < 1 || nx > maxX || ny < 1 || ny > maxY) continue;
        out.add(new int[]{nx, ny});
      }
    }
    return out;
  }
}
