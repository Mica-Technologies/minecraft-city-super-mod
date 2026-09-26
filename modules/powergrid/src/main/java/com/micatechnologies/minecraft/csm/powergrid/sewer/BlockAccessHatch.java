package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An aluminium double-leaf access hatch set flush in the ground over a wet well or a valve
 * vault: its top is the block's top, so it is placed in the ground, in the hole the slab has for
 * it. Each block is one leaf, hinged at the model's west edge inside a concrete collar with the
 * aluminium frame on its top; the item places both leaves, the second beside the first to the
 * model's east facing the other way, so they meet over the middle of the opening.
 *
 * <p>A click opens both leaves ({@link #OPEN}, stored above the facing) and the next shuts them.
 * Open, each leaf stands on its hinge; the valve vault's opening is then open to fall into, and
 * the wet well's safety grate stays shut under the leaves, to be walked on. Breaking either leaf
 * takes both and drops one hatch. Eight states; nothing ticks.</p>
 *
 * @since 2026.9
 */
public class BlockAccessHatch extends AbstractBlockRotatableNSEW {

  /** Whether the leaves are open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  /** Set while one leaf takes its pair with it, so the pair does not take it again. */
  private static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  private static final double C = 1.5 / 16;
  /** Open, facing north: the collar's three walls and the leaf standing on its hinge. */
  private static final AxisAlignedBB[] OPEN_BOXES = {
      new AxisAlignedBB(0, 0, 0, C, 1, 1), new AxisAlignedBB(C, 0, 0, 1, 1, C),
      new AxisAlignedBB(C, 0, 1 - C, 1, 1, 1), new AxisAlignedBB(0, 1, C, 0.07, 1.5, 1 - C)};
  /** The wet well's safety grate, under the leaves. */
  private static final AxisAlignedBB GRATE = new AxisAlignedBB(C, 14.4 / 16, C, 1, 14.9 / 16,
      1 - C);

  private final String registryName;
  private final boolean grate;

  /**
   * @param registryName its registry name
   * @param grate        whether a safety grate stays shut under the open leaves (a wet well)
   */
  public BlockAccessHatch(String registryName, boolean grate) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0, true);
    this.registryName = registryName;
    this.grate = grate;
    PENDING.remove();
    setDefaultState(getDefaultState().withProperty(OPEN, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, false);
  }

  /** Where the other leaf of the leaf at {@code pos} is: the model's east. */
  private static BlockPos pairOf(BlockPos pos, IBlockState state) {
    return pos.offset(state.getValue(FACING).rotateY());
  }

  /** The other leaf's state, or null if it is not there. */
  @Nullable
  private IBlockState pair(IBlockAccess world, BlockPos pos, IBlockState state) {
    IBlockState other = world.getBlockState(pairOf(pos, state));
    return other.getBlock() == this
        && other.getValue(FACING) == state.getValue(FACING).getOpposite() ? other : null;
  }

  /** Places the second leaf, or refuses the hatch if its cell is taken. */
  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    BlockPos other = pairOf(pos, state);
    if (!world.getBlockState(other).getBlock().isReplaceable(world, other)) {
      DEMOLISHING.set(true);
      try {
        world.setBlockToAir(pos);
      } finally {
        DEMOLISHING.set(false);
      }
      if (placer instanceof EntityPlayer) {
        EntityPlayer player = (EntityPlayer) placer;
        if (!player.capabilities.isCreativeMode) {
          player.inventory.addItemStackToInventory(new ItemStack(this));
        }
        player.sendStatusMessage(new TextComponentTranslation("csm.utilities.hatch.blocked",
            other.getX(), other.getY(), other.getZ()), true);
      }
      return;
    }
    world.setBlockState(other, getDefaultState().withProperty(FACING,
        state.getValue(FACING).getOpposite()), 3);
  }

  /** Takes the other leaf with it; only the leaf broken drops the hatch. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get() && pair(world, pos, state) != null) {
      DEMOLISHING.set(true);
      try {
        world.setBlockToAir(pairOf(pos, state));
      } finally {
        DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      IBlockState other = pair(world, pos, state);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      if (other != null) {
        world.setBlockState(pairOf(pos, state), other.withProperty(OPEN, open), 3);
      }
      world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN
          : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.8F, open ? 0.8F : 0.9F);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    if (!state.getValue(OPEN)) {
      addCollisionBoxToList(pos, entityBox, boxes, FULL_BLOCK_AABB);
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB b : OPEN_BOXES) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(b, facing));
    }
    if (grate) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(GRATE, facing));
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.UP && !state.getValue(OPEN) ? BlockFaceShape.SOLID
        : BlockFaceShape.UNDEFINED;
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

  /** Cutout: the safety grate. */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
