package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import utils.CardUtils;
import utils.GameActionUtils;
import utils.HighlightUtils;
import utils.SpellUtils;

/**
 * SC21: Play Spell Card
 * SC25: Play Unit Card
 * Handles card selection, deselection, and triggering highlights.
 */
public class CardClicked implements EventProcessor {

  @Override
  public void processEvent(ActorRef out, GameState gs, JsonNode message) {
    if (gs.gameOver || gs.currentTurn != GameActionUtils.HUMAN_PLAYER) return;

    int handPosition = message.get("position").asInt();

    // Deselect if clicking the already-selected card
    if (gs.selectedCardPosition == handPosition) {
      CardUtils.clearCardSelection(out, gs);
      return;
    }

    // Validate hand position
    if (handPosition < 1 || handPosition > gs.hand1.size()) {
      CardUtils.clearCardSelection(out, gs);
      return;
    }

    Card card = gs.hand1.get(handPosition - 1);

    // SC5: Check if player can afford it
    if (gs.player1.getMana() < card.getManacost()) {
      BasicCommands.addPlayer1Notification(out,
          "Not enough mana! Need " + card.getManacost() + ", have " + gs.player1.getMana(), 2);
      return;
    }

    // Clear previous selection/highlights
    CardUtils.clearCardSelection(out, gs);
    GameActionUtils.clearSelectionState(out, gs);

    gs.selectedCardPosition = handPosition;
    gs.pendingCard = card;

    // Highlight selected card
    CardUtils.highlightCardInHand(out, gs, handPosition);

    if (card.isCreature()) {
      // SC25: Play Unit Card — highlight valid summon positions
      gs.awaitingUnitSummon = true;
      HighlightUtils.highlightSummonPositions(out, gs, GameActionUtils.HUMAN_PLAYER);
      BasicCommands.addPlayer1Notification(out,
          "Click a highlighted tile to summon " + card.getCardname()
          + " (cost " + card.getManacost() + ")", 2);
    } else {
      // SC21: Play Spell Card
      String targetType = SpellUtils.getSpellTargetType(card.getCardname());
      if ("NO_TARGET".equals(targetType)) {
        // Play immediately — no targeting needed
        CardUtils.spendMana(out, gs, 1, card.getManacost());
        SpellUtils.applySpell(out, gs, card.getCardname(), -1, -1);
        int idx = gs.selectedCardPosition - 1;
        CardUtils.clearCardSelection(out, gs);
        CardUtils.removeCardFromHand(out, gs, 1, idx);
      } else {
        gs.awaitingSpellTarget = true;
        HighlightUtils.highlightSpellTargets(out, gs, GameActionUtils.HUMAN_PLAYER, targetType);
        BasicCommands.addPlayer1Notification(out,
            "Click a target for " + card.getCardname()
            + " (cost " + card.getManacost() + ")", 2);
      }
    }
  }
}
