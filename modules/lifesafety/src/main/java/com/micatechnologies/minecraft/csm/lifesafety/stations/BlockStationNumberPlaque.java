package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A station number plaque. Click to count the units digit up, sneak-click the tens: any number
 * from 0 to 99. The number lives on a tile entity, since a hundred of them do not fit in
 * metadata; the model reads its two digits as {@link #TENS} and {@link #ONES}, actual state only,
 * each of which swaps one digit texture in the blockstate.
 *
 * @since 2026.9
 */
public class BlockStationNumberPlaque extends BlockFireProtectionProp implements
    ICsmTileEntityProvider {

  public static final PropertyInteger TENS = PropertyInteger.create("tens", 0, 9);
  public static final PropertyInteger ONES = PropertyInteger.create("ones", 0, 9);

  public BlockStationNumberPlaque(String registryName, int[] box) {
    super(registryName, box, false);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(TENS, 0).withProperty(ONES, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, TENS, ONES);
  }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    TileEntity te = world.getTileEntity(pos);
    int n = te instanceof TileEntityStationNumber ? ((TileEntityStationNumber) te).getNumber() : 1;
    return actual.withProperty(TENS, n / 10).withProperty(ONES, n % 10);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityStationNumber) {
        TileEntityStationNumber plaque = (TileEntityStationNumber) te;
        int n = plaque.getNumber();
        int tens = n / 10;
        int ones = n % 10;
        if (player.isSneaking()) {
          tens = (tens + 1) % 10;
        } else {
          ones = (ones + 1) % 10;
        }
        plaque.setNumber(tens * 10 + ones);
        plaque.markDirtySync(world, pos, state, true);
      }
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStationNumber.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystationnumber";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStationNumber();
  }
}
