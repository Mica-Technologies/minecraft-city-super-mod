package com.micatechnologies.minecraft.csm.transit.platform;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * What the stepped station signs share (the platform number sign, the number band column and
 * the station name sign), the aisle sign's pattern: a click steps what the sign shows up one, a
 * sneaking click down one, round from the last to the first; the value lives in a
 * {@link TileEntityPlatformSign} and reaches the model as actual state, where the blockstate
 * swaps the face's texture for it.
 *
 * @since 2026.9
 */
public final class PlatformSigns {

  /** The tile entity name every stepped sign registers its tile entity under. */
  public static final String TILE_ENTITY_NAME = "tileentityplatformsign";

  private PlatformSigns() {
  }

  /**
   * The value of the sign at {@code pos}, 1 if its tile entity is not there. Safe off the main
   * thread: a chunk being rendered is read without creating a tile entity in it.
   *
   * @param world the world
   * @param pos   the sign
   * @param count how many values the sign has
   *
   * @return 1 to {@code count}
   */
  public static int valueAt(IBlockAccess world, BlockPos pos, int count) {
    TileEntity te = world instanceof ChunkCache
        ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
        : world.getTileEntity(pos);
    int value = te instanceof TileEntityPlatformSign ? ((TileEntityPlatformSign) te).getValue()
        : 1;
    return Math.max(1, Math.min(count, value));
  }

  /**
   * Steps the sign at {@code pos} (server side) and says on the action bar what it now shows.
   *
   * @param world   the world
   * @param pos     the sign
   * @param player  the player who clicked
   * @param count   how many values the sign has
   * @param langKey the action bar message, given what the sign shows
   * @param labels  what each value reads as, or null for the number itself
   */
  public static void step(World world, BlockPos pos, EntityPlayer player, int count,
      String langKey, String[] labels) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPlatformSign)) {
      return;
    }
    TileEntityPlatformSign sign = (TileEntityPlatformSign) te;
    int current = Math.max(1, Math.min(count, sign.getValue()));
    int step = player.isSneaking() ? count - 1 : 1;
    int next = (current - 1 + step) % count + 1;
    sign.setValue(next);
    world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
        0.3F, 0.8F);
    player.sendStatusMessage(new TextComponentTranslation(langKey,
        labels != null ? labels[next - 1] : String.valueOf(next)), true);
  }
}
