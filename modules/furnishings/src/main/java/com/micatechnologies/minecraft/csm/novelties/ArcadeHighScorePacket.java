package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server message posting a finished run's score to the cabinet it was played on, for one
 * of the games in {@link ArcadeCatalog}. The player's name is not carried on the wire — the handler
 * takes it from the sending player, so a client cannot attribute a score to somebody else.
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class ArcadeHighScorePacket implements IMessage {

  /**
   * Upper bound on the game identifier read off the wire. The handler additionally rejects anything
   * not in the catalogue; this only stops an oversized string being allocated in the first place.
   *
   * @since 1.0
   */
  private static final int MAX_GAME_ID_CHARS = 64;

  /**
   * The position of the cabinet the score was scored on.
   *
   * @since 1.0
   */
  private BlockPos pos;

  /**
   * The identifier of the game that was played.
   *
   * @since 1.0
   */
  private String gameId;

  /**
   * The score being posted.
   *
   * @since 1.0
   */
  private int score;

  /**
   * Constructs an empty {@link ArcadeHighScorePacket}, as required by the network layer.
   *
   * @since 1.0
   */
  public ArcadeHighScorePacket() {
  }

  /**
   * Constructs an {@link ArcadeHighScorePacket} for the specified cabinet, game and score.
   *
   * @param pos    the position of the cabinet played
   * @param gameId the identifier of the game played
   * @param score  the score achieved
   *
   * @since 1.0
   */
  public ArcadeHighScorePacket(BlockPos pos, String gameId, int score) {
    this.pos = pos;
    this.gameId = gameId;
    this.score = score;
  }

  /**
   * Reads the packet contents from the supplied buffer.
   *
   * @param buf the buffer to read from
   *
   * @since 1.0
   */
  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.gameId = CsmPacketUtils.readBoundedString(buf, MAX_GAME_ID_CHARS);
    this.score = buf.readInt();
  }

  /**
   * Writes the packet contents to the supplied buffer.
   *
   * @param buf the buffer to write to
   *
   * @since 1.0
   */
  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buf, this.gameId);
    buf.writeInt(this.score);
  }

  /**
   * Retrieves the position of the cabinet the score belongs to.
   *
   * @return the cabinet position
   *
   * @since 1.0
   */
  public BlockPos getPos() {
    return pos;
  }

  /**
   * Retrieves the identifier of the game that was played.
   *
   * @return the game identifier, which the handler still validates against the catalogue
   *
   * @since 1.0
   */
  public String getGameId() {
    return gameId;
  }

  /**
   * Retrieves the score being posted, clamped to a sane range so a malformed or hostile packet
   * cannot store a nonsense value on the marquee.
   *
   * @return the score, clamped to 0..{@link TileEntityArcadeCabinet#MAX_SCORE}
   *
   * @since 1.0
   */
  public int getScore() {
    return Math.max(0, Math.min(TileEntityArcadeCabinet.MAX_SCORE, score));
  }
}
