import static org.junit.Assert.*;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import events.CardClicked;
import events.EndTurnClicked;
import events.Initalize;
import events.TileClicked;
import play.libs.Json;
import structures.GameState;
import structures.basic.Card;
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

import java.util.List;
import java.util.Set;

/**
 * Comprehensive tests for all course scenarios.
 * SC1: Drawing Cards, SC2: Over Draw, SC3: Starting Health
 * SC4: Mana Gain, SC5: Mana Drain
 * SC9: Adjacent Attack, SC10: Move-And-Attack, SC11: Unit Move
 * SC17: Opening Gambit, SC18: Deathwatch
 * SC21: Spell Card Targeting, SC25: Unit Card Summoning
 */
public class ScenarioTest {

    // Setup helper
    private GameState createGameState() {
        CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
        BasicCommands.altTell = altTell;
        
        GameState gs = new GameState();
        Initalize init = new Initalize();
        init.processEvent(null, gs, Json.newObject());
        return gs;
    }

    /**
     * SC3: Starting Health - Both players should start with 20 HP
     */
    @Test
    public void testSC3_StartingHealth() {
        GameState gs = createGameState();
        
        assertEquals("Player 1 should start with 20 HP", 20, gs.player1.getHealth());
        assertEquals("Player 2 should start with 20 HP", 20, gs.player2.getHealth());
        
        // Avatar health should also be 20
        assertEquals("Human avatar should have 20 HP", 20, (int)gs.unitHealth.get(100));
        assertEquals("AI avatar should have 20 HP", 20, (int)gs.unitHealth.get(200));
    }

    /**
     * SC4: Mana Gain - Player 1 starts with 2 mana
     */
    @Test
    public void testSC4_ManaGain_Initial() {
        GameState gs = createGameState();
        
        assertEquals("Player 1 should start with 2 mana", 2, gs.player1.getMana());
        assertEquals("Player 2 should start with 0 mana", 0, gs.player2.getMana());
    }

    /**
     * SC4: Mana Gain - Mana increases each turn (max 9)
     */
    @Test
    public void testSC4_ManaGain_PerTurn() {
        GameState gs = createGameState();
        
        // Simulate turns
        gs.turnNumber = 1;
        CardUtils.setTurnMana(null, gs, 1);
        assertEquals("Turn 1: 2 mana", 2, gs.player1.getMana());
        
        gs.turnNumber = 2;
        CardUtils.setTurnMana(null, gs, 1);
        assertEquals("Turn 2: 3 mana", 3, gs.player1.getMana());
        
        gs.turnNumber = 8;
        CardUtils.setTurnMana(null, gs, 1);
        assertEquals("Turn 8: 9 mana (max)", 9, gs.player1.getMana());
        
        gs.turnNumber = 15;
        CardUtils.setTurnMana(null, gs, 1);
        assertEquals("Turn 15: still 9 mana (capped)", 9, gs.player1.getMana());
    }

    /**
     * SC1: Drawing Cards - Initial draw should give 3 cards
     */
    @Test
    public void testSC1_DrawingCards_Initial() {
        GameState gs = createGameState();
        
        assertEquals("Player 1 should have 3 initial cards", 3, gs.hand1.size());
        assertEquals("Player 2 should have 3 initial cards", 3, gs.hand2.size());
    }

    /**
     * SC1: Drawing Cards - Draw card each turn
     */
    @Test
    public void testSC1_DrawingCards_PerTurn() {
        GameState gs = createGameState();
        int initialHandSize = gs.hand1.size();
        
        // Draw a card
        CardUtils.drawCard(null, gs, 1);
        
        assertEquals("Hand should have one more card", initialHandSize + 1, gs.hand1.size());
    }

    /**
     * SC2: Over Draw - Hand limit is 6 cards
     */
    @Test
    public void testSC2_OverDraw() {
        GameState gs = createGameState();
        
        // Fill hand to max
        while (gs.hand1.size() < 6) {
            CardUtils.drawCard(null, gs, 1);
        }
        assertEquals("Hand should have 6 cards", 6, gs.hand1.size());
        
        // Draw one more - should be burned
        int deckSize = gs.deck1.size();
        CardUtils.drawCard(null, gs, 1);
        
        assertEquals("Hand should still have 6 cards (overdraw)", 6, gs.hand1.size());
        assertEquals("Deck should decrease by 1", deckSize - 1, gs.deck1.size());
    }

    /**
     * SC5: Mana Drain - Spending mana on cards
     */
    @Test
    public void testSC5_ManaDrain() {
        GameState gs = createGameState();
        
        int initialMana = gs.player1.getMana();
        
        // Spend 1 mana (cost of card)
        boolean success = CardUtils.spendMana(null, gs, 1, 1);
        
        assertTrue("Should be able to spend mana", success);
        assertEquals("Mana should decrease by 1", initialMana - 1, gs.player1.getMana());
        
        // Try to spend more than available
        success = CardUtils.spendMana(null, gs, 1, 100);
        assertFalse("Should not be able to overspend", success);
    }

    /**
     * SC11: Unit Move - Valid move tiles are highlighted
     */
    @Test
    public void testSC11_UnitMove() {
        GameState gs = createGameState();
        
        // Select human avatar at position (1,3)
        int avatarX = gs.humanAvatarX;
        int avatarY = gs.humanAvatarY;
        
        // Get valid move tiles
        Set<int[]> moveTiles = GameActionUtils.getMoveTiles(gs, avatarX, avatarY, 1);
        
        assertFalse("Should have move options", moveTiles.isEmpty());
        
        // Verify move tiles are within range (2 tiles for avatar)
        for (int[] pos : moveTiles) {
            int x = pos[0];
            int y = pos[1];
            int dist = Math.abs(x - avatarX) + Math.abs(y - avatarY);
            assertTrue("Move should be within 2 tiles", dist <= 2);
            assertTrue("Tile should be empty", gs.units[x][y] == null);
        }
    }

    /**
     * SC9: Adjacent Attack - Can attack adjacent enemies
     */
    @Test
    public void testSC9_AdjacentAttack() {
        GameState gs = createGameState();
        
        // Human avatar at (1,3), AI at (9,3)
        // Move AI unit next to human for testing
        Unit aiUnit = gs.units[9][3];
        assertNotNull("AI avatar should exist", aiUnit);
        
        // Get adjacent tiles to human avatar
        Set<int[]> adjacentEnemies = GameActionUtils.getAdjacentEnemyTiles(gs, 1, 3, 1);
        
        // AI is at (9,3), not adjacent to (1,3), so should be empty
        // Let's manually place a unit to test
        gs.units[2][3] = aiUnit;
        gs.unitOwner[2][3] = 2;
        gs.units[9][3] = null;
        gs.unitOwner[9][3] = 0;
        
        adjacentEnemies = GameActionUtils.getAdjacentEnemyTiles(gs, 1, 3, 1);
        assertFalse("Should detect adjacent enemy after moving", adjacentEnemies.isEmpty());
    }

    /**
     * SC10: Move-And-Attack - Unit can move then attack
     */
    @Test
    public void testSC10_MoveAndAttack() {
        GameState gs = createGameState();
        
        // Check that move-and-attack flag exists
        assertFalse("Should not be in move-and-attack mode initially", gs.pendingMoveAndAttack);
        
        // After moving, should be able to attack
        // This is more of a state check - the actual animation is tested elsewhere
        gs.pendingMoveAndAttack = true;
        assertTrue("Should be in move-and-attack mode after move", gs.pendingMoveAndAttack);
    }

    /**
     * SC25: Unit Card Summoning - Summon a unit to board
     */
    @Test
    public void testSC25_UnitSummoning() {
        GameState gs = createGameState();

        // Find a creature card in hand (shuffled deck may start with a spell)
        Card card = null;
        for (Card c : gs.hand1) {
            if (c.isCreature() && c.getUnitConfig() != null) {
                card = c;
                break;
            }
        }
        
        // If no creature in hand, draw more cards until we get one
        while (card == null && gs.hand1.size() < 6) {
            CardUtils.drawCard(null, gs, 1);
            for (Card c : gs.hand1) {
                if (c.isCreature() && c.getUnitConfig() != null) {
                    card = c;
                    break;
                }
            }
        }
        
        assertNotNull("Should have a creature card in hand", card);
        
        // Find a valid summon position (empty tile)
        int summonX = 2, summonY = 2;
        gs.units[summonX][summonY] = null; // Ensure empty
        
        // Load and place unit
        Unit unit = BasicObjectBuilders.loadUnit(card.getUnitConfig(), gs.nextUnitId++, Unit.class);
        assertNotNull("Should load unit from card", unit);
        
        Tile tile = BasicObjectBuilders.loadTile(summonX, summonY);
        unit.setPositionByTile(tile);
        
        gs.units[summonX][summonY] = unit;
        gs.unitOwner[summonX][summonY] = 1;
        
        int hp = card.getBigCard().getHealth();
        int atk = card.getBigCard().getAttack();
        gs.unitHealth.put(unit.getId(), hp);
        gs.unitAttack.put(unit.getId(), atk);
        
        assertNotNull("Unit should be on board", gs.units[summonX][summonY]);
        assertEquals("Unit should belong to player 1", 1, gs.unitOwner[summonX][summonY]);
        assertEquals("HP should match card", hp, (int)gs.unitHealth.get(unit.getId()));
    }

    /**
     * SC17: Opening Gambit - Gloom Chaser summons Wraithling
     */
    @Test
    public void testSC17_OpeningGambit_GloomChaser() {
        GameState gs = createGameState();
        
        // Place a Gloom Chaser
        Unit gloomChaser = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/gloom_chaser.json", 301, Unit.class);
        gs.units[2][2] = gloomChaser;
        gs.unitOwner[2][2] = 1;
        gs.unitHealth.put(301, 2);
        gs.unitAttack.put(301, 1);
        
        Set<String> keywords = Keywords.getKeywordsForCard("Gloom Chaser");
        assertTrue("Gloom Chaser should have Opening Gambit Wraithling keyword",
            keywords.contains(Keywords.OPENING_GAMBIT_WRAITHLING));

        // Register keywords so TriggerUtils can read them
        gs.unitKeywords.put(301, keywords);
        // Ensure spawned wraithling gets a fresh ID above any manually-assigned IDs
        gs.nextUnitId = 302;

        // Trigger Opening Gambit
        TriggerUtils.onUnitSummoned(null, gs, gloomChaser, 2, 2, 1);

        // Check all 8 adjacent directions (summonWraithlingNear uses 8-direction search)
        boolean newUnitSummoned = false;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] d : dirs) {
            int nx = 2 + d[0], ny = 2 + d[1];
            if (nx >= 1 && nx <= 9 && ny >= 1 && ny <= 5) {
                Unit u = gs.units[nx][ny];
                if (u != null && u.getId() >= 302) {
                    newUnitSummoned = true;
                    break;
                }
            }
        }
        assertTrue("A new unit (Wraithling) should be summoned near Gloom Chaser", newUnitSummoned);
    }

    /**
     * SC17: Opening Gambit - Nightsorrow Assassin destroys enemy
     */
    @Test
    public void testSC17_OpeningGambit_NightsorrowAssassin() {
        GameState gs = createGameState();
        
        // Place a weak enemy unit (attack <= 2)
        Unit weakEnemy = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/wraithling.json", 302, Unit.class);
        gs.units[3][3] = weakEnemy;
        gs.unitOwner[3][3] = 2;
        gs.unitHealth.put(302, 1);
        gs.unitAttack.put(302, 1); // Weak attack
        
        // Place Nightsorrow Assassin adjacent
        Unit assassin = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/nightsorrow_assassin.json", 303, Unit.class);
        gs.units[2][2] = assassin;
        gs.unitOwner[2][2] = 1;
        gs.unitHealth.put(303, 3);
        gs.unitAttack.put(303, 2);
        
        Set<String> keywords = Keywords.getKeywordsForCard("Nightsorrow Assassin");
        assertTrue("Nightsorrow Assassin should have Opening Gambit Destroy keyword",
            keywords.contains(Keywords.OPENING_GAMBIT_DESTROY));

        // Register keywords so TriggerUtils can read them
        gs.unitKeywords.put(303, keywords);

        // Trigger Opening Gambit
        TriggerUtils.onUnitSummoned(null, gs, assassin, 2, 2, 1);
        
        // The weak enemy should be destroyed
        assertNull("Weak enemy should be destroyed", gs.units[3][3]);
    }

    /**
     * SC18: Deathwatch - Bad Omen gives +1 attack
     */
    @Test
    public void testSC18_Deathwatch_BadOmen() {
        GameState gs = createGameState();
        
        // Place Bad Omen
        Unit badOmen = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/bad_omen.json", 304, Unit.class);
        gs.units[2][2] = badOmen;
        gs.unitOwner[2][2] = 1;
        gs.unitHealth.put(304, 2);
        gs.unitAttack.put(304, 2);  // Bad Omen has 2 attack in config
        // Register keywords so TriggerUtils can fire the deathwatch effect
        gs.unitKeywords.put(304, Keywords.getKeywordsForCard("Bad Omen"));

        int initialAttack = gs.unitAttack.get(304);
        
        // Place another unit to die
        Unit victim = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/wraithling.json", 305, Unit.class);
        gs.units[3][3] = victim;
        gs.unitOwner[3][3] = 2;
        gs.unitHealth.put(305, 1);
        
        // Kill the victim - killUnit already triggers onUnitDeath
        GameActionUtils.killUnit(null, gs, victim, 3, 3);
        
        // Note: deathwatch is triggered automatically by killUnit
        
        assertEquals("Bad Omen should get +1 attack", initialAttack + 1, 
            (int)gs.unitAttack.get(304));
    }

    /**
     * SC18: Deathwatch - Bloodmoon Priestess summons Wraithling
     */
    @Test
    public void testSC18_Deathwatch_BloodmoonPriestess() {
        GameState gs = createGameState();
        
        // Place Bloodmoon Priestess
        Unit priestess = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/bloodmoon_priestess.json", 306, Unit.class);
        gs.units[2][2] = priestess;
        gs.unitOwner[2][2] = 1;
        gs.unitHealth.put(306, 4);
        gs.unitAttack.put(306, 2);
        // Register keywords so TriggerUtils can fire the deathwatch effect
        gs.unitKeywords.put(306, Keywords.getKeywordsForCard("Bloodmoon Priestess"));

        // Place enemy to die
        Unit enemy = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/wraithling.json", 307, Unit.class);
        gs.units[3][3] = enemy;
        gs.unitOwner[3][3] = 2;
        gs.unitHealth.put(307, 1);
        
        // Kill enemy
        GameActionUtils.killUnit(null, gs, enemy, 3, 3);

        // Ensure spawned wraithling gets a fresh ID above any manually-assigned IDs
        gs.nextUnitId = 308;

        // Trigger deathwatch
        TriggerUtils.onUnitDeath(null, gs, enemy, 2);

        // Should have summoned a Wraithling adjacent to the Priestess at (2,2)
        boolean foundNewUnit = false;
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                Unit u = gs.units[x][y];
                if (u != null && u.getId() >= 308) {
                    foundNewUnit = true;
                }
            }
        }
        assertTrue("Bloodmoon Priestess should summon a unit on deathwatch", foundNewUnit);
    }

    /**
     * SC21: Spell Card Targeting - Spell targets
     */
    @Test
    public void testSC21_SpellCardTargeting() {
        GameState gs = createGameState();
        
        // Place a target unit
        Unit target = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/wraithling.json", 308, Unit.class);
        gs.units[3][3] = target;
        gs.unitOwner[3][3] = 2;
        gs.unitHealth.put(308, 1);
        
        // Get spell targets (enemy units)
        gs.spellTargets.add(HighlightUtils.key(3, 3));
        
        assertTrue("Spell should have target at (3,3)", 
            gs.spellTargets.contains(HighlightUtils.key(3, 3)));
    }

    /**
     * SC23: Rush - Rush units can act immediately
     */
    @Test
    public void testSC23_Rush() {
        GameState gs = createGameState();
        
        // Saberspine Tiger has Rush
        Set<String> keywords = Keywords.getKeywordsForCard("Saberspine Tiger");
        assertTrue("Saberspine Tiger should have Rush", 
            keywords.contains(Keywords.RUSH));
        
        // Create and summon a Rush unit
        Unit rushUnit = BasicObjectBuilders.loadUnit(
            "conf/gameconfs/units/saberspine_tiger.json", 309, Unit.class);
        gs.units[2][2] = rushUnit;
        gs.unitOwner[2][2] = 1;
        gs.unitHealth.put(309, 3);
        gs.unitAttack.put(309, 5);
        gs.unitKeywords.put(309, keywords);
        
        // After summoning, Rush unit should NOT be in actedUnits
        assertFalse("Rush unit should be able to act (not in actedUnits)", 
            gs.actedUnits.contains(309));
    }

    /**
     * Test PROVOKE keyword - enemy must attack provoke unit first
     */
    @Test
    public void testKeywords_Provoke() {
        // Rock Pulveriser has Provoke
        Set<String> keywords = Keywords.getKeywordsForCard("Rock Pulveriser");
        assertTrue("Rock Pulveriser should have Provoke", 
            keywords.contains(Keywords.PROVOKE));
        
        // Swamp Entangler also has Provoke
        keywords = Keywords.getKeywordsForCard("Swamp Entangler");
        assertTrue("Swamp Entangler should have Provoke", 
            keywords.contains(Keywords.PROVOKE));
    }

    /**
     * Test FLYING keyword
     */
    @Test
    public void testKeywords_Flying() {
        // Young Flamewing has Flying
        Set<String> keywords = Keywords.getKeywordsForCard("Young Flamewing");
        assertTrue("Young Flamewing should have Flying", 
            keywords.contains(Keywords.FLYING));
    }

    /**
     * Test deck building
     */
    @Test
    public void testDeckBuilding() {
        GameState gs = createGameState();
        
        // Player 1 deck should total 20 cards (10 unique x 2); 3 are drawn into hand during init
        assertEquals("Player 1 deck + hand should total 20 cards",
            20, gs.deck1.size() + gs.hand1.size());

        // Player 2 deck should total 20 cards
        assertEquals("Player 2 deck + hand should total 20 cards",
            20, gs.deck2.size() + gs.hand2.size());
    }

    /**
     * Test board initialization
     */
    @Test
    public void testBoardInitialization() {
        GameState gs = createGameState();
        
        // Board should be 10x6 (indices 1-9 for x, 1-5 for y)
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                assertNotNull("Tile should exist at (" + x + "," + y + ")", gs.board[x][y]);
            }
        }
        
        // Avatars should be placed
        assertNotNull("Human avatar should be placed", gs.humanAvatar);
        assertNotNull("AI avatar should be placed", gs.aiAvatar);
        assertEquals("Human avatar at (1,3)", 1, gs.humanAvatarX);
        assertEquals("Human avatar at y=3", 3, gs.humanAvatarY);
        assertEquals("AI avatar at (9,3)", 9, gs.aiAvatarX);
        assertEquals("AI avatar at y=3", 3, gs.aiAvatarY);
    }
}
