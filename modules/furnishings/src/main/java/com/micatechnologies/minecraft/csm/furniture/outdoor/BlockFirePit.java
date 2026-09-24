package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.ISwitchable;
import com.micatechnologies.minecraft.csm.furniture.residential.LampSwitching;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A backyard fire pit, a ring of stone or a steel bowl with logs in it, lit and put out as the
 * fireplace is: flint and steel lights it, an empty hand lights it or puts it out, and a change of
 * redstone power does either ({@link LampSwitching}), so a light switch works it. Lit
 * ({@link #LIT}), the animated flames and glowing embers are swapped into its model, it gives a
 * fire's light, sends up flames and smoke, and now and then crackles, quietly. Like the
 * fireplace's, the fire is only drawn: it sets nothing alight and hurts no one.
 *
 * <p>{@link #LIT} is stored in the bit above the facing, {@link LampSwitching#POWERED} in the top
 * bit.</p>
 *
 * @since 2026.9
 */
public class BlockFirePit extends BlockResidentialFurniture implements ISwitchable {

  /** Whether the fire is burning. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  private final int lightLevel;
  /** The height of the fire's bed above the block's floor, in sixteenths. */
  private final double fireY;

  /**
   * Constructs a fire pit.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param metal        whether it is a steel bowl rather than a ring of stone
   * @param fireY        the height of the fire's bed, in sixteenths
   * @param lightLevel   the light it gives lit
   */
  public BlockFirePit(String registryName, int[] box, boolean metal, double fireY,
      int lightLevel) {
    super(registryName, box, metal ? Material.IRON : Material.ROCK,
        metal ? SoundType.METAL : SoundType.STONE, 2.0F);
    this.lightLevel = lightLevel;
    this.fireY = fireY;
    setDefaultState(getDefaultState().withProperty(LIT, false)
        .withProperty(LampSwitching.POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIT, LampSwitching.POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(LIT, (meta & 4) != 0)
        .withProperty(LampSwitching.POWERED, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(LIT) ? 4 : 0)
        | (state.getValue(LampSwitching.POWERED) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, false).withProperty(LampSwitching.POWERED, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? lightLevel : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    ItemStack held = player.getHeldItem(hand);
    boolean lit = state.getValue(LIT);
    boolean flint = held.getItem() == Items.FLINT_AND_STEEL;
    if (!flint && !held.isEmpty()) {
      return false;
    }
    if (flint && lit) {
      return true;
    }
    if (!world.isRemote) {
      if (flint) {
        held.damageItem(1, player);
      }
      setLit(world, pos, state, !lit);
      SoundEvent sound = lit ? SoundEvents.BLOCK_FIRE_EXTINGUISH
          : SoundEvents.ITEM_FLINTANDSTEEL_USE;
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, lit ? 0.4F : 0.8F,
          lit ? 1.6F : 1.0F);
    }
    return true;
  }

  /**
   * Lights the fire or puts it out.
   *
   * @param world the world
   * @param pos   the block clicked or powered
   * @param state its state
   * @param lit   whether it is to burn
   */
  protected void setLit(World world, BlockPos pos, IBlockState state, boolean lit) {
    world.setBlockState(pos, state.withProperty(LIT, lit), 3);
  }

  /** A change of redstone power lights it or puts it out ({@link LampSwitching}). */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(LampSwitching.POWERED)) {
      IBlockState next = state.withProperty(LampSwitching.POWERED, powered);
      world.setBlockState(pos, next, 3);
      if (powered != state.getValue(LIT)) {
        setLit(world, pos, next, powered);
      }
    }
  }

  /** Flames and smoke over the logs, and now and then the crackle. */
  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    if (!state.getValue(LIT)) {
      return;
    }
    for (int i = 0; i < 3; i++) {
      double x = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.3;
      double z = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.3;
      double y = pos.getY() + (fireY + 1 + rand.nextDouble() * 4) / 16.0;
      world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
      if (i == 0) {
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 0.4, z, 0.0, 0.03, 0.0);
      }
    }
    if (rand.nextInt(3) == 0) {
      SoundEvent crackle = FurnishingsSounds.FIREPLACE_CRACKLE.getSoundEvent();
      if (crackle != null) {
        world.playSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, crackle,
            SoundCategory.BLOCKS, 0.4F, 0.85F + rand.nextFloat() * 0.25F, false);
      }
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
