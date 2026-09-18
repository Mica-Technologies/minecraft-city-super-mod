package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockPane;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A window pane that joins others of its kind into one window: clear, tinted, one-way, wired,
 * bullet-resistant or frosted ({@link GlassKind}).
 *
 * <p>Like a vanilla pane it runs out to each side it connects on -- another pane, a {@link
 * BlockGlazing} block, or a solid face such as the wall it is set in. Unlike one, it knows which
 * of those connections are the same window: each side is {@link Side#NONE} (no pane that way),
 * {@link Side#EDGE} (the pane runs to the edge of the block and meets a wall or different glass)
 * or {@link Side#GLASS} (it runs on into the same glass). A thin dark bronze frame is drawn only
 * where the window ends -- at an {@code EDGE}, along the top where no pane of the same glass is
 * above and along the bottom where none is below -- so any run and stack of panes reads as one
 * window. A mullion stands at the centre wherever the pane turns or branches, where glass would
 * otherwise meet glass at an angle, and at a pane's free end, where it is the frame.</p>
 *
 * <p>One-way glass is dark on the side it faces, clear on the other. Its facing is stored, as the
 * direction the placer was looking; if that runs along the pane, the actual state turns it a
 * quarter so it is one of the pane's two faces.</p>
 *
 * <p>The models come from {@code dev-env-utils/scripts/gen_glazing.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockGlazingPane extends AbstractBlock {

  /**
   * What a pane does toward one side.
   *
   * @since 1.0
   */
  public enum Side implements IStringSerializable {
    /** No pane that way. */
    NONE("none"),
    /** The pane runs to the block's edge and meets a wall or different glass: framed there. */
    EDGE("edge"),
    /** The pane runs on into the same glass: no frame. */
    GLASS("glass");

    private final String name;

    Side(String name) {
      this.name = name;
    }

    @Override
    @Nonnull
    public String getName() {
      return name;
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<Side> NORTH = PropertyEnum.create("north", Side.class);
  public static final PropertyEnum<Side> EAST = PropertyEnum.create("east", Side.class);
  public static final PropertyEnum<Side> SOUTH = PropertyEnum.create("south", Side.class);
  public static final PropertyEnum<Side> WEST = PropertyEnum.create("west", Side.class);
  /** The same glass above or below. */
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  /** A mullion stands at the centre: the pane turns or branches here, or ends free here. */
  public static final PropertyBool POST = PropertyBool.create("post");

  /** Half the pane's thickness, for its box. */
  private static final double HALF = 1.0 / 16.0;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockGlazingPane}.
   *
   * @param registryName {@code glass_pane_<kind>}
   *
   * @since 1.0
   */
  public BlockGlazingPane(String registryName) {
    super(pendingMaterial(registryName), SoundType.GLASS, "pickaxe", 0,
        GlassKind.fromRegistryName(registryName).hardness(),
        GlassKind.fromRegistryName(registryName).resistance(), 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.GLASS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN, POST);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /**
   * Faces away from the player who places it: that is the outside, which one-way glass draws
   * dark.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  private Side side(IBlockAccess world, BlockPos pos, EnumFacing toward) {
    BlockPos at = pos.offset(toward);
    IBlockState other = world.getBlockState(at);
    if (other.getBlock() == this) {
      return Side.GLASS;
    }
    if (other.getBlock() instanceof BlockGlazingPane || other.getBlock() instanceof BlockPane
        || other.getBlockFaceShape(world, at, toward.getOpposite()) == BlockFaceShape.SOLID) {
      return Side.EDGE;
    }
    return Side.NONE;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    Side n = side(worldIn, pos, EnumFacing.NORTH);
    Side e = side(worldIn, pos, EnumFacing.EAST);
    Side s = side(worldIn, pos, EnumFacing.SOUTH);
    Side w = side(worldIn, pos, EnumFacing.WEST);
    boolean alongX = e != Side.NONE || w != Side.NONE;
    boolean alongZ = n != Side.NONE || s != Side.NONE;
    // A mullion where the pane turns or branches, and at a free end, where it is the frame. A lone
    // pane is drawn along x, as though it ran from wall to wall.
    int arms = (n != Side.NONE ? 1 : 0) + (e != Side.NONE ? 1 : 0) + (s != Side.NONE ? 1 : 0)
        + (w != Side.NONE ? 1 : 0);
    boolean post = (alongX && alongZ) || arms == 1;
    EnumFacing facing = state.getValue(FACING);
    if ((!alongZ && facing.getAxis() == EnumFacing.Axis.X)
        || (alongZ && !alongX && facing.getAxis() == EnumFacing.Axis.Z)) {
      facing = facing.rotateY();
    }
    return state.withProperty(NORTH, n).withProperty(EAST, e).withProperty(SOUTH, s)
        .withProperty(WEST, w)
        .withProperty(UP, worldIn.getBlockState(pos.up()).getBlock() == this)
        .withProperty(DOWN, worldIn.getBlockState(pos.down()).getBlock() == this)
        .withProperty(POST, post)
        .withProperty(FACING, facing);
  }

  /**
   * The pane's boxes: a thin centre, and an arm out to each side it runs to. A lone pane runs
   * along x.
   */
  private static AxisAlignedBB[] boxes(IBlockState actual) {
    boolean n = actual.getValue(NORTH) != Side.NONE;
    boolean e = actual.getValue(EAST) != Side.NONE;
    boolean s = actual.getValue(SOUTH) != Side.NONE;
    boolean w = actual.getValue(WEST) != Side.NONE;
    if (!n && !e && !s && !w) {
      e = true;
      w = true;
    }
    double lo = 0.5 - HALF;
    double hi = 0.5 + HALF;
    return new AxisAlignedBB[]{
        new AxisAlignedBB(lo, 0, lo, hi, 1, hi),
        n ? new AxisAlignedBB(lo, 0, 0, hi, 1, 0.5) : null,
        s ? new AxisAlignedBB(lo, 0, 0.5, hi, 1, 1) : null,
        w ? new AxisAlignedBB(0, 0, lo, 0.5, 1, hi) : null,
        e ? new AxisAlignedBB(0.5, 0, lo, 1, 1, hi) : null};
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(worldIn, pos);
    for (AxisAlignedBB box : boxes(actual)) {
      if (box != null) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
      }
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB union = null;
    for (AxisAlignedBB box : boxes(state.getActualState(source, pos))) {
      if (box != null) {
        union = union == null ? box : union.union(box);
      }
    }
    return union;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER_SMALL
        : BlockFaceShape.MIDDLE_POLE_THIN;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}
