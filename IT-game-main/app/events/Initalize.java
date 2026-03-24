package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.CardUtils;
import utils.StaticConfFiles;

/**
 * Full game initialization.
 * SC3: Starting Health, SC4: Mana Gain (initial), SC1: Drawing Cards
 */
public class Initalize implements EventProcessor {

  @Override
  public void processEvent(ActorRef out, GameState gs, JsonNode message) {

    gs.gameInitalised = true;

    // Draw board tiles
    for (int x = 1; x <= 9; x++) {
      for (int y = 1; y <= 5; y++) {
        Tile t = BasicObjectBuilders.loadTile(x, y);
        gs.board[x][y] = t;
        BasicCommands.drawTile(out, t, 0);
      }
    }

    // SC3: Starting Health
    gs.player1.setHealth(20);
    gs.player2.setHealth(20);
    BasicCommands.setPlayer1Health(out, gs.player1);
    BasicCommands.setPlayer2Health(out, gs.player2);

    // SC4: Mana Gain — player 1 starts with 2 mana
    gs.player1.setMana(2);
    gs.player2.setMana(0);
    BasicCommands.setPlayer1Mana(out, gs.player1);
    BasicCommands.setPlayer2Mana(out, gs.player2);

    // Place avatars
    int hx = gs.humanAvatarX, hy = gs.humanAvatarY;
    int ax = gs.aiAvatarX,    ay = gs.aiAvatarY;

    try {
      Unit human = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 100, Unit.class);
      Unit ai    = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar,    200, Unit.class);

      human.setPositionByTile(gs.board[hx][hy]);
      ai.setPositionByTile(gs.board[ax][ay]);

      gs.units[hx][hy]    = human;  gs.unitOwner[hx][hy] = 1;
      gs.unitHealth.put(100, 20);   gs.unitAttack.put(100, 2);  gs.unitMaxHealth.put(100, 20);
      gs.humanAvatar = human;

      gs.units[ax][ay]    = ai;     gs.unitOwner[ax][ay] = 2;
      gs.unitHealth.put(200, 20);   gs.unitAttack.put(200, 2);  gs.unitMaxHealth.put(200, 20);
      gs.aiAvatar = ai;

      BasicCommands.drawUnit(out, human, gs.board[hx][hy]);
      BasicCommands.setUnitHealth(out, human, 20);
      BasicCommands.setUnitAttack(out, human, 2);
      try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

      BasicCommands.drawUnit(out, ai, gs.board[ax][ay]);
      BasicCommands.setUnitHealth(out, ai, 20);
      BasicCommands.setUnitAttack(out, ai, 2);
      try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

    } catch (Exception e) {
      BasicCommands.addPlayer1Notification(out, "Init error: " + e.getMessage(), 3);
      return;
    }

    // SC1: Drawing Cards — build decks, draw 3 initial cards each
    gs.deck1 = CardUtils.buildDeck(1);
    gs.deck2 = CardUtils.buildDeck(2);
    for (int i = 0; i < 3; i++) {
      CardUtils.drawCard(out, gs, 1);
      CardUtils.drawCard(out, gs, 2);
    }

    BasicCommands.addPlayer1Notification(out,
        "Game started! Your turn. Mana: 2, Cards: " + gs.hand1.size(), 3);
  }
}
