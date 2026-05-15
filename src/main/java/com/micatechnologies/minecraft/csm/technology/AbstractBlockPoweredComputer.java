package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Base class for the powered-computer family (iMac, iMac Pro, MacBook Pro). Adds a
 * {@link #POWERED} boolean property that drives a screen on/off texture swap and a small
 * light contribution. Player interaction:
 * <ul>
 *   <li><b>Sneak + right-click</b> — toggle the computer on or off without opening the GUI</li>
 *   <li><b>Right-click while off</b> — turn the computer on (booting up; second click opens the
 *       gag desktop GUI)</li>
 *   <li><b>Right-click while on</b> — open the gag desktop GUI</li>
 * </ul>
 *
 * <p>Existing world placements made before this block became functional decode cleanly because
 * the metadata encoding ({@code facing | (powered ? 8 : 0)}) is a strict superset of the old
 * factory's facing-only encoding — old metas 0–5 still resolve to the correct facing with
 * powered set to false (the cosmetic default).</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public abstract class AbstractBlockPoweredComputer extends AbstractBlock
    implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockDirectional.FACING;
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** GUI handler ID shared across every computer block. */
  public static final int GUI_ID = 15;

  protected AbstractBlockPoweredComputer(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, 0F,
        lightOpacity);
    setDefaultState(blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false));
  }

  /** Light value emitted when the screen is lit, on a 0–15 scale. */
  protected int getPoweredLightLevel() {
    return 9;
  }

  // region State ↔ meta

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int facingVal = meta & 7;
    if (facingVal > 5) {
      facingVal = 0;
    }
    boolean poweredVal = (meta & 8) != 0;
    return getDefaultState()
        .withProperty(FACING, EnumFacing.byIndex(facingVal))
        .withProperty(POWERED, poweredVal);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex() | (state.getValue(POWERED) ? 8 : 0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState()
        .withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer))
        .withProperty(POWERED, false);
  }

  // endregion

  // region Bounding box (with facing-aware rotation)

  @Override
  @Nonnull
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState actualState = source.getBlockState(pos).getActualState(source, pos);
    if (actualState.getProperties().containsKey(FACING)) {
      return RotationUtils.rotateBoundingBoxByFacing(
          getBlockBoundingBox(actualState, source, pos),
          actualState.getValue(FACING));
    }
    return SQUARE_BOUNDING_BOX;
  }

  // endregion

  // region Visual / behavior overrides

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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState actual = state.getActualState(world, pos);
    return actual.getValue(POWERED) ? getPoweredLightLevel() : 0;
  }

  // endregion

  // region Interaction

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    boolean powered = state.getValue(POWERED);

    if (player.isSneaking()) {
      // Sneak toggles power without opening the GUI.
      if (!worldIn.isRemote) {
        worldIn.setBlockState(pos, state.withProperty(POWERED, !powered), 3);
      }
      return true;
    }

    if (!powered) {
      // First click on a powered-off computer: turn the screen on. The next click
      // (no longer powered=false) opens the GUI — gives the player a "boot up" beat
      // before the desktop appears.
      if (!worldIn.isRemote) {
        worldIn.setBlockState(pos, state.withProperty(POWERED, true), 3);
      }
      return true;
    }

    // Powered and not sneaking → open the desktop GUI.
    if (!worldIn.isRemote) {
      player.openGui(Csm.instance, GUI_ID, worldIn, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  // The base AbstractBlock removes the tile entity in breakBlock() automatically.

  // endregion

  // region Tile entity wiring

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityComputer.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycomputer";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityComputer();
  }

  // endregion
}
