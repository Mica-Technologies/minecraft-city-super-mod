package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyEnum;
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
 * A hanging department sign: a panel two metres wide on two rods from the ceiling, centred on its
 * block, naming a department on both faces (PRODUCE, BAKERY, DELI, ... CUSTOMER SERVICE, each in
 * its own colour with its pictograms). Right-click steps to the next department and round again;
 * a sneaking click steps back. The department is kept by a {@link TileEntityDepartmentSign} and
 * read by {@code getActualState} ({@link #DEPARTMENT}); the blockstate picks the sign's band of
 * the shared sign sheets for it, as the aisle sign swaps its number.
 *
 * @since 2026.9
 */
public class BlockDepartmentSign extends BlockResidentialFurniture
    implements ICsmTileEntityProvider {

  /** The department the sign names. */
  public static final PropertyEnum<StoreDepartment> DEPARTMENT =
      PropertyEnum.create("department", StoreDepartment.class);

  /**
   * Constructs a department sign.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockDepartmentSign(String registryName, int[] box) {
    super(registryName, box, FixtureMaterial.PLASTIC.getMaterial(),
        FixtureMaterial.PLASTIC.getSound(), FixtureMaterial.PLASTIC.getHardness());
    setDefaultState(getDefaultState().withProperty(DEPARTMENT, StoreDepartment.PRODUCE));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, DEPARTMENT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return super.getActualState(state, world, pos).withProperty(DEPARTMENT,
        departmentAt(world, pos));
  }

  /**
   * The department of the sign at {@code pos}: produce if its tile entity is not there. Safe off
   * the main thread: a chunk being rendered is read without creating a tile entity in it.
   *
   * @param world the world
   * @param pos   the sign
   *
   * @return its department
   */
  public static StoreDepartment departmentAt(IBlockAccess world, BlockPos pos) {
    TileEntity te = world instanceof ChunkCache
        ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
        : world.getTileEntity(pos);
    return te instanceof TileEntityDepartmentSign
        ? ((TileEntityDepartmentSign) te).getDepartment() : StoreDepartment.PRODUCE;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityDepartmentSign) {
        TileEntityDepartmentSign sign = (TileEntityDepartmentSign) te;
        StoreDepartment[] all = StoreDepartment.values();
        int step = player.isSneaking() ? all.length - 1 : 1;
        sign.setDepartment(all[(sign.getDepartment().ordinal() + step) % all.length]);
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
    return TileEntityDepartmentSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydepartmentsign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityDepartmentSign();
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
