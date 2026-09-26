package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A mile marker (MUTCD 2H.05 reference location sign): one of the eight D10 plates, with its
 * number set in world rather than baked into a texture, so one block serves every mile.
 *
 * <p>An ordinary {@link AbstractBlockSign} -- eight facings, the extension post, the setback
 * and back-to-back pairing, every sign post and mount -- whose plate is an ordinary model out of
 * its blockstate. What the tile entity holds reaches the model as the unlisted {@link #LEGEND}
 * property, not as block properties: a number is a thousand values and would multiply the
 * states by a thousand, where an unlisted property adds none. {@code MileMarkerBakedModel} then
 * adds the numerals (and on an enhanced plate the direction, route shield and route number) to
 * the plate's quads, baked into the chunk mesh like the plate, with no tile entity renderer.</p>
 *
 * <p>GUI id 37. See "Mile Markers" in {@code assets/docs/TRAFFIC_SIGNS.md}.</p>
 *
 * @since 2026.9
 */
public class BlockMileMarkerSign extends AbstractBlockSign implements ICsmTileEntityProvider {

  /** GUI id this block opens. */
  public static final int GUI_ID = 37;

  /** What the plate says, from the tile entity. */
  public static final IUnlistedProperty<MileMarkerLegend> LEGEND =
      new IUnlistedProperty<MileMarkerLegend>() {
        @Override
        public String getName() {
          return "legend";
        }

        @Override
        public boolean isValid(MileMarkerLegend value) {
          return true;
        }

        @Override
        public Class<MileMarkerLegend> getType() {
          return MileMarkerLegend.class;
        }

        @Override
        public String valueToString(MileMarkerLegend value) {
          return String.valueOf(value);
        }
      };

  /**
   * Carries the plate to {@link #getBlockRegistryName()}, which the superclass constructor calls
   * before this class's fields are assigned; see {@link BlockTrafficSign}.
   */
  private static final ThreadLocal<MileMarkerLayout> PENDING = new ThreadLocal<>();

  private final MileMarkerLayout layout;

  /**
   * One plate's block.
   *
   * @param layout the plate
   */
  public BlockMileMarkerSign(MileMarkerLayout layout) {
    this(stash(layout), layout);
  }

  private BlockMileMarkerSign(Void ignored, MileMarkerLayout layout) {
    this.layout = layout;
  }

  private static Void stash(MileMarkerLayout layout) {
    PENDING.set(layout);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return (layout != null ? layout : PENDING.get()).getRegistryName();
  }

  /** The plate this block draws. */
  public MileMarkerLayout getLayout() {
    return layout;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityMileMarkerSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitymilemarkersign";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityMileMarkerSign();
  }

  /**
   * Opens the editor, and consumes the click on both sides (see
   * {@link BlockDynamicRouteMarkerSign#onBlockActivated}).
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
    return new CsmExtendedBlockState(this, new IProperty[]{FACING, DOWNWARD, SHIFT},
        new IUnlistedProperty[]{LEGEND});
  }

  /**
   * The base sign's state with the legend its tile entity sets. Falls back to the plate's
   * default legend when the tile entity is not there yet, as during chunk load.
   */
  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    TileEntity tileEntity = world.getTileEntity(pos);
    MileMarkerLegend legend = tileEntity instanceof TileEntityMileMarkerSign
        ? ((TileEntityMileMarkerSign) tileEntity).legend(layout)
        : MileMarkerLegend.defaultFor(layout);
    return ((IExtendedBlockState) state).withProperty(LEGEND, legend);
  }
}
