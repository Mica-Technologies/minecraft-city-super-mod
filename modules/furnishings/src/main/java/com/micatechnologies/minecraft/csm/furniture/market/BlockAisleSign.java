package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * A hanging aisle sign: a panel on two rods from the ceiling, the aisle's number on both faces.
 * Right-click steps the number up, 1 to {@link #NUMBERS} and round again; a sneaking click steps
 * it down. The number is kept by a {@link TileEntityAisleSign} and read by
 * {@code getActualState} ({@link #NUMBER}); the blockstate swaps the face's texture for it.
 *
 * @since 2026.9
 */
public class BlockAisleSign extends BlockResidentialFurniture implements ICsmTileEntityProvider {

  /** How many numbers a sign can show. */
  public static final int NUMBERS = 16;

  /** The number the sign shows. */
  public static final PropertyInteger NUMBER = PropertyInteger.create("number", 1, NUMBERS);

  /**
   * Constructs an aisle sign.
   *
   * @param registryName its registry name, ending in its colour
   * @param box          its box facing north, in sixteenths
   */
  public BlockAisleSign(String registryName, int[] box) {
    super(registryName, box, FixtureMaterial.PLASTIC.getMaterial(),
        FixtureMaterial.PLASTIC.getSound(), FixtureMaterial.PLASTIC.getHardness());
    setDefaultState(getDefaultState().withProperty(NUMBER, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, NUMBER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return super.getActualState(state, world, pos).withProperty(NUMBER, numberAt(world, pos));
  }

  /**
   * The number of the sign at {@code pos}: 1 if its tile entity is not there. Safe off the main
   * thread: a chunk being rendered is read without creating a tile entity in it.
   *
   * @param world the world
   * @param pos   the sign
   *
   * @return its number
   */
  public static int numberAt(IBlockAccess world, BlockPos pos) {
    TileEntity te = world instanceof ChunkCache
        ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
        : world.getTileEntity(pos);
    return te instanceof TileEntityAisleSign ? ((TileEntityAisleSign) te).getNumber() : 1;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityAisleSign) {
        TileEntityAisleSign sign = (TileEntityAisleSign) te;
        int step = player.isSneaking() ? NUMBERS - 1 : 1;
        sign.setNumber((sign.getNumber() - 1 + step) % NUMBERS + 1);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
            0.3F, 0.8F);
      }
    }
    return true;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityAisleSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityaislesign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityAisleSign();
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
