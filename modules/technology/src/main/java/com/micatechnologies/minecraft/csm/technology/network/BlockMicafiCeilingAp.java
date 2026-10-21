package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.technology.network.MicafiLight.Ring;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A Micafi ceiling access point: a round white disc on the ceiling (facing down) or, set on a
 * floor or a table, facing up. Only those two ways: a disc needs no turn about its own axis, and
 * the facing comes from the face clicked, never from where the placer looks, so placing one
 * while flying cannot stand it on its edge. A side clicked in its upper half hangs it from the
 * ceiling, in its lower half sets it on the floor, as a slab is placed.
 *
 * <p>Right-click changes the ring ({@link MicafiLight.Ring}): blue, white, off, adopting. The
 * facing is one bit of metadata and the ring two; the blockstate swaps the ring texture, so
 * nothing ticks and there is no tile entity.</p>
 *
 * @since 2026.10
 */
public class BlockMicafiCeilingAp extends AbstractBlock {

  /** Up (standing on a floor) or down (hung under a ceiling). */
  public static final PropertyDirection FACING = PropertyDirection.create("facing",
      EnumFacing.Plane.VERTICAL);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boxUp;
  private final AxisAlignedBB boxDown;

  /**
   * @param registryName the block's registry name
   * @param box          the device's box facing up (standing on the floor), in sixteenths: x0, y0,
   *                     z0, x1, y1, z1; hung from a ceiling it is mirrored top to bottom
   */
  public BlockMicafiCeilingAp(String registryName, double[] box) {
    super(pending(registryName), SoundType.METAL, "pickaxe", 1, 1F, 10F, 0F, 0);
    this.registryName = registryName;
    this.boxUp = new AxisAlignedBB(box[0] / 16, box[1] / 16, box[2] / 16, box[3] / 16,
        box[4] / 16, box[5] / 16);
    this.boxDown = new AxisAlignedBB(box[0] / 16, 1 - box[4] / 16, box[2] / 16, box[3] / 16,
        1 - box[1] / 16, box[5] / 16);
    PENDING_REGISTRY_NAME.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN)
        .withProperty(MicafiLight.RING, Ring.BLUE));
  }

  private static Material pending(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, MicafiLight.RING);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(FACING, (meta & 1) == 0 ? EnumFacing.DOWN : EnumFacing.UP)
        .withProperty(MicafiLight.RING, MicafiLight.fromBits(MicafiLight.RING, meta >> 1));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(FACING) == EnumFacing.UP ? 1 : 0)
        | (state.getValue(MicafiLight.RING).ordinal() << 1);
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    boolean ceiling = facing == EnumFacing.DOWN
        || (facing != EnumFacing.UP && hitY > 0.5F);
    return getDefaultState().withProperty(FACING, ceiling ? EnumFacing.DOWN : EnumFacing.UP);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, state.withProperty(MicafiLight.RING,
          MicafiLight.next(state.getValue(MicafiLight.RING))), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 1.2F);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(FACING) == EnumFacing.UP ? boxUp : boxDown;
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
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  /** Cutout: the round face is a square picture with its corners cut away. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
