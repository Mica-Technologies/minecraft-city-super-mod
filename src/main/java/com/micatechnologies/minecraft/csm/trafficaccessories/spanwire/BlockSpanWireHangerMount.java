package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEWUD;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A wire mount that can hang from a span of messenger cable.
 *
 * <p>This is the existing {@code tlitehorzwiremount} and {@code tlitevertwiremount} blocks with a
 * tile entity added (the plan's D5). Everything else about them is unchanged: same registry
 * names, same models, same hitboxes, same placement. A mount that is never linked into a span
 * carries an empty tile entity and behaves exactly as it did before, which is what keeps every
 * existing build that used these decoratively working untouched.
 */
public class BlockSpanWireHangerMount extends BlockTrafficAccessoryNSEWUD
    implements ICsmTileEntityProvider {

  /** GUI id this block opens; kept here so the handler and the block cannot drift apart. */
  public static final int GUI_ID = 22;

  /**
   * Whether this mount is part of a span, exposed to the model so a linked one can stop drawing
   * its own bracket.
   *
   * <p>The bracket in the block model is the decorative mount these blocks have always been.
   * Once a mount is on a span the renderer draws the real hardware -- a mast standing on the
   * payload, a saddle over the messenger, coiled conductor -- and the old bracket is then a
   * second bracket in the same place. Worse, it hangs into the block below, where a signal that
   * has risen to meet the cable now is, and shows through the lens as a black square.
   *
   * <p>Derived, never stored: it lives only in {@link #getActualState}, so block metadata still
   * holds nothing but the facing and an existing build's mounts are bit-for-bit unchanged.
   */
  public static final PropertyBool LINKED = PropertyBool.create("linked");

  /**
   * Whether this block hands its model over to the span renderer once it is linked.
   *
   * <p>True for the bracket mounts, whose model is a second bracket in the same place as the
   * hardware the renderer draws. False for anything whose model <em>is</em> the thing hanging on
   * the wire -- the disconnect box -- which must keep drawing.
   *
   * <p>Read during {@link #createBlockState()}, which runs from the {@code Block} constructor, so
   * it must stay a plain constant and not touch instance state.
   */
  protected boolean hidesModelWhenLinked() {
    return true;
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return hidesModelWhenLinked()
        ? new BlockStateContainer(this, FACING, LINKED)
        : new BlockStateContainer(this, FACING);
  }

  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    if (!hidesModelWhenLinked()) {
      return state;
    }
    final TileEntity te = world.getTileEntity(pos);
    return state.withProperty(LINKED,
        te instanceof AbstractTileEntitySpanWireAttachment
            && ((AbstractTileEntitySpanWireAttachment) te).getSpan() != null);
  }

  public BlockSpanWireHangerMount(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySpanWireHanger.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityspanwirehanger";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySpanWireHanger();
  }

  /**
   * Opens the mount's configuration screen, and consumes the click on <b>both</b> sides.
   *
   * <p>The both-sides part is not incidental: the screen is client-only, so a server that fell
   * through to the default here would use the held item instead and right-clicking a mount to
   * configure it would place a block. The same trap is documented on
   * {@code BlockDynamicStreetSign}.
   *
   * <p>Declines the click while the span wire tool is held, so the tool keeps its own reading of
   * a mount rather than being shadowed by this screen.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (player.getHeldItem(hand).getItem() instanceof ItemSpanWireTool) {
      return false;
    }
    if (world.isRemote) {
      player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  /**
   * Re-reads what this mount carries when a neighbouring block changes -- which is how a sign or
   * a box placed under an already-strung span gets its strap without anything having to poll.
   */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    final net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntitySpanWireHanger) {
      ((TileEntitySpanWireHanger) te).refreshPayload();
    }
  }

  /**
   * Drops this mount out of its span before the tile entity that knows about the span is
   * removed. The cable closes over the gap; the rest of the span is left strung.
   */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    SpanWireManager.onAttachmentRemoved(worldIn, pos);
    super.breakBlock(worldIn, pos, state);
  }
}
