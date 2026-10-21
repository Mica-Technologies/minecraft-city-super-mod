package com.micatechnologies.minecraft.csm.technology.rack;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A 42U server rack, two blocks tall: the open frame, the cabinets with a perforated or a glass
 * door, and the pre-filled cabinets. Placed as one piece into its block and the one above, as a
 * door is, and broken as one piece from either; only the lower cell ({@link #UPPER} false)
 * drops the rack and holds the tile entity.
 *
 * <p>Cabinets side by side in a row join: each draws its side panels only where the row ends,
 * {@link #PANEL_W} and {@link #PANEL_E} from actual state, named in model space (the model's
 * west is the viewer's right as they face the door). A cabinet joins any other cabinet, fillable
 * or pre-filled, facing the same way at the same height, so a pre-filled rack sits in a row of
 * filled ones unseen. The open frame has no panels and no join state.</p>
 *
 * <p>Stored: the facing (bits 0-1) and {@link #UPPER} (bit 2).</p>
 *
 * @since 2026.10
 */
public class BlockServerRack extends AbstractBlockRack {

  /** Whether this is the upper cell. */
  public static final PropertyBool UPPER = PropertyBool.create("upper");

  /** Whether the row ends on the model's west side, so the panel is drawn there. */
  public static final PropertyBool PANEL_W = PropertyBool.create("panel_w");

  /** Whether the row ends on the model's east side. */
  public static final PropertyBool PANEL_E = PropertyBool.create("panel_e");

  /**
   * A fillable rack.
   *
   * @param registryName its registry name
   * @param frame        {@link RackFrame#OPEN_42U} or {@link RackFrame#CABINET_42U}
   */
  public BlockServerRack(String registryName, RackFrame frame) {
    this(registryName, frame, false);
  }

  /**
   * A rack, pre-filled or not.
   *
   * @param registryName its registry name
   * @param frame        {@link RackFrame#OPEN_42U} or {@link RackFrame#CABINET_42U}
   * @param preFilled    whether its units are drawn by its blockstate, with no tile entity
   */
  public BlockServerRack(String registryName, RackFrame frame, boolean preFilled) {
    super(registryName, frame, !preFilled);
    IBlockState d = getDefaultState().withProperty(UPPER, false);
    if (!frame.isOpen()) {
      d = d.withProperty(PANEL_W, true).withProperty(PANEL_E, true);
    }
    setDefaultState(d);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    List<IProperty<?>> props = new ArrayList<>();
    props.add(FACING);
    props.add(UPPER);
    if (!pendingFrame().isOpen()) {
      props.add(PANEL_W);
      props.add(PANEL_E);
    }
    IProperty<?>[] array = props.toArray(new IProperty<?>[0]);
    return pendingFillable()
        ? new CsmExtendedBlockState(this, array, new IUnlistedProperty[]{CONTENTS})
        : new CsmBlockStateContainer(this, array);
  }

  @Override
  public int cellOf(IBlockState state) {
    return state.getValue(UPPER) ? 1 : 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(UPPER, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(UPPER) ? 4 : 0);
  }

  /** Whether a cabinet at {@code pos}'s neighbour on {@code side} continues the row. */
  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side, IBlockState state) {
    IBlockState other = world.getBlockState(pos.offset(side));
    Block block = other.getBlock();
    return block instanceof BlockServerRack
        && ((BlockServerRack) block).getFrame() == getFrame()
        && other.getValue(FACING) == state.getValue(FACING)
        && other.getValue(UPPER) == state.getValue(UPPER);
  }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    if (getFrame().isOpen()) {
      return actual;
    }
    EnumFacing facing = state.getValue(FACING);
    return actual.withProperty(PANEL_W, !joins(world, pos, facing.rotateYCCW(), state))
        .withProperty(PANEL_E, !joins(world, pos, facing.rotateY(), state));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  // --- placing and breaking as one piece -------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return pos.getY() < world.getHeight() - 1 && super.canPlaceBlockAt(world, pos)
        && world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(UPPER, false);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    world.setBlockState(pos.up(), state.withProperty(UPPER, true), 3);
  }

  /** A creative player breaking the upper cell takes the lower with it, dropping no rack. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(UPPER) && player.capabilities.isCreativeMode) {
      BlockPos below = pos.down();
      if (world.getBlockState(below).getBlock() == this) {
        dropContents(world, below);
        world.setBlockToAir(below);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /**
   * The other cell goes with this one: the lower cell drops its units and clears the upper; an
   * upper cell broken on its own breaks the lower, which drops the rack and its units.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(UPPER)) {
      BlockPos below = pos.down();
      if (world.getBlockState(below).getBlock() == this) {
        world.destroyBlock(below, true);
      }
    } else {
      dropContents(world, pos);
      if (world.getBlockState(pos.up()).getBlock() == this) {
        world.setBlockToAir(pos.up());
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(UPPER) ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  /** Every cell drops (and picks) the rack itself, whatever its facing. */
  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }
}
