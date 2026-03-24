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
 * Handles all tile click interactions:
 * SC11: Unit Action: Move
 * SC9:  Unit Action: Adjacent Attack
 * SC10: Unit Action: Move-And-Attack
 * SC25: Play Unit Card (summon)
 * SC21: Play Spell Card (targeting)
 */
public class TileClicked implements EventProcessor {

  @Override
  public void processEvent(ActorRef out, GameState gs, JsonNode message) {
    if (gs.gameOver) return;

    int tilex = message.get("tilex").asInt();
    int tiley = message.get("tiley").asInt();
    int clickedKey = HighlightUtils.key(tilex, tiley);

    gs.selectedTileX = tilex;
    gs.selectedTileY = tiley;

    // === Card play: awaiting summon position ===
    if (gs.awaitingUnitSummon && gs.pendingCard != null) {
      if (gs.summonTargets.contains(clickedKey)) {
        summonUnit(out, gs, gs.pendingCard, tilex, tiley);
      } else {
        // Clicked invalid tile — cancel card selection
        CardUtils.clearCardSelection(out, gs);
      }
      return;
    }

    // === Card play: awaiting spell target ===
    if (gs.awaitingSpellTarget && gs.pendingCard != null) {
      if (gs.spellTargets.contains(clickedKey)) {
        Card card = gs.pendingCard;
        int handIdx = gs.selectedCardPosition - 1;
        CardUtils.spendMana(out, gs, 1, card.getManacost());
        CardUtils.clearCardSelection(out, gs);
        SpellUtils.applySpell(out, gs, card.getCardname(), tilex, tiley);
        CardUtils.removeCardFromHand(out, gs, 1, handIdx);
      } else {
        CardUtils.clearCardSelection(out, gs);
      }
      return;
    }

    // === Unit attack ===
    if (gs.selectedUnit != null && gs.attackTargets.contains(clickedKey)) {
      Unit target = gs.units[tilex][tiley];
      int owner = gs.unitOwner[tilex][tiley];
      if (target != null && owner == GameActionUtils.AI_PLAYER) {
        GameActionUtils.resolveAttack(out, gs,
            gs.selectedUnit, gs.selectedUnitX, gs.selectedUnitY,
            target, tilex, tiley);
        return;
      }
    }

    // === Unit move ===
    if (gs.selectedUnit != null && gs.moveTargets.contains(clickedKey)) {
      Tile dest = HighlightUtils.getTile(gs, tilex, tiley);
      if (dest == null) return;

      int oldX = gs.selectedUnitX, oldY = gs.selectedUnitY;
      Unit movingUnit = gs.selectedUnit;

      HighlightUtils.clearAll(out, gs);
      gs.moveTargets.clear();
      gs.attackTargets.clear();
      gs.pendingMoveAndAttack = true;

      BasicCommands.moveUnitToTile(out, movingUnit, dest);

      // Update board state immediately
      gs.units[tilex][tiley] = movingUnit;
      gs.unitOwner[tilex][tiley] = GameActionUtils.HUMAN_PLAYER;
      gs.units[oldX][oldY] = null;
      gs.unitOwner[oldX][oldY] = 0;

      // Update avatar position tracking
      if (oldX == gs.humanAvatarX && oldY == gs.humanAvatarY) {
        gs.humanAvatarX = tilex;
        gs.humanAvatarY = tiley;
      }

      BasicCommands.addPlayer1Notification(out,
          "Moving unit to (" + tilex + "," + tiley + ")...", 2);
      return;
    }

    // === Select a unit ===
    Unit clickedUnit = gs.units[tilex][tiley];
    int owner = gs.unitOwner[tilex][tiley];

    if (clickedUnit == null || owner != GameActionUtils.HUMAN_PLAYER) {
      GameActionUtils.clearSelectionState(out, gs);
      return;
    }

    gs.selectedUnit = clickedUnit;
    gs.selectedUnitX = tilex;
    gs.selectedUnitY = tiley;
    gs.pendingMoveAndAttack = false;

    GameActionUtils.refreshActionHighlights(out, gs, tilex, tiley, owner);
  }

  /**
   * SC25: Summon a unit card onto the board at (tx, ty).
   * SC17: Fires Opening Gambit triggers.
   * SC23: Rush units can act immediately; others are added to actedUnits.
   */
  private void summonUnit(ActorRef out, GameState gs, Card card, int tx, int ty) {
    // SC5: Spend mana
    if (!CardUtils.spendMana(out, gs, 1, card.getManacost())) {
      BasicCommands.addPlayer1Notification(out, "Not enough mana!", 2);
      CardUtils.clearCardSelection(out, gs);
      return;
    }

    int handIdx = gs.selectedCardPosition - 1;
    CardUtils.clearCardSelection(out, gs);

    // Load the unit from its config
    Unit unit = BasicObjectBuilders.loadUnit(card.getUnitConfig(), gs.nextUnitId++, Unit.class);
    if (unit == null) {
      BasicCommands.addPlayer1Notification(out, "Failed to load unit!", 2);
      CardUtils.removeCardFromHand(out, gs, 1, handIdx);
      return;
    }

    Tile tile = HighlightUtils.getTile(gs, tx, ty);
    if (tile == null) return;

    unit.setPositionByTile(tile);

    // Stats come from the card's bigCard
    int atk = card.getBigCard().getAttack();
    int hp  = card.getBigCard().getHealth();
    atk = Math.max(atk, 0);
    hp  = Math.max(hp, 1);

    gs.units[tx][ty]    = unit;
    gs.unitOwner[tx][ty] = GameActionUtils.HUMAN_PLAYER;
    gs.unitHealth.put(unit.getId(), hp);
    gs.unitAttack.put(unit.getId(), atk);
    gs.unitMaxHealth.put(unit.getId(), hp);

    // Assign keywords from card name
    java.util.Set<String> kw = Keywords.getKeywordsForCard(card.getCardname());
    if (!kw.isEmpty()) gs.unitKeywords.put(unit.getId(), kw);

    // SC23: Rush units can act this turn; others cannot
    boolean hasRush = kw.contains(Keywords.RUSH);
    if (!hasRush) {
      gs.actedUnits.add(unit.getId());
    }
    gs.summonedThisTurn.add(unit.getId());

    // Play summon animation
    EffectAnimation summonFx = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
    if (summonFx != null) BasicCommands.playEffectAnimation(out, summonFx, tile);

    BasicCommands.drawUnit(out, unit, tile);
    BasicCommands.setUnitHealth(out, unit, hp);
    BasicCommands.setUnitAttack(out, unit, atk);

    // SC17: Fire Opening Gambit triggers
    TriggerUtils.onUnitSummoned(out, gs, unit, tx, ty, GameActionUtils.HUMAN_PLAYER);

    // Remove card from hand
    CardUtils.removeCardFromHand(out, gs, 1, handIdx);

    BasicCommands.addPlayer1Notification(out,
        "Summoned " + card.getCardname() + " (" + atk + "/" + hp + ")"
        + (hasRush ? " [RUSH - can attack!]" : ""), 2);
  }
}
