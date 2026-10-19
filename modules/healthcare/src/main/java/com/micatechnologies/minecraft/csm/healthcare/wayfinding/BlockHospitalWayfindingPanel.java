package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.wayfinding.HospitalWayfindingSign.Preset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * The hospital wayfinding panel ({@code hospital_wayfinding_panel}): a department or direction
 * sign of any size up to 16 x 6 blocks, built from cells placed side by side and stacked, all
 * facing the same way, hung from the ceiling on rods. A drop-in for Transit's
 * {@code airport_wayfinding_panel}: the same metadata (the facing, S 0 W 1 N 2 E 3, towards the
 * reader), the same tile entity NBT ({@link TileEntityHospitalWayfindingPanel}) and the same
 * rules for what a panel is ({@link HospitalWayfindingSign}), with hospital pictograms and
 * department colours.
 *
 * <p><b>Baked, not rendered.</b> Transit's panel draws its legend with a renderer every frame.
 * This one bakes it into the chunk: each cell's model is a charcoal slab with a frame on the
 * panel's outer edges (four actual-state booleans, 4 x 16 = 64 states), and
 * {@link HospitalWayfindingBakedModel} adds the cell's own share of the background, pictogram,
 * arrow, legend and hanger rods from one sheet, clipped to the cell. The cell's place in its panel
 * is worked out on the server when a cell is added or removed and kept in its tile entity, so the
 * model needs nothing but its own cell. Nothing runs per frame and nothing ticks. The panel gives
 * a little light (8), as a lit sign would.</p>
 *
 * <p>No editor screen: a {@code /blockdata} sets the sign, as on Transit's; by hand, a click
 * steps through the {@link Preset presets} and a sneak-click through the arrows.</p>
 *
 * @since 2026.10
 */
public class BlockHospitalWayfindingPanel extends BlockHealthcare implements
    ICsmTileEntityProvider {

  public static final PropertyBool EDGE_LEFT = PropertyBool.create("edge_left");
  public static final PropertyBool EDGE_RIGHT = PropertyBool.create("edge_right");
  public static final PropertyBool EDGE_TOP = PropertyBool.create("edge_top");
  public static final PropertyBool EDGE_BOTTOM = PropertyBool.create("edge_bottom");

  private static final IProperty<?>[] EDGES = {EDGE_LEFT, EDGE_RIGHT, EDGE_TOP, EDGE_BOTTOM};

  /** What the cell draws beyond its slab, from its tile entity and the ceiling above it. */
  public static final IUnlistedProperty<HospitalWayfindingFace> FACE =
      new IUnlistedProperty<HospitalWayfindingFace>() {
        @Override
        public String getName() {
          return "face";
        }

        @Override
        public boolean isValid(HospitalWayfindingFace value) {
          return value != null;
        }

        @Override
        public Class<HospitalWayfindingFace> getType() {
          return HospitalWayfindingFace.class;
        }

        @Override
        public String valueToString(HospitalWayfindingFace value) {
          return value.line1;
        }
      };

  /** Constructs the panel. */
  public BlockHospitalWayfindingPanel() {
    super("hospital_wayfinding_panel", new int[]{0, 0, 6, 16, 16, 10}, HealthcareMaterial.METAL,
        Mount.FLOOR, true);
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return EDGES;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(EDGE_LEFT, true).withProperty(EDGE_RIGHT, true)
        .withProperty(EDGE_TOP, true).withProperty(EDGE_BOTTOM, true);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this,
        new IProperty[]{FACING, EDGE_LEFT, EDGE_RIGHT, EDGE_TOP, EDGE_BOTTOM},
        new IUnlistedProperty[]{FACE});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    EnumFacing left = HospitalWayfindingSign.leftOf(facing);
    return state
        .withProperty(EDGE_LEFT, !HospitalWayfindingSign.joins(world, pos.offset(left), this,
            facing))
        .withProperty(EDGE_RIGHT, !HospitalWayfindingSign.joins(world,
            pos.offset(left.getOpposite()), this, facing))
        .withProperty(EDGE_TOP, !HospitalWayfindingSign.joins(world, pos.up(), this, facing))
        .withProperty(EDGE_BOTTOM, !HospitalWayfindingSign.joins(world, pos.down(), this,
            facing));
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    if (!(te instanceof TileEntityHospitalWayfindingPanel)) {
      return state;
    }
    TileEntityHospitalWayfindingPanel cell = (TileEntityHospitalWayfindingPanel) te;
    float[] rods = null;
    if (cell.getPanelHeight() > 0 && cell.getRow() == cell.getPanelHeight() - 1) {
      rods = HospitalWayfindingSign.rodsOf(world, pos, cell.getCol(), cell.getPanelWidth());
    }
    return ((IExtendedBlockState) state).withProperty(FACE, cell.face(rods));
  }

  /** A lit sign: a little light of its own. */
  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return 8;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  // --- the panel ---------------------------------------------------------------------------

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (world.isRemote) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityHospitalWayfindingPanel
        && !((TileEntityHospitalWayfindingPanel) te).hasOwnSign()) {
      HospitalWayfindingSign.adoptNeighbourSign(world, pos, this, facing,
          (TileEntityHospitalWayfindingPanel) te);
    }
    HospitalWayfindingSign.refresh(world, pos, this, facing);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    super.breakBlock(world, pos, state);
    if (!world.isRemote) {
      HospitalWayfindingSign.refresh(world, pos, this, state.getValue(FACING));
    }
  }

  /**
   * A click shows the next preset on the whole panel, a sneak-click turns its arrow to the next
   * direction; both with an empty hand.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityHospitalWayfindingPanel)) {
      return true;
    }
    TileEntityHospitalWayfindingPanel cell = (TileEntityHospitalWayfindingPanel) te;
    if (player.isSneaking()) {
      HospitalWayfindingSign.Arrow next = cell.getArrow().next();
      HospitalWayfindingSign.applyToPanel(world, pos, cell.getLine1(), cell.getLine2(),
          cell.getPictogram(), next, cell.getScheme(), cell.isDoubleSided());
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.healthcare.wayfinding.arrow", next.getId()), true);
    } else {
      Preset p = Preset.after(cell.getLine1(), cell.getLine2());
      HospitalWayfindingSign.applyToPanel(world, pos, p.getLine1(), p.getLine2(),
          p.getPictogram(), cell.getArrow(), p.getScheme(), cell.isDoubleSided());
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.healthcare.wayfinding.preset", p.getLabel()), true);
    }
    return true;
  }

  // --- tile entity ---------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHospitalWayfindingPanel.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhospitalwayfindingpanel";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityHospitalWayfindingPanel();
  }
}
