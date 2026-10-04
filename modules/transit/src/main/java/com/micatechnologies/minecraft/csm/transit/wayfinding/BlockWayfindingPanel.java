package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.panel.CellPanel;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The large hanging sign: a backlit terminal sign of any size up to
 * {@value WayfindingSign#MAX_WIDTH} x {@value WayfindingSign#MAX_HEIGHT} blocks, built from cells
 * of this block placed side by side and stacked, all facing the same way (they face the player
 * who places them). It is the big brother of the one-block airport signs, made to be read from
 * the floor under a high terminal ceiling.
 *
 * <p>Each cell's baked model is a graphite slab four pixels deep with a frame on the panel's
 * outer edges only ({@link #EDGE_LEFT}, {@link #EDGE_RIGHT}, {@link #EDGE_TOP},
 * {@link #EDGE_BOTTOM}, actual state, named as the reader sees them). The legend is not baked: the
 * panel's bottom-left cell, its controller, draws it once across the whole face, with its
 * hanger rods ({@link TileEntityWayfindingPanelRenderer}). Right-click any cell with an empty hand
 * to edit the sign ({@link WayfindingPanelGui}); see {@link WayfindingSign} for what it can say.</p>
 *
 * <p>The model comes from {@code dev-env-utils/scripts/gen_transit_airport.py}'s
 * {@code wayfinding_panel()}.</p>
 *
 * @since 2026.10
 */
public class BlockWayfindingPanel extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** The editor's GUI id, unique across the mod. */
  public static final int GUI_ID = 46;

  /** No cell of the panel to the reader's left: the frame runs down this side. */
  public static final PropertyBool EDGE_LEFT = PropertyBool.create("edge_left");
  public static final PropertyBool EDGE_RIGHT = PropertyBool.create("edge_right");
  public static final PropertyBool EDGE_TOP = PropertyBool.create("edge_top");
  public static final PropertyBool EDGE_BOTTOM = PropertyBool.create("edge_bottom");

  /**
   * Constructs the block.
   *
   * @param registryName its registry name
   */
  public BlockWayfindingPanel(String registryName) {
    super(registryName, new double[]{0, 0, 6, 16, 16, 10}, false, 0);
    setDefaultState(getDefaultState().withProperty(EDGE_LEFT, true)
        .withProperty(EDGE_RIGHT, true).withProperty(EDGE_TOP, true)
        .withProperty(EDGE_BOTTOM, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, EDGE_LEFT, EDGE_RIGHT, EDGE_TOP,
        EDGE_BOTTOM);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    CellPanel.noteActualState(world);
    EnumFacing facing = state.getValue(FACING);
    EnumFacing left = WayfindingSign.leftOf(facing);
    return state
        .withProperty(EDGE_LEFT, !WayfindingSign.joins(world, pos.offset(left), this, facing))
        .withProperty(EDGE_RIGHT,
            !WayfindingSign.joins(world, pos.offset(left.getOpposite()), this, facing))
        .withProperty(EDGE_TOP, !WayfindingSign.joins(world, pos.up(), this, facing))
        .withProperty(EDGE_BOTTOM, !WayfindingSign.joins(world, pos.down(), this, facing));
  }

  /**
   * A cell placed against a panel takes the panel's sign, so that the panel keeps its legend
   * whichever cell ends up its controller.
   */
  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    TileEntity mine = world.getTileEntity(pos);
    if (!(mine instanceof TileEntityWayfindingPanel)) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    EnumFacing left = WayfindingSign.leftOf(facing);
    for (EnumFacing side : new EnumFacing[]{left.getOpposite(), EnumFacing.UP, left,
        EnumFacing.DOWN}) {
      BlockPos at = pos.offset(side);
      if (!WayfindingSign.joins(world, at, this, facing)) {
        continue;
      }
      TileEntity other = world.getTileEntity(at);
      if (other instanceof TileEntityWayfindingPanel) {
        TileEntityWayfindingPanel cell = (TileEntityWayfindingPanel) mine;
        cell.copySign((TileEntityWayfindingPanel) other);
        cell.markDirtySync(world, pos, true);
        return;
      }
    }
  }

  /** Right-click any cell with an empty hand to edit the panel's sign. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      // opened on the cell clicked, which the edit names: the server checks reach to it, and a
      // wide panel's controller may be further off than that
      player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityWayfindingPanel.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitywayfindingpanel";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityWayfindingPanel();
  }
}
