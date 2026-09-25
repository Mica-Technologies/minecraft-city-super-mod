package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.transit.platform.BlockStationNameSign;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * What an entrance name board says: SUBWAY, METRO, then the station name sign's ten invented
 * names ({@link BlockStationNameSign#NAMES}), in the order {@code gen_transit_stations.py}'s
 * {@code LEGENDS} draws them. The roof counts one more at the front, for no board at all.
 *
 * @since 2026.9
 */
public final class StationLegends {

  /** What each board reads, in order. */
  public static final String[] LEGENDS = new String[2 + BlockStationNameSign.NAMES.length];

  static {
    LEGENDS[0] = "SUBWAY";
    LEGENDS[1] = "METRO";
    System.arraycopy(BlockStationNameSign.NAMES, 0, LEGENDS, 2,
        BlockStationNameSign.NAMES.length);
  }

  private StationLegends() {
  }

  /**
   * Steps the stepped value at {@code pos} (server side), a sneaking click back, and says on the
   * action bar what the board now reads.
   *
   * @param world  the world
   * @param pos    the block
   * @param player the player who clicked
   * @param count  how many values it has
   * @param none   whether value 1 is no board (the roof) rather than the first legend
   */
  public static void step(World world, BlockPos pos, EntityPlayer player, int count,
      boolean none) {
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
    int index = none ? next - 2 : next - 1;
    ITextComponent label = index < 0
        ? new TextComponentTranslation("csm.transit.entrance.none")
        : new TextComponentString(LEGENDS[index]);
    player.sendStatusMessage(new TextComponentTranslation("csm.transit.entrance.legend", label),
        true);
  }
}
