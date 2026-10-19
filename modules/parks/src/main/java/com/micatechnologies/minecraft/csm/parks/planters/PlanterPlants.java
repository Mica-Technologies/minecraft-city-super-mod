package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * What may be planted in a planter, and the item a plant comes back as.
 *
 * <p>Plantable:</p>
 * <ul>
 *   <li>CSM Parks' plantings: every {@link BlockParkProp} of kind {@code SHRUB}, {@code PLANT}
 *       or {@code CACTUS} (the shrubs, flowers, grasses, flower beds, small plants, cacti and
 *       succulents, the young saguaro), except the nursery-potted ones;</li>
 *   <li>a nursery-potted plant ({@code potted_*}): the plant in it is planted, tipped out of its
 *       pot, and a survival player keeps the pot (a vanilla flower pot, what the Fabricator
 *       charges for it);</li>
 *   <li>vanilla's cactus, and any {@link BlockBush}: flowers, saplings, tall grass and fern, the
 *       dead bush, mushrooms, and other mods' bushes. Not the two-block plants
 *       ({@link BlockDoublePlant}), the lily pad, which floats, or a melon or pumpkin stem,
 *       which draws toward its fruit.</li>
 * </ul>
 *
 * @since 2026.10
 */
public final class PlanterPlants {

  /** The prefix of a nursery-potted plant's registry name. */
  public static final String POTTED = "potted_";

  private PlanterPlants() {
  }

  /** A plant from an item: the state to plant, and whether it came out of a nursery pot. */
  public static final class Planting {

    public final IBlockState state;
    public final boolean potted;

    Planting(IBlockState state, boolean potted) {
      this.state = state;
      this.potted = potted;
    }
  }

  /**
   * The plant inside a nursery-potted plant's registry name.
   *
   * @param path a registry name's path
   * @return the plant's path, or null if this is not a potted plant
   */
  @Nullable
  public static String unpotted(String path) {
    return path.startsWith(POTTED) && path.length() > POTTED.length()
        ? path.substring(POTTED.length()) : null;
  }

  /**
   * Whether a block state may be planted.
   *
   * @param state the state
   * @return whether it may
   */
  public static boolean isPlantable(@Nullable IBlockState state) {
    if (state == null) {
      return false;
    }
    Block block = state.getBlock();
    if (block instanceof ICsmPlanter) {
      return false;
    }
    if (block instanceof BlockParkProp) {
      BlockParkProp.Kind kind = ((BlockParkProp) block).getKind();
      ResourceLocation name = block.getRegistryName();
      return (kind == BlockParkProp.Kind.SHRUB || kind == BlockParkProp.Kind.PLANT
          || kind == BlockParkProp.Kind.CACTUS)
          && (name == null || !name.getPath().startsWith(POTTED));
    }
    if (block == Blocks.CACTUS) {
      return true;
    }
    if (block instanceof BlockDoublePlant || block instanceof BlockLilyPad
        || block instanceof BlockStem) {
      return false;
    }
    return block instanceof BlockBush;
  }

  /**
   * The plant an item plants.
   *
   * @param stack the item
   * @return the planting, or null if the item plants nothing
   */
  @Nullable
  public static Planting plantFor(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
      return null;
    }
    Block block = ((ItemBlock) stack.getItem()).getBlock();
    boolean potted = false;
    if (block instanceof BlockParkProp && block.getRegistryName() != null) {
      String bare = unpotted(block.getRegistryName().getPath());
      if (bare != null) {
        block = CsmRegistry.getBlock(bare);
        if (block == null) {
          return null;
        }
        potted = true;
      }
    }
    IBlockState state;
    try {
      state = block.getStateFromMeta(potted ? 0
          : stack.getItem().getMetadata(stack.getMetadata()));
    } catch (RuntimeException e) {
      return null;
    }
    return isPlantable(state) ? new Planting(state, potted) : null;
  }

  /**
   * The plant a stored name and variant stand for. A potted plant's name stands for the plant
   * in it, so a generator may name either.
   *
   * @param id the plant
   * @return its state, or null if it is not a plantable block
   */
  @Nullable
  public static IBlockState resolve(PlantId id) {
    ResourceLocation name = new ResourceLocation(id.getNamespace(), id.getPath());
    if (!ForgeRegistries.BLOCKS.containsKey(name)) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(name);
    if (block instanceof BlockParkProp) {
      String bare = unpotted(id.getPath());
      if (bare != null) {
        block = CsmRegistry.getBlock(bare);
      }
    }
    if (block == null || block == Blocks.AIR) {
      return null;
    }
    IBlockState state;
    try {
      state = block.getStateFromMeta(id.getMeta());
    } catch (RuntimeException e) {
      return null;
    }
    return isPlantable(state) ? state : null;
  }

  /**
   * A plant's stored form.
   *
   * @param state a plantable state
   * @return its registry name and metadata
   */
  public static PlantId idOf(IBlockState state) {
    Block block = state.getBlock();
    ResourceLocation name = block.getRegistryName();
    int meta;
    try {
      meta = block.getMetaFromState(state);
    } catch (RuntimeException e) {
      meta = 0;
    }
    return new PlantId(name == null ? "minecraft" : name.getNamespace(),
        name == null ? "air" : name.getPath(), meta);
  }

  /**
   * The item a planted plant comes back as when it is taken out or its planter is broken.
   *
   * @param state the plant
   * @param world the planter's world
   * @param pos   the planter's position
   * @return the item, possibly empty
   */
  public static ItemStack itemFor(IBlockState state, World world, BlockPos pos) {
    Block block = state.getBlock();
    try {
      ItemStack stack = block.getItem(world, pos, state);
      if (!stack.isEmpty()) {
        return stack;
      }
    } catch (RuntimeException e) {
      // A modded plant that reads the world at the position sees the planter; fall through.
    }
    Item item = Item.getItemFromBlock(block);
    return item == Items.AIR ? ItemStack.EMPTY
        : new ItemStack(item, 1, block.damageDropped(state));
  }
}
