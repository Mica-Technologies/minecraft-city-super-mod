package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import net.minecraft.tileentity.TileEntity;
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
 * A fireplace with its mantelpiece, two blocks wide, against a wall. A right-click with flint
 * and steel lights it (using the flint and steel as lighting anything does); an empty hand lights
 * it or puts it out. Lit ({@link #LIT}, stored in both blocks), the logs burn -- an animated
 * flame in the blockstate, embers glowing under them -- it gives a fire's light, sends up flames
 * and a little smoke, and now and then crackles, quietly. The fire is drawn and never real: it
 * sets nothing alight and hurts no one. Redstone lights it too: power coming to either block
 * lights it and power going puts it out, so a linked light switch works it; which it was last is
 * kept by a {@link TileEntityPowerMemory} in part 0.
 *
 * <p>{@link #LIT} is the top bit of the metadata, over {@link WidePieces#PART} and the facing.
 * Where the fire is in the model, facing north across both blocks, is the generator's
 * ({@code gen_furniture_living.py}, {@code FIREBOX}).</p>
 *
 * @since 2026.9
 */
public class BlockFireplace extends BlockResidentialWide
    implements ICsmTileEntityProvider, ISwitchable {

  /** Whether the fire is burning. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  /** The fire's bed across both blocks, facing north, in sixteenths. */
  private static final double FIRE_X0 = 12;
  private static final double FIRE_X1 = 20;
  private static final double FIRE_Z0 = 11.5;
  private static final double FIRE_Z1 = 13.5;
  private static final double FIRE_Y = 2.5;

  /**
   * Constructs a fireplace.
   *
   * @param registryName its registry name, ending in its mantel's finish
   * @param box          its box facing north across both blocks, in sixteenths
   */
  public BlockFireplace(String registryName, int[] box) {
    super(registryName, box, Material.ROCK, SoundType.STONE, 2.0F);
    setDefaultState(getDefaultState().withProperty(LIT, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, WidePieces.PART, LIT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 7).withProperty(LIT, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(LIT) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? 13 : 0;
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

  /** Lights or puts out both blocks. */
  private void setLit(World world, BlockPos pos, IBlockState state, boolean lit) {
    world.setBlockState(pos, state.withProperty(LIT, lit), 3);
    BlockPos other = WidePieces.otherCell(pos, state);
    if (WidePieces.isOtherCell(world, other, state)) {
      world.setBlockState(other, world.getBlockState(other).withProperty(LIT, lit), 3);
    }
  }

  /**
   * Flames and a wisp of smoke from this block's half of the fire, and, from part 0 only, now
   * and then the fire's crackle. The flames the eye follows are the animated texture; these
   * only lift off it.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    if (!state.getValue(LIT)) {
      return;
    }
    int part = state.getValue(WidePieces.PART);
    double lo = Math.max(FIRE_X0, part * 16) - part * 16;
    double hi = Math.min(FIRE_X1, part * 16 + 16) - part * 16;
    EnumFacing facing = state.getValue(FACING);
    for (int i = 0; i < 2; i++) {
      double mx = lo + rand.nextDouble() * (hi - lo);
      double mz = FIRE_Z0 + rand.nextDouble() * (FIRE_Z1 - FIRE_Z0);
      double[] w = toWorld(facing, mx, mz);
      double y = pos.getY() + (FIRE_Y + rand.nextDouble() * 1.5) / 16.0;
      world.spawnParticle(EnumParticleTypes.FLAME, pos.getX() + w[0], y, pos.getZ() + w[1],
          0.0, 0.004, 0.0);
      if (i == 0 && rand.nextInt(3) == 0) {
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + w[0], y + 0.25,
            pos.getZ() + w[1], 0.0, 0.01, 0.0);
      }
    }
    if (part == 0 && rand.nextInt(4) == 0) {
      SoundEvent crackle = FurnishingsSounds.FIREPLACE_CRACKLE.getSoundEvent();
      if (crackle != null) {
        world.playSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, crackle,
            SoundCategory.BLOCKS, 0.35F, 0.9F + rand.nextFloat() * 0.2F, false);
      }
    }
  }

  /** A point in the model, facing north, in sixteenths, turned to the block: {x, z} in blocks. */
  static double[] toWorld(EnumFacing facing, double mx, double mz) {
    double x;
    double z;
    switch (facing) {
      case EAST:
        x = 16 - mz;
        z = mx;
        break;
      case SOUTH:
        x = 16 - mx;
        z = 16 - mz;
        break;
      case WEST:
        x = mz;
        z = 16 - mx;
        break;
      default:
        x = mx;
        z = mz;
        break;
    }
    return new double[]{x / 16.0, z / 16.0};
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  /** A change of redstone power to either block lights the fire or puts it out. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
      return;
    }
    BlockPos other = WidePieces.otherCell(pos, state);
    BlockPos main = state.getValue(WidePieces.PART) == 0 ? pos : other;
    TileEntity te = world.getTileEntity(main);
    if (!(te instanceof TileEntityPowerMemory)) {
      return;
    }
    TileEntityPowerMemory memory = (TileEntityPowerMemory) te;
    boolean powered = world.isBlockPowered(pos) || world.isBlockPowered(other);
    if (powered != memory.wasPowered()) {
      memory.setPowered(powered);
      if (powered != state.getValue(LIT)) {
        setLit(world, pos, state, powered);
      }
    }
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return state.getValue(WidePieces.PART) == 0;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPowerMemory.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitypowermemory";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return (meta & 4) == 0 ? new TileEntityPowerMemory() : null;
  }
}
