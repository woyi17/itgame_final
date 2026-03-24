package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;

/**
 * Handles card drawing, deck building, and hand management.
 * SC1: Drawing Cards, SC2: Over Draw, SC4: Mana Gain, SC5: Mana Drain
 */
public final class CardUtils {

  private CardUtils() {}

  public static final int MAX_HAND_SIZE = 6;

  // Player 1 (human) deck card config files
  private static final String[] PLAYER1_CARDS = {
    "conf/gameconfs/cards/1_1_c_u_bad_omen.json",
    "conf/gameconfs/cards/1_2_c_s_hornoftheforsaken.json",
    "conf/gameconfs/cards/1_3_c_u_gloom_chaser.json",
    "conf/gameconfs/cards/1_4_c_u_shadow_watcher.json",
    "conf/gameconfs/cards/1_5_c_s_wraithling_swarm.json",
    "conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json",
    "conf/gameconfs/cards/1_7_c_u_rock_pulveriser.json",
    "conf/gameconfs/cards/1_8_c_s_dark_terminus.json",
    "conf/gameconfs/cards/1_9_c_u_bloodmoon_priestess.json",
    "conf/gameconfs/cards/1_a1_c_u_shadowdancer.json"
  };

  // Player 2 (AI) deck card config files
  private static final String[] PLAYER2_CARDS = {
    "conf/gameconfs/cards/2_1_c_u_skyrock_golem.json",
    "conf/gameconfs/cards/2_2_c_u_swamp_entangler.json",
    "conf/gameconfs/cards/2_3_c_u_silverguard_knight.json",
    "conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json",
    "conf/gameconfs/cards/2_5_c_s_beamshock.json",
    "conf/gameconfs/cards/2_6_c_u_young_flamewing.json",
    "conf/gameconfs/cards/2_7_c_u_silverguard_squire.json",
    "conf/gameconfs/cards/2_8_c_u_ironcliff_guardian.json",
    "conf/gameconfs/cards/2_9_c_s_sundrop_elixir.json",
    "conf/gameconfs/cards/2_a1_c_s_truestrike.json"
  };

  /**
   * Builds and shuffles a player's deck (2 copies of each card).
   */
  public static List<Card> buildDeck(int player) {
    String[] configs = (player == 1) ? PLAYER1_CARDS : PLAYER2_CARDS;
    List<Card> deck = new ArrayList<>();
    int idBase = (player == 1) ? 1 : 21;
    int idCounter = idBase;
    for (int copy = 0; copy < 2; copy++) {
      for (String config : configs) {
        Card card = BasicObjectBuilders.loadCard(config, idCounter++, Card.class);
        if (card != null) deck.add(card);
      }
    }
    Collections.shuffle(deck);
    return deck;
  }

  /**
   * SC1/SC2: Draws a card from player's deck to hand.
   * If hand is full (>=6 cards) the card is burned (overdraw).
   */
  public static void drawCard(ActorRef out, GameState gs, int player) {
    List<Card> deck = (player == 1) ? gs.deck1 : gs.deck2;
    List<Card> hand = (player == 1) ? gs.hand1 : gs.hand2;

    if (deck.isEmpty()) {
      if (player == 1)
        BasicCommands.addPlayer1Notification(out, "Your deck is empty!", 2);
      return;
    }

    Card drawn = deck.remove(0);

    if (hand.size() >= MAX_HAND_SIZE) {
      // SC2: Over Draw — card is burned
      if (player == 1)
        BasicCommands.addPlayer1Notification(out, "Overdraw! " + drawn.getCardname() + " burned.", 2);
      return;
    }

    hand.add(drawn);

    // Only show hand display for player 1
    if (player == 1) {
      int position = hand.size();
      BasicCommands.drawCard(out, drawn, position, 0);
    }
  }

  /**
   * Removes a card from the player's hand and redraws the hand display.
   */
  public static void removeCardFromHand(ActorRef out, GameState gs, int player, int handIndex) {
    List<Card> hand = (player == 1) ? gs.hand1 : gs.hand2;
    if (handIndex < 0 || handIndex >= hand.size()) return;

    hand.remove(handIndex);

    if (player == 1) {
      // Redraw all remaining cards in hand
      refreshHandDisplay(out, gs);
    }
  }

  /**
   * Redraws all cards in hand1 from scratch.
   */
  public static void refreshHandDisplay(ActorRef out, GameState gs) {
    // Delete all 6 positions
    for (int i = 1; i <= MAX_HAND_SIZE; i++) {
      BasicCommands.deleteCard(out, i);
    }
    // Redraw
    for (int i = 0; i < gs.hand1.size(); i++) {
      BasicCommands.drawCard(out, gs.hand1.get(i), i + 1, 0);
    }
  }

  /**
   * SC4: Set mana at the start of a turn.
   * Turn 1 = 2 mana, Turn 2 = 3 mana, ..., max 9.
   */
  public static void setTurnMana(ActorRef out, GameState gs, int player) {
    int mana = Math.min(gs.turnNumber + 1, 9);
    Player p = (player == 1) ? gs.player1 : gs.player2;
    p.setMana(mana);
    if (player == 1) BasicCommands.setPlayer1Mana(out, p);
    else             BasicCommands.setPlayer2Mana(out, p);
  }

  /**
   * SC5: Spend mana when a card is played.
   * Returns true if the player can afford the card.
   */
  public static boolean spendMana(ActorRef out, GameState gs, int player, int cost) {
    Player p = (player == 1) ? gs.player1 : gs.player2;
    if (p.getMana() < cost) return false;
    p.setMana(p.getMana() - cost);
    if (player == 1) BasicCommands.setPlayer1Mana(out, p);
    else             BasicCommands.setPlayer2Mana(out, p);
    return true;
  }

  /**
   * Highlights the selected card in hand and deselects others.
   */
  public static void highlightCardInHand(ActorRef out, GameState gs, int selectedPosition) {
    for (int i = 0; i < gs.hand1.size(); i++) {
      int pos = i + 1;
      int mode = (pos == selectedPosition) ? 1 : 0;
      BasicCommands.drawCard(out, gs.hand1.get(i), pos, mode);
    }
  }

  /**
   * Clears any card selection state.
   */
  public static void clearCardSelection(ActorRef out, GameState gs) {
    gs.selectedCardPosition = 0;
    gs.pendingCard = null;
    gs.awaitingUnitSummon = false;
    gs.awaitingSpellTarget = false;
    gs.summonTargets.clear();
    gs.spellTargets.clear();
    HighlightUtils.clearAll(out, gs);
    refreshHandDisplay(out, gs);
  }
}
