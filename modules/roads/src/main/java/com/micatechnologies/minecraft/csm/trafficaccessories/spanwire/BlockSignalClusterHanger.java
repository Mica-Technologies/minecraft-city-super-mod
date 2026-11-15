package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalHeadConfigTool;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalHeadConfigToolMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * A signal cluster hanger: placed in the block above the middle of a cluster of heads, under a
 * span wire or a mast arm, it draws the bracketing that holds them. The heads stay ordinary signal
 * blocks where they were placed; see {@link TileEntitySignalClusterHanger}.
 *
 * <p>Its block model is never drawn in the world: the renderer draws all of its hardware, linked
 * or not, in the hanger's colour. The bracket model is the item icon only.
 */
public class BlockSignalClusterHanger extends BlockSpanWireClusterMount {

  public BlockSignalClusterHanger(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube);
  }

  /**
   * No linked/unlinked model swap: the world model is empty either way, so the property would
   * double the block states for nothing.
   */
  @Override
  protected boolean hidesModelWhenLinked() {
    return false;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySignalClusterHanger.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitysignalclusterhanger";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySignalClusterHanger();
  }

  /**
   * Sneak with an empty hand, or a click with the signal head config tool in its body colour mode,
   * cycles the hardware colour; anything else opens the mount screen as on any wire mount (which
   * has a colour row for the hanger too). Consumed on both sides, so the client does not go on to
   * use the held item.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    final ItemStack held = player.getHeldItem(hand);
    final boolean emptyHandSneak = player.isSneaking() && held.isEmpty();
    // The signal head config tool in its body colour mode paints the hanger as it paints a head.
    // Sneaking with the tool never reaches here: vanilla skips the block for a sneaking player
    // holding an item, and the tool switches its mode instead.
    final boolean toolPaints = held.getItem() instanceof ItemSignalHeadConfigTool
        && ItemSignalHeadConfigTool.getMode(held) == ItemSignalHeadConfigToolMode.CYCLE_BODY_COLOR;
    if (emptyHandSneak || toolPaints) {
      if (emptyHandSneak && hand != EnumHand.MAIN_HAND) {
        return true;
      }
      if (!world.isRemote) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySignalClusterHanger) {
          final TrafficSignalBodyColor color = ((TileEntitySignalClusterHanger) te).cycleColor();
          player.sendStatusMessage(
              new TextComponentString("Hanger colour: " + color.getFriendlyName()), true);
        }
      }
      return true;
    }
    return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
  }
}
