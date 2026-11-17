package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.constructionsite.BuildingGuiProvider;
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
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * An address plaque for a house or small building (GitHub #364): a house number and an optional
 * street line on a plate hung flat on a wall, as stand-off numerals, as gold leaf on transom
 * glass, or in a lit number box. One class, constructed by registry name; what each block is
 * comes from its {@link PlaqueStyle}.
 *
 * <p><b>Stored:</b> the facing only, the way to the wall (S 0, W 1, N 2, E 3 in metadata, as the
 * room plate and the wall finishes): 4 states a block. Placed against the side of a block, it
 * hangs on that block; placed on a floor or ceiling, on the wall the player faces. Nothing checks
 * for a wall, so a {@code /setblock} with a metadata is exactly what a hand placement makes.</p>
 *
 * <p><b>The words</b> are the tile entity's ({@link TileEntityAddressPlaque}, NBT in
 * {@link AddressPlaque}), edited with an empty-handed click ({@link AddressPlaqueGui}) or a
 * {@code /blockdata}, and baked into the chunk by {@link PlaqueBakedModel}: nothing is drawn per
 * frame and nothing ticks.</p>
 *
 * <p><b>Thin.</b> The hitbox is the plate (or the numerals) and no deeper, it has no collision,
 * and it is never a full or solid face, so a plaque goes beside a door, under a stoop hood or on
 * a pier at any height without taking a block of depth from anything.</p>
 *
 * @since 2026.11
 */
public class BlockAddressPlaque extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /** What the plaque says, from its tile entity; absent before the tile entity has loaded. */
  public static final IUnlistedProperty<AddressPlaque> PLAQUE =
      new IUnlistedProperty<AddressPlaque>() {
        @Override
        public String getName() {
          return "plaque";
        }

        @Override
        public boolean isValid(AddressPlaque value) {
          return value != null;
        }

        @Override
        public Class<AddressPlaque> getType() {
          return AddressPlaque.class;
        }

        @Override
        public String valueToString(AddressPlaque value) {
          return value.toString();
        }
      };

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final PlaqueStyle style;

  /**
   * Constructs a plaque block.
   *
   * @param registryName one of the {@link PlaqueStyle} registry names
   */
  public BlockAddressPlaque(String registryName) {
    super(material(registryName), sound(registryName), "pickaxe", 0, 0.8F, 3F, 0F, 0);
    this.registryName = registryName;
    this.style = PlaqueStyle.byRegistryName(registryName);
    if (style == null) {
      throw new IllegalArgumentException("no plaque style " + registryName);
    }
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING.remove();
  }

  private static Material material(String registryName) {
    PENDING.set(registryName);
    if (registryName.endsWith("_stone")) {
      return Material.ROCK;
    }
    if (registryName.endsWith("_wood")) {
      return Material.WOOD;
    }
    if (registryName.endsWith("_decal")) {
      return Material.CIRCUITS;
    }
    return Material.IRON;
  }

  private static SoundType sound(String registryName) {
    if (registryName.endsWith("_stone")) {
      return SoundType.STONE;
    }
    if (registryName.endsWith("_wood")) {
      return SoundType.WOOD;
    }
    if (registryName.endsWith("_decal")) {
      return SoundType.GLASS;
    }
    return SoundType.METAL;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  public PlaqueStyle getStyle() {
    return style;
  }

  // --- state ---------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{FACING},
        new IUnlistedProperty[]{PLAQUE});
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

  @Nullable
  static AddressPlaque plaqueAt(IBlockAccess world, BlockPos pos) {
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    return te instanceof TileEntityAddressPlaque ? ((TileEntityAddressPlaque) te).getPlaque()
        : null;
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    AddressPlaque plaque = plaqueAt(world, pos);
    return plaque == null ? state : ((IExtendedBlockState) state).withProperty(PLAQUE, plaque);
  }

  // --- tile entity ---------------------------------------------------------------------------

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityAddressPlaque();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityAddressPlaque.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityaddressplaque";
  }

  // --- use -----------------------------------------------------------------------------------

  /** An empty-handed click opens the editor. */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !playerIn.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (worldIn.isRemote) {
      playerIn.openGui(Csm.instance, BuildingGuiProvider.ADDRESS_PLAQUE_GUI_ID, worldIn,
          pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  /** A lit lamp box gives light; read through the chunk, never creating a tile entity. */
  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    if (style != PlaqueStyle.LAMP_BOX) {
      return 0;
    }
    TileEntity te;
    if (world instanceof World) {
      World w = (World) world;
      te = w.isBlockLoaded(pos)
          ? w.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : null;
    } else {
      te = BlockUtils.getTileEntitySafe(world, pos);
    }
    return te instanceof TileEntityAddressPlaque
        && ((TileEntityAddressPlaque) te).getPlaque().isLit() ? PlaqueStyle.LAMP_LIGHT : 0;
  }

  // --- shape ---------------------------------------------------------------------------------

  /** The plate, or the numerals, from the same layout the model draws; the wall to the north. */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    AddressPlaque plaque = source == null ? null : plaqueAt(source, pos);
    float[] b = PlaqueLayout.layout(style, plaque == null ? AddressPlaque.DEFAULT : plaque).box;
    // the reader looks at the wall (north): their x is the model's x
    AxisAlignedBB north = new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0,
        b[4] / 16.0, b[5] / 16.0);
    return turn(north, state.getValue(FACING));
  }

  /** A box drawn with the wall to the north, turned to the block's wall (as the model turns). */
  static AxisAlignedBB turn(AxisAlignedBB box, EnumFacing facing) {
    switch (facing) {
      case SOUTH:
        return new AxisAlignedBB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY,
            1 - box.minZ);
      case EAST:
        return new AxisAlignedBB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY,
            box.maxX);
      case WEST:
        return new AxisAlignedBB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY,
            1 - box.minX);
      default:
        return box;
    }
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
