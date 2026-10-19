package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A room sign beside a door: a plate with the room's number over its name. One class,
 * constructed by registry name ({@code room_plate_<colour>}).
 *
 * <p>Stored: the facing only (the way to the wall, horizontal index S0 W1 N2 E3). The legend is
 * the tile entity's ({@link TileEntityRoomPlate}: {@code num}, {@code name}), set with
 * {@code /blockdata}, and drawn by the plate's baked model from one glyph sheet
 * ({@link RoomPlateBakedModel}), baked into the chunk once per number and name; nothing is drawn
 * per frame, and the tile entity is never ticked. A click cycles the name and a sneak-click steps
 * the number by one, for building by hand. 4 states.</p>
 *
 * <p>Why a tile entity and a baked model rather than metadata or a renderer: a number and a name
 * are some 90,000 combinations, far past the four bits of metadata left after the facing and
 * past any sane state count, and a renderer would draw every plate in a hospital every frame.
 * A data-only tile entity costs a few bytes a plate and nothing a frame.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockRoomPlate extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /** The legend, from the tile entity: number * 16 + name, or null before it has loaded. */
  public static final IUnlistedProperty<Integer> LEGEND = new IUnlistedProperty<Integer>() {
    @Override
    public String getName() {
      return "legend";
    }

    @Override
    public boolean isValid(Integer value) {
      return value != null;
    }

    @Override
    public Class<Integer> getType() {
      return Integer.class;
    }

    @Override
    public String valueToString(Integer value) {
      return String.valueOf(value);
    }
  };

  /** SHARED with gen_wall_finishes.PLATE and PLATE_D. */
  private static final AxisAlignedBB NORTH = new AxisAlignedBB(1 / 16.0, 2 / 16.0, 0, 15 / 16.0,
      14 / 16.0, 0.5 / 16);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockRoomPlate}.
   *
   * @param registryName {@code room_plate_<colour>}
   *
   * @since 1.0
   */
  public BlockRoomPlate(String registryName) {
    super(pending(registryName), SoundType.METAL, null, 0, 0.5F, 2F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pending(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.CIRCUITS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{FACING},
        new IUnlistedProperty[]{LEGEND});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /** Against the wall clicked; on a floor or ceiling, the wall the player faces. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing wall = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : placer.getHorizontalFacing();
    return getDefaultState().withProperty(FACING, wall);
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    if (!(te instanceof TileEntityRoomPlate)) {
      return state;
    }
    TileEntityRoomPlate plate = (TileEntityRoomPlate) te;
    return ((IExtendedBlockState) state).withProperty(LEGEND,
        plate.getNumber() * 16 + plate.getName());
  }

  /**
   * A click shows the next room name; a sneak-click the next room number.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !playerIn.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!worldIn.isRemote) {
      TileEntity te = worldIn.getTileEntity(pos);
      if (te instanceof TileEntityRoomPlate) {
        TileEntityRoomPlate plate = (TileEntityRoomPlate) te;
        if (playerIn.isSneaking()) {
          int next = plate.getNumber() >= RoomPlateGlyphs.MAX_NUMBER ? -1
              : plate.getNumber() + 1;
          plate.set(next, plate.getName());
        } else {
          plate.set(plate.getNumber(), (plate.getName() + 1) % RoomPlateGlyphs.NAMES.length);
        }
        plate.markDirtySync(worldIn, pos, state, true);
      }
    }
    return true;
  }

  // --- tile entity ------------------------------------------------------------------------------

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityRoomPlate();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRoomPlate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityroomplate";
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(NORTH, state.getValue(FACING));
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
}
