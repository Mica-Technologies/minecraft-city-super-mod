package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A light switch linked to a block it is not beside: how the link is made, kept and used.
 *
 * <p><b>Making it.</b> With a light switch in hand, right-clicking a block that switches with
 * redstone ({@link #isTarget}: this module's lamps, lights, fan, candles and fireplace, Core's
 * powered blocks, and vanilla's redstone lamp, doors, trapdoors, gates, pistons, dispensers,
 * note blocks, TNT, powered rails, hoppers, wire, repeaters and comparators) links the item to
 * it, kept in the item's NBT; sneak-right-clicking the air, or a block that is not one, clears
 * it. The switch is then placed anywhere within {@value #MAX_DISTANCE} blocks of it, in the same
 * dimension, and its tile entity keeps the link.</p>
 *
 * <p><b>Using it.</b> Minecraft powers a block only from its neighbours, and a vanilla block
 * reads nothing else, so switching on puts a relay ({@link BlockSwitchRelay}: invisible, not
 * solid, not breakable by a player) in a free cell beside the linked block, which powers that
 * block and nothing else, as a lever's attached block is powered; switching off takes it away.
 * Nothing ticks: the relay is placed and removed on the switch's click, and removes itself when
 * the block it powers or the switch that placed it goes. A relay is never put where a block
 * already is; with no free cell beside the linked block the switch says so.</p>
 *
 * @since 2026.9
 */
public final class SwitchLinks {

  /** The farthest a switch may be from the block it is linked to, in blocks. */
  public static final int MAX_DISTANCE = 32;

  /** The relay's registry name. */
  public static final String RELAY = "switch_relay";

  private static final String KEY_LINK = "csmSwitchLink";
  private static final String MSG = "csm.furnishings.switch.";
  /** Where a relay goes, most preferred first: below and above before the sides. */
  private static final EnumFacing[] RELAY_SIDES = {EnumFacing.DOWN, EnumFacing.UP,
      EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

  private SwitchLinks() {
  }

  /**
   * Whether a light switch can be linked to the block {@code state}: whether it switches with
   * redstone.
   *
   * @param state the block
   *
   * @return whether it is a target
   */
  public static boolean isTarget(IBlockState state) {
    Block b = state.getBlock();
    return b instanceof ISwitchable || b instanceof AbstractPoweredBlockRotatableNSEWUD
        || b instanceof BlockRedstoneLight || b instanceof BlockDoor
        || b instanceof BlockTrapDoor || b instanceof BlockFenceGate
        || b instanceof BlockPistonBase || b instanceof BlockDispenser || b instanceof BlockNote
        || b instanceof BlockTNT || b instanceof BlockRailPowered || b instanceof BlockHopper
        || b instanceof BlockRedstoneWire || b instanceof BlockRedstoneDiode
        || b instanceof BlockCommandBlock;
  }

  // --- the link on the item ----------------------------------------------------------------

  /**
   * The link a switch item carries: {x, y, z, dimension}, or null for none.
   *
   * @param stack the item
   *
   * @return the link, or null
   */
  @Nullable
  public static int[] readLink(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null || !tag.hasKey(KEY_LINK)) {
      return null;
    }
    int[] link = tag.getIntArray(KEY_LINK);
    return link.length == 4 ? link : null;
  }

  /**
   * Links the switch item to the block at {@code pos}, and says so (server side).
   *
   * @param stack  the item
   * @param player who linked it
   * @param world  the world
   * @param pos    the block
   */
  public static void link(ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
    NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    tag.setIntArray(KEY_LINK, new int[]{pos.getX(), pos.getY(), pos.getZ(),
        world.provider.getDimension()});
    stack.setTagCompound(tag);
    tell(player, "linked", nameOf(world, pos), pos.getX(), pos.getY(), pos.getZ());
  }

  /**
   * Clears the switch item's link, and says so if it had one (server side).
   *
   * @param stack  the item
   * @param player who cleared it
   */
  public static void clear(ItemStack stack, EntityPlayer player) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null || !tag.hasKey(KEY_LINK)) {
      return;
    }
    tag.removeTag(KEY_LINK);
    stack.setTagCompound(tag.isEmpty() ? null : tag);
    tell(player, "cleared");
  }

  // --- the placed switch -------------------------------------------------------------------

  /**
   * Gives a switch just placed at {@code pos} the link its item carried, if the linked block is
   * in the same dimension and near enough, and says which (server side).
   *
   * @param world  the world
   * @param pos    the switch
   * @param stack  the item it was placed from
   * @param player who placed it, or null
   */
  public static void placed(World world, BlockPos pos, ItemStack stack,
      @Nullable EntityPlayer player) {
    int[] link = readLink(stack);
    TileEntityLightSwitch te = switchAt(world, pos);
    if (link == null || te == null) {
      return;
    }
    BlockPos target = new BlockPos(link[0], link[1], link[2]);
    if (link[3] != world.provider.getDimension()) {
      tell(player, "other_dimension");
      return;
    }
    double distance = Math.sqrt(target.distanceSq(pos));
    if (distance > MAX_DISTANCE) {
      tell(player, "too_far", (int) Math.round(distance), MAX_DISTANCE);
      return;
    }
    te.setTarget(target);
    tell(player, "linked", nameOf(world, target), target.getX(), target.getY(), target.getZ());
  }

  /**
   * Tells {@code player} what the switch at {@code pos} is linked to.
   *
   * @param world  the world
   * @param pos    the switch
   * @param player who asked
   */
  public static void describe(World world, BlockPos pos, EntityPlayer player) {
    TileEntityLightSwitch te = switchAt(world, pos);
    BlockPos target = te == null ? null : te.getTarget();
    if (target == null) {
      tell(player, "not_linked");
    } else {
      tell(player, "link_info", nameOf(world, target), target.getX(), target.getY(),
          target.getZ());
    }
  }

  /**
   * Powers the linked block of the switch at {@code pos}: puts a relay beside it (server side).
   *
   * @param world  the world
   * @param pos    the switch
   * @param player who switched it, told if it cannot be done, or null
   */
  public static void powerOn(World world, BlockPos pos, @Nullable EntityPlayer player) {
    TileEntityLightSwitch te = switchAt(world, pos);
    BlockPos target = te == null ? null : te.getTarget();
    if (target == null) {
      return;
    }
    if (te.getRelay() != null && isRelayOf(world, te.getRelay(), pos)) {
      return;
    }
    if (!world.isBlockLoaded(target)) {
      tell(player, "unloaded");
      return;
    }
    if (world.isAirBlock(target)) {
      tell(player, "gone");
      return;
    }
    Block relay = CsmRegistry.getBlock(RELAY);
    if (relay == null) {
      return;
    }
    for (EnumFacing side : RELAY_SIDES) {
      BlockPos cell = target.offset(side);
      if (cell.equals(pos) || !world.isBlockLoaded(cell) || !world.isAirBlock(cell)
          || cell.getY() < 0 || cell.getY() >= world.getHeight()) {
        continue;
      }
      // Placed without telling the neighbours, so that the relay knows its switch, and the
      // switch its relay, before the linked block looks at either.
      world.setBlockState(cell, relay.getDefaultState()
          .withProperty(BlockSwitchRelay.FACING, side), 2);
      if (world.getTileEntity(cell) instanceof TileEntitySwitchRelay) {
        ((TileEntitySwitchRelay) world.getTileEntity(cell)).setSwitch(pos);
      }
      te.setRelay(cell);
      world.notifyNeighborsOfStateChange(cell, relay, false);
      world.notifyNeighborsOfStateChange(target, relay, false);
      return;
    }
    tell(player, "no_room", nameOf(world, target));
  }

  /**
   * Stops powering the linked block of the switch at {@code pos}: takes its relay away (server
   * side).
   *
   * @param world the world
   * @param pos   the switch
   */
  public static void powerOff(World world, BlockPos pos) {
    TileEntityLightSwitch te = switchAt(world, pos);
    if (te == null || te.getRelay() == null) {
      return;
    }
    BlockPos cell = te.getRelay();
    te.setRelay(null);
    if (world.isBlockLoaded(cell) && isRelayOf(world, cell, pos)) {
      world.setBlockToAir(cell);
    }
  }

  /** Whether {@code cell} holds a relay placed by the switch at {@code switchPos}. */
  private static boolean isRelayOf(World world, BlockPos cell, BlockPos switchPos) {
    if (!(world.getBlockState(cell).getBlock() instanceof BlockSwitchRelay)) {
      return false;
    }
    return world.getTileEntity(cell) instanceof TileEntitySwitchRelay
        && switchPos.equals(((TileEntitySwitchRelay) world.getTileEntity(cell)).getSwitch());
  }

  @Nullable
  private static TileEntityLightSwitch switchAt(World world, BlockPos pos) {
    return world.getTileEntity(pos) instanceof TileEntityLightSwitch
        ? (TileEntityLightSwitch) world.getTileEntity(pos) : null;
  }

  // --- saying so -----------------------------------------------------------------------------

  /** The block at {@code pos}'s name, as its item would show it, for a message. */
  private static ITextComponent nameOf(World world, BlockPos pos) {
    if (!world.isBlockLoaded(pos)) {
      return new TextComponentString("?");
    }
    IBlockState state = world.getBlockState(pos);
    @SuppressWarnings("deprecation")
    ItemStack item = state.getBlock().getItem(world, pos, state);
    if (!item.isEmpty()) {
      return new TextComponentTranslation(item.getTranslationKey() + ".name");
    }
    return new TextComponentTranslation(state.getBlock().getTranslationKey() + ".name");
  }

  private static void tell(@Nullable EntityPlayer player, String key, Object... args) {
    if (player != null) {
      player.sendStatusMessage(new TextComponentTranslation(MSG + key, args), false);
    }
  }
}
