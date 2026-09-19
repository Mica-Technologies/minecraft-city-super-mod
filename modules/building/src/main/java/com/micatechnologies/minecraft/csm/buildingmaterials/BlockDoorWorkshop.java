package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.constructionsite.BuildingGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The Door Workshop: where custom doors are made. Three blocks go in -- the frame, the upper panel
 * and the lower panel -- and a door that is made of them comes out, with the movement, sound,
 * redstone mode, sensor, speed and auto-close set on the workshop's screen. A custom door put in
 * its edit slot can be re-programmed without being remade.
 *
 * <p>The block itself is an ordinary cube; its {@link TileEntityDoorWorkshop} holds the slots
 * and is never ticked or drawn.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockDoorWorkshop extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /**
   * Constructs a {@link BlockDoorWorkshop}.
   *
   * @since 1.0
   */
  public BlockDoorWorkshop() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 2.5F, 5F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  @Override
  public String getBlockRegistryName() {
    return "door_workshop";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /** The tool board faces whoever put it down. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (playerIn.isSneaking() && !playerIn.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!worldIn.isRemote && worldIn.getTileEntity(pos) instanceof TileEntityDoorWorkshop) {
      playerIn.openGui(Csm.instance, BuildingGuiProvider.DOOR_WORKSHOP_GUI_ID, worldIn,
          pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  /** Whatever is in the slots is dropped with the workshop. */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (!worldIn.isRemote && te instanceof TileEntityDoorWorkshop) {
      TileEntityDoorWorkshop workshop = (TileEntityDoorWorkshop) te;
      for (int i = 0; i < workshop.getInventory().getSlots(); i++) {
        ItemStack stack = workshop.getInventory().getStackInSlot(i);
        if (!stack.isEmpty()) {
          spawnAsEntity(worldIn, pos, stack);
        }
      }
    }
    super.breakBlock(worldIn, pos, state);
  }

  // --- tile entity: the slots and the screen's settings -------------------------------------------

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityDoorWorkshop();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDoorWorkshop.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydoorworkshop";
  }

  // --- shape --------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
