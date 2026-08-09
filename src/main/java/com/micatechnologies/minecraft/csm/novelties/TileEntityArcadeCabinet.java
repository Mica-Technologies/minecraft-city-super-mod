package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Backing tile entity for the multi-game arcade cabinet. It holds nothing about a game in progress —
 * that lives entirely in the client-side {@code ArcadeGui} — only the best score anyone has posted on
 * this machine, tracked separately for each of the games it has installed, so the select screen can
 * show a record beside every title the way a real multi-game board does.
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class TileEntityArcadeCabinet extends AbstractTileEntity {

  /**
   * The largest score a cabinet will store. Well above anything reachable in play; it exists so a
   * malformed or hostile packet cannot park an absurd number on the marquee. Declared here rather
   * than on {@code ArcadeGame} so the server-side packet handler never has to touch a class whose
   * method signatures reach into client-only rendering types.
   *
   * @since 1.0
   */
  public static final int MAX_SCORE = 99_999_999;

  /**
   * NBT key for the compound holding one record per game.
   *
   * @since 1.0
   */
  private static final String NBT_RECORDS = "recs";

  /**
   * NBT key for a record's score.
   *
   * @since 1.0
   */
  private static final String NBT_SCORE = "s";

  /**
   * NBT key for a record's holder name.
   *
   * @since 1.0
   */
  private static final String NBT_HOLDER = "n";

  /**
   * Upper bound on a stored holder name. Vanilla names top out well below this; the cap exists only
   * so a hostile client cannot grow the cabinet's NBT without bound.
   *
   * @since 1.0
   */
  private static final int MAX_HOLDER_NAME_CHARS = 48;

  /**
   * The best score posted on this cabinet for each game, indexed alongside
   * {@link ArcadeCatalog#GAME_IDS}.
   *
   * @since 1.0
   */
  private final int[] highScores = new int[ArcadeCatalog.GAME_IDS.length];

  /**
   * The name attached to each entry of {@link #highScores}.
   *
   * @since 1.0
   */
  private final String[] highScoreHolders = new String[ArcadeCatalog.GAME_IDS.length];

  /**
   * Constructs a {@link TileEntityArcadeCabinet} with an empty score table.
   *
   * @since 1.0
   */
  public TileEntityArcadeCabinet() {
    java.util.Arrays.fill(highScoreHolders, "");
  }

  /**
   * Retrieves the index of the specified game in the score table.
   *
   * @param gameId the game's identifier
   *
   * @return the index, or -1 if the identifier is not in the catalogue
   *
   * @since 1.0
   */
  private static int indexOf(String gameId) {
    for (int i = 0; i < ArcadeCatalog.GAME_IDS.length; i++) {
      if (ArcadeCatalog.GAME_IDS[i].equals(gameId)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Retrieves the best score posted on this cabinet for the specified game.
   *
   * @param gameId the game's identifier
   *
   * @return the high score, or zero if the game has never been played here
   *
   * @since 1.0
   */
  public int getHighScore(String gameId) {
    int index = indexOf(gameId);
    return index < 0 ? 0 : highScores[index];
  }

  /**
   * Retrieves the name of the player holding this cabinet's record for the specified game.
   *
   * @param gameId the game's identifier
   *
   * @return the record holder's name, never {@code null}
   *
   * @since 1.0
   */
  public String getHighScoreHolder(String gameId) {
    int index = indexOf(gameId);
    if (index < 0 || highScoreHolders[index] == null) {
      return "";
    }
    return highScoreHolders[index];
  }

  /**
   * Server-side setter which records a new high score for a game, but only if it actually beats the
   * stored one. Ties are rejected so the original holder keeps the record.
   *
   * @param gameId the game's identifier
   * @param score  the score being posted
   * @param holder the name of the player posting it
   *
   * @return {@code true} if the score was accepted and stored
   *
   * @since 1.0
   */
  public boolean tryPostHighScore(String gameId, int score, String holder) {
    int index = indexOf(gameId);
    if (index < 0 || score <= highScores[index]) {
      return false;
    }
    highScores[index] = Math.min(MAX_SCORE, score);
    if (holder == null) {
      holder = "";
    }
    if (holder.length() > MAX_HOLDER_NAME_CHARS) {
      holder = holder.substring(0, MAX_HOLDER_NAME_CHARS);
    }
    highScoreHolders[index] = holder;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
    return true;
  }

  /**
   * Reads the cabinet's score table from the supplied NBT tag compound. Records are keyed by game
   * identifier rather than by position, so adding a game to the catalogue later cannot shuffle an
   * existing cabinet's records onto the wrong titles.
   *
   * @param compound the NBT tag compound to read from
   *
   * @since 1.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    java.util.Arrays.fill(highScores, 0);
    java.util.Arrays.fill(highScoreHolders, "");
    if (!compound.hasKey(NBT_RECORDS)) {
      return;
    }
    NBTTagCompound records = compound.getCompoundTag(NBT_RECORDS);
    for (int i = 0; i < ArcadeCatalog.GAME_IDS.length; i++) {
      String gameId = ArcadeCatalog.GAME_IDS[i];
      if (!records.hasKey(gameId)) {
        continue;
      }
      NBTTagCompound record = records.getCompoundTag(gameId);
      highScores[i] = record.getInteger(NBT_SCORE);
      highScoreHolders[i] = record.getString(NBT_HOLDER);
    }
  }

  /**
   * Writes the cabinet's score table to the supplied NBT tag compound. A cabinet nobody has played
   * writes nothing, keeping the common case free of NBT.
   *
   * @param compound the NBT tag compound to write to
   *
   * @return the supplied NBT tag compound
   *
   * @since 1.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    NBTTagCompound records = new NBTTagCompound();
    for (int i = 0; i < ArcadeCatalog.GAME_IDS.length; i++) {
      if (highScores[i] <= 0) {
        continue;
      }
      NBTTagCompound record = new NBTTagCompound();
      record.setInteger(NBT_SCORE, highScores[i]);
      if (highScoreHolders[i] != null && !highScoreHolders[i].isEmpty()) {
        record.setString(NBT_HOLDER, highScoreHolders[i]);
      }
      records.setTag(ArcadeCatalog.GAME_IDS[i], record);
    }
    if (records.getSize() > 0) {
      compound.setTag(NBT_RECORDS, records);
    }
    return compound;
  }
}
