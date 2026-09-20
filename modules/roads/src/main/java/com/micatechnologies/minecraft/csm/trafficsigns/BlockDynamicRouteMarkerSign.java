package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * The route marker a route is posted under: an Interstate, US, county, state or provincial
 * shield on the mod's own sign post, with the route number typed in rather than baked into the
 * texture. Every marker the dynamic highway guide sign offers is available here.
 *
 * <p>It is an ordinary {@link AbstractBlockSign}, so it behaves as the other 472 signs do and is
 * compatible with everything they are: the eight facings, the extension post onto a slab or
 * through a guardrail, the setback in front of a signal arm or under a span wire, the
 * back-to-back pairing that puts two faces on one post, and every sign pole, mount and back in
 * the catalogue.</p>
 *
 * <p>What the tile entity holds is only the shield and the route number. The shield is exposed
 * as the {@link #SHIELD} block property, so the marker's face is painted by an ordinary block
 * model out of {@code blockstates/dynamic_route_marker_sign.json} -- one texture per marker,
 * generated from the guide sign atlas -- and the renderer draws nothing but the route number
 * over it. A marker with no number drawn is a marker with no tile entity work at all.</p>
 *
 * <p>GUI id 28. See {@code assets/docs/TRAFFIC_SIGNS.md}.</p>
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class BlockDynamicRouteMarkerSign extends AbstractBlockSign
    implements ICsmTileEntityProvider {

  /** GUI id this block opens; kept here so the handler and the block cannot drift apart. */
  public static final int GUI_ID = 28;

  /**
   * Which marker the plate is painted with.
   *
   * <p>Not stored in metadata -- there are sixty-seven markers and four bits, and the eight
   * facings already fill them. It is read off the tile entity in {@link #getActualState}, the
   * way {@code DOWNWARD} and {@code SHIFT} are read off the neighbours.</p>
   */
  public static final PropertyEnum<GuideSignShieldType> SHIELD =
      PropertyEnum.create("shield", GuideSignShieldType.class);

  @Override
  public String getBlockRegistryName() {
    return "dynamic_route_marker_sign";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDynamicRouteMarkerSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydynamicroutemarkersign";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityDynamicRouteMarkerSign();
  }

  /**
   * Opens the editor, and consumes the click on BOTH sides.
   *
   * <p>The GUI is client-only, but a {@code @SideOnly(Side.CLIENT)} override is absent from the
   * class on a server entirely, so the server would fall through to the default and use the held
   * item -- right-clicking to edit would place a block. Returning true on both sides is what the
   * other configurable signs do for the same reason.</p>
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, DOWNWARD, SHIFT, SHIELD);
  }

  /**
   * The base sign's shift and extension post, plus the marker this position is set to.
   *
   * <p>Falls back to the default marker when the tile entity is not there: this runs during
   * chunk load before tile entities are attached, and a missing one must not throw.</p>
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn, @NotNull BlockPos pos) {
    return super.getActualState(state, worldIn, pos).withProperty(SHIELD, shieldAt(worldIn, pos));
  }

  /**
   * The marker stored at this position, or the default when nothing is there to ask.
   *
   * @param source the block access
   * @param pos    the position
   *
   * @return the marker to paint
   *
   * @since 1.0
   */
  private static GuideSignShieldType shieldAt(IBlockAccess source, BlockPos pos) {
    if (source == null) {
      return GuideSignShieldType.INTERSTATE;
    }
    TileEntity tileEntity = source.getTileEntity(pos);
    if (tileEntity instanceof TileEntityDynamicRouteMarkerSign) {
      return ((TileEntityDynamicRouteMarkerSign) tileEntity).getShield();
    }
    return GuideSignShieldType.INTERSTATE;
  }
}
