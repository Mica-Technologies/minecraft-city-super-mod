package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
 * A tiled platform column with a navy band round it showing PLATFORM and its number on all
 * four faces. It stacks with the other columns ({@link BlockPlatformColumn}) and steps its
 * number as the hanging number sign does ({@link PlatformSigns},
 * {@link BlockPlatformNumberSign#NUMBER}); the blockstate draws the band's model for the number.
 *
 * @since 2026.9
 */
public class BlockPlatformColumnNumber extends BlockPlatformColumn
    implements ICsmTileEntityProvider {

  /**
   * Constructs a number band column.
   *
   * @param registryName its registry name
   * @param box          its box, in sixteenths
   */
  public BlockPlatformColumnNumber(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(BlockPlatformNumberSign.NUMBER, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UP, DOWN, BlockPlatformNumberSign.NUMBER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return super.getActualState(state, world, pos).withProperty(BlockPlatformNumberSign.NUMBER,
        PlatformSigns.valueAt(world, pos, BlockPlatformNumberSign.NUMBERS));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !world.isRemote) {
      PlatformSigns.step(world, pos, player, BlockPlatformNumberSign.NUMBERS,
          "csm.transit.platform.number", null);
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
