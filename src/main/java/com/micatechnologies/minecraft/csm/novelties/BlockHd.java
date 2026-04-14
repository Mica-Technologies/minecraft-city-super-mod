package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.CsmSounds;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Interactive hand dryer block that plays a drying sound and spawns cloud particles for a timed
 * duration when right-clicked.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class BlockHd extends AbstractBlockRotatableNSEWUD {

  private static final long DRYER_DURATION_MS = 16000;

  @SideOnly(Side.CLIENT)
  private static Map<BlockPos, Long> activeDryers;

  @SideOnly(Side.CLIENT)
  private static Map<BlockPos, Long> getActiveDryers() {
    if (activeDryers == null) {
      activeDryers = new HashMap<>();
    }
    return activeDryers;
  }

  public BlockHd() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "hd";
  }

    /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB(0.000000, 0.000000, 0.250000, 1.000000, 0.500000, 1.000000);
    }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      SoundEvent sound = CsmSounds.SOUND.HANDDRYER.getSoundEvent();
      if (sound != null) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
    }
    if (world.isRemote) {
      getActiveDryers().put(pos.toImmutable(), System.currentTimeMillis());
    }
    return true;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    Map<BlockPos, Long> dryers = getActiveDryers();
    Long activatedAt = dryers.get(pos);
    if (activatedAt != null) {
      long elapsed = System.currentTimeMillis() - activatedAt;
      if (elapsed < DRYER_DURATION_MS) {
        double offsetX = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double offsetY = pos.getY() + 0.15 + rand.nextDouble() * 0.15;
        double offsetZ = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        world.spawnParticle(EnumParticleTypes.CLOUD, offsetX, offsetY, offsetZ,
            0.0, -0.03, 0.0);
      } else {
        dryers.remove(pos);
      }
    }
    // Periodically clean up stale entries from broken/unloaded dryers
    if (rand.nextInt(100) == 0) {
      long now = System.currentTimeMillis();
      dryers.values().removeIf(t -> now - t > DRYER_DURATION_MS + 5000);
    }
  }
}
