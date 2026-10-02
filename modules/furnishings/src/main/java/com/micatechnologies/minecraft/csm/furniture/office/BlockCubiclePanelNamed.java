package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A full-height cubicle panel with a name on one face: a slide-in name plate (two lines) or a
 * larger cubicle sign (three lines, the department on a dark band), {@link CubicleSignStyle}.
 * It is a {@link BlockCubiclePanel} in every other way, so it joins, stacks and turns corners
 * with the plain panels, and they with it: put it in a cubicle wall where the name belongs,
 * usually the block beside the opening.
 *
 * <p>The plate is on the face toward whoever placed the panel ({@link #FACING}, stored), so
 * standing in the aisle and placing it puts the name on the aisle side; a panel standing alone
 * runs across that view, as a plain panel does. The holder and the blank insert are part of the
 * baked model; the words are {@link TileEntityCubicleNamePlate}'s, drawn by
 * {@link TileEntityCubicleNamePlateRenderer}. To stack, the plate goes on whichever panel of the
 * stack it is placed as: the lower one of a 1.5 m wall reads at desk height, the upper one of a
 * two-metre wall at eye height.</p>
 *
 * <p>Right-click the plate with an empty hand, or sneak and right-click anywhere on the panel,
 * to edit the words. The other face takes a shelf as a plain panel's does: right-click it with
 * an empty hand ({@link #BACK_SHELF}, stored).</p>
 *
 * @since 2026.10
 */
public class BlockCubiclePanelNamed extends BlockCubiclePanel implements ICsmTileEntityProvider {

  /** The editor's GUI id, unique across the mod. */
  public static final int GUI_ID = 45;

  /** The face the plate is on (stored). */
  public static final PropertyDirection FACING =
      PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
  /** Whether a shelf hangs on the face behind the plate (stored). */
  public static final PropertyBool BACK_SHELF = PropertyBool.create("shelf");
  /**
   * Whether the sign is drawn a block and a half wide, a quarter block over each neighbour:
   * where it has a full-height plain panel on both sides, running on in line with it (actual
   * state). Always false for the name plate. See {@link CubicleSignStyle}.
   */
  public static final PropertyBool WIDE = PropertyBool.create("wide");

  /** How far the plate stands proud of the panel's fabric, in blocks. */
  private static final double PLATE_DEPTH = 1.0 / 16.0;
  /** The fabric's face, from the middle of the block, in blocks. */
  private static final double FABRIC = 0.75 / 16.0;

  private final CubicleSignStyle style;

  /**
   * Constructs a full-height cubicle panel carrying a name.
   *
   * @param registryName its registry name, ending in its fabric
   * @param style        the plate or sign it carries
   */
  public BlockCubiclePanelNamed(String registryName, CubicleSignStyle style) {
    super(registryName, 16);
    this.style = style;
  }

  /**
   * What it carries.
   *
   * @return its style
   */
  public CubicleSignStyle getStyle() {
    return style;
  }

  @Override
  protected IBlockState defaultPanelState(IBlockState base) {
    return base.withProperty(FACING, EnumFacing.SOUTH).withProperty(BACK_SHELF, false)
        .withProperty(WIDE, false);
  }

  @Override
  protected boolean standsAlongX(IBlockState state) {
    return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z;
  }

  @Nullable
  @Override
  protected EnumFacing shelfFace(IBlockState state) {
    return state.getValue(BACK_SHELF) ? state.getValue(FACING).getOpposite() : null;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, FACING, BACK_SHELF,
        WIDE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    return actual.withProperty(WIDE, style.canWiden() && isWide(world, pos, actual));
  }

  /**
   * Whether the sign has room to be drawn wide: on both sides along its face, a full-height
   * cubicle panel without a plate of its own (which the wide sign would overlap), joined to
   * this one.
   *
   * @param world  the world
   * @param pos    the panel
   * @param actual its actual state, sides resolved
   *
   * @return whether it is wide
   */
  private static boolean isWide(IBlockAccess world, BlockPos pos, IBlockState actual) {
    EnumFacing face = actual.getValue(FACING);
    for (EnumFacing side : new EnumFacing[]{face.rotateY(), face.rotateYCCW()}) {
      if (actual.getValue(sideProperty(side)) != Side.PANEL) {
        return false;
      }
      IBlockState next = world.getBlockState(pos.offset(side));
      if (!(next.getBlock() instanceof BlockCubiclePanel)
          || next.getBlock() instanceof BlockCubiclePanelNamed
          || ((BlockCubiclePanel) next.getBlock()).getHeight() < 16) {
        return false;
      }
    }
    return true;
  }

  private static PropertyEnum<Side> sideProperty(
      EnumFacing side) {
    switch (side) {
      case NORTH:
        return NORTH;
      case SOUTH:
        return SOUTH;
      case EAST:
        return EAST;
      default:
        return WEST;
    }
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(BACK_SHELF) ? 4 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(BACK_SHELF, (meta & 4) != 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    // The plate faces whoever placed it.
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  /**
   * Right-click the plate with an empty hand, or sneak and right-click anywhere, to edit the
   * words; right-click the face behind the plate with an empty hand to hang a shelf there, again
   * to take it down.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    EnumFacing plate = state.getValue(FACING);
    if (player.isSneaking() || side == plate) {
      if (world.isRemote) {
        player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
      }
      return true;
    }
    if (side != plate.getOpposite() || !isBroadFace(world, pos, state, side)) {
      return false;
    }
    if (!world.isRemote) {
      boolean remove = state.getValue(BACK_SHELF);
      world.setBlockState(pos, state.withProperty(BACK_SHELF, !remove), 3);
      world.playSound(null, pos, remove ? SoundEvents.BLOCK_WOOD_BREAK
          : SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.8F, 1.1F);
    }
    return true;
  }

  /** The panel's box, reaching out over the plate so a click on it lands on its face. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    AxisAlignedBB box = super.getBlockBoundingBox(state, source, pos);
    double front = 0.5 - FABRIC - PLATE_DEPTH;
    switch (state.getValue(FACING)) {
      case NORTH:
        return new AxisAlignedBB(box.minX, box.minY, Math.min(box.minZ, front), box.maxX,
            box.maxY, box.maxZ);
      case SOUTH:
        return new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, box.maxY,
            Math.max(box.maxZ, 1 - front));
      case WEST:
        return new AxisAlignedBB(Math.min(box.minX, front), box.minY, box.minZ, box.maxX,
            box.maxY, box.maxZ);
      default:
        return new AxisAlignedBB(box.minX, box.minY, box.minZ, Math.max(box.maxX, 1 - front),
            box.maxY, box.maxZ);
    }
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityCubicleNamePlate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycubiclenameplate";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityCubicleNamePlate();
  }
}
