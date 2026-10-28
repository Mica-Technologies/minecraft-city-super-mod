package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A free-standing EV charger (#315): the Level 2 pedestal, with one port or two, and the DC fast
 * charger. Each is a {@link BlockUtilityBox} unit two cells tall, as the parking meters are: this
 * block, the root, draws the whole charger and an invisible part fills the cell above.
 *
 * <p>Its status light ({@link EvChargerStatus}) is in metadata above the facing: unpowered it is
 * available (green), powered charging (blue), and a right-click with an empty hand sets or clears
 * a fault (red). Redstone is read at the root, the bottom cell. There is no tile entity and
 * nothing ticks.</p>
 *
 * @since 2026.10
 */
public class BlockEvCharger extends BlockUtilityBox {

  /** What a charger is; only its model and price differ. */
  public enum Kind {
    LEVEL_2, DC_FAST
  }

  private final Kind kind;

  public BlockEvCharger(String registryName, UtilityBoxSpec spec, Kind kind) {
    super(registryName, spec);
    this.kind = kind;
  }

  public Kind getKind() {
    return kind;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, EvChargerStatus.PROPERTY);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return EvChargerStatus.toMeta(state, super.getMetaFromState(state));
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3)
        .withProperty(EvChargerStatus.PROPERTY, EvChargerStatus.fromMeta(meta));
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    EvChargerStatus.refresh(world, pos, state);
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    EvChargerStatus.refresh(world, pos, world.getBlockState(pos));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    EvChargerStatus.toggleFault(world, pos, state, player);
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}
