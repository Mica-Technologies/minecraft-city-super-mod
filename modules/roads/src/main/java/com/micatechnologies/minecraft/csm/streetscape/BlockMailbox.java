package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * A mailbox: a {@link BlockUtilityBox} unit with one or more locked compartments.
 *
 * <p>Anyone may post into an owned compartment; only its owner, or an operator, may take out.
 * The player who places the box owns every compartment, and from any compartment's screen hands
 * it to a named player or frees it; a free compartment is claimed by the next player to open it.
 * Only the placer or an operator can break the box, from any cell of it, and what it holds
 * drops. It cannot be blown up.</p>
 *
 * <p>Which compartment a click opens is the door nearest the point on the front the player is
 * looking at. Each door is given facing north, as {x0, y0, x1, y1} in pixels on the front face,
 * y from the root cell's floor. With an item in hand, a click on a compartment the player may
 * not open posts the held stack straight in.</p>
 *
 * <p>The screen's mode and compartment travel in the GUI id, so the server decides what the
 * client's container holds and the two cannot disagree: see {@link #guiId}.</p>
 *
 * @version 1.0
 */
public class BlockMailbox extends BlockUtilityBox implements ICsmTileEntityProvider {

  /** The mailbox screen's GUI id, unique across the mod, in the low byte of {@link #guiId}. */
  public static final int GUI_ID = 34;

  /** A free compartment: no slots, and a button to claim it. */
  public static final int MODE_FREE = 0;
  /** Someone else's compartment: one slot to post through. */
  public static final int MODE_POST = 1;
  /** The player's own compartment (or any, for an operator): its slots. */
  public static final int MODE_OPEN = 2;

  private final float[][] doors;
  private final int[] slots;
  private final String[] doorNames;

  /**
   * @param registryName the registry name
   * @param spec         the unit's size and box
   * @param doors        each compartment's door facing north, {x0, y0, x1, y1} in pixels
   * @param slots        each compartment's slot count
   * @param doorNames    what each compartment is called ("Box 3", "Parcel 1")
   */
  public BlockMailbox(String registryName, UtilityBoxSpec spec, float[][] doors, int[] slots,
      String[] doorNames) {
    super(registryName, spec);
    if (doors.length != slots.length || doors.length != doorNames.length || doors.length > 31) {
      throw new IllegalArgumentException("A mailbox needs a door, a size and a name for each of "
          + "at most 31 compartments");
    }
    this.doors = doors;
    this.slots = slots;
    this.doorNames = doorNames;
  }

  public int getCompartmentCount() {
    return slots.length;
  }

  public int getSlots(int compartment) {
    return slots[compartment];
  }

  public String getCompartmentName(int compartment) {
    return doorNames[compartment];
  }

  /** The GUI id carrying the compartment and mode: id, compartment and mode a byte each. */
  public static int guiId(int compartment, int mode) {
    return GUI_ID | (compartment << 8) | (mode << 16);
  }

  public static boolean isMailboxGui(int id) {
    return (id & 0xFF) == GUI_ID;
  }

  public static int guiCompartment(int id) {
    return (id >> 8) & 0xFF;
  }

  public static int guiMode(int id) {
    return (id >> 16) & 0xFF;
  }

  // ----------------------------------------------------------------------------------------
  // Permissions
  // ----------------------------------------------------------------------------------------

  static boolean isOperator(EntityPlayer player) {
    return player.canUseCommand(2, "");
  }

  /** Whether {@code player} may hand out compartments: the placer, or an operator. */
  static boolean mayManage(EntityPlayer player, TileEntityMailbox box) {
    return box.isPlacer(player) || isOperator(player);
  }

  /** The mode compartment {@code i}'s screen opens in for {@code player}. */
  static int modeFor(EntityPlayer player, TileEntityMailbox box, int i) {
    if (box.isOwner(i, player) || isOperator(player)) {
      return MODE_OPEN;
    }
    return box.getOwner(i) == null ? MODE_FREE : MODE_POST;
  }

  @Override
  public boolean mayBreakUnit(World world, BlockPos root, EntityPlayer player) {
    TileEntity te = world.getTileEntity(root);
    return !(te instanceof TileEntityMailbox) || ((TileEntityMailbox) te).getPlacer() == null
        || mayManage(player, (TileEntityMailbox) te);
  }

  // ----------------------------------------------------------------------------------------
  // Placing, clicking, breaking
  // ----------------------------------------------------------------------------------------

  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityMailbox) {
      TileEntityMailbox box = (TileEntityMailbox) te;
      box.init(slots);
      box.setPlacer(placer.getUniqueID(), placer.getName());
      box.markDirtySync(world, pos, true);
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityMailbox)) {
      return true;
    }
    TileEntityMailbox box = (TileEntityMailbox) te;
    box.init(slots);
    int i = compartmentAt(world, pos, state.getValue(FACING), player);
    int mode = modeFor(player, box, i);
    ItemStack held = player.getHeldItem(hand);
    if (mode == MODE_POST && !held.isEmpty()) {
      int before = held.getCount();
      ItemStack left = box.post(i, held, player);
      player.setHeldItem(hand, left);
      player.sendStatusMessage(new TextComponentString(left.getCount() == before
          ? doorNames[i] + " is full" : "Posted to " + box.getOwnerName(i)), true);
      return true;
    }
    openCompartment(player, world, pos, i, box);
    return true;
  }

  /** Opens compartment {@code i}'s screen, in the mode the server decides for this player. */
  static void openCompartment(EntityPlayer player, World world, BlockPos root, int i,
      TileEntityMailbox box) {
    player.openGui(Csm.instance, guiId(i, modeFor(player, box, i)), world, root.getX(),
        root.getY(), root.getZ());
  }

  /**
   * The compartment whose door is nearest where the player is looking on the unit. The click's
   * own hit point is no use here: a click on a part cell arrives relative to that cell.
   */
  private int compartmentAt(World world, BlockPos root, EnumFacing facing, EntityPlayer player) {
    if (doors.length == 1) {
      return 0;
    }
    Vec3d eye = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
    Vec3d end = eye.add(player.getLookVec().scale(8.0));
    RayTraceResult hit = world.rayTraceBlocks(eye, end, false, true, false);
    if (hit == null || hit.hitVec == null) {
      return 0;
    }
    Vec3d local = hit.hitVec.subtract(root.getX(), root.getY() + getRoadSurfaceOffset(world,
        root), root.getZ());
    // Turn the point back to facing north: the inverse of each facing's turn.
    EnumFacing back = facing.getAxis() == EnumFacing.Axis.X ? facing.getOpposite() : facing;
    AxisAlignedBB p = UtilityBoxSpec.rotate(new AxisAlignedBB(local.x, local.y, local.z,
        local.x, local.y, local.z), back);
    double x = p.minX * 16.0;
    double y = p.minY * 16.0;
    int best = 0;
    double bestD = Double.MAX_VALUE;
    for (int i = 0; i < doors.length; i++) {
      float[] d = doors[i];
      double dx = Math.max(0, Math.max(d[0] - x, x - d[2]));
      double dy = Math.max(0, Math.max(d[1] - y, y - d[3]));
      double dist = dx * dx + dy * dy;
      if (dist < bestD) {
        bestD = dist;
        best = i;
      }
    }
    return best;
  }

  /** Only the placer or an operator can break it; everyone else cannot even crack it. */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    if (!mayBreakUnit(world, pos, player)) {
      return false;
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  @Override
  @SuppressWarnings("deprecation")
  public float getPlayerRelativeBlockHardness(@Nonnull IBlockState state,
      @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
    return mayBreakUnit(world, pos, player)
        ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
  }

  /** Mail is not lost to a creeper: the box, and so every cell of it, shrugs off explosions. */
  @Override
  public float getExplosionResistance(@Nonnull World world, @Nonnull BlockPos pos,
      @Nullable Entity exploder, @Nonnull Explosion explosion) {
    return 6000000.0F;
  }

  /** What the compartments hold drops where the box stood. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityMailbox) {
        ((TileEntityMailbox) te).forEachItem(stack -> InventoryHelper.spawnItemStack(world,
            pos.getX(), pos.getY(), pos.getZ(), stack.copy()));
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityMailbox.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitymailbox";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityMailbox();
  }
}
