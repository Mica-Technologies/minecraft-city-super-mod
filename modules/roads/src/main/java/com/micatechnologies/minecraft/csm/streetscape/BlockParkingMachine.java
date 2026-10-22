package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.economy.CsmEmeralds;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A parking garage's ticket machines (#302), standing at a car window's height beside a
 * {@link BlockParkingGate}:
 *
 * <ul>
 *   <li>the <b>ticket dispenser</b> ({@code parking_ticket_dispenser}): a click hands out a
 *   {@link ItemParkingTicket} stamped with the time and lifts the nearest gate;</li>
 *   <li>the <b>exit pay kiosk</b> ({@code parking_exit_kiosk}): a click with a ticket charges by
 *   the hour since it was issued (rounded up, at least one, at most 24), in emeralds or, with
 *   SUM's economy, money, as the parking meters do; takes the ticket and lifts the nearest gate.
 *   Its owner (whoever placed it) or an op sneak-clicks with an empty hand to change the hourly
 *   rate (1, 2, 3, 5, 10, free).</li>
 * </ul>
 *
 * <p>The gate a machine lifts is the nearest within four blocks, so a machine pairs with the gate
 * beside it with no linking. A gate in any mode lifts for a ticket.</p>
 *
 * @since 2026.10
 */
public class BlockParkingMachine extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  private static final long HOUR_MS = 60L * 60L * 1000L;

  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0, 0, 4 / 16.0, 12 / 16.0, 1,
      12 / 16.0);

  private final String registryName;
  private final boolean exit;

  public BlockParkingMachine(String registryName, boolean exit) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 0F, 0);
    this.registryName = registryName;
    this.exit = exit;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    TileEntity te = world.getTileEntity(pos);
    if (!world.isRemote && te instanceof TileEntityParkingKiosk) {
      ((TileEntityParkingKiosk) te).setOwner(placer.getUniqueID());
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return true;
    }
    Item ticketItem = CsmRegistry.getItem("parking_ticket");
    if (!(ticketItem instanceof ItemParkingTicket)) {
      return true;
    }
    ItemParkingTicket tickets = (ItemParkingTicket) ticketItem;
    if (!exit) {
      ItemStack ticket = tickets.issue();
      if (!player.inventory.addItemStackToInventory(ticket)) {
        player.dropItem(ticket, false);
      }
      world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.6F,
          1.4F);
      tell(player, "Take your ticket -- "
          + (BlockParkingGate.grantNearest(world, pos) ? "the gate is open" : "no gate nearby"));
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityParkingKiosk)) {
      return true;
    }
    TileEntityParkingKiosk kiosk = (TileEntityParkingKiosk) te;
    ItemStack held = player.getHeldItem(hand);
    if (held.isEmpty() && player.isSneaking()) {
      if ((kiosk.getOwner() != null && kiosk.getOwner().equals(player.getUniqueID()))
          || player.canUseCommand(2, "")) {
        tell(player, "Rate: " + rate(kiosk.cycleRate()) + " an hour");
      } else {
        tell(player, "Only this kiosk's owner can change its rate");
      }
      return true;
    }
    if (held.getItem() != tickets) {
      tell(player, "Insert your parking ticket -- " + (kiosk.getRate() == 0 ? "free"
          : rate(kiosk.getRate()) + " an hour"));
      return true;
    }
    long entered = ItemParkingTicket.entryTime(held);
    long hours = entered < 0 ? 24 : Math.min(24,
        Math.max(1, (System.currentTimeMillis() - entered + HOUR_MS - 1) / HOUR_MS));
    String paid;
    if (kiosk.getRate() == 0) {
      paid = "No charge";
    } else if (ParkingPaymentSum.isAvailable()) {
      double amount = hours * (double) kiosk.getRate();
      String refused = ParkingPaymentSum.spend(player, amount, String.format(Locale.ROOT,
          "Parking at %d, %d, %d", pos.getX(), pos.getY(), pos.getZ()));
      if (refused != null) {
        tell(player, refused);
        return true;
      }
      paid = "Paid " + String.format(Locale.ROOT, "$%.2f", amount);
    } else {
      int cost = (int) (hours * kiosk.getRate());
      if (!player.capabilities.isCreativeMode && !CsmEmeralds.take(player, cost)) {
        tell(player, hours + " h at " + rate(kiosk.getRate()) + " an hour is "
            + CsmEmeralds.format(cost) + ": not enough emeralds");
        return true;
      }
      paid = "Paid " + CsmEmeralds.format(cost);
    }
    held.shrink(1);
    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.6F, 1.6F);
    tell(player, paid + " for " + hours + " h -- "
        + (BlockParkingGate.grantNearest(world, pos) ? "thank you, drive safely"
        : "no gate nearby"));
    return true;
  }

  private static String rate(int rate) {
    return rate == 0 ? "free" : ParkingPaymentSum.isAvailable()
        ? String.format(Locale.ROOT, "$%d", rate) : CsmEmeralds.format(rate);
  }

  private static void tell(EntityPlayer player, String message) {
    player.sendStatusMessage(new TextComponentString(message), true);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  // the exit kiosk keeps its owner and rate; the dispenser has nothing to keep

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return exit;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityParkingKiosk.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityparkingkiosk";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return exit ? new TileEntityParkingKiosk() : null;
  }
}
