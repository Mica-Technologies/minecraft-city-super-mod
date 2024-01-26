package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZSixteen;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTESRProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlockControllableSignal extends AbstractBlockRotatableHZSixteen
    implements ICsmTESRProvider<TileEntityTrafficSignalHead> {

  public static final int SIGNAL_RED = 0;
  public static final int SIGNAL_YELLOW = 1;
  public static final int SIGNAL_GREEN = 2;
  public static final int SIGNAL_OFF = 3;
  public static final PropertyInteger COLOR = PropertyInteger.create("color", 0, 3);
  public static final PropertyEnum<TileEntityTrafficSignalHead.BODY_COLOR> BODY_COLOR =
      PropertyEnum.create("bodycolor", TileEntityTrafficSignalHead.BODY_COLOR.class);
  public static final PropertyEnum<TileEntityTrafficSignalHead.VISOR_TYPE> VISOR_TYPE =
      PropertyEnum.create("visortype", TileEntityTrafficSignalHead.VISOR_TYPE.class);


  public AbstractBlockControllableSignal(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  public static void changeSignalColor(World world, BlockPos blockPos, int signalColor) {
    IBlockState blockState = world.getBlockState(blockPos);
    if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
      world.setBlockState(blockPos, blockState.withProperty(COLOR, signalColor));
    } else {
      System.err.println(
          "Cannot set traffic signal color of a non-traffic signal block: " + blockState.getBlock()
              .getLocalizedName() + " at " + blockPos);
    }

    // Reset request count (if applicable)
    if (blockState.getBlock() instanceof AbstractBlockTrafficSignalRequester) {
      // Reset request count if entering/leaving walk phase (green is walk phase, yellow is end
      // of walk phase)
      if (signalColor == SIGNAL_GREEN || signalColor == SIGNAL_YELLOW) {
        AbstractBlockTrafficSignalRequester.resetRequestCount(world, blockPos);
      }
    }
  }

  public static AbstractBlockControllableSignal getSignalBlockInstanceOrNull(World world,
      BlockPos blockPos) {
    AbstractBlockControllableSignal returnVal = null;
    if (world != null && blockPos != null) {
      try {
        IBlockState blockState = world.getBlockState(blockPos);
        if (blockState.getBlock() instanceof AbstractBlockControllableSignal) {
          returnVal = (AbstractBlockControllableSignal) blockState.getBlock();
        }
      } catch (Exception e) {
        System.err.println(
            "Error getting signal block instance at " + blockPos + ": " + e.getMessage());
      }
    }

    // Log error if null
    if (returnVal == null) {
      System.err.println("Error getting signal block instance at " + blockPos);
    }

    return returnVal;
  }

  @Override
  public IBlockState getStateFromMeta(int meta) {
    int colorVal = meta % 4;
    int facingVal = (meta - colorVal) / 4;

    if (facingVal > 0) {
      DirectionSixteen directionSixteen = DirectionSixteen.fromIndex(facingVal);
      if (directionSixteen != null) {
        return getDefaultState().withProperty(FACING, directionSixteen)
            .withProperty(COLOR, colorVal);
      }
    }

    return getDefaultState().withProperty(COLOR, colorVal);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int colorVal = state.getValue(COLOR);
    return colorVal;
  }

  /**
   * Gets the {@link IBlockState} of the block to use for placement with the specified parameters.
   *
   * @param worldIn the world the block is being placed in
   * @param pos     the position the block is being place at
   * @param facing  the facing direction of the placement hit
   * @param hitX    the X coordinate of the placement hit
   * @param hitY    the Y coordinate of the placement hit
   * @param hitZ    the Z coordinate of the placement hit
   * @param meta    the meta value of the block state
   * @param placer  the placer of the block
   *
   * @return the {@link IBlockState} of the block to use for placement
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState stateForPlacement =
        super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead) {
      TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) tileEntity;
      return stateForPlacement.withProperty(COLOR, SIGNAL_OFF)
          .withProperty(BODY_COLOR, signalHead.getBodyColor())
          .withProperty(VISOR_TYPE, signalHead.getVisorType());
    } else {
      return stateForPlacement.withProperty(COLOR, SIGNAL_OFF);
    }
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, COLOR, BODY_COLOR, VISOR_TYPE);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead) {
      TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) tileEntity;
      return state.withProperty(BODY_COLOR, signalHead.getBodyColor())
          .withProperty(VISOR_TYPE, signalHead.getVisorType())
          .withProperty(FACING, signalHead.getFacing());
    }
    return state;
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return 15;
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
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
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
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * Gets a new tile entity for the block.
   *
   * @param worldIn the world
   * @param meta    the block metadata
   *
   * @return the new tile entity for the block
   *
   * @since 1.1
   */
  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficSignalHead();
  }

  /**
   * Gets the tile entity class for the block.
   *
   * @return the tile entity class for the block
   *
   * @since 1.0
   */
  public Class<TileEntityTrafficSignalHead> getTileEntityClass() {
    return TileEntityTrafficSignalHead.class;
  }

  /**
   * Gets the tile entity name for the block.
   *
   * @return the tile entity name for the block
   *
   * @since 1.0
   */
  public String getTileEntityName() {
    return "tileentitytrafficsignalhead";
  }

  /**
   * Gets the tile entity special renderer (TESR) class for the block.
   *
   * @return the TESR class for the block
   *
   * @since 1.0
   */
  @Override
  public TileEntitySpecialRenderer<TileEntityTrafficSignalHead> getNewTESR() {
    return new TrafficSignalTESR();
  }


  public abstract SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos);

  public abstract boolean doesFlash();

  public enum SIGNAL_SIDE {
    FLASHING_LEFT,
    FLASHING_RIGHT,
    LEFT,
    THROUGH,
    RIGHT,
    PEDESTRIAN,
    PEDESTRIAN_BEACON,
    PEDESTRIAN_ACCESSORY,
    PROTECTED,
    NA_SENSOR
  }
}