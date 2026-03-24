package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.CardUtils;
import utils.GameActionUtils;
import utils.HighlightUtils;
import utils.Keywords;
import utils.SpellUtils;
import utils.StaticConfFiles;
import utils.TriggerUtils;

/**
 * SC16: End Turn — handles full turn cycle including AI turn.
 * SC1:  Drawing Cards (draw at start of each turn)
 * SC4:  Mana Gain (gain mana each turn)
 */
public class EndTurnClicked implements EventProcessor {

  @Override
  public void processEvent(ActorRef out, GameState gs, JsonNode message) {
    if (gs.gameOver) return;

    // Clear any pending selection/highlight state
    GameActionUtils.clearSelectionState(out, gs);
    CardUtils.clearCardSelection(out, gs);

    // === AI TURN ===
    gs.currentTurn = 2;
    gs.turnNumber++;
    BasicCommands.addPlayer1Notification(out, "AI Turn...", 2);

    // SC4: Give AI its mana
    CardUtils.setTurnMana(out, gs, 2);

    // SC1: AI draws a card
    CardUtils.drawCard(out, gs, 2);

    // AI plays cards from hand before moving units
    runAICards(out, gs);

    // Simple AI: try to attack with each unit, then move toward human
    runSimpleAI(out, gs);

    sleep(500);

    // === HUMAN TURN ===
    gs.currentTurn = 1;
    gs.turnNumber++;

    // Clear per-turn tracking for human units
    gs.actedUnits.clear();
    gs.summonedThisTurn.clear();
    // Stunned units become un-stunned after a full round (remove stuns placed by AI this turn)
    gs.stunnedUnits.clear();

    // SC4: Give player 1 mana
    CardUtils.setTurnMana(out, gs, 1);

    // SC1: Draw a card for human
    CardUtils.drawCard(out, gs, 1);

    BasicCommands.addPlayer1Notification(out,
        "Your turn! Mana: " + gs.player1.getMana() + ", Cards: " + gs.hand1.size(), 3);
  }

  /**
   * Simple AI logic: attack adjacent enemies, then move toward the human avatar.
   */
  private static void runSimpleAI(ActorRef out, GameState gs) {
    // Collect all AI units
    java.util.List<int[]> aiUnits = new java.util.ArrayList<>();
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] != null && gs.unitOwner[x][y] == GameActionUtils.AI_PLAYER) {
          aiUnits.add(new int[]{x, y});
        }
      }
    }

    for (int[] pos : aiUnits) {
      if (gs.gameOver) return;
      int x = pos[0], y = pos[1];
      Unit unit = gs.units[x][y];
      if (unit == null) continue;
      if (gs.stunnedUnits.contains(unit.getId())) continue; // SC29: stunned skip their turn
      if (gs.actedUnits.contains(unit.getId())) continue;   // summoned without Rush this turn

      // Try to attack first
      java.util.Set<int[]> targets = GameActionUtils.getAdjacentEnemyTiles(gs, x, y, GameActionUtils.AI_PLAYER);
      if (!targets.isEmpty()) {
        int[] t = targets.iterator().next();
        Unit target = gs.units[t[0]][t[1]];
        if (target != null) {
          GameActionUtils.resolveAttack(out, gs, unit, x, y, target, t[0], t[1]);
          sleep(300);
          continue;
        }
      }

      // Try to move toward human avatar
      java.util.Set<int[]> moves = GameActionUtils.getMoveTiles(gs, x, y, GameActionUtils.AI_PLAYER);
      if (!moves.isEmpty()) {
        int bestX = x, bestY = y;
        int bestDist = Integer.MAX_VALUE;
        int bestFriendly = Integer.MAX_VALUE;
        int hx = gs.humanAvatarX, hy = gs.humanAvatarY;
        for (int[] m : moves) {
          int dist = Math.abs(m[0] - hx) + Math.abs(m[1] - hy);
          // Count adjacent friendly units at this tile (prefer tiles with fewer neighbours)
          int friendly = countAdjacentFriendly(gs, m[0], m[1], x, y);
          // Primary: closer to human; tie-break: fewer friendly neighbours
          if (dist < bestDist || (dist == bestDist && friendly < bestFriendly)) {
            bestDist = dist; bestFriendly = friendly; bestX = m[0]; bestY = m[1];
          }
        }
        if (bestX != x || bestY != y) {
          Unit movingUnit = gs.units[x][y];
          gs.units[bestX][bestY] = movingUnit;
          gs.unitOwner[bestX][bestY] = GameActionUtils.AI_PLAYER;
          gs.units[x][y] = null;
          gs.unitOwner[x][y] = 0;
          if (pos[0] == gs.aiAvatarX && pos[1] == gs.aiAvatarY) {
            gs.aiAvatarX = bestX; gs.aiAvatarY = bestY;
          }
          Tile tile = HighlightUtils.getTile(gs, bestX, bestY);
          if (tile != null) {
            BasicCommands.moveUnitToTile(out, movingUnit, tile);
            movingUnit.setPositionByTile(tile);
          }
          sleep(400);

          // Check for attack after move
          java.util.Set<int[]> atkAfterMove = GameActionUtils.getAdjacentEnemyTiles(gs, bestX, bestY, GameActionUtils.AI_PLAYER);
          if (!atkAfterMove.isEmpty()) {
            int[] t = atkAfterMove.iterator().next();
            Unit target = gs.units[t[0]][t[1]];
            if (target != null) {
              GameActionUtils.resolveAttack(out, gs, movingUnit, bestX, bestY, target, t[0], t[1]);
              sleep(300);
            }
          }
        }
      }
    }
  }

  // =========================================================
  // AI Card Playing
  // =========================================================

  /** Play cards from the AI's hand: at most 2 unit summons + all affordable spells. */
  private static void runAICards(ActorRef out, GameState gs) {
    java.util.List<Card> hand = gs.hand2;
    if (hand.isEmpty()) return;

    // Work on a sorted copy (cheapest first to maximise cards played)
    java.util.List<Card> sorted = new java.util.ArrayList<>(hand);
    sorted.sort((a, b) -> Integer.compare(a.getManacost(), b.getManacost()));

    java.util.List<Card> played = new java.util.ArrayList<>();
    int unitSummons = 0;
    java.util.List<int[]> usedPositions = new java.util.ArrayList<>();
    for (Card card : sorted) {
      if (gs.gameOver) break;
      if (gs.player2.getMana() < card.getManacost()) continue;

      if (card.isCreature()) {
        if (unitSummons >= 2) continue; // cap at 2 summons per turn to avoid visual crowding
        int[] pos = tryPlayAIUnit(out, gs, card, usedPositions);
        if (pos != null) { played.add(card); unitSummons++; usedPositions.add(pos); sleep(400); }
      } else {
        if (tryPlayAISpell(out, gs, card)) { played.add(card); sleep(400); }
      }
    }
    hand.removeAll(played);
  }

  /** Summon an AI unit card on the best available adjacent tile. Returns the position used, or null. */
  private static int[] tryPlayAIUnit(ActorRef out, GameState gs, Card card, java.util.List<int[]> usedPositions) {
    java.util.List<int[]> positions = getAISummonPositions(gs);
    if (positions.isEmpty()) return null;

    // Exclude positions adjacent to units summoned this turn (avoid crowding)
    if (!usedPositions.isEmpty()) {
      java.util.List<int[]> spread = new java.util.ArrayList<>();
      for (int[] p : positions) {
        boolean tooClose = false;
        for (int[] u : usedPositions) {
          if (Math.abs(p[0] - u[0]) <= 1 && Math.abs(p[1] - u[1]) <= 1) { tooClose = true; break; }
        }
        if (!tooClose) spread.add(p);
      }
      if (!spread.isEmpty()) positions = spread;
    }

    int hx = gs.humanAvatarX, hy = gs.humanAvatarY;
    int ax = gs.aiAvatarX,    ay = gs.aiAvatarY;
    int[] best = positions.get(0);
    int bestToHuman = Integer.MAX_VALUE;
    int bestFromAI  = 0;
    for (int[] p : positions) {
      int toHuman  = Math.abs(p[0] - hx) + Math.abs(p[1] - hy);
      int fromAI   = Math.abs(p[0] - ax) + Math.abs(p[1] - ay);
      if (toHuman < bestToHuman || (toHuman == bestToHuman && fromAI > bestFromAI)) {
        bestToHuman = toHuman; bestFromAI = fromAI; best = p;
      }
    }
    aiSummonUnit(out, gs, card, best[0], best[1]);
    return best;
  }

  /** Place the unit on the board for the AI player. */
  private static void aiSummonUnit(ActorRef out, GameState gs, Card card, int tx, int ty) {
    if (!CardUtils.spendMana(out, gs, 2, card.getManacost())) return;

    Unit unit = BasicObjectBuilders.loadUnit(card.getUnitConfig(), gs.nextUnitId++, Unit.class);
    if (unit == null) return;

    Tile tile = HighlightUtils.getTile(gs, tx, ty);
    if (tile == null) return;

    unit.setPositionByTile(tile);
    int atk = Math.max(card.getBigCard().getAttack(), 0);
    int hp  = Math.max(card.getBigCard().getHealth(), 1);

    gs.units[tx][ty]     = unit;
    gs.unitOwner[tx][ty] = GameActionUtils.AI_PLAYER;
    gs.unitHealth.put(unit.getId(), hp);
    gs.unitAttack.put(unit.getId(), atk);
    gs.unitMaxHealth.put(unit.getId(), hp);

    java.util.Set<String> kw = Keywords.getKeywordsForCard(card.getCardname());
    if (!kw.isEmpty()) gs.unitKeywords.put(unit.getId(), kw);

    // Rush units can act immediately; all others must wait until next turn
    if (!kw.contains(Keywords.RUSH)) gs.actedUnits.add(unit.getId());
    gs.summonedThisTurn.add(unit.getId());

    EffectAnimation fx = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
    if (fx != null) BasicCommands.playEffectAnimation(out, fx, tile);

    BasicCommands.drawUnit(out, unit, tile);
    BasicCommands.setUnitHealth(out, unit, hp);
    BasicCommands.setUnitAttack(out, unit, atk);

    TriggerUtils.onUnitSummoned(out, gs, unit, tx, ty, GameActionUtils.AI_PLAYER);

    BasicCommands.addPlayer1Notification(out,
        "AI summoned " + card.getCardname() + " (" + atk + "/" + hp + ")", 2);
  }

  /** Try to play an AI spell card; returns true if the spell was cast. */
  private static boolean tryPlayAISpell(ActorRef out, GameState gs, Card card) {
    String name = card.getCardname();
    int[] target;
    switch (name) {
      case "Beamshock":
        target = findHighestAttackHumanUnit(gs);
        break;
      case "Truestrike":
        target = new int[]{gs.humanAvatarX, gs.humanAvatarY};
        break;
      case "Sundrop Elixir":
        target = findMostDamagedAIUnit(gs);
        break;
      default:
        return false;
    }
    if (target == null) return false;
    CardUtils.spendMana(out, gs, 2, card.getManacost());
    SpellUtils.applySpell(out, gs, name, target[0], target[1]);
    return true;
  }

  /** Empty tiles adjacent (8-dir) to any AI-owned unit. */
  private static java.util.List<int[]> getAISummonPositions(GameState gs) {
    java.util.Set<Integer> seen = new java.util.HashSet<>();
    java.util.List<int[]> result = new java.util.ArrayList<>();
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] == null || gs.unitOwner[x][y] != GameActionUtils.AI_PLAYER) continue;
        for (int[] d : dirs) {
          int nx = x + d[0], ny = y + d[1];
          if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
          if (gs.units[nx][ny] != null) continue;
          if (seen.add(GameState.posKey(nx, ny))) result.add(new int[]{nx, ny});
        }
      }
    }
    return result;
  }

  /** Human unit with the highest attack value — best Beamshock target. */
  private static int[] findHighestAttackHumanUnit(GameState gs) {
    int[] best = null; int bestAtk = -1;
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] == null || gs.unitOwner[x][y] != GameActionUtils.HUMAN_PLAYER) continue;
        int atk = gs.unitAttack.getOrDefault(gs.units[x][y].getId(), 0);
        if (atk > bestAtk) { bestAtk = atk; best = new int[]{x, y}; }
      }
    }
    return best;
  }

  /** AI unit that has lost the most HP — best Sundrop Elixir target. */
  private static int[] findMostDamagedAIUnit(GameState gs) {
    int[] best = null; int bestMissing = 0;
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        if (gs.units[x][y] == null || gs.unitOwner[x][y] != GameActionUtils.AI_PLAYER) continue;
        int id = gs.units[x][y].getId();
        int missing = gs.unitMaxHealth.getOrDefault(id, 0) - gs.unitHealth.getOrDefault(id, 0);
        if (missing > bestMissing) { bestMissing = missing; best = new int[]{x, y}; }
      }
    }
    return best; // null if nothing is damaged
  }

  /** Count AI units adjacent to (nx, ny), excluding the unit currently at (ox, oy). */
  private static int countAdjacentFriendly(GameState gs, int nx, int ny, int ox, int oy) {
    int count = 0;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    for (int[] d : dirs) {
      int ax = nx + d[0], ay = ny + d[1];
      if (ax < 1 || ax > 9 || ay < 1 || ay > 5) continue;
      if (ax == ox && ay == oy) continue; // skip the moving unit's current tile
      if (gs.units[ax][ay] != null && gs.unitOwner[ax][ay] == GameActionUtils.AI_PLAYER) count++;
    }
    return count;
  }

  private static void sleep(int ms) {
    if (ms <= 0) return;
    try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }
}
