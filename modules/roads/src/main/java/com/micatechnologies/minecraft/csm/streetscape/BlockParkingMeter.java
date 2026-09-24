package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A parking meter or pay station: a {@link BlockUtilityBox} unit that sells time.
 *
 * <p>Right-click with an empty hand for the meter's screen (time left, pay, and for the owner or
 * an operator the settings and the takings). With emeralds in hand, when the meter takes
 * emeralds, a click buys one block of time on the head that was clicked. With anything else in
 * hand a click shows the time left. {@link ParkingPayments} does the paying.</p>
 *
 * <p>A meter gives a redstone signal while any of its heads has run out, and a never-paid head
 * has. It flips on time without a ticking tile entity: {@link #refresh} tells the neighbours to
 * look again and schedules one block update at the next
 * expiry (at most {@link #MAX_CHECK_TICKS} ahead, since the world keeps one pending update per
 * block and the other head's expiry may come first). A pay station's spaces are not physical, so
 * it gives none.</p>
 *
 * @version 1.0
 */
public class BlockParkingMeter extends BlockUtilityBox implements ICsmTileEntityProvider {

  /** The meter screen's GUI id, unique across the mod. */
  public static final int GUI_ID = 33;

  /** The longest a meter waits before checking its heads again. */
  private static final int MAX_CHECK_TICKS = 400;

  /** What a meter is, which decides its display and whether it has numbered spaces. */
  public enum Kind {
    MECHANICAL, DIGITAL, STATION
  }

  private final Kind kind;
  /** Each head's display facing north: centre x, centre y, face z, width, height, in pixels. */
  private final float[][] heads;

  /**
   * @param registryName the registry name
   * @param spec         the unit's size and box
   * @param kind         mechanical, digital or pay station
   * @param heads        each head's display, facing north: {cx, cy, faceZ, width, height} in
   *                     pixels; a pay station has one, its screen
   */
  public BlockParkingMeter(String registryName, UtilityBoxSpec spec, Kind kind,
      float[][] heads) {
    super(registryName, spec);
    this.kind = kind;
    this.heads = heads;
  }

  public Kind getKind() {
    return kind;
  }

  public float[][] getHeads() {
    return heads;
  }

  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityParkingMeter) {
      TileEntityParkingMeter meter = (TileEntityParkingMeter) te;
      meter.setOwner(placer.getUniqueID(), placer.getName());
      meter.setSpaces(kind == Kind.STATION ? TileEntityParkingMeter.DEFAULT_SPACES
          : heads.length);
      meter.markDirtySync(world, pos, true);
    }
    refresh(world, pos);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    ItemStack held = player.getHeldItem(hand);
    boolean emerald = held.getItem() == Items.EMERALD;
    if (world.isRemote) {
      if (held.isEmpty() || (emerald && kind == Kind.STATION)) {
        player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
      }
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityParkingMeter) || held.isEmpty()
        || (emerald && kind == Kind.STATION)) {
      return true;
    }
    TileEntityParkingMeter meter = (TileEntityParkingMeter) te;
    if (emerald && !ParkingPaymentSum.isAvailable()) {
      ParkingPayments.pay(player, world, pos, meter, headAt(state.getValue(FACING), hitX, hitZ),
          1);
    } else {
      ParkingPayments.tell(player, ParkingPayments.status(meter));
    }
    return true;
  }

  /** Which head a click at (hitX, hitZ) in the cell was nearest, from the front. */
  private int headAt(EnumFacing facing, float hitX, float hitZ) {
    if (heads.length < 2) {
      return 0;
    }
    float local;
    switch (facing) {
      case SOUTH:
        local = 1 - hitX;
        break;
      case EAST:
        local = hitZ;
        break;
      case WEST:
        local = 1 - hitZ;
        break;
      default:
        local = hitX;
        break;
    }
    int best = 0;
    for (int i = 1; i < heads.length; i++) {
      if (Math.abs(heads[i][0] / 16f - local) < Math.abs(heads[best][0] / 16f - local)) {
        best = i;
      }
    }
    return best;
  }

  // ----------------------------------------------------------------------------------------
  // Redstone
  // ----------------------------------------------------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(IBlockState state) {
    return kind != Kind.STATION;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return kind != Kind.STATION;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    if (kind == Kind.STATION) {
      return 0;
    }
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityParkingMeter
        && ((TileEntityParkingMeter) te).anyExpired(System.currentTimeMillis()) ? 15 : 0;
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    refresh(world, pos);
  }

  /**
   * Tells the neighbours to read the redstone signal again, and schedules the next check.
   * Called after every payment and by the scheduled update itself.
   */
  static void refresh(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    TileEntity te = world.getTileEntity(pos);
    if (!(block instanceof BlockParkingMeter) || !(te instanceof TileEntityParkingMeter)) {
      return;
    }
    BlockParkingMeter meterBlock = (BlockParkingMeter) block;
    TileEntityParkingMeter meter = (TileEntityParkingMeter) te;
    long now = System.currentTimeMillis();
    // Always tell the neighbours rather than remembering the last output to compare with: that
    // would be one more field that must survive saves and syncs. This runs on a payment and at
    // most every MAX_CHECK_TICKS while time is running, so it is cheap.
    if (meterBlock.kind != Kind.STATION) {
      world.notifyNeighborsOfStateChange(pos, block, false);
    }
    long next = meter.nextExpiry(now);
    if (next > 0) {
      long ticks = (next - now) / 50 + 1;
      world.scheduleUpdate(pos, block, (int) Math.min(MAX_CHECK_TICKS, Math.max(1, ticks)));
    }
  }

  /** The takings in emeralds are not lost with the meter: they drop where it stood. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityParkingMeter) {
        long left = ((TileEntityParkingMeter) te).getStoredEmeralds();
        while (left > 0) {
          int n = (int) Math.min(64, left);
          world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5,
              pos.getZ() + 0.5, new ItemStack(Items.EMERALD, n)));
          left -= n;
        }
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityParkingMeter.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityparkingmeter";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityParkingMeter();
  }
}
