package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The activation mat for an automatic sliding door ({@link BlockAutoSlidingDoor}): a pixel-thin
 * rubber mat laid on the floor in front of the door. Someone stepping on it opens the sliding
 * door(s) beside it -- the four cells around it at its own height, and the cell above it -- and
 * each step pushes back the time the door shuts itself.
 *
 * <p>A mat is told it is being walked on through {@link #onEntityCollision}, the same way a
 * pressure plate is: the game's walking callback ({@code onEntityWalk}) goes to the block under
 * the walker's feet, which for a mat a pixel thick is the floor beneath it, never the mat. The
 * callback runs only while someone is on the mat, and does nothing more than once every
 * {@link #COOLDOWN} ticks a mat; the door's own single scheduled tick does the closing. No state,
 * no tile entity, nothing ticks.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockAutoDoorMat extends AbstractBlock {

  /** How often one mat acts while someone stands on it, in ticks. */
  private static final int COOLDOWN = 10;
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 1.0 / 16, 1);

  /** When each mat last acted, per world: the cooldown. */
  private static final Map<World, Map<BlockPos, Long>> LAST =
      Collections.synchronizedMap(new WeakHashMap<>());

  public BlockAutoDoorMat() {
    super(Material.CLOTH, SoundType.CLOTH, null, 0, 0.5F, 1F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "auto_door_mat";
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
    trigger(worldIn, pos, entityIn);
  }

  @Override
  public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
    trigger(worldIn, pos, entityIn);
  }

  private void trigger(World world, BlockPos pos, Entity entity) {
    if (world.isRemote || !BlockAutoSlidingDoor.opensFor(entity)
        || entity.posY > pos.getY() + 0.5) {
      return;
    }
    long now = world.getTotalWorldTime();
    Map<BlockPos, Long> last = LAST.computeIfAbsent(world, w -> new HashMap<>());
    Long then = last.get(pos);
    if (then != null && now - then < COOLDOWN && now >= then) {
      return;
    }
    last.put(pos.toImmutable(), now);
    for (EnumFacing side : EnumFacing.values()) {
      if (side == EnumFacing.DOWN) {
        continue;
      }
      BlockPos at = pos.offset(side);
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() instanceof BlockAutoSlidingDoor) {
        ((BlockAutoSlidingDoor) s.getBlock()).sensorOpen(world, at, pos);
      }
    }
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
