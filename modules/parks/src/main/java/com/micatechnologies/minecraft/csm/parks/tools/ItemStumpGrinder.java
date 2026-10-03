package com.micatechnologies.minecraft.csm.parks.tools;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.parks.ParksSounds;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The stump grinder: a hand-guided machine that grinds away the stump a felling leaves, and its
 * roots, down to mulch.
 *
 * <p>Its engine (fuel, pull start, idling, stalling) is the chainsaw's ({@link ItemFuelledTool}).
 * Running, it is held against a stump the way the chainsaw is held against a log: hold the left
 * button on it, about two seconds for a log's hardness of 2 (vanilla's and this module's). A log
 * still carrying a tree is refused with a hint to fell it first ({@link TreeToolEvents}).</p>
 *
 * <p>The grind takes the stump and its roots ({@link StumpGrinding}): logs only, never one above
 * the stump, none that another log still stands on. Nothing drops; where the stump stood, if the
 * cell is clear and the ground under it solid, a layer of this module's ground mulch is left.
 * Fuel and wear are paid per log, as the chainsaw pays them.</p>
 *
 * @since 2026.10
 */
public class ItemStumpGrinder extends ItemFuelledTool {

  /** The registry name of the mulch left where a stump stood. */
  static final String MULCH = "ground_mulch";
  /**
   * How fast it grinds a stump while running: a log of hardness 2 takes 2 x 30 / 1.5 = 40 ticks,
   * two seconds, of holding.
   */
  private static final float GRIND_SPEED = 1.5F;
  /** Of the logs ground at once, how many play their break effect. */
  private static final int EFFECTS = 12;
  /** The shortest gap between two grinding sounds for one player, in ticks. */
  private static final int GRIND_SOUND_GAP = 30;

  /** When each player's grinding sound last started, so clicking again does not stack them. */
  private static final Map<EntityPlayer, Long> GRIND_SOUND_AT = new WeakHashMap<>();

  public ItemStumpGrinder() {
    super(1000);
  }

  @Override
  public String getItemRegistryName() {
    return "stump_grinder";
  }

  @Override
  protected String getMessagePrefix() {
    return "csm.parks.grinder";
  }

  @Override
  protected String getUseTooltipKey() {
    return "csm.parks.grinder.tooltip.use";
  }

  @Override
  protected ParksSounds getStartSound() {
    return ParksSounds.STUMP_GRINDER_START;
  }

  @Override
  protected ParksSounds getIdleSound() {
    return ParksSounds.STUMP_GRINDER_IDLE;
  }

  @Override
  protected float getIdleVolume() {
    return 0.7F;
  }

  /** How the world reads to {@link StumpGrinding}: logs of any mod, this module's included. */
  static StumpGrinding.Cells cells(World world) {
    return p -> world.isBlockLoaded(p) && AnyTrees.isLog(world, p, world.getBlockState(p));
  }

  /**
   * How the world reads to {@link StumpGrinding} as ground: the natural soil a stump stands in.
   * Dirt, grass, sand, gravel, clay and the like, by material; never stone, wood or anything
   * built.
   */
  static StumpGrinding.Cells soil(World world) {
    return p -> {
      if (!world.isBlockLoaded(p)) {
        return false;
      }
      Material m = world.getBlockState(p).getMaterial();
      return m == Material.GROUND || m == Material.GRASS || m == Material.SAND
          || m == Material.CLAY;
    };
  }

  /** Tells the player the log is a standing tree, not a stump. */
  public void standing(EntityPlayer player) {
    player.sendStatusMessage(new TextComponentTranslation("csm.parks.grinder.standing"), true);
  }

  /**
   * Plays the grinding sound, unless this player's last one started too recently: called as the
   * button goes down on a stump, so the sound runs while it is held. Server side.
   */
  public void grindSound(World world, EntityPlayer player) {
    long now = world.getTotalWorldTime();
    Long last = GRIND_SOUND_AT.get(player);
    if (last != null && now - last >= 0 && now - last < GRIND_SOUND_GAP) {
      return;
    }
    GRIND_SOUND_AT.put(player, now);
    play(world, player, ParksSounds.STUMP_GRINDER_GRIND, 1.0F);
  }

  // --- grinding ---

  @Override
  public float getDestroySpeed(@Nonnull ItemStack stack, IBlockState state) {
    if (isRunning(stack)
        && (state.getMaterial() == Material.WOOD || state.getBlock() instanceof BlockLog)) {
      return GRIND_SPEED;
    }
    return 1.0F;
  }

  /**
   * A running grinder finishing on a stump (the break event has already passed): the stump and
   * its roots go here, with no drops, and mulch is left where the stump stood. A standing log is
   * not broken at all. Anything else goes the ordinary way.
   */
  @Override
  public boolean onBlockStartBreak(@Nonnull ItemStack stack, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player) {
    World world = player.world;
    if (!isRunning(stack)) {
      return false;
    }
    IBlockState state = world.getBlockState(pos);
    if (!AnyTrees.isLog(world, pos, state)) {
      return false;
    }
    StumpGrinding.Cells cells = cells(world);
    if (StumpGrinding.isStanding(cells, pos)) {
      return true;
    }
    if (world.isRemote) {
      return false;
    }
    StumpGrinding.Cells soil = soil(world);
    Set<BlockPos> logs = StumpGrinding.grind(cells, soil, pos);
    if (logs.isEmpty()) {
      // A log standing on a floor or a foundation, or too tall to be a stump: not ground.
      player.sendStatusMessage(new TextComponentTranslation("csm.parks.grinder.notstump"), true);
      return true;
    }
    int groundLevel = StumpGrinding.groundLevel(cells, soil, pos);
    int ground = 0;
    for (BlockPos p : logs) {
      if (!p.equals(pos) && !world.isBlockModifiable(player, p)) {
        continue;
      }
      IBlockState s = world.getBlockState(p);
      if (ground++ < EFFECTS) {
        world.playEvent(2001, p, Block.getStateId(s));
      }
      world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
    }
    grindSound(world, player);
    placeMulch(world, player, new BlockPos(pos.getX(), groundLevel + 1, pos.getZ()));
    payForLogs(stack, player, Math.max(1, ground));
    return true;
  }

  /** Leaves ground mulch where the stump stood, if the cell is clear and stands on solid ground. */
  private static void placeMulch(World world, EntityPlayer player, BlockPos pos) {
    Block mulch = CsmRegistry.getBlock(MULCH);
    if (mulch == null || !world.isAirBlock(pos) || !world.isBlockModifiable(player, pos)) {
      return;
    }
    BlockPos below = pos.down();
    IBlockState ground = world.getBlockState(below);
    if (!ground.isSideSolid(world, below, EnumFacing.UP) || !mulch.canPlaceBlockAt(world, pos)) {
      return;
    }
    world.setBlockState(pos, mulch.getDefaultState(), 3);
  }
}
