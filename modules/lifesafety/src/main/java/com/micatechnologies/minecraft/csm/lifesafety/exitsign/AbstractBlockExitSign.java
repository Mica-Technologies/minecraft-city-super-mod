package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An exit sign whose arrows, letter colour, housing, mount, emergency heads and legend are chosen
 * after it is placed, instead of one block per combination.
 *
 * <p>Metadata holds only what changes without the player: the facing (the way the legend reads
 * out) and, for a mains-powered sign, {@link #POWERED}. The setup lives in a
 * {@link TileEntityExitSign} and reaches the model through {@link #getActualState}, where each
 * option the block's {@link ExitSignSpec} offers is a property the multipart blockstate picks
 * parts with. Nothing about the sign is drawn by a renderer.
 *
 * <p>Redstone is mains power, as it is for the emergency lights: powered is normal operation,
 * unpowered is the battery taking over, which lights the emergency heads. The legend is lit
 * either way, as on a real sign.
 *
 * <p>The item carries the setup too (under {@link #ITEM_TAG}), so a sign that is broken,
 * pick-blocked or placed from a creative preset keeps it.
 *
 * @since 2026.9
 */
public abstract class AbstractBlockExitSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** Mains power: true is normal operation, false is running on battery. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** The item stack tag holding a sign's setup. */
  public static final String ITEM_TAG = "csmExitSign";

  /** The light an emergency head throws while the sign is on battery. */
  private static final int HEADS_LIGHT = 15;

  private static final AxisAlignedBB WALL_BOX =
      new AxisAlignedBB(0.0, 0.25, 0.8125, 1.0, 0.9375, 1.0);
  private static final AxisAlignedBB HUNG_BOX =
      new AxisAlignedBB(0.0, 0.25, 0.40625, 1.0, 1.0, 0.59375);

  protected AbstractBlockExitSign() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, false);
    IBlockState base = this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    if (getSpec().isMainsPowered()) {
      base = base.withProperty(POWERED, false);
    }
    setDefaultState(getSpec().apply(base, getSpec().getDefaults()));
  }

  /**
   * What this sign offers. Called from the {@code Block} constructor, so it must return a static
   * field, never an instance one.
   */
  public abstract ExitSignSpec getSpec();

  // --- state --------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    List<IProperty<?>> properties = new ArrayList<>();
    properties.add(FACING);
    if (getSpec().isMainsPowered()) {
      properties.add(POWERED);
    }
    properties.addAll(getSpec().properties());
    return new BlockStateContainer(this, properties.toArray(new IProperty<?>[0]));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    IBlockState state = getDefaultState()
        .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    return getSpec().isMainsPowered() ? state.withProperty(POWERED, (meta & 4) != 0) : state;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex();
    if (getSpec().isMainsPowered() && state.getValue(POWERED)) {
      meta |= 4;
    }
    return meta;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return getSpec().apply(state, getConfig(worldIn, pos));
  }

  /**
   * The sign's setup at {@code pos}, clamped to what this block offers; the block's defaults if
   * the tile entity is not there. Safe off the main thread: a chunk being rendered is read
   * without creating a tile entity in it.
   */
  public ExitSignConfig getConfig(IBlockAccess world, BlockPos pos) {
    TileEntity te = world instanceof ChunkCache
        ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
        : world.getTileEntity(pos);
    return te instanceof TileEntityExitSign
        ? getSpec().clamp(((TileEntityExitSign) te).getConfig())
        : getSpec().getDefaults();
  }

  // --- placement ----------------------------------------------------------------------------

  /**
   * Faces away from the wall it is placed against, or toward the player if placed on a ceiling
   * or floor.
   */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing out = facing.getAxis().isHorizontal()
        ? facing : placer.getHorizontalFacing().getOpposite();
    IBlockState state = getDefaultState().withProperty(FACING, out);
    if (getSpec().isMainsPowered()) {
      state = state.withProperty(POWERED, worldIn.getRedstonePowerFromNeighbors(pos) > 0);
    }
    return state;
  }

  /**
   * The mount a sign placed against {@code side} of a block gets: the ceiling if it was placed
   * under one, otherwise the wall. End mounts are only ever chosen in the sign's screen.
   */
  static Mount mountForPlacement(EnumFacing side) {
    return side == EnumFacing.DOWN ? Mount.CEILING : Mount.WALL;
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (!getSpec().isMainsPowered()) {
      return;
    }
    boolean powered = world.getRedstonePowerFromNeighbors(pos) > 0;
    if (state.getValue(POWERED) != powered) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
    }
  }

  // --- light --------------------------------------------------------------------------------

  /**
   * The sign's own glow, or full light while it is on battery with emergency heads fitted.
   */
  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    if (state.getBlock() != this) {
      return super.getLightValue(state, world, pos);
    }
    if (getSpec().isMainsPowered() && !state.getValue(POWERED)
        && getConfig(world, pos).getHeads() != Heads.NONE) {
      return HEADS_LIGHT;
    }
    return getSpec().getLightValue();
  }

  // --- the item keeps the setup ---------------------------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBlockExitSign(this);
  }

  /** A stack of this sign carrying {@code config}. */
  public ItemStack stackFor(ExitSignConfig config) {
    ItemStack stack = new ItemStack(this);
    NBTTagCompound tag = new NBTTagCompound();
    tag.setTag(ITEM_TAG, getSpec().clamp(config).write(new NBTTagCompound()));
    stack.setTagCompound(tag);
    return stack;
  }

  /** The setup a stack carries, or this block's defaults if it carries none. */
  public ExitSignConfig configOf(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null || !tag.hasKey(ITEM_TAG)) {
      return getSpec().getDefaults();
    }
    return getSpec().clamp(ExitSignConfig.read(tag.getCompoundTag(ITEM_TAG),
        getSpec().getDefaults()));
  }

  /** One stack per preset, so the creative tab shows the common setups ready to place. */
  @Override
  @SideOnly(Side.CLIENT)
  public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
    for (ExitSignConfig preset : getSpec().getPresets()) {
      items.add(stackFor(preset));
    }
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
    return stackFor(getConfig(world, pos));
  }

  /**
   * Keeps the block, and so its tile entity, in the world until it has been harvested, so the
   * drop can read the setup. {@link #harvestBlock} removes it afterwards.
   */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
    if (willHarvest) {
      return true;
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  @Override
  public void harvestBlock(@Nonnull World worldIn, @Nonnull EntityPlayer player,
      @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable TileEntity te,
      @Nonnull ItemStack stack) {
    super.harvestBlock(worldIn, player, pos, state, te, stack);
    worldIn.setBlockToAir(pos);
  }

  @Override
  public void getDrops(@Nonnull NonNullList<ItemStack> drops, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune) {
    drops.add(stackFor(getConfig(world, pos)));
  }

  // --- tile entity --------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityExitSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityexitsign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityExitSign();
  }

  // --- shape --------------------------------------------------------------------------------

  /**
   * Drawn facing north, as every rotatable block's box is; the base class turns it to the facing.
   * A wall-mounted sign sits against the block's south face; a hung one down its middle.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return getConfig(source, pos).getMount() == Mount.WALL ? WALL_BOX : HUNG_BOX;
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
    return getSpec().isMainsPowered();
  }

  /** Cutout, not translucent: the rounded housings' corners are the only alpha on the face. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
