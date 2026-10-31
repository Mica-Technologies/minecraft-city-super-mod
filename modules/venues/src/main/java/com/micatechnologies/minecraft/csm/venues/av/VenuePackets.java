package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * What the venues module's packet handlers share: the reach check and the string codec.
 *
 * <p>A unit's screen is opened from any of its cells but names its master, which on a
 * centre-hung scoreboard may be eleven blocks from the cell clicked. So a unit is in reach when
 * the nearest point of its outline is within the standard 8 blocks of the player's eyes, and
 * anything else when its own block is ({@link CsmPacketUtils#canPlayerReach}). Either way its
 * chunk must already be loaded: a packet never loads one.</p>
 *
 * @since 2026.10
 */
public final class VenuePackets {

  private static final double REACH_SQ = 8.0 * 8.0;

  private VenuePackets() {
  }

  /**
   * Whether a player may edit the block at {@code pos} by packet.
   *
   * @param player the sender
   * @param pos    the block (a unit's master)
   *
   * @return true if the handler may go on
   */
  public static boolean canReach(EntityPlayerMP player, BlockPos pos) {
    if (player == null || pos == null || player.world == null
        || !player.world.isBlockLoaded(pos) || !player.capabilities.allowEdit) {
      return false;
    }
    World world = player.world;
    if (!world.isBlockModifiable(player, pos)) {
      return false;
    }
    IBlockState s = world.getBlockState(pos);
    if (!(s.getBlock() instanceof BlockVenueUnit)) {
      return CsmPacketUtils.canPlayerReach(player, pos);
    }
    AxisAlignedBB box = ((BlockVenueUnit) s.getBlock()).worldOutline(pos,
        s.getValue(BlockVenue.FACING));
    Vec3d eye = player.getPositionEyes(1.0F);
    double x = Math.max(box.minX, Math.min(box.maxX, eye.x));
    double y = Math.max(box.minY, Math.min(box.maxY, eye.y));
    double z = Math.max(box.minZ, Math.min(box.maxZ, eye.z));
    return eye.squareDistanceTo(x, y, z) <= REACH_SQ;
  }

  /** Writes an int-prefixed UTF-8 string, for {@link CsmPacketUtils#readBoundedString}. */
  static void writeString(ByteBuf buf, String s) {
    byte[] bytes = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }
}
