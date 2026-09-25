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
 * A station name panel on the wall, porcelain enamel white on navy. A click steps through the
 * invented names in {@link #NAMES}, a sneaking click back ({@link PlatformSigns}); the
 * blockstate draws a texture per name. The names are the generator's ({@code STATION_NAMES} in
 * {@code gen_transit_platforms.py}), in the same order, and none is a real station's.
 *
 * @since 2026.9
 */
public class BlockStationNameSign extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** What each name reads, in the order the sign steps through them. */
  public static final String[] NAMES = {"ALDER PARK", "CIVIC SQUARE", "FOUNDRY ROW",
      "HARBOR LIGHTS", "KESTREL HILL", "LANTERN QUAY", "MILLSTONE", "ORCHARD END",
      "SAXTON CROSS", "WILLOW BEND"};

  /** Which name the sign shows, counting from 1. */
  public static final PropertyInteger NAME = PropertyInteger.create("name", 1, NAMES.length);

  /**
   * Constructs a station name sign.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockStationNameSign(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(NAME, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, NAME);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NAME, PlatformSigns.valueAt(world, pos, NAMES.length));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !world.isRemote) {
      PlatformSigns.step(world, pos, player, NAMES.length, "csm.transit.station.name", NAMES);
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
