package com.micatechnologies.minecraft.csm.venues.lines;

import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A court's or a track's marking anchor ({@code court_acrylic_anchor},
 * {@code track_marking_anchor}): a cell that shows its own piece of the layout and tells the
 * painted cells round it which layout they belong to and where they sit in it. Its tile entity
 * ({@link TileEntityLineAnchor}) holds the layout, the anchor's cell in it and the colours.
 *
 * <p><b>Metadata</b>: the quarter turns clockwise from the layout's frame to the world's, 0-3:
 * 0 its length runs north-south as drawn (+x east), 1 its +x south, 2 west, 3 north. Placed by
 * hand, the court's far end (its -z, the north end as drawn) points the way the placer looks,
 * and the anchor is the court's centre cell. A sneak-click with an empty hand steps the layout
 * (and puts the anchor back at its centre cell).</p>
 *
 * @since 2026.10
 */
public class BlockLineAnchor extends BlockLineCell implements ICsmTileEntityProvider {

  /** Quarter turns from the layout's frame. Stored. */
  public static final PropertyInteger ROT = PropertyInteger.create("rot", 0, 3);

  /**
   * Constructs an anchor.
   *
   * @param registryName its registry name
   * @param surface      what it is made of
   * @param family       its family
   */
  public BlockLineAnchor(String registryName, Surface surface, LineArtFamily family) {
    super(registryName, surface, family, null);
    setDefaultState(blockState.getBaseState().withProperty(ROT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{ROT}, new IUnlistedProperty[]{KEY});
  }

  /** An anchor state's quarter turns. */
  static int rotation(IBlockState state) {
    return state.getValue(ROT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(ROT, meta & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(ROT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(ROT,
        (placer.getHorizontalFacing().getHorizontalIndex() + 2) & 3);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityLineAnchor) {
        TileEntityLineAnchor a = (TileEntityLineAnchor) te;
        a.stepLayout(family().layoutCount());
        player.sendStatusMessage(new TextComponentTranslation(
            "csm.venues.lines." + family().layout(a.getLayout()).name), true);
      }
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityLineAnchor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitylineanchor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityLineAnchor();
  }
}
