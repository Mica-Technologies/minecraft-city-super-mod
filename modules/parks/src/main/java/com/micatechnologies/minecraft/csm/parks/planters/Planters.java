package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * What every planter ({@link ICsmPlanter}) does the same way: the unlisted property its plant
 * travels in, the click that plants or takes out, the drop when it is broken, and the two
 * render passes it draws in.
 *
 * @since 2026.10
 */
public final class Planters {

  /**
   * The planted plant's state, from the tile entity, for the baked model. Unlisted, so a planter
   * has exactly the states it had before it could be planted.
   */
  public static final IUnlistedProperty<IBlockState> PLANT = new IUnlistedProperty<IBlockState>() {
    @Override
    public String getName() {
      return "plant";
    }

    @Override
    public boolean isValid(IBlockState value) {
      return true;
    }

    @Override
    public Class<IBlockState> getType() {
      return IBlockState.class;
    }

    @Override
    public String valueToString(IBlockState value) {
      return String.valueOf(value);
    }
  };

  /** The unlisted properties a planter's state container is built with. */
  public static final IUnlistedProperty<?>[] UNLISTED = {PLANT};

  private Planters() {
  }

  /**
   * A planter's state with its plant, for its baked model. The tile entity is looked up without
   * being created (a chunk builder's world must not create one), so a planter never planted has
   * none and costs a map lookup.
   *
   * @param block the planter
   * @param state its actual state
   * @param world the world, possibly a chunk builder's cache
   * @param pos   its position
   * @return the state, with the plant when there is one
   */
  public static IBlockState extendedState(Block block, IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState) || !(block instanceof ICsmPlanter)
        || ((ICsmPlanter) block).getSoil(state) == null) {
      return state;
    }
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    IBlockState plant = te instanceof TileEntityPlanter
        ? ((TileEntityPlanter) te).getPlantState() : null;
    return plant == null ? state : ((IExtendedBlockState) state).withProperty(PLANT, plant);
  }

  /**
   * Whether a planter draws in a pass: its own (cutout) and the cutout-mipped one a plant's
   * leaves are drawn in. The baked model hands each pass only its own faces, so an empty
   * planter is drawn exactly as before.
   *
   * @param layer the pass
   * @return whether the planter has faces in it
   */
  public static boolean drawsIn(BlockRenderLayer layer) {
    return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * A click on a planter. With a plantable item: plants it (taking back what was there, if
   * anything). With an empty main hand, or sneaking: takes the plant out, into the hand.
   *
   * @return whether the click was used
   */
  public static boolean activate(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand) {
    Block block = state.getBlock();
    if (!(block instanceof ICsmPlanter)
        || ((ICsmPlanter) block).getSoil(state.getActualState(world, pos)) == null) {
      return false;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPlanter)) {
      return false;
    }
    TileEntityPlanter planter = (TileEntityPlanter) te;
    IBlockState current = planter.getPlantState();
    ItemStack held = player.getHeldItem(hand);

    if (held.isEmpty() || player.isSneaking()) {
      if (current == null || hand != EnumHand.MAIN_HAND) {
        return false;
      }
      if (!world.isRemote) {
        ItemStack out = PlanterPlants.itemFor(current, world, pos);
        planter.setPlant(null);
        if (held.isEmpty()) {
          player.setHeldItem(hand, out);
        } else {
          give(player, out);
        }
        sound(world, pos, player, current, false);
      }
      return true;
    }

    PlanterPlants.Planting planting = PlanterPlants.plantFor(held);
    if (planting == null) {
      return false;
    }
    if (planting.state == current) {
      return true;
    }
    if (!world.isRemote) {
      boolean creative = player.capabilities.isCreativeMode;
      if (current != null && !creative) {
        give(player, PlanterPlants.itemFor(current, world, pos));
      }
      planter.setPlant(planting.state);
      if (!creative) {
        held.shrink(1);
        if (planting.potted) {
          give(player, new ItemStack(Items.FLOWER_POT));
        }
      }
      sound(world, pos, player, planting.state, true);
    }
    return true;
  }

  private static void give(EntityPlayer player, ItemStack stack) {
    if (!stack.isEmpty() && !player.inventory.addItemStackToInventory(stack)) {
      player.dropItem(stack, false);
    }
  }

  private static void sound(World world, BlockPos pos, @Nullable EntityPlayer player,
      IBlockState plant, boolean placed) {
    SoundType type = plant.getBlock().getSoundType(plant, world, pos, player);
    world.playSound(null, pos, placed ? type.getPlaceSound() : type.getBreakSound(),
        SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
  }

  /**
   * Drops what was planted, from {@code breakBlock}, before the tile entity goes.
   *
   * @param world the world
   * @param pos   the planter's position
   */
  public static void dropPlant(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityPlanter) {
      IBlockState plant = ((TileEntityPlanter) te).getPlantState();
      if (plant != null) {
        Block.spawnAsEntity(world, pos, PlanterPlants.itemFor(plant, world, pos));
      }
    }
  }

  /**
   * A creative player breaking a planter gets nothing from it, as with any block: forgets the
   * plant, from {@code onBlockHarvested}, so {@link #dropPlant} drops nothing.
   *
   * @param world  the world
   * @param pos    the planter's position
   * @param player who broke it
   */
  public static void harvested(World world, BlockPos pos, EntityPlayer player) {
    if (!world.isRemote && player.capabilities.isCreativeMode) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityPlanter) {
        ((TileEntityPlanter) te).clearSilently();
      }
    }
  }
}
