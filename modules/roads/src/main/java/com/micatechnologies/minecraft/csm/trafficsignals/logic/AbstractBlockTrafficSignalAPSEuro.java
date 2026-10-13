package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalAPSEuro;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * A European push button: a tall box on the pole with its symbols on the front and a yellow
 * request lamp under them that lights when pressed and goes out when the walk comes. Behaves as
 * every other push button does (linked as a pedestrian accessory, with locator, press and walk
 * sounds); see {@link TileEntityTrafficSignalAPSEuro} for the lamp.
 *
 * @since 2026.10
 */
public abstract class AbstractBlockTrafficSignalAPSEuro extends AbstractBlockTrafficSignalAPS {

  /** Whether the request lamp is lit: actual state, read from the tile entity. */
  public static final PropertyBool REQUESTED = PropertyBool.create("requested");

  public AbstractBlockTrafficSignalAPSEuro() {
    super(Material.ROCK);
  }

  /**
   * The housing as the model draws it, facing north against the block's south face; the numbers
   * are the ones {@code gen_euro_push_button.py} prints.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(5.675 / 16, 1.5 / 16, 12.475 / 16, 10.325 / 16, 13.35 / 16, 1);
  }

  /** The lamp is actual state only; what is stored always has it off. */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta).withProperty(REQUESTED, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    boolean result = super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY,
        hitZ);
    // Light the lamp on the press itself, not a tick later
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityTrafficSignalAPSEuro) {
      ((TileEntityTrafficSignalAPSEuro) te).updateRequestLamp();
    }
    return result;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficSignalAPSEuro.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytrafficsignalapseuro";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficSignalAPSEuro();
  }

  @Override
  protected @NotNull BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, COLOR, ARROW_ORIENTATION, REQUESTED);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state, @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    TileEntity te = BlockUtils.getTileEntitySafe(worldIn, pos);
    boolean lit = te instanceof TileEntityTrafficSignalAPSEuro
        && ((TileEntityTrafficSignalAPSEuro) te).isRequestLampLit();
    return super.getActualState(state, worldIn, pos).withProperty(REQUESTED, lit);
  }
}
