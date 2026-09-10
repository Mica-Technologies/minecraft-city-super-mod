package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * A barricade that can carry a mounted sign and warning lights.
 *
 * <p>Everything about what a barricade CARRIES lives here; what a barricade IS -- how many rails,
 * how it stands up, whether it joins onto its neighbours -- is left to the subclass. A rigid
 * trestle barricade and a folding one look nothing alike and are put up differently, but a sign
 * is bolted to either the same way and the lights clamp to either the same way.</p>
 *
 * <p>The three shape questions the renderer has to ask -- how tall, where the uprights are, how
 * thick the rails are -- are methods rather than constructor arguments, so a subclass with
 * different proportions answers them without every barricade having to pass numbers it shares
 * with all the others.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockWorkZoneBarricade extends BlockWorkZoneDeviceDiagonal
    implements ICsmTileEntityProvider {

  /**
   * How tall this barricade's uprights are, in 1/16 block units. The renderer clamps warning
   * lights to the top of them and mounts a sign against the rails below.
   *
   * @since 1.0
   */
  private final float topY;

  /**
   * Constructs an {@link AbstractBlockWorkZoneBarricade} instance.
   *
   * @param registryName the registry name of the barricade
   * @param topY         the height of its uprights, in 1/16 block units
   * @param boundingBox  the bounding box of the barricade, in block space
   *
   * @since 1.0
   */
  protected AbstractBlockWorkZoneBarricade(String registryName, float topY,
      AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
    this.topY = topY;
  }

  /**
   * Gets how tall this barricade's uprights are, in 1/16 block units.
   *
   * @return the upright height
   *
   * @since 1.0
   */
  public float getTopY() {
    return topY;
  }

  /**
   * Gets where this barricade's left-hand upright stands, in 1/16 block units.
   *
   * @return the upright's x position
   *
   * @since 1.0
   */
  public float getLeftUprightX() {
    return BarricadeGeometry.LEFT_UPRIGHT_X;
  }

  /**
   * Gets where this barricade's right-hand upright stands, in 1/16 block units.
   *
   * @return the upright's x position
   *
   * @since 1.0
   */
  public float getRightUprightX() {
    return BarricadeGeometry.RIGHT_UPRIGHT_X;
  }

  /**
   * Gets half the thickness of this barricade's rails, in 1/16 block units. A mounted sign
   * stands clear of the rails by this much plus its own standoff.
   *
   * @return half the rail thickness
   *
   * @since 1.0
   */
  public float getRailHalfZ() {
    return BarricadeGeometry.RAIL_HALF_Z;
  }

  /**
   * Gets where the centre plane of this barricade's rails is, in 1/16 block units.
   *
   * <p>Not always the block's axis. A barricade whose legs trail off to one side is centred in
   * its cell as a WHOLE, which leaves its panel forward of the middle; a light or a sign put on
   * the axis would then hang in front of nothing.</p>
   *
   * @return the rails' centre z
   *
   * @since 1.0
   */
  public float getRailCentreZ() {
    return BarricadeGeometry.RAIL_CENTRE_Z;
  }

  /**
   * Mounts a sign, cycles the warning lights, or takes a mounted sign back off.
   *
   * <p>Holding a sign and clicking mounts THAT sign, whatever it is, rather than picking from a
   * list someone had to curate: the renderer reads the sign's own face and panel shape off its
   * model, so a sign added to the mod later works here the day it is added. Clicking with an
   * empty hand cycles the lights, and sneaking takes the sign off and hands it back.</p>
   *
   * @param world  the world the block is in
   * @param pos    the block position
   * @param state  the block state
   * @param player the player
   * @param hand   the hand used
   * @param facing the face clicked
   * @param hitX   the x coordinate of the hit
   * @param hitY   the y coordinate of the hit
   * @param hitZ   the z coordinate of the hit
   *
   * @return true if the click was handled
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (!(tileEntity instanceof TileEntityBarricade)) {
      return false;
    }
    TileEntityBarricade barricade = (TileEntityBarricade) tileEntity;
    ItemStack held = player.getHeldItem(hand);

    if (player.isSneaking()) {
      if (!world.isRemote) {
        removeSign(world, pos, state, player, barricade);
      }
      return true;
    }

    Block signBlock = signBlockOf(held);
    if (signBlock != null) {
      if (!world.isRemote) {
        removeSign(world, pos, state, player, barricade);
        barricade.setSign(signBlock.getRegistryName());
        if (!player.isCreative()) {
          held.shrink(1);
        }
        barricade.markDirtySync(world, pos, state);
        say(player, "Mounted " + new ItemStack(signBlock).getDisplayName());
      }
      return true;
    }

    if (!world.isRemote) {
      BarricadeFlashers next = barricade.getFlashers().next();
      barricade.setFlashers(next);
      barricade.markDirtySync(world, pos, state);
      say(player, "Warning lights: " + next.getFriendlyName());
    }
    return true;
  }

  /**
   * Takes any mounted sign off and gives it back to the player.
   *
   * @param world     the world the block is in
   * @param pos       the block position
   * @param state     the block state
   * @param player    the player
   * @param barricade the barricade's tile entity
   *
   * @since 1.0
   */
  private void removeSign(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, TileEntityBarricade barricade) {
    Block mounted = barricade.getSignBlock();
    barricade.setSign(null);
    barricade.markDirtySync(world, pos, state);
    if (mounted == null) {
      return;
    }
    ItemStack returned = new ItemStack(mounted);
    if (!player.isCreative() && !player.inventory.addItemStackToInventory(returned)) {
      player.dropItem(returned, false);
    }
    say(player, "Removed " + returned.getDisplayName());
  }

  /**
   * Gets the sign block a held stack would mount, or null if the stack is not a sign.
   *
   * <p>A sign is any block of this mod's whose class sits in the traffic signs package. Testing
   * the package rather than keeping a list is what makes every sign in the mod eligible without
   * anything having to enumerate them.</p>
   *
   * @param stack the held stack
   *
   * @return the sign block, or null
   *
   * @since 1.0
   */
  @Nullable
  private static Block signBlockOf(ItemStack stack) {
    if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
      return null;
    }
    Block block = ((ItemBlock) stack.getItem()).getBlock();
    if (block == null || block.getRegistryName() == null) {
      return null;
    }
    return block.getClass().getName().contains(".trafficsigns.") ? block : null;
  }

  /**
   * Tells the player what just changed, on the action bar rather than in chat so that cycling
   * does not fill the log.
   *
   * @param player  the player
   * @param message the message
   *
   * @since 1.0
   */
  private static void say(EntityPlayer player, String message) {
    player.sendStatusMessage(new TextComponentString(
        TextFormatting.YELLOW + message), true);
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityBarricade();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBarricade.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityworkzonebarricade";
  }
}
