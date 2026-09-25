package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
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
 * The hanging platform number sign: PLATFORM over a big number, on both faces. A click steps
 * the number from 1 to {@link #NUMBERS} and round again, a sneaking click steps it down
 * ({@link PlatformSigns}); the blockstate draws a texture per number.
 *
 * @since 2026.9
 */
public class BlockPlatformNumberSign extends BlockPlatformFixture
    implements ICsmTileEntityProvider {

  /** How many numbers a sign can show. */
  public static final int NUMBERS = 20;

  /** The number the sign shows. */
  public static final PropertyInteger NUMBER = PropertyInteger.create("number", 1, NUMBERS);

  /**
   * Constructs a number sign.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPlatformNumberSign(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(NUMBER, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, NUMBER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NUMBER, PlatformSigns.valueAt(world, pos, NUMBERS));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !world.isRemote) {
      PlatformSigns.step(world, pos, player, NUMBERS, "csm.transit.platform.number", null);
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformSign.class;
  }

  @Override
  public String getTileEntityName() {
    return PlatformSigns.TILE_ENTITY_NAME;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformSign();
  }
}
