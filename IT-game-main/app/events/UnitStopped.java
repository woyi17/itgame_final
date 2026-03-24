package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import utils.GameActionUtils;

/**
 * Indicates that a unit instance has stopped moving.
 */
public class UnitStopped implements EventProcessor {

  @Override
  public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
    int unitId = message.get("id").asInt();
    int tilex = message.get("tilex").asInt();
    int tiley = message.get("tiley").asInt();

    GameActionUtils.finishMoveAndMaybeAttack(out, gameState, unitId, tilex, tiley);
  }
}
