package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A corner guard: a stainless or white vinyl angle on the outside corner of a wall, as in a
 * hospital or school corridor. One class, constructed by registry name.
 *
 * <p>Place it on the wall face beside the corner, nearer the corner edge: its facing is the way to
 * the wall and its {@link #EDGE} the side the corner is on (left or right as seen facing the wall),
 * taken from which half of the face was clicked. One flange lies on that face; the other wraps
 * round onto the wall's end face, outside the guard's own cell. Stack them for a full-height
 * guard.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockCornerGuard extends AbstractBlock {

  /**
   * Which edge of the wall face the corner is on, seen from the room, facing the wall.
   *
   * @since 1.0
   */
  public enum Edge implements IStringSerializable {
    LEFT, RIGHT;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<Edge> EDGE = PropertyEnum.create("edge", Edge.class);

  /** The left-hand guard with the wall to the north, within its own cell. */
  private static final AxisAlignedBB LEFT_NORTH =
      new AxisAlignedBB(0, 0, 0, 1.5 / 16, 1, 0.75 / 16);
  private static final AxisAlignedBB RIGHT_NORTH =
      new AxisAlignedBB(1 - 1.5 / 16, 0, 0, 1, 1, 0.75 / 16);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockCornerGuard}.
   *
   * @param registryName {@code corner_guard_steel} or {@code corner_guard_white}
   *
   * @since 1.0
   */
  public BlockCornerGuard(String registryName) {
    super(pendingMaterial(registryName), registryName.contains("steel") ? SoundType.METAL
        : SoundType.STONE, registryName.contains("steel") ? "pickaxe" : null, 0, 0.6F, 3F, 0F,
        0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(EDGE, Edge.LEFT));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    // Vinyl comes off by hand, as clay does.
    return registryName.contains("steel") ? Material.IRON : Material.CLAY;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, EDGE);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(EDGE, (meta & 4) != 0 ? Edge.RIGHT : Edge.LEFT);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex()
        | (state.getValue(EDGE) == Edge.RIGHT ? 4 : 0);
  }

  /**
   * Against the wall clicked, on the side of the face nearer the click; stacked on another guard,
   * the same as it.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    for (EnumFacing v : new EnumFacing[]{EnumFacing.DOWN, EnumFacing.UP}) {
      IBlockState other = worldIn.getBlockState(pos.offset(v));
      if (other.getBlock() == this) {
        return getDefaultState().withProperty(FACING, other.getValue(FACING))
            .withProperty(EDGE, other.getValue(EDGE));
      }
    }
    EnumFacing wall = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : placer.getHorizontalFacing();
    // How far along the face toward the left (anticlockwise from the wall) the click was.
    EnumFacing left = wall.rotateYCCW();
    double along = (hitX - 0.5) * left.getXOffset() + (hitZ - 0.5) * left.getZOffset();
    return getDefaultState().withProperty(FACING, wall)
        .withProperty(EDGE, along > 0 ? Edge.LEFT : Edge.RIGHT);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(state.getValue(EDGE) == Edge.LEFT ? LEFT_NORTH : RIGHT_NORTH,
        state.getValue(FACING));
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.CUTOUT;
  }
}
