package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.IEmergencyLightBlock;
import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetyGuiProvider;
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
import net.minecraft.util.EnumHand;
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
 * <p>On battery, emergency heads glow and throw a light cone forward, drawn by the emergency
 * lights' renderer from the bulbs {@link #getBulbs} places; the tile entity asks to be rendered only
 * then, so a sign without heads, or on mains power, costs nothing per frame.
 *
 * <p>The item carries the setup too (under {@link #ITEM_TAG}), so a sign that is broken,
 * pick-blocked or placed from a creative preset keeps it.
 *
 * @since 2026.9
 */
public abstract class AbstractBlockExitSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider, IEmergencyLightBlock {

  /** Mains power: true is normal operation, false is running on battery. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** The item stack tag holding a sign's setup. */
  public static final String ITEM_TAG = "csmExitSign";

  /** The light an emergency head throws while the sign is on battery. */
  private static final int HEADS_LIGHT = 15;

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

  /** Right-click opens the sign's setup screen. */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (worldIn.isRemote) {
      playerIn.openGui(Csm.instance, LifeSafetyGuiProvider.EXIT_SIGN_GUI_ID, worldIn, pos.getX(),
          pos.getY(), pos.getZ());
    }
    return true;
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

  // --- emergency heads' glow -----------------------------------------------------------------

  /** Lit on battery, and only with heads fitted. */
  @Override
  public boolean isEmergencyLightActive(IBlockAccess world, BlockPos pos, IBlockState state) {
    return getSpec().isMainsPowered() && !state.getValue(POWERED)
        && getConfig(world, pos).getHeads() != Heads.NONE;
  }

  @Override
  public EnumFacing getEmergencyLightFacing(IBlockState state) {
    return state.getValue(FACING);
  }

  /** The heads sit right beside the face; the emergency lights' wide cone would wash it out. */
  @Override
  public boolean hasNarrowCone() {
    return true;
  }

  /** The bulbs depend on the heads and the mount, so each pair is its own glow list. */
  @Override
  public int getGlowVariant(IBlockAccess world, BlockPos pos, IBlockState state) {
    ExitSignConfig config = getConfig(world, pos);
    return config.getHeads().ordinal() + Heads.values().length * config.getMount().ordinal();
  }

  /**
   * Each head's lens, as gen_exit_signs.py's {@code head_elements} places it: on an arm off each
   * end, centred on the face (or above each top corner if {@link #hasHeadsOnTop}), its lens a
   * little in front of the body. The glow's square core is kept inside a round lamp's lens disc.
   * An end mount has no head on its wall side.
   */
  @Override
  public float[][] getBulbs(IBlockAccess world, BlockPos pos, IBlockState state) {
    ExitSignConfig config = getConfig(world, pos);
    if (config.getHeads() == Heads.NONE) {
      return new float[0][];
    }
    boolean square = config.getHeads() == Heads.SQUARE;
    float front = config.getMount() == Mount.WALL ? 14 : 7;
    float lens = front - (square ? 0.75f : 0.5f);
    float r = square ? 1.5f : 1.1f;
    float bottom = (float) getFaceBottom();
    float top = bottom + (float) FACE_HEIGHT;
    boolean onTop = hasHeadsOnTop();
    float cx = onTop ? 14 : 18.25f;
    float cy = onTop ? top + 2 : bottom + (float) FACE_HEIGHT / 2;
    List<float[]> bulbs = new ArrayList<>();
    if (onTop || config.getMount() != Mount.END_LEFT) {
      bulbs.add(new float[]{cx - r, cy - r, lens, cx + r, cy + r, front});
    }
    if (onTop || config.getMount() != Mount.END_RIGHT) {
      bulbs.add(new float[]{16 - cx - r, cy - r, lens, 16 - cx + r, cy + r, front});
    }
    return bulbs.toArray(new float[0][]);
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

  /** The face's bottom edge in model pixels; the face is 10.5 px tall. */
  protected double getFaceBottom() {
    return 4.5;
  }

  /** Whether emergency heads sit above the top corners (true) or off the ends (false). */
  protected boolean hasHeadsOnTop() {
    return false;
  }

  /**
   * The fixture's outline, as the generator draws it (gen_exit_signs.py): facing north, as every
   * rotatable block's box is, and turned to the facing by the base class. A wall-mounted sign
   * sits against the block's south face, a hung one down its middle with its canopy up to the
   * ceiling; emergency heads widen it past the block's ends or raise it above the top corners.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    ExitSignConfig config = getConfig(source, pos);
    double bottom = getFaceBottom();
    double top = bottom + FACE_HEIGHT;
    boolean wall = config.getMount() == Mount.WALL;
    double minX = 0;
    double maxX = 16;
    if (config.getHeads() != Heads.NONE) {
      if (hasHeadsOnTop()) {
        top += HEAD_SIZE;
      } else {
        minX -= HEAD_SIZE;
        maxX += HEAD_SIZE;
      }
    }
    if (config.getMount() == Mount.CEILING) {
      top = 16;
    }
    return new AxisAlignedBB(minX / 16, bottom / 16, (wall ? 13.25 : 6.25) / 16, maxX / 16,
        top / 16, (wall ? 16 : 9.75) / 16);
  }

  /** The face's height in model pixels. */
  private static final double FACE_HEIGHT = 10.5;

  /** How far a lamp head and its arm stand off the sign, in model pixels. */
  private static final double HEAD_SIZE = 4;

  /**
   * The item model a stack with {@code config} shows: one per legend, letter colour, housing and
   * heads, written by gen_exit_signs.py under {@code models/item/}.
   */
  public String getItemModelName(ExitSignConfig config) {
    ExitSignConfig c = getSpec().clamp(config);
    return getBlockRegistryName() + "_" + c.getLegend().getName() + "_"
        + c.getLetters().getName() + "_" + c.getHousing().getName() + "_"
        + c.getHeads().getName();
  }

  /** Points the item at the icon for its stack's setup, so every preset looks like itself. */
  @Override
  public void registerModels() {
    ExitSignItemModels.register(this);
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
