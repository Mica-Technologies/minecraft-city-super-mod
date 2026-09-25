package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.PlatformSigns;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformSign;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
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
 * The subway entrance kiosk's roof, one block at a time, in an agency's livery: a deck at the
 * foot of its block (so it rests on the glass walls below), a soffit with a light (always on,
 * light 11), and the agency's fascia on each side where no roof of any agency continues
 * ({@link #NORTH} to {@link #WEST}, actual state, world sides).
 *
 * <p>A click with an empty hand steps the block's name board ({@link #LEGEND}): none, SUBWAY,
 * METRO, then the station names ({@link StationLegends}); a sneaking click steps back. The board
 * is drawn on every side of the block where the fascia is, so set it on the blocks along the
 * kiosk's front. The value lives in the platform signs' {@link TileEntityPlatformSign}. A click
 * holding anything is left to the item, so a block can still be placed against the roof.</p>
 *
 * @since 2026.9
 */
public class BlockStationEntranceRoof extends AbstractBlock implements ICsmTileEntityProvider {

  /** How many values the board steps through: none, then each legend. */
  public static final int COUNT = 1 + StationLegends.LEGENDS.length;

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyInteger LEGEND = PropertyInteger.create("legend", 1, COUNT);

  private static final AxisAlignedBB DECK = new AxisAlignedBB(0, 0, 0, 1, 4 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a roof.
   *
   * @param registryName its registry name, ending in its agency
   */
  public BlockStationEntranceRoof(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.5F, 8.0F, 11 / 15.0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(NORTH, false)
        .withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false)
        .withProperty(LEGEND, 1));
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
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST, LEGEND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, isRoof(world, pos.north()))
        .withProperty(EAST, isRoof(world, pos.east()))
        .withProperty(SOUTH, isRoof(world, pos.south()))
        .withProperty(WEST, isRoof(world, pos.west()))
        .withProperty(LEGEND, PlatformSigns.valueAt(world, pos, COUNT));
  }

  private static boolean isRoof(IBlockAccess world, BlockPos at) {
    return world.getBlockState(at).getBlock() instanceof BlockStationEntranceRoof;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      StationLegends.step(world, pos, player, COUNT, true);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return DECK;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformSign.class;
  }

  @Override
  public String getTileEntityName() {
    return PlatformSigns.TILE_ENTITY_NAME;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformSign();
  }
}
