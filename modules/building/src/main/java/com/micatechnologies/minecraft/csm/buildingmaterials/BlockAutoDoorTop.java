package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Half;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Hinge;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmGlassLayer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The third block of a 3-block-tall automatic sliding door ({@code auto_door_tall_top_<finish>}):
 * the top of the panel and the operator header, above a tall slider's or sidelite's upper half.
 *
 * <p>It keeps no state of its own. Its actual state is the door's below it -- what kind of door
 * ({@link #KIND}), which cell of a leaf, its facing, hinge, whether it is open or moving -- and its
 * models are the 2-tall doors' upper-half models, so a tall door's top is the same picture as a
 * short door's head. The tall door places it ({@link BlockAutoSlidingDoor#onBlockPlacedBy}) and
 * takes it away; it goes by itself if the door below it goes, and breaking it breaks the door. A
 * click on it is a click on the door. While the door moves, the slide renderer draws its panel
 * part moving with the rest (its baked model is drawn only at rest, as the door's is).</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockAutoDoorTop extends AbstractBlock implements ICsmGlassFronted {

  /** What the door below it is. */
  public enum Kind implements IStringSerializable {
    NONE, SLIDER, WIDE, TELE, SIDELITE;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyEnum<Kind> KIND = PropertyEnum.create("kind", Kind.class);

  /** SHARED with gen_doors.HEADER_Z / TELE_HEADER_Z: the header's depth, in blocks. */
  private static final double HEADER_Z0 = 2.5 / 16;
  private static final double TELE_HEADER_Z0 = 0.5 / 16;
  private static final double HEADER_Z1 = 9.5 / 16;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockAutoDoorTop}.
   *
   * @param registryName {@code auto_door_tall_top_<finish>}
   *
   * @since 1.0
   */
  public BlockAutoDoorTop(String registryName) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 0, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(KIND, Kind.NONE)
        .withProperty(BlockBuildingDoor.FACING, EnumFacing.NORTH)
        .withProperty(BlockAutoSlidingDoorMulti.CELL, 0)
        .withProperty(BlockBuildingDoor.HINGE, Hinge.LEFT)
        .withProperty(BlockBuildingDoor.OPEN, false)
        .withProperty(BlockBuildingDoor.SWING, false));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /**
   * The finish, the last word of the name: the tall doors of a finish place the top of it.
   *
   * @return the finish
   *
   * @since 1.0
   */
  public String finish() {
    String n = getBlockRegistryName();
    return n.substring(n.lastIndexOf('_') + 1);
  }

  // --- state ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, BlockBuildingDoor.FACING, KIND,
        BlockAutoSlidingDoorMulti.CELL, BlockBuildingDoor.HINGE, BlockBuildingDoor.OPEN,
        BlockBuildingDoor.SWING);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  /** The upper half of an automatic door below {@code pos}, or null. */
  @Nullable
  private static IBlockState doorBelow(IBlockAccess world, BlockPos pos) {
    IBlockState below = world.getBlockState(pos.down());
    return below.getBlock() instanceof BlockAutoSlidingDoor
        && below.getValue(BlockBuildingDoor.HALF) == Half.UPPER ? below : null;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    IBlockState below = doorBelow(worldIn, pos);
    if (below == null) {
      return getDefaultState();
    }
    BlockAutoSlidingDoor block = (BlockAutoSlidingDoor) below.getBlock();
    IBlockState door = below.getActualState(worldIn, pos.down());
    Kind kind = block.fixed() ? Kind.SIDELITE
        : block instanceof BlockAutoSlidingDoorMulti
            ? ((BlockAutoSlidingDoorMulti) block).wide() ? Kind.WIDE : Kind.TELE : Kind.SLIDER;
    return getDefaultState().withProperty(KIND, kind)
        .withProperty(BlockBuildingDoor.FACING, door.getValue(BlockBuildingDoor.FACING))
        .withProperty(BlockAutoSlidingDoorMulti.CELL, kind == Kind.WIDE || kind == Kind.TELE
            ? door.getValue(BlockAutoSlidingDoorMulti.CELL) : 0)
        .withProperty(BlockBuildingDoor.HINGE, door.getValue(BlockBuildingDoor.HINGE))
        .withProperty(BlockBuildingDoor.OPEN, door.getValue(BlockBuildingDoor.OPEN))
        .withProperty(BlockBuildingDoor.SWING, door.getValue(BlockBuildingDoor.SWING));
  }

  // --- the door below ---------------------------------------------------------------------------

  /** It goes when the door below it goes. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (doorBelow(worldIn, pos) == null) {
      worldIn.setBlockToAir(pos);
    }
  }

  /** Breaking it breaks the door, which drops from its lower half as ever. */
  @Override
  public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (!worldIn.isRemote && doorBelow(worldIn, pos) != null) {
      worldIn.destroyBlock(pos.down(2), !player.capabilities.isCreativeMode);
    }
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    IBlockState below = doorBelow(worldIn, pos);
    return below != null && below.getBlock().onBlockActivated(worldIn, pos.down(), below, playerIn,
        hand, facing, hitX, hitY, hitZ);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    IBlockState below = doorBelow(world, pos);
    return below == null ? ItemStack.EMPTY : new ItemStack(below.getBlock());
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public EnumPushReaction getPushReaction(IBlockState state) {
    return EnumPushReaction.DESTROY;
  }

  // --- shape ------------------------------------------------------------------------------------

  /** The header's depth across the cell: the panel's top is inside it. */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState top = getActualState(state, source, pos);
    double z0 = top.getValue(KIND) == Kind.TELE ? TELE_HEADER_Z0 : HEADER_Z0;
    AxisAlignedBB north = new AxisAlignedBB(0, 0, z0, 1, 1, HEADER_Z1);
    return BlockGarageDoor.turn(north, top.getValue(BlockBuildingDoor.FACING));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
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
    return BlockRenderLayer.TRANSLUCENT;
  }

  /** Glass in the translucent pass, frame and header in the cutout pass, as the door's. */
  @Override
  public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
    return CsmGlassLayer.drawsIn(layer);
  }
}
