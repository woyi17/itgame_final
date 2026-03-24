package utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Defines all unit keyword ability constants and maps card names to their keywords.
 */
public final class Keywords {

  private Keywords() {}

  // Unit keywords
  public static final String PROVOKE                      = "PROVOKE";
  public static final String RUSH                         = "RUSH";
  public static final String FLYING                       = "FLYING";
  public static final String CANT_ATTACK_FIRST_TURN       = "CANT_ATTACK_FIRST_TURN";

  // Deathwatch triggers (fire when any unit dies)
  public static final String DEATHWATCH_ATTACK_BUFF       = "DEATHWATCH_ATTACK_BUFF";   // Bad Omen
  public static final String DEATHWATCH_STAT_BUFF         = "DEATHWATCH_STAT_BUFF";     // Shadow Watcher
  public static final String DEATHWATCH_SUMMON_WRAITHLING = "DEATHWATCH_SUMMON_WRAITHLING"; // Bloodmoon Priestess
  public static final String DEATHWATCH_SHADOW            = "DEATHWATCH_SHADOW";        // Shadowdancer

  // Opening Gambit triggers (fire when this unit is summoned)
  public static final String OPENING_GAMBIT_WRAITHLING    = "OPENING_GAMBIT_WRAITHLING"; // Gloom Chaser
  public static final String OPENING_GAMBIT_DESTROY       = "OPENING_GAMBIT_DESTROY";   // Nightsorrow Assassin

  // Reactive abilities
  public static final String AVATAR_DAMAGE_BUFF           = "AVATAR_DAMAGE_BUFF"; // Silverguard Squire

  /**
   * Returns the set of keywords for a unit based on the card name it was summoned from.
   */
  public static Set<String> getKeywordsForCard(String cardName) {
    Set<String> kw = new HashSet<>();
    if (cardName == null) return kw;
    switch (cardName) {
      case "Bad Omen":
        kw.add(DEATHWATCH_ATTACK_BUFF);
        break;
      case "Gloom Chaser":
        kw.add(OPENING_GAMBIT_WRAITHLING);
        break;
      case "Shadow Watcher":
        kw.add(DEATHWATCH_STAT_BUFF);
        break;
      case "Nightsorrow Assassin":
        kw.add(OPENING_GAMBIT_DESTROY);
        break;
      case "Rock Pulveriser":
        kw.add(PROVOKE);
        break;
      case "Bloodmoon Priestess":
        kw.add(DEATHWATCH_SUMMON_WRAITHLING);
        break;
      case "Shadowdancer":
        kw.add(DEATHWATCH_SHADOW);
        break;
      case "Swamp Entangler":
        kw.add(PROVOKE);
        break;
      case "Silverguard Knight":
        kw.add(PROVOKE);
        break;
      case "Silverguard Squire":
        kw.add(AVATAR_DAMAGE_BUFF);
        break;
      case "Saberspine Tiger":
        kw.add(RUSH);
        break;
      case "Young Flamewing":
        kw.add(FLYING);
        break;
      case "Ironcliff Guardian":
        kw.add(PROVOKE);
        kw.add(CANT_ATTACK_FIRST_TURN);
        break;
      default:
        break;
    }
    return kw;
  }

  public static boolean hasKeyword(structures.GameState gs, int unitId, String keyword) {
    Set<String> kw = gs.unitKeywords.get(unitId);
    return kw != null && kw.contains(keyword);
  }

  public static void addKeyword(structures.GameState gs, int unitId, String keyword) {
    gs.unitKeywords.computeIfAbsent(unitId, k -> new HashSet<>()).add(keyword);
  }
}
