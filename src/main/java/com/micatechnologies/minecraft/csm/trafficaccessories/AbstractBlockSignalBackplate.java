package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import javax.annotation.Nonnull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Abstract base class for traffic signal backplate blocks. A single computed
 * {@link #MODEL_VARIANT} property exposes the adjacent signal's tilt and horizontal-orientation
 * together, so each of the ten (tilt &times; horizontal) pairings can map to its own model in
 * the blockstate JSON. The variant is derived per render frame from the
 * {@link TileEntityTrafficSignalHead} in the block behind this backplate; no tile entity or
 * metadata storage is required on the backplate itself.
 */
public abstract class AbstractBlockSignalBackplate extends AbstractBlockRotatableNSEWUD
    implements ICsmNoSnowAccumulation, ICsmTileEntityProvider {

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySignalBackplate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitysignalbackplate";
  }

  @Override
  public TileEntity createNewTileEntity(net.minecraft.world.World worldIn, int meta) {
    return new TileEntitySignalBackplate();
  }

  /**
   * The combined tilt + horizontal-orientation property used for model selection. Forge v1
   * blockstates can only branch model selection on one property at a time, so tilt and
   * horizontal are collapsed into this single enum.
   */
  public static final PropertyEnum<BackplateModelVariant> MODEL_VARIANT =
      PropertyEnum.create("modelvariant", BackplateModelVariant.class);


  public AbstractBlockSignalBackplate(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
        lightLevel, lightOpacity, false);
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(MODEL_VARIANT, BackplateModelVariant.V_NONE));
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MODEL_VARIANT);
  }

  /**
   * Nothing is drawn as chunk geometry; the plate is drawn entirely by
   * {@code TileEntitySignalBackplateRenderer}.
   *
   * <p>The alternative was a derived "is it shifted" property that swapped in an empty model, so
   * that an unmoved plate could stay in the cheap chunk batch. It does not survive contact with
   * the actual blockstates: four families across two dialects, twenty of which enumerate all one
   * hundred and twenty property combinations by hand and would have doubled, and a fitted subclass
   * that builds its own state container the property would have broken. One render path for every
   * plate is both less code and less to get wrong, and it is the path that can be shifted, tilted
   * or rotated freely later.
   *
   * <p>The item model is unaffected -- that comes from the blockstate's {@code inventory} variant,
   * which is not block rendering.
   */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.INVISIBLE;
  }

  /**
   * Computes the actual model variant by reading the signal head behind this backplate.
   * "Behind" is the block one step in the opposite direction of {@link #FACING}, with a
   * forward fallback since the backplate can sit in front of the signal in some configurations.
   */
  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    TrafficSignalBodyTilt tilt = TrafficSignalBodyTilt.NONE;
    boolean horizontal = false;

    BlockPos signalPos = null;
    TileEntity te = worldIn.getTileEntity(pos.offset(facing.getOpposite()));
    if (te instanceof TileEntityTrafficSignalHead) {
      signalPos = pos.offset(facing.getOpposite());
      tilt = ((TileEntityTrafficSignalHead) te).getBodyTilt();
    } else {
      te = worldIn.getTileEntity(pos.offset(facing));
      if (te instanceof TileEntityTrafficSignalHead) {
        signalPos = pos.offset(facing);
        tilt = ((TileEntityTrafficSignalHead) te).getBodyTilt();
      }
    }

    if (signalPos != null) {
      IBlockState signalState = worldIn.getBlockState(signalPos);
      if (signalState.getBlock() instanceof AbstractBlockControllableSignalHead) {
        horizontal = ((AbstractBlockControllableSignalHead) signalState.getBlock())
            .isHorizontal(worldIn, signalPos);
      }
    }

    return state.withProperty(MODEL_VARIANT, BackplateModelVariant.of(tilt, horizontal));
  }

  /**
   * The tilt of the head at a position, or {@link TrafficSignalBodyTilt#NONE} if there is none.
   *
   * <p>Read straight from the head's tile entity rather than inferred from this plate's model
   * variant, so the plate turns by what the head actually did.
   *
   * @param world     the world to read.
   * @param signalPos the head's position.
   *
   * @return the head's body tilt, never null.
   */
  public static TrafficSignalBodyTilt tiltOf(IBlockAccess world, BlockPos signalPos) {
    final TileEntity te = world.getTileEntity(signalPos);
    return te instanceof TileEntityTrafficSignalHead
        ? ((TileEntityTrafficSignalHead) te).getBodyTilt()
        : TrafficSignalBodyTilt.NONE;
  }

  /**
   * How far a span wire has moved the head at this position, in model units, or zero.
   *
   * <p>All three axes. A plate is bolted to the back of its head and has to go wherever the head
   * went, and a span moves a head sideways as well as up -- following only the rise would leave the
   * plate behind by however far the head slid under its clamp.
   *
   * <p>Read from the head's own tile entity, which is the same cached value its renderer uses, so
   * the plate moves exactly when the head has actually moved rather than whenever it merely happens
   * to hang near a span.
   *
   * @param world     the world to read.
   * @param signalPos the head's position.
   *
   * @return the head's span offset in model units, never null.
   */
  public static Vec3d spanOffsetOf(IBlockAccess world, BlockPos signalPos) {
    final TileEntity te = world.getTileEntity(signalPos);
    return te instanceof TileEntityTrafficSignalHead
        ? ((TileEntityTrafficSignalHead) te).getSpanWireOffset()
        : Vec3d.ZERO;
  }

  /**
   * Just the rise from {@link #spanOffsetOf}, for callers that only shift vertically.
   *
   * @param world     the world to read.
   * @param signalPos the head's position.
   *
   * @return the vertical part of the head's span offset, in model units.
   */
  public static float spanRiseOf(IBlockAccess world, BlockPos signalPos) {
    final TileEntity te = world.getTileEntity(signalPos);
    return te instanceof TileEntityTrafficSignalHead
        ? ((TileEntityTrafficSignalHead) te).getSpanWireYOffset()
        : 0.0f;
  }

  /**
   * The head a plate at this position belongs to, or null -- the same search
   * {@link #getActualState} makes, exposed so the head's renderer can ask the question from the
   * other end.
   *
   * <p>Behind first, then in front: a plate normally sits behind its head, but some configurations
   * put it in front, and the original lookup has always allowed both.
   */
  public static BlockPos findSignalFor(IBlockAccess world, BlockPos platePos, EnumFacing facing) {
    if (world.getTileEntity(platePos.offset(facing.getOpposite()))
        instanceof TileEntityTrafficSignalHead) {
      return platePos.offset(facing.getOpposite());
    }
    if (world.getTileEntity(platePos.offset(facing)) instanceof TileEntityTrafficSignalHead) {
      return platePos.offset(facing);
    }
    return null;
  }

  /**
   * Given a block position, checks if the block there is a backplate and, if so, searches
   * along its facing axis for an adjacent signal or controllable signal block. Returns the
   * position of the signal found, or {@code null} if the block is not a backplate or no
   * signal is adjacent.
   *
   * @param world the world
   * @param pos   the position of the potential backplate block
   * @return the BlockPos of the adjacent signal, or null
   */
  public static BlockPos findSignalBehind(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (!(block instanceof AbstractBlockSignalBackplate)) {
      return null;
    }
    EnumFacing facing = state.getValue(FACING);
    EnumFacing[] horizontalDirs = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST};
    for (EnumFacing checkDir : horizontalDirs) {
      if (checkDir.getAxis() != facing.getAxis()) continue;
      BlockPos signalPos = pos.offset(checkDir);
      TileEntity te = world.getTileEntity(signalPos);
      if (te instanceof com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead) {
        return signalPos;
      }
    }
    return null;
  }

}
