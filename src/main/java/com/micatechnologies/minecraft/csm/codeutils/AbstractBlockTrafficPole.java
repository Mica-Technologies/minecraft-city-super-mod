package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEWUD;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator1;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator2;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator3;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator4;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignMount;
import com.micatechnologies.minecraft.csm.trafficsignals.AbstractBlockControllableCrosswalkSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedBaseMount;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedLeftMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedRearMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedRightMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkLeftMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkLeftMount90Deg;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMount90Deg;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMountGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkSignalDouble;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkSignalSingle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkRightMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkRightMount90Deg;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter1;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter2;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockBlankoutBox;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBox;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockSnowBlock;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlockRotatableNSEWUD} with the addition of traffic pole mounting properties
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @see AbstractBlockRotatableNSEWUD
 */
public abstract class AbstractBlockTrafficPole extends AbstractBlockRotatableNSEWUD {

  /**
   * Enumerated type representing the color of the traffic pole.
   */
  public enum TRAFFIC_POLE_COLOR implements IStringSerializable {
    BLACK,
    SILVER,
    TAN,
    WHITE,
    UNPAINTED;

    @Override
    public String getName() {
      return this.toString().toLowerCase();
    }
  }

  /**
   * The east mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_EAST = PropertyBool.create("mounteast");

  /**
   * The west mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_WEST = PropertyBool.create("mountwest");

  /**
   * The up mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_UP = PropertyBool.create("mountup");

  /**
   * The down mounting property.
   *
   * @since 1.0
   */
  public static final PropertyBool MOUNT_DOWN = PropertyBool.create("mountdown");

  /**
   * The list of global ignore blocks. Includes CSM accessory mount blocks that should never cause
   * a pole to mount, plus vanilla natural/decorative blocks whose geometry would not align with a
   * pole's mounting visuals (snow layers, plants, rails, wires, torches, carpets, etc.). Subclass
   * relationships are honored: for example, {@link BlockBush} covers flowers, saplings, tall
   * grass, dead bushes, mushrooms, lily pads, double plants, and crops.
   */
  public static final Class<?>[] IGNORE_BLOCK =
      {BlockControllableCrosswalkDoubleWordedBaseMount.class,
          BlockControllableCrosswalkDoubleWordedLeftMount.class,
          BlockControllableCrosswalkDoubleWordedRightMount.class,
          BlockControllableCrosswalkDoubleWordedRearMount.class,
          BlockControllableCrosswalkLeftMount.class,
          BlockControllableCrosswalkLeftMount90Deg.class, BlockControllableCrosswalkMount.class,
          BlockControllableCrosswalkMount90Deg.class, BlockControllableCrosswalkRightMount.class,
          BlockControllableCrosswalkRightMount90Deg.class, BlockControllableCrosswalkTweeter1.class,
          BlockControllableCrosswalkTweeter2.class,
          BlockTrafficSignalFatigueMitigator1.class, BlockTrafficSignalFatigueMitigator2.class,
          BlockTrafficSignalFatigueMitigator3.class, BlockTrafficSignalFatigueMitigator4.class,
          BlockTrafficLightSensorBox.class, BlockTrafficStreetNameSignMount.class,
          BlockTrafficAccessoryNSEWUD.class, BlockControllableCrosswalkMountGray.class,
          BlockControllableCrosswalkSignalSingle.class, BlockControllableCrosswalkSignalDouble.class,
          AbstractBlockControllableCrosswalkSignalNew.class,
          // Vehicle-signal heads (BlockControllableSignal, BlockControllableHawkSignal, etc.)
          // have their own built-in mount hardware plus the Pelco-style standalone mount
          // blocks, so poles should never auto-sprout a stub into a signal head.
          AbstractBlockControllableSignalHead.class,
          BlockSnow.class, BlockSnowBlock.class, BlockBush.class, BlockLeaves.class,
          BlockVine.class, BlockCarpet.class, BlockTorch.class, BlockRedstoneWire.class,
          BlockRailBase.class, BlockCactus.class, BlockReed.class, BlockWeb.class,
          BlockBlankoutBox.class,
          com.micatechnologies.minecraft.csm.trafficaccessories.BlockLaneControlSignal.class};


  /**
   * Cached combined ignore-block array (global + subclass-specific), computed lazily on first use
   * to avoid array allocation on every getActualState() call.
   */
  private volatile Class<?>[] cachedCombinedIgnoreBlock;

  /**
   * Constructs an {@link AbstractBlockTrafficPole} instance.
   *
   * @since 1.0
   */
  public AbstractBlockTrafficPole() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * Creates a new {@link BlockStateContainer} for the block with the required property for
   * rotation.
   *
   * @return a new {@link BlockStateContainer} for the block
   *
   * @see Block#createBlockState()
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN);
  }

  /**
   * Updates the block state based on the presence of adjacent blocks, considering the block's
   * facing direction.
   * <p>
   * This method is called to retrieve the actual state of the block, taking into account its
   * environment. It checks for the presence of blocks in each direction (east, west, north, south,
   * up, down) relative to the block's facing direction. The method uses helper methods to determine
   * if there are blocks adjacent to the block and updates the block state with properties
   * indicating the presence of these blocks.
   * </p>
   *
   * @param state   The current block state.
   * @param worldIn The world in which the block is located.
   * @param pos     The position of the block in the world.
   *
   * @return The updated block state with properties indicating the presence of adjacent blocks.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);

    if (cachedCombinedIgnoreBlock == null) {
      cachedCombinedIgnoreBlock = buildCombinedIgnoreBlock(getIgnoreBlock());
    }
    Class<?>[] ignoreBlock = cachedCombinedIgnoreBlock;

    // Check for blocks in each direction relative to the block's facing direction.
    // Uses the precomputed rotation lookup table (BlockUtils.getRelativeFacingOpposite) so
    // the 4 per-frame switch evaluations + 4 getOpposite() calls collapse to 4 array reads.
    boolean isBlockToEast = isMountableAdjacent(worldIn,
        pos.offset(BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.EAST)),
        ignoreBlock);
    boolean isBlockToWest = isMountableAdjacent(worldIn,
        pos.offset(BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.WEST)),
        ignoreBlock);
    boolean isBlockAbove = isMountableAdjacent(worldIn,
        pos.offset(BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.UP)), ignoreBlock);
    boolean isBlockBelow = isMountableAdjacent(worldIn,
        pos.offset(BlockUtils.getRelativeFacingOpposite(facing, EnumFacing.DOWN)),
        ignoreBlock);

    // Update the block state with the presence of blocks in each direction
    return state.withProperty(MOUNT_EAST, isBlockToEast).withProperty(MOUNT_WEST, isBlockToWest)
        .withProperty(MOUNT_UP, isBlockAbove).withProperty(MOUNT_DOWN, isBlockBelow);
  }

  /**
   * Builds a combined ignore-block array by merging the global {@link #IGNORE_BLOCK} list with
   * the subclass-specific list returned by {@link #getIgnoreBlock()}. This is used by both
   * {@link AbstractBlockTrafficPole} and {@link AbstractBlockTrafficPoleDiagonal} to avoid
   * duplicating the merge logic.
   *
   * @param blockSpecificIgnore the subclass-specific ignore list (may be {@code null})
   *
   * @return the combined ignore-block array
   */
  protected static Class<?>[] buildCombinedIgnoreBlock(Class<?>[] blockSpecificIgnore) {
    if (blockSpecificIgnore == null) {
      return IGNORE_BLOCK;
    }
    Class<?>[] combined = new Class<?>[IGNORE_BLOCK.length + blockSpecificIgnore.length];
    System.arraycopy(IGNORE_BLOCK, 0, combined, 0, IGNORE_BLOCK.length);
    System.arraycopy(blockSpecificIgnore, 0, combined, IGNORE_BLOCK.length,
        blockSpecificIgnore.length);
    return combined;
  }

  /**
   * Checks whether a pole should render a mount/connector for the block at {@code pos}. This
   * applies two filters on top of the vanilla "is something solid here" test: the compiled
   * class-based ignore list ({@link #IGNORE_BLOCK} + subclass additions) and the user-managed,
   * config-driven registry-name ignore set
   * ({@link CsmConfig#getTrafficPoleIgnoreBlockIds()}). The config set is read by reference on
   * every call, so edits made through {@code /csm poleignore} or {@code /csm reloadconfig} take
   * effect on the next block update with no explicit cache invalidation required here.
   *
   * @param worldIn     the world/block access
   * @param pos         the adjacent position to test
   * @param ignoreBlock the combined class-based ignore array
   *
   * @return {@code true} if the pole should render a connector toward {@code pos}
   */
  protected static boolean isMountableAdjacent(IBlockAccess worldIn, BlockPos pos,
      Class<?>[] ignoreBlock) {
    ResourceLocation registryName = worldIn.getBlockState(pos).getBlock().getRegistryName();
    if (registryName != null
        && CsmConfig.getTrafficPoleIgnoreBlockIds().contains(registryName)) {
      return false;
    }
    return BlockUtils.getIsBlockToSide(worldIn, pos, ignoreBlock);
  }

  /**
   * Abstract method which must be implemented to return the block classes of blocks which should be
   * ignored when checking for adjacent blocks.
   *
   * @return Array of block classes to ignore when checking for adjacent blocks.
   */
  public abstract Class<?>[] getIgnoreBlock();

  public abstract TRAFFIC_POLE_COLOR getTrafficPoleColor();
}
