package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cubicle panel: a fabric-covered office partition, thin as a fence, that joins into walls of
 * any plan. Each side ({@link #NORTH}, {@link #EAST}, {@link #SOUTH}, {@link #WEST}, in world
 * directions) is {@link Side#PANEL} where it runs on into another panel or meets a solid wall,
 * {@link Side#END} where the panel runs to the block's edge and stops there with an end post,
 * and {@link Side#NONE} where there is nothing. A post stands in the middle only where two
 * panels meet at a corner, a tee or a cross (the blockstate asks for two sides at right
 * angles); along a straight run there is none, and a run's last block carries its panel on to
 * the block's edge, so posts appear only at ends and corners. A panel with no neighbour at all
 * stands across the direction it was placed facing, with an end post at each edge.
 *
 * <p>Panels stack: a full-height panel with another panel on it leaves off its top cap
 * ({@link #UP}), so two read as one two-metre panel, and a half panel on a full one as the
 * usual 1.5 m. A panel may carry a shelf on either face: right-click a face with an empty hand
 * to hang one there, again to take it down ({@link #SHELF}, stored, with the axis the lone
 * panel stands along).</p>
 *
 * @since 2026.9
 */
public class BlockCubiclePanel extends AbstractBlock {

  /** What a side of the panel does. */
  public enum Side implements IStringSerializable {
    /** Nothing on that side. */
    NONE,
    /** The panel runs on into another panel, or into a wall. */
    PANEL,
    /** The panel runs to the edge of the block and ends there in a post. */
    END;

    @Nonnull
    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** Which face of the panel carries a shelf, in world directions. */
  public enum Shelf implements IStringSerializable {
    NONE, NORTH, SOUTH, EAST, WEST;

    @Nonnull
    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }

    /**
     * The shelf on a face.
     *
     * @param face a horizontal face
     *
     * @return its shelf
     */
    static Shelf of(EnumFacing face) {
      switch (face) {
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
  }

  public static final PropertyEnum<Side> NORTH = PropertyEnum.create("north", Side.class);
  public static final PropertyEnum<Side> EAST = PropertyEnum.create("east", Side.class);
  public static final PropertyEnum<Side> SOUTH = PropertyEnum.create("south", Side.class);
  public static final PropertyEnum<Side> WEST = PropertyEnum.create("west", Side.class);
  /** Another panel stands on this full-height one. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** Whether a panel standing alone runs east-west (stored). */
  public static final PropertyBool ALONG_X = PropertyBool.create("along_x");
  /** The shelf, if it has one (stored). */
  public static final PropertyEnum<Shelf> SHELF = PropertyEnum.create("shelf", Shelf.class);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  /** Half the panel's thickness, in blocks. */
  private static final double HALF = 1.0 / 16.0;
  /** How far a shelf stands out from the panel's face, in blocks. */
  private static final double SHELF_DEPTH = 6.0 / 16.0;

  private final String registryName;
  /** The panel's height, in sixteenths. */
  private final int height;

  /**
   * Constructs a cubicle panel.
   *
   * @param registryName its registry name, ending in its fabric
   * @param height       its height in sixteenths: 16 for a full panel, 8 for a half
   */
  public BlockCubiclePanel(String registryName, int height) {
    super(stash(registryName), SoundType.CLOTH, "axe", 0, 1.0F, 2.0F, 0.0F, 0);
    this.registryName = registryName;
    this.height = height;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(NORTH, Side.NONE)
        .withProperty(EAST, Side.NONE).withProperty(SOUTH, Side.NONE)
        .withProperty(WEST, Side.NONE).withProperty(UP, false).withProperty(ALONG_X, true)
        .withProperty(SHELF, Shelf.NONE));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.CLOTH;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  /**
   * The panel's height.
   *
   * @return its height in sixteenths
   */
  public int getHeight() {
    return height;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, ALONG_X, SHELF);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(ALONG_X) ? 0 : 1) | (state.getValue(SHELF).ordinal() << 1);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    int shelf = (meta >> 1) & 7;
    return getDefaultState().withProperty(ALONG_X, (meta & 1) == 0)
        .withProperty(SHELF, Shelf.values()[shelf < Shelf.values().length ? shelf : 0]);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    // The panel stands across the placer's view.
    return getDefaultState().withProperty(ALONG_X,
        placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.Z);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    Side[] sides = new Side[4];
    int joined = 0;
    for (EnumFacing f : EnumFacing.HORIZONTALS) {
      boolean j = joins(world, pos, f);
      sides[f.getHorizontalIndex()] = j ? Side.PANEL : Side.NONE;
      if (j) {
        joined++;
      }
    }
    if (joined == 0) {
      // Alone: across the way it was placed, an end post at each edge.
      boolean alongX = state.getValue(ALONG_X);
      EnumFacing a = alongX ? EnumFacing.EAST : EnumFacing.NORTH;
      sides[a.getHorizontalIndex()] = Side.END;
      sides[a.getOpposite().getHorizontalIndex()] = Side.END;
    } else {
      // A run's last block: the panel goes on to the edge and ends there.
      for (EnumFacing f : EnumFacing.HORIZONTALS) {
        if (sides[f.getHorizontalIndex()] == Side.NONE
            && sides[f.getOpposite().getHorizontalIndex()] == Side.PANEL
            && sides[f.rotateY().getHorizontalIndex()] == Side.NONE
            && sides[f.rotateYCCW().getHorizontalIndex()] == Side.NONE) {
          sides[f.getHorizontalIndex()] = Side.END;
        }
      }
    }
    IBlockState above = world.getBlockState(pos.up());
    return state.withProperty(NORTH, sides[EnumFacing.NORTH.getHorizontalIndex()])
        .withProperty(EAST, sides[EnumFacing.EAST.getHorizontalIndex()])
        .withProperty(SOUTH, sides[EnumFacing.SOUTH.getHorizontalIndex()])
        .withProperty(WEST, sides[EnumFacing.WEST.getHorizontalIndex()])
        .withProperty(UP, height >= 16 && above.getBlock() instanceof BlockCubiclePanel);
  }

  /** Whether the panel runs on past {@code side}: into another panel, or into a solid wall. */
  private boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos other = pos.offset(side);
    IBlockState state = world.getBlockState(other);
    if (state.getBlock() instanceof BlockCubiclePanel) {
      return true;
    }
    return state.getBlockFaceShape(world, other, side.getOpposite()) == BlockFaceShape.SOLID;
  }

  /**
   * Right-click a face of the panel with an empty hand to hang a shelf on it; again to take it
   * down. A shelf hangs only on a face along which the panel runs.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking() || !player.getHeldItem(hand).isEmpty()
        || side.getAxis() == EnumFacing.Axis.Y) {
      return false;
    }
    IBlockState actual = getActualState(state, world, pos);
    // The face clicked must be a broad face: the panel runs across it.
    Side left = actual.getValue(property(side.rotateY()));
    Side right = actual.getValue(property(side.rotateYCCW()));
    if (left == Side.NONE && right == Side.NONE) {
      return false;
    }
    if (!world.isRemote) {
      Shelf shelf = Shelf.of(side);
      boolean remove = state.getValue(SHELF) == shelf;
      world.setBlockState(pos, state.withProperty(SHELF, remove ? Shelf.NONE : shelf), 3);
      world.playSound(null, pos, remove ? SoundEvents.BLOCK_WOOD_BREAK
          : SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.8F, 1.1F);
    }
    return true;
  }

  private static PropertyEnum<Side> property(EnumFacing side) {
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

  /** The panel's box: its arms' extent, the shelf's depth on its side, up to its height. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    IBlockState a = getActualState(state, source, pos);
    double x0 = 0.5 - HALF;
    double x1 = 0.5 + HALF;
    double z0 = 0.5 - HALF;
    double z1 = 0.5 + HALF;
    if (a.getValue(WEST) != Side.NONE) {
      x0 = 0;
    }
    if (a.getValue(EAST) != Side.NONE) {
      x1 = 1;
    }
    if (a.getValue(NORTH) != Side.NONE) {
      z0 = 0;
    }
    if (a.getValue(SOUTH) != Side.NONE) {
      z1 = 1;
    }
    switch (a.getValue(SHELF)) {
      case NORTH:
        z0 = Math.min(z0, 0.5 - HALF - SHELF_DEPTH);
        break;
      case SOUTH:
        z1 = Math.max(z1, 0.5 + HALF + SHELF_DEPTH);
        break;
      case EAST:
        x1 = Math.max(x1, 0.5 + HALF + SHELF_DEPTH);
        break;
      case WEST:
        x0 = Math.min(x0, 0.5 - HALF - SHELF_DEPTH);
        break;
      default:
        break;
    }
    return new AxisAlignedBB(x0, 0, z0, x1, height / 16.0, z1);
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
