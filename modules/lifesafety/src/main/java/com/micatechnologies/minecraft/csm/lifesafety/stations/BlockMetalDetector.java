package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A walk-through metal detector, two blocks tall (the model stands up into the block above, so
 * leave it air). Walk through it carrying metal -- a weapon, a tool, armour other than leather, or
 * anything named iron, gold, steel, chain and the like -- and it beeps, its lamp goes red and it
 * gives a redstone pulse for a second; otherwise its lamp stays green.
 *
 * <p>Only the frame's two uprights collide; the gap between them is open. A player standing in it
 * is looked at once a second at most (the time is kept on the player), so standing in the arch
 * costs nothing.</p>
 *
 * <p>Stored in metadata: the facing and {@link #ALARM}.</p>
 *
 * @since 2026.9
 */
public class BlockMetalDetector extends BlockFireProtectionProp {

  public static final PropertyBool ALARM = PropertyBool.create("alarm");

  /** Words in an item's registry path that mean it is metal. */
  private static final String[] METAL_WORDS = {"iron", "gold", "steel", "chain", "metal",
      "copper", "tin", "aluminum", "aluminium", "bucket", "minecart", "compass", "clock", "key",
      "knife", "gun", "pistol", "rifle"};

  /** How long an alarm holds, in ticks. */
  private static final int ALARM_TICKS = 20;

  private static final String LAST_CHECK_KEY = "csmMetalDetectorCheck";

  private static final AxisAlignedBB[] UPRIGHTS = {
      new AxisAlignedBB(0, 0, 6 / 16.0, 2 / 16.0, 1, 10 / 16.0),
      new AxisAlignedBB(14 / 16.0, 0, 6 / 16.0, 1, 1, 10 / 16.0)};

  public BlockMetalDetector(String registryName, int[] box) {
    super(registryName, box, true);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ALARM, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ALARM);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ALARM) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(ALARM, (meta & 4) != 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actual) {
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB box : UPRIGHTS) {
      // The uprights stand either side of the way through: across x facing north or south,
      // across z facing east or west.
      AxisAlignedBB turned = facing.getAxis() == EnumFacing.Axis.Z ? box
          : new AxisAlignedBB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX);
      addCollisionBoxToList(pos, entityBox, collidingBoxes, turned);
    }
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
    if (world.isRemote || !(entity instanceof EntityPlayer)) {
      return;
    }
    long now = world.getTotalWorldTime();
    long last = entity.getEntityData().getLong(LAST_CHECK_KEY);
    if (now - last < ALARM_TICKS) {
      return;
    }
    entity.getEntityData().setLong(LAST_CHECK_KEY, now);
    if (carriesMetal((EntityPlayer) entity)) {
      world.setBlockState(pos, state.withProperty(ALARM, true), 3);
      world.scheduleUpdate(pos, this, ALARM_TICKS);
      SoundEvent beep = LifeSafetySounds.METAL_DETECTOR_ALARM.getSoundEvent();
      if (beep != null) {
        world.playSound(null, pos, beep, SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (state.getValue(ALARM)) {
      world.setBlockState(pos, state.withProperty(ALARM, false), 3);
    }
  }

  /** Whether a player is carrying anything this detector calls metal. */
  public static boolean carriesMetal(EntityPlayer player) {
    for (List<ItemStack> list : new List[]{player.inventory.mainInventory,
        player.inventory.armorInventory, player.inventory.offHandInventory}) {
      for (ItemStack stack : list) {
        if (!stack.isEmpty() && isMetal(stack)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMetal(ItemStack stack) {
    Item item = stack.getItem();
    if (item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemHoe
        || item instanceof ItemShears || item instanceof ItemFlintAndSteel) {
      return true;
    }
    if (item instanceof ItemArmor
        && ((ItemArmor) item).getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER) {
      return true;
    }
    ResourceLocation name = item.getRegistryName();
    if (name == null) {
      return false;
    }
    String path = name.getPath().toLowerCase(Locale.ROOT);
    for (String word : METAL_WORDS) {
      if (path.contains(word)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(@Nonnull IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return state.getValue(ALARM) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}
