package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.furniture.market.BlockDisplayCase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * The pawn counter: a glass counter case that buys whatever a player hands it at the server's
 * prices ({@link PawnValuation}), through its screen ({@link GuiPawnCounter}). Server-run, so
 * only those with {@value PawnPermissions#NODE_ADMIN} place or break one: it pays from nothing,
 * and a player who could put one down would have a money tap wherever they liked. Counters side
 * by side join as display cases do; a sneaking click with an empty hand or redstone switches
 * their lights.
 *
 * @since 2026.10
 */
public class BlockPawnCounter extends BlockDisplayCase {

  /** The pawn screen. */
  public static final int GUI_PAWN = 60;
  /** The resale screen. */
  public static final int GUI_RESALE = 61;

  public BlockPawnCounter(String registryName, int[] box) {
    super(registryName, box);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
    if (!world.isRemote) {
      open(player, pos, GUI_PAWN);
    }
    return true;
  }

  /** Opens one of the counter's screens, server side. */
  static void open(EntityPlayer player, BlockPos pos, int gui) {
    player.openGui(Csm.instance, gui, player.world, pos.getX(), pos.getY(), pos.getZ());
  }

  /** Placed by someone without the node: taken back up, the item returned. */
  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    PawnBlocks.placed(world, pos, this, placer);
  }

  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    return PawnBlocks.mayBreak(world, player)
        && super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  @Override
  @SuppressWarnings("deprecation")
  public float getPlayerRelativeBlockHardness(@Nonnull IBlockState state,
      @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
    return PawnBlocks.mayDig(player)
        ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
  }

  @Override
  public float getExplosionResistance(World world, BlockPos pos, @Nullable Entity exploder,
      Explosion explosion) {
    return PawnBlocks.RESISTANCE;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPawnCounter.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitypawncounter";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPawnCounter();
  }
}
