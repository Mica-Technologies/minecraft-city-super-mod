package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.parks.ParksSounds;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.TreeFelling;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The chainsaw: fells a whole tree from one cut, at the price of fuel and a little patience.
 *
 * <p>Its engine (fuel, pull start, idling, stalling) is {@link ItemFuelledTool}'s, shared with the
 * stump grinder. Running, it mines wood very fast. Cutting a log fells the tree standing on it at
 * once: this module's trees by {@link TreeFelling}, anyone else's by {@link AnyTreeFelling}; sneak
 * to cut only that log. Fuel and wear are paid per log felled, and the felling leaves a few brush
 * piles around the stump ({@link BrushPiles}).</p>
 *
 * @since 2026.10
 */
public class ItemChainsaw extends ItemFuelledTool {

  /** How fast it cuts wood while running. */
  private static final float WOOD_SPEED = 20.0F;
  /** How fast it cuts leaves while running. */
  private static final float LEAVES_SPEED = 8.0F;

  public ItemChainsaw() {
    super(1200);
  }

  @Override
  public String getItemRegistryName() {
    return "chainsaw";
  }

  @Override
  protected String getMessagePrefix() {
    return "csm.parks.chainsaw";
  }

  @Override
  protected String getUseTooltipKey() {
    return "csm.parks.chainsaw.tooltip.fell";
  }

  @Override
  protected ParksSounds getStartSound() {
    return ParksSounds.CHAINSAW_START;
  }

  @Override
  protected ParksSounds getIdleSound() {
    return ParksSounds.CHAINSAW_IDLE;
  }

  // --- cutting ---

  @Override
  public float getDestroySpeed(@Nonnull ItemStack stack, IBlockState state) {
    if (!isRunning(stack)) {
      return 1.0F;
    }
    Material m = state.getMaterial();
    if (m == Material.WOOD || state.getBlock() instanceof BlockLog) {
      return WOOD_SPEED;
    }
    if (m == Material.LEAVES || m == Material.PLANTS || m == Material.VINE) {
      return LEAVES_SPEED;
    }
    return 1.0F;
  }

  @Override
  public int getHarvestLevel(ItemStack stack, @Nonnull String toolClass,
      @Nullable EntityPlayer player, @Nullable IBlockState blockState) {
    return isRunning(stack) && "axe".equals(toolClass) ? 3 : -1;
  }

  @Override
  @Nonnull
  public Set<String> getToolClasses(ItemStack stack) {
    return isRunning(stack) ? Collections.singleton("axe") : Collections.emptySet();
  }

  /**
   * A running saw cutting a log (the break event has already passed): the log is cut here, the
   * tree on it felled, and fuel, wear and brush piles paid for every log. Returning {@code true}
   * keeps the game from breaking the log a second time. Sneaking, or anything that is not a log,
   * goes the ordinary way.
   */
  @Override
  public boolean onBlockStartBreak(@Nonnull ItemStack stack, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player) {
    World world = player.world;
    if (world.isRemote || !isRunning(stack) || player.isSneaking()) {
      return false;
    }
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    boolean ours = block instanceof BlockTreeLog;
    if (!ours && !AnyTrees.isLog(world, pos, state)) {
      return false;
    }
    boolean creative = player.capabilities.isCreativeMode;
    TileEntity te = world.getTileEntity(pos);
    world.playEvent(player, 2001, pos, Block.getStateId(state));
    block.onBlockHarvested(world, pos, state, player);
    if (!world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3)) {
      return true;
    }
    block.onPlayerDestroy(world, pos, state);
    if (!creative) {
      block.harvestBlock(world, player, pos, state, te, stack.copy());
    }
    int felled;
    if (ours) {
      felled = TreeFelling.fell(world, pos, player).size();
    } else {
      felled = AnyTrees.fell(world, pos, player);
    }
    int logs = felled + 1;
    play(world, player, ParksSounds.CHAINSAW_CUT, 1.0F);
    if (felled > 0) {
      BrushPiles.place(world, player, pos, logs);
    }
    payForLogs(stack, player, logs);
    return true;
  }

  @Override
  public boolean onBlockDestroyed(@Nonnull ItemStack stack, @Nonnull World world,
      @Nonnull IBlockState state, @Nonnull BlockPos pos, @Nonnull EntityLivingBase entity) {
    if (!world.isRemote && isRunning(stack) && state.getBlockHardness(world, pos) > 0.0F) {
      stack.damageItem(1, entity);
    }
    return true;
  }
}
