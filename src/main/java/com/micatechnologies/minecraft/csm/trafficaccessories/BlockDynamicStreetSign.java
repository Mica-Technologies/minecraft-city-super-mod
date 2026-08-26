package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
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

/**
 * The player-configurable intersection street-name blade. Everything visible is drawn by
 * {@link TileEntityDynamicStreetSignRenderer} from the JSON document on the tile entity; the
 * block itself only carries the facing, the hitbox, and the redstone hook for switched
 * illumination.
 *
 * <p>GUI id 20. See {@code assets/docs/DYNAMIC_STREET_SIGN_SYSTEM.md}.
 */
public class BlockDynamicStreetSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** GUI id this block opens; kept here so the handler and the block cannot drift apart. */
  public static final int GUI_ID = 20;

  // Thickness of the flat-mount hitbox. Covers the whole assembly, not just the panel: the
  // renderer's frontmost layer (the extruded frame) stands a full sign pixel proud of the
  // painted face, and a hitbox that stopped at the face would miss a click on the frame.
  private static final double FLAT_THICKNESS = 2.5 / 16.0;
  // A hanging blade sits centered in the block's depth, so its hitbox is a centered slab
  // rather than one hugging a face. Wide enough for the frame on BOTH faces, since a hanging
  // blade is lettered and framed on its reverse too.
  private static final double HANGING_NEAR = 6.0 / 16.0;
  private static final double HANGING_FAR = 10.0 / 16.0;

  public BlockDynamicStreetSign() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "dynamic_street_sign";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDynamicStreetSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydynamicstreetsign";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityDynamicStreetSign();
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  /**
   * The hitbox follows the mount the player picked, because the two mounts put the panel in
   * completely different places: a flat blade is a thin slab against the face the TESR draws
   * on, while a hanging blade is a slab through the middle of the block readable from both
   * sides. Using one box for both left the hanging blade untargetable from one side.
   *
   * <p>Face mapping for the flat case is derived from the TESR's rotation, exactly as on the
   * dynamic guide sign: FACING SOUTH/WEST/NORTH/EAST puts the panel on the south/east/north/
   * west face.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    EnumFacing facing = state.getValue(BlockHorizontal.FACING);
    if (getMountType(source, pos) == StreetSignMount.HANGING) {
      switch (facing) {
        case SOUTH:
        case NORTH:
          return new AxisAlignedBB(0.0, 0.0, HANGING_NEAR, 1.0, 1.0, HANGING_FAR);
        case WEST:
        case EAST:
          return new AxisAlignedBB(HANGING_NEAR, 0.0, 0.0, HANGING_FAR, 1.0, 1.0);
        default:
          return FULL_BLOCK_AABB;
      }
    }
    final double t = FLAT_THICKNESS;
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
        return FULL_BLOCK_AABB;
    }
  }

  /**
   * The mount stored on this position's tile entity, or the default when the entity is not
   * loaded yet -- {@code getBlockBoundingBox} is called during chunk load before tile
   * entities are attached, so this must never assume one is there.
   */
  private static StreetSignMount getMountType(IBlockAccess source, BlockPos pos) {
    if (source == null) {
      return new StreetSignData().getMountType();
    }
    TileEntity tileEntity = source.getTileEntity(pos);
    if (tileEntity instanceof TileEntityDynamicStreetSign) {
      return ((TileEntityDynamicStreetSign) tileEntity).getSignData().getMountType();
    }
    return new StreetSignData().getMountType();
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
   * The blade has no redstone behavior of its own, but its internal illumination can be
   * wired: a sign whose light mode is REDSTONE lights while the block is powered. The power
   * state is cached on the tile entity (and synced) rather than kept in block meta, which is
   * full with FACING.
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
    if (tileEntity instanceof TileEntityDynamicStreetSign) {
      ((TileEntityDynamicStreetSign) tileEntity)
          .setPowered(world.getRedstonePowerFromNeighbors(pos) > 0);
    }
  }

  /** Redstone wire runs up to the blade so its illumination can be switched. */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing facing) {
    return true;
  }
}
