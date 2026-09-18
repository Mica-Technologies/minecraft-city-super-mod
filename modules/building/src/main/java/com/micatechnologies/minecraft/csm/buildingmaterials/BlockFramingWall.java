package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A framed wall: studs in track, joining whatever framing of the same kind stands beside it.
 *
 * <h3>Why this is not {@link com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW}</h3>
 *
 * <p>That class orients a block's geometry AND its bounding box by {@code FACING}. A framed wall
 * is oriented by its neighbours instead, and its geometry is drawn from absolute compass
 * directions so one arm model can be rotated into all four (see {@link FramingJoins}). Inheriting
 * the facing rotation would turn that geometry a second time, which is the bounding-box fault the
 * pole family has hit before. This extends {@link AbstractBlock} and adds {@code FACING} itself.</p>
 *
 * <h3>Geometry</h3>
 *
 * <p>Post and arm, like a fence. A post at the centre of the block, and an arm from that post
 * toward each side that connects. A straight run is then posts and arms alternating, which reads
 * as studs at regular spacing; an L grows a real corner post where the two arms meet; a T grows
 * three arms off one post. A wall drawn instead as a panel down one axis cannot do corners at all:
 * two perpendicular runs in adjacent blocks are centred half a block apart and never meet.</p>
 *
 * <p>An isolated block has nothing to orient it, so it falls back to {@code FACING} and draws a
 * full panel along that axis.</p>
 *
 * <h3>State</h3>
 *
 * <p>512 states. {@code FACING} (4) and {@link FramingInsulation} (2) are stored, which is 8 of
 * the 16 metadata values; the six connections — four horizontal, plus up and down — are
 * actual-state only and cost no metadata.</p>
 *
 * @version 1.0
 * @see FramingJoins
 * @see ICsmFramingMember
 * @since 2026.9
 */
public abstract class BlockFramingWall extends AbstractBlock implements ICsmFramingMember {

  /**
   * Which way the block was placed.
   *
   * <p>Does NOT orient a connected wall — the connections do that. This is read only for the
   * isolated block, which has no neighbours to take an axis from, and by the asymmetric members
   * that come later (a door frame's hinge side, drywall hung on one face). Only its axis is used,
   * through {@code north|south} / {@code east|west} alternation in the blockstate.</p>
   *
   * @since 1.0
   */
  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /**
   * What is packed into the stud bays.
   *
   * @since 1.0
   */
  public static final PropertyEnum<FramingInsulation> INSULATION =
      PropertyEnum.create("insulation", FramingInsulation.class);

  /** Metadata bits 0-1 hold the facing; bits 2-3 hold the insulation. */
  private static final int FACING_MASK = 0b11;
  private static final int INSULATION_SHIFT = 2;
  private static final int INSULATION_MASK = 0b11;

  /**
   * Reads the insulation out of a metadata value, which is also an item's damage value.
   *
   * @param meta the metadata
   *
   * @return the insulation it encodes
   *
   * @since 1.0
   */
  public static FramingInsulation insulationFromMeta(int meta) {
    return FramingInsulation.fromBits((meta >> INSULATION_SHIFT) & INSULATION_MASK);
  }

  /**
   * The metadata that encodes an insulation on its own, with the facing left at zero. This is what
   * an item's damage value holds.
   *
   * @param insulation the insulation
   *
   * @return the metadata
   *
   * @since 1.0
   */
  public static int metaForInsulation(FramingInsulation insulation) {
    return insulation.ordinal() << INSULATION_SHIFT;
  }

  /**
   * Constructs a {@link BlockFramingWall}.
   *
   * @param material         the material of the block
   * @param soundType        the sound type of the block
   * @param harvestToolClass the harvest tool class of the block
   * @param harvestLevel     the harvest level of the block
   * @param hardness         the block's hardness
   * @param resistance       the block's resistance to explosions
   *
   * @since 1.0
   */
  protected BlockFramingWall(Material material, SoundType soundType, String harvestToolClass,
      int harvestLevel, float hardness, float resistance) {
    // Light opacity 0: a wall that has not been closed in yet does not block light, which is half
    // of what makes a building read as unfinished.
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, 0F, 0);
    setDefaultState(blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(INSULATION, FramingInsulation.NONE));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, INSULATION,
        FramingJoins.NORTH, FramingJoins.EAST, FramingJoins.SOUTH, FramingJoins.WEST,
        FramingJoins.UP, FramingJoins.DOWN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & FACING_MASK))
        .withProperty(INSULATION, insulationFromMeta(meta));
  }

  /**
   * Whether this member has stud bays that insulation can be packed into.
   *
   * <p>False by default. A rough opening has no bay between its king studs, and a floor runner has
   * no studs at all, so neither offers the choice — and neither should show an extra stack in the
   * creative tab that looks identical to the first.</p>
   *
   * @return {@code true} if every {@link FramingInsulation} value is meaningful here
   *
   * @since 1.0
   */
  public boolean supportsInsulation() {
    return false;
  }

  /**
   * An item that carries the insulation in its metadata, so each variant can be held, named and
   * placed. Without it the state would be reachable only through {@code /setblock}.
   *
   * @since 1.0
   */
  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBlockFramingWall(this);
  }

  /**
   * Shows one stack per insulation in the creative tab, for the members that have bays.
   *
   * @since 1.0
   */
  @Override
  @SideOnly(Side.CLIENT)
  public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
    if (!supportsInsulation()) {
      super.getSubBlocks(tab, items);
      return;
    }
    for (FramingInsulation insulation : FramingInsulation.values()) {
      items.add(new ItemStack(this, 1, metaForInsulation(insulation)));
    }
  }

  /**
   * Registers an item model for every insulation, not just for metadata zero.
   *
   * <p>The default registers one model for metadata zero alone, which leaves every other variant
   * showing the missing-texture chequer — which is exactly what the first build of this did. Each
   * insulation is pointed at a named variant of its own, so the stacks in the creative tab are
   * told apart by their icons and not only by their names.</p>
   *
   * @since 1.0
   */
  @Override
  public void registerModels() {
    if (!supportsInsulation()) {
      super.registerModels();
      return;
    }
    Item item = Item.getItemFromBlock(this);
    for (FramingInsulation insulation : FramingInsulation.values()) {
      String variant = insulation == FramingInsulation.NONE
          ? "inventory" : "inventory_" + insulation.getName();
      Csm.proxy.setCustomModelResourceLocation(item, metaForInsulation(insulation), variant);
    }
  }

  /**
   * Drops the wall with the insulation it was built with, rather than an empty one.
   *
   * @since 1.0
   */
  @Override
  public int damageDropped(IBlockState state) {
    return metaForInsulation(state.getValue(INSULATION));
  }

  /**
   * Middle-clicking a wall picks the variant actually there.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
    return new ItemStack(this, 1, damageDropped(state));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex()
        | (state.getValue(INSULATION).ordinal() << INSULATION_SHIFT);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState()
        .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(INSULATION, insulationFromMeta(meta));
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return FramingJoins.resolve(this, super.getActualState(state, worldIn, pos), worldIn, pos);
  }

  /**
   * A full cube. A framed wall is seen through but not walked through, which is what a real one
   * does — the studs are 16 in apart and a person does not fit between them at this scale.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
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
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
      @Nullable EnumFacing side) {
    return false;
  }

  /**
   * Cutout, so the open bays are seen through and the punched knockouts in the studs read as
   * holes. This is the whole point of the content.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
