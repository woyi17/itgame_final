package structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Holds information about the on-going game.
 * Extended to support all 32 scenarios.
 */
public class GameState {

  public boolean gameInitalised = false;
  public boolean gameOver = false;

  // Board
  public Tile[][] board = new Tile[10][6];
  public Unit[][] units = new Unit[10][6];
  public int[][] unitOwner = new int[10][6];

  // Unit selection
  public int selectedTileX = -1;
  public int selectedTileY = -1;
  public Unit selectedUnit = null;
  public int selectedUnitX = -1;
  public int selectedUnitY = -1;

  // Board highlighting
  public Set<Integer> highlighted = new HashSet<>();
  public Set<Integer> moveHighlighted = new HashSet<>();
  public Set<Integer> attackHighlighted = new HashSet<>();
  public Set<Integer> moveTargets = new HashSet<>();
  public Set<Integer> attackTargets = new HashSet<>();
  public Set<Integer> summonTargets = new HashSet<>();
  public Set<Integer> spellTargets = new HashSet<>();

  // Combat stats keyed by unit id
  public Map<Integer, Integer> unitHealth = new HashMap<>();
  public Map<Integer, Integer> unitAttack = new HashMap<>();
  public Map<Integer, Integer> unitMaxHealth = new HashMap<>();

  // Unit keywords keyed by unit id
  public Map<Integer, Set<String>> unitKeywords = new HashMap<>();

  // Per-turn tracking
  public Set<Integer> actedUnits = new HashSet<>();
  public Set<Integer> stunnedUnits = new HashSet<>();
  public Set<Integer> summonedThisTurn = new HashSet<>();

  // Move-and-attack state
  public boolean pendingMoveAndAttack = false;

  // Players (SC3: Starting Health)
  public Player player1 = new Player(20, 0);
  public Player player2 = new Player(20, 0);

  // Card system (SC1, SC2, SC4, SC5)
  public List<Card> hand1 = new ArrayList<>();
  public List<Card> hand2 = new ArrayList<>();
  public List<Card> deck1 = new ArrayList<>();
  public List<Card> deck2 = new ArrayList<>();

  // Turn management
  public int turnNumber = 1;
  public int currentTurn = 1; // 1=human, 2=AI

  // Avatar references
  public Unit humanAvatar = null;
  public Unit aiAvatar = null;
  public int humanAvatarX = 1;
  public int humanAvatarY = 3;
  public int aiAvatarX = 9;
  public int aiAvatarY = 3;

  // Card selection state (SC21, SC25)
  public int selectedCardPosition = 0;
  public Card pendingCard = null;
  public boolean awaitingUnitSummon = false;
  public boolean awaitingSpellTarget = false;

  // ID counter for dynamically spawned units
  public int nextUnitId = 300;

  public static int posKey(int x, int y) {
    return (x << 8) | (y & 0xFF);
  }
}
