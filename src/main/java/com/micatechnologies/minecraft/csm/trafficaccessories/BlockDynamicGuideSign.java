package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDynamicGuideSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  public BlockDynamicGuideSign() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "dynamic_guide_sign";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDynamicGuideSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydynamicguidesign";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityDynamicGuideSign();
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    player.openGui(Csm.instance, 14, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // Thin slab matching the visible sign panel, in WORLD orientation -- already correct for
    // the block's facing, not a canonical box waiting to be rotated. See getBoundingBox below,
    // which stops the base class rotating it a second time.
    //
    // The panel is modeled at z = 14.5..16 and rotated by 0/90/180/270 degrees for
    // SOUTH/WEST/NORTH/EAST, landing it on the south/east/north/west face respectively. The
    // east/west pair being crossed is not a typo: the panel sits on the side OPPOSITE the
    // direction it reads toward, which a west-facing sign viewed from directly overhead shows
    // plainly -- it sits hard against the east edge of its own block.
    EnumFacing facing = state.getValue(BlockHorizontal.FACING);
    final double t = 1.5 / 16.0;
    switch (facing) {
      case SOUTH:
        return new AxisAlignedBB(0.0, 0.0, 1.0 - t, 1.0, 1.0, 1.0);
      case NORTH:
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, t);
      case WEST:
        return new AxisAlignedBB(1.0 - t, 0.0, 0.0, 1.0, 1.0, 1.0);
      case EAST:
        return new AxisAlignedBB(0.0, 0.0, 0.0, t, 1.0, 1.0);
      default:
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }
  }

  /**
   * Takes the box above as final instead of rotating it.
   *
   * <p>{@link AbstractBlockRotatableNSEW#getBoundingBox} rotates whatever
   * {@code getBlockBoundingBox} returns by the block's facing, which is the right service for a
   * block that describes itself once in a canonical orientation. This one does not: it knows
   * where the TESR put its panel for each facing and says so directly, so going through that
   * rotation turned the box a second time and landed it on the wrong face.
   *
   * <p>The measured result before this override: the box came out on the north face for
   * <em>all four</em> facings, so only a north-facing sign was ever right. That is invisible in
   * a screenshot -- the block still has a hitbox, still inside itself -- and the earlier attempt
   * to fix it by turning the switch above another 90 degrees was compensating for the second
   * rotation rather than removing it, which is why the mapping kept looking wrong.
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return getBlockBoundingBox(state, source, pos);
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * The sign itself has no redstone behavior, but its lighting can be wired: a sign whose
   * light mode is REDSTONE lights while the block is powered. The power state is cached on
   * the tile entity (and synced) rather than kept in block meta, which is full with FACING.
   */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    updatePoweredState(world, pos);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    updatePoweredState(world, pos);
  }

  private void updatePoweredState(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityDynamicGuideSign) {
      ((TileEntityDynamicGuideSign) tileEntity)
          .setPowered(world.getRedstonePowerFromNeighbors(pos) > 0);
    }
  }

  /** Redstone wire runs up to the sign so its lighting can be switched. */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing facing) {
    return true;
  }
}
