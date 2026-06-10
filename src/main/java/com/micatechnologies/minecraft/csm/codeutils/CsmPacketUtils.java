package com.micatechnologies.minecraft.csm.codeutils;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Shared helpers for safe network packet decoding and server-side validation of
 * client-to-server packets.
 *
 * <p>Decode helpers ({@link #readBoundedBytes}, {@link #readBoundedString},
 * {@link #readBoundedCount}) guard against allocation attacks: a length-prefixed payload's
 * claimed length is attacker-controlled, so it must be validated against both a caller-supplied
 * maximum and the bytes actually available in the buffer <em>before</em> any allocation.
 * Throwing {@link IndexOutOfBoundsException} from {@code fromBytes} is the correct failure
 * mode — netty/FML treats the packet as malformed and disconnects the sender.</p>
 *
 * <p>Validation helpers ({@link #canPlayerReach}, {@link #isOperatorOrCreative}) implement the
 * standard server-side checks every client-to-server block-targeting packet handler must run
 * before mutating world state. See the security review in
 * {@code assets/docs/agent_progress/PERFORMANCE_IMPROVEMENT_PLAN.md} (§16).</p>
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public final class CsmPacketUtils {

  /**
   * Squared maximum distance (8 blocks) a player may be from a block to edit it via a packet.
   * Matches vanilla container interaction distance.
   */
  private static final double MAX_REACH_DISTANCE_SQ = 64.0;

  private CsmPacketUtils() {
  }

  /**
   * Reads an int-prefixed byte array, rejecting negative, oversized, or lying length prefixes
   * before allocating.
   *
   * @param buf    the buffer to read from
   * @param maxLen the maximum acceptable length in bytes
   *
   * @return the decoded bytes
   *
   * @throws IndexOutOfBoundsException if the claimed length is negative, exceeds {@code maxLen},
   *                                   or exceeds the bytes actually remaining in the buffer
   */
  public static byte[] readBoundedBytes(ByteBuf buf, int maxLen) {
    int len = buf.readInt();
    if (len < 0 || len > maxLen || len > buf.readableBytes()) {
      throw new IndexOutOfBoundsException(
          "Bad byte array length " + len + " (max " + maxLen + ", readable "
              + buf.readableBytes() + ")");
    }
    byte[] bytes = new byte[len];
    buf.readBytes(bytes);
    return bytes;
  }

  /**
   * Reads an int-prefixed UTF-8 string, rejecting negative, oversized, or lying length prefixes
   * before allocating.
   *
   * @param buf    the buffer to read from
   * @param maxLen the maximum acceptable length in bytes
   *
   * @return the decoded string
   *
   * @throws IndexOutOfBoundsException if the claimed length is invalid (see
   *                                   {@link #readBoundedBytes})
   */
  public static String readBoundedString(ByteBuf buf, int maxLen) {
    return new String(readBoundedBytes(buf, maxLen), StandardCharsets.UTF_8);
  }

  /**
   * Reads an int element count for a length-prefixed list, rejecting negative, oversized, or
   * lying counts before the caller allocates a collection sized to it.
   *
   * @param buf             the buffer to read from
   * @param maxCount        the maximum acceptable element count
   * @param bytesPerElement the minimum encoded size of one element, used to reject counts that
   *                        could not possibly fit in the remaining buffer
   *
   * @return the validated element count
   *
   * @throws IndexOutOfBoundsException if the claimed count is negative, exceeds
   *                                   {@code maxCount}, or implies more payload than remains in
   *                                   the buffer
   */
  public static int readBoundedCount(ByteBuf buf, int maxCount, int bytesPerElement) {
    int count = buf.readInt();
    if (count < 0 || count > maxCount
        || (long) count * bytesPerElement > buf.readableBytes()) {
      throw new IndexOutOfBoundsException(
          "Bad element count " + count + " (max " + maxCount + ", readable "
              + buf.readableBytes() + ")");
    }
    return count;
  }

  /**
   * Checks whether the player may edit the block at {@code pos} via a packet: the chunk must
   * already be loaded (a client packet must never force-load chunks) and the player must be
   * within standard interaction reach (8 blocks).
   *
   * @param player the sending player
   * @param pos    the targeted block position
   *
   * @return true if the handler may proceed
   */
  public static boolean canPlayerReach(EntityPlayerMP player, BlockPos pos) {
    if (player == null || pos == null) {
      return false;
    }
    World world = player.world;
    if (world == null || !world.isBlockLoaded(pos)) {
      return false;
    }
    return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
        <= MAX_REACH_DISTANCE_SQ;
  }

  /**
   * Checks whether the player is a level-2 operator or in creative mode. Used to gate
   * infrastructure configuration packets whose GUIs are op/creative-gated in-world.
   *
   * @param player the sending player
   *
   * @return true if the player is an operator or in creative mode
   */
  public static boolean isOperatorOrCreative(EntityPlayerMP player) {
    return player != null && (player.canUseCommand(2, "") || player.isCreative());
  }
}
