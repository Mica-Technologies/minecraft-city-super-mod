package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The wrench that configures a guardrail in place.
 *
 * <p>Two gestures, no screen, which is how every other roadside block in this mod is configured —
 * the mount kit and the barricades both cycle on a click, and a guardrail having its own window
 * would be the odd one out.</p>
 *
 * <ul>
 *   <li><b>Right-click</b> — takes the post under this length of rail away, or puts it back. Real
 *   posts stand about every other cell rather than at every one.</li>
 *   <li><b>Sneak + right-click</b> — steps through the four rail blocks: metal post, wooden post,
 *   and the double-sided pair of each.</li>
 * </ul>
 *
 * <p>The second gesture SWAPS THE BLOCK, because post material and sidedness are separate blocks
 * rather than state — a guardrail run is long, and a tile entity per cell would put thousands of
 * them along a highway. Swapping carries the facing and the post across, which is the only thing
 * that makes the split invisible to whoever is building with it.</p>
 *
 * @version 1.0
 * @see BlockGuardrail
 * @since 2026.9
 */
public class ItemGuardrailTool extends AbstractItem {

  /**
   * Each rail block and the one that follows it in the cycle.
   *
   * <p>A ring rather than two independent toggles: one gesture covering both material and
   * sidedness beats inventing a third click for a tool that is meant to stay simple. Order is
   * metal, wooden, metal double, wooden double, and round again.</p>
   *
   * @since 1.0
   */
  private static final Map<String, String> NEXT_VARIANT = new LinkedHashMap<>();

  static {
    // Each rail type cycles within its OWN family. Stepping from a W-beam to a thrie beam would
    // change the rail rather than an option on it, and would break the run either side of the
    // block the moment it happened — the rails no longer match.
    ring("w_beam_guardrail", "w_beam_guardrail_wood",
        "w_beam_guardrail_double", "w_beam_guardrail_wood_double");
    ring("thrie_beam_guardrail", "thrie_beam_guardrail_wood",
        "thrie_beam_guardrail_double", "thrie_beam_guardrail_wood_double");
    // Neither of these is ever built on a wooden post, so their ring is just the sidedness.
    ring("box_beam_guardrail", "box_beam_guardrail_double");
    ring("cable_barrier", "cable_barrier_double");
  }

  /** Links each name to the next and the last back to the first. */
  private static void ring(String... names) {
    for (int i = 0; i < names.length; i++) {
      NEXT_VARIANT.put(names[i], names[(i + 1) % names.length]);
    }
  }

  @Override
  public String getItemRegistryName() {
    return "guardrail_tool";
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
      EnumFacing facing, float hitX, float hitY, float hitZ) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockGuardrail)) {
      return EnumActionResult.PASS;
    }
    if (world.isRemote) {
      // Answer the hand on the client so the arm swings, but change nothing: the server owns the
      // world and will send the result back.
      return EnumActionResult.SUCCESS;
    }

    if (player.isSneaking()) {
      return cycleVariant(player, world, pos, state);
    }
    return togglePost(player, world, pos, state);
  }

  /** Takes the post away or puts it back, leaving everything else alone. */
  private EnumActionResult togglePost(EntityPlayer player, World world, BlockPos pos,
      IBlockState state) {
    boolean post = !state.getValue(BlockGuardrail.POST);
    world.setBlockState(pos, state.withProperty(BlockGuardrail.POST, post), 3);
    say(player, post ? "Post added" : "Post removed");
    return EnumActionResult.SUCCESS;
  }

  /** Swaps this rail for the next in the ring, carrying its facing and post across. */
  private EnumActionResult cycleVariant(EntityPlayer player, World world, BlockPos pos,
      IBlockState state) {
    BlockGuardrail current = (BlockGuardrail) state.getBlock();
    Block next = lookup(NEXT_VARIANT.get(current.getBlockRegistryName()));
    if (!(next instanceof BlockGuardrail)) {
      return EnumActionResult.PASS;
    }
    IBlockState swapped = next.getDefaultState()
        .withProperty(BlockGuardrail.FACING, state.getValue(BlockGuardrail.FACING))
        .withProperty(BlockGuardrail.POST, state.getValue(BlockGuardrail.POST));
    world.setBlockState(pos, swapped, 3);
    say(player, next.getLocalizedName());
    return EnumActionResult.SUCCESS;
  }

  @Nullable
  private static Block lookup(@Nullable String registryName) {
    if (registryName == null) {
      return null;
    }
    return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("csm", registryName));
  }

  private static void say(EntityPlayer player, String message) {
    player.sendStatusMessage(new TextComponentString(message), true);
  }
}
