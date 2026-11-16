package com.micatechnologies.minecraft.csm.transit.shelter;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleBystander;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A bus shelter at real scale, drawn by {@code gen_transit_shelters.py}: glass and steel, a
 * cantilever canopy or a flat roof ({@link BusShelterStyle}), in one agency's livery each.
 *
 * <p>A shelter is built of columns. Each column is three blocks tall or more: a
 * {@link BusShelterSection#BOTTOM}, a {@link BusShelterSection#SECOND}, any number of
 * {@link BusShelterSection#MIDDLE} sections and the {@link BusShelterSection#TOP}, which carries
 * the roof, about 2.75 m up at three tall. Only the top is stored ({@link #SECTION} reads
 * {@code TOP} or {@code BOTTOM} from meta); the rest is actual state, worked out from the column.
 * A column is broken as one piece from any of its sections.</p>
 *
 * <p>The item ({@link ItemShelter}) places a whole shelter, {@link #FOOT_LENGTH} columns long
 * and {@link #FOOT_DEPTH} rows deep, the front row in the block clicked; sneaking, it places one
 * column. Switched to compact (a sneaking click in the air), it places {@link #COMPACT_LENGTH}
 * columns in one row instead, for a narrow pavement. Placed beside a shelter of the same block it takes that shelter's height, so the roof
 * carries on level. In survival the footprint takes an item a column, and one column is placed
 * when there are not enough. Clicking a shelter with the item makes every column of that shelter a section
 * taller; sneaking, a section shorter, down to three.</p>
 *
 * <p>Shelters join. Set side by side, facing the same way, they read as one shelter of any
 * length: {@link #LEFT} and {@link #RIGHT} say whether the same block continues on the sitter's
 * left and right, and end walls, end posts and the roof's end overhang are drawn only where it
 * does not. Set one in front of another, they make a deeper shelter: {@link #AHEAD} and
 * {@link #BEHIND} say whether it continues in front and behind, so the back wall and bench are
 * drawn only in the back row and the fascia only along the front.</p>
 *
 * <p>The roof light is lit when the shelter is placed. A click with an empty hand switches it,
 * and so does a change of redstone power at any section -- on when power comes, off when it
 * goes, the Residential lamps' pattern -- so a daylight sensor or a switch works it. Either
 * switches the whole shelter the block is part of, up to {@link #REACH} columns. Stored, in the
 * bits above the facing: whether this is the top (4), then {@link #LIT} in the top and
 * {@link #POWERED} (whether redstone powered the column when it last looked) in the bottom.</p>
 *
 * <p>The glass shelter leaves the sitter's left end of its back row open, in an empty frame two
 * blocks tall, for a Signage ad panel set against it. Nothing here knows about that panel: the
 * two meet only in the world.</p>
 *
 * @since 2026.9
 */
public class BlockBusShelter extends AbstractBlockRotatableNSEW
    implements ICsmTrafficPoleBystander, ICsmGlassFronted {

  /** Which section of its column this is; only TOP or BOTTOM is stored. */
  public static final PropertyEnum<BusShelterSection> SECTION =
      PropertyEnum.create("section", BusShelterSection.class);
  /** The roof light is on; stored in the top. */
  public static final PropertyBool LIT = PropertyBool.create("lit");
  /** Redstone powered the column when it last looked; stored in the bottom. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");
  /** The shelter continues to the sitter's left. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The shelter continues to the sitter's right. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** The shelter continues in front of this block: this is not its front row. */
  public static final PropertyBool AHEAD = PropertyBool.create("ahead");
  /** The shelter continues behind this block: this is not its back row. */
  public static final PropertyBool BEHIND = PropertyBool.create("behind");

  /** The fewest sections a column has. */
  public static final int MIN_HEIGHT = 3;
  /** The most sections a column grows to. */
  public static final int MAX_HEIGHT = 12;
  /** The columns a placed shelter runs along its length. */
  public static final int FOOT_LENGTH = 4;
  /** The rows a placed shelter runs front to back. */
  public static final int FOOT_DEPTH = 2;
  /** The columns a compact shelter runs along its length, one row deep. */
  public static final int COMPACT_LENGTH = 3;
  /** The most columns of one shelter a switch, or a change of height, reaches. */
  public static final int REACH = 256;

  /** The light a lit roof light gives. */
  private static final int LIGHT = 10;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  /** Set while a shelter moves its own sections, so a section removed does not take its column. */
  private static final ThreadLocal<Boolean> RESHAPING = ThreadLocal.withInitial(() -> false);

  private final String registryName;
  private final BusShelterStyle style;

  /**
   * Constructs a shelter.
   *
   * @param registryName its registry name
   * @param style        its style
   */
  public BlockBusShelter(String registryName, BusShelterStyle style) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 0.0F, 0);
    this.registryName = registryName;
    this.style = style;
    setDefaultState(getDefaultState().withProperty(SECTION, BusShelterSection.BOTTOM)
        .withProperty(LIT, false).withProperty(POWERED, false).withProperty(LEFT, false)
        .withProperty(RIGHT, false).withProperty(AHEAD, false).withProperty(BEHIND, false));
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

  /** The shelter's style. */
  public BusShelterStyle getStyle() {
    return style;
  }

  // --- state ---------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, SECTION, LIT, POWERED, LEFT, RIGHT, AHEAD,
        BEHIND);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    boolean top = (meta & 4) != 0;
    boolean bit = (meta & 8) != 0;
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(SECTION, top ? BusShelterSection.TOP : BusShelterSection.BOTTOM)
        .withProperty(LIT, top && bit).withProperty(POWERED, !top && bit);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    boolean top = isTop(state);
    boolean bit = top ? state.getValue(LIT) : state.getValue(POWERED);
    return state.getValue(FACING).getHorizontalIndex() | (top ? 4 : 0) | (bit ? 8 : 0);
  }

  private static boolean isTop(IBlockState state) {
    return state.getValue(SECTION) == BusShelterSection.TOP;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    boolean top = isTop(state);
    BusShelterSection section = BusShelterSection.TOP;
    if (!top) {
      if (!below(world, pos, facing)) {
        section = BusShelterSection.BOTTOM;
      } else {
        section = below(world, pos.down(), facing) ? BusShelterSection.MIDDLE
            : BusShelterSection.SECOND;
      }
    }
    return state.withProperty(SECTION, section)
        .withProperty(LEFT, continues(world, pos.offset(facing.rotateYCCW()), facing, top))
        .withProperty(RIGHT, continues(world, pos.offset(facing.rotateY()), facing, top))
        .withProperty(AHEAD, continues(world, pos.offset(facing), facing, top))
        .withProperty(BEHIND, continues(world, pos.offset(facing.getOpposite()), facing, top));
  }

  /** Whether the block under {@code pos} is a lower section of the same column. */
  private boolean below(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState under = world.getBlockState(pos.down());
    return under.getBlock() == this && under.getValue(FACING) == facing && !isTop(under);
  }

  /** Whether the same shelter, facing the same way, the same kind of section, is at {@code at}. */
  private boolean continues(IBlockAccess world, BlockPos at, EnumFacing facing, boolean top) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == facing && isTop(other) == top;
  }

  private boolean part(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState s = world.getBlockState(at);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  /** The bottom section of the column {@code pos} is in. */
  private BlockPos bottomOf(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    BlockPos at = pos;
    for (int i = 0; i < MAX_HEIGHT * 2 && below(world, at, facing); i++) {
      at = at.down();
    }
    return at;
  }

  /** The top section of the column whose bottom is {@code bottom}, or the highest one there. */
  private BlockPos topOf(IBlockAccess world, BlockPos bottom, EnumFacing facing) {
    BlockPos at = bottom;
    for (int i = 0; i < MAX_HEIGHT * 2; i++) {
      if (isTop(world.getBlockState(at)) || !part(world, at.up(), facing)) {
        return at;
      }
      at = at.up();
    }
    return at;
  }

  // --- placing, resizing and breaking ------------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return pos.getY() < world.getHeight() - MIN_HEIGHT && super.canPlaceBlockAt(world, pos)
        && free(world, pos.up()) && free(world, pos.up(2));
  }

  private static boolean free(World world, BlockPos at) {
    return world.getBlockState(at).getBlock().isReplaceable(world, at);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  /** Whether a column {@code height} tall fits at {@code base}. */
  private boolean columnFits(World world, BlockPos base, int height, EntityPlayer player,
      ItemStack stack) {
    if (base.getY() < 0 || base.getY() + height > world.getHeight()) {
      return false;
    }
    for (int i = 0; i < height; i++) {
      BlockPos at = base.up(i);
      if (!free(world, at) || !player.canPlayerEdit(at, EnumFacing.UP, stack)) {
        return false;
      }
    }
    return true;
  }

  /** Sets a column {@code height} tall at {@code base}, its light as given. */
  private void placeColumn(World world, BlockPos base, EnumFacing facing, boolean lit,
      int height) {
    IBlockState lower = getDefaultState().withProperty(FACING, facing);
    world.setBlockState(base, lower, 3);
    for (int i = 1; i < height - 1; i++) {
      world.setBlockState(base.up(i), lower, 3);
    }
    world.setBlockState(base.up(height - 1),
        lower.withProperty(SECTION, BusShelterSection.TOP).withProperty(LIT, lit), 3);
  }

  /** The light a new column takes: the shelter's it joins, or on. */
  private boolean joinedLight(World world, List<BlockPos> bases, EnumFacing facing) {
    for (BlockPos base : bases) {
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = base.offset(side);
        if (part(world, next, facing) && !below(world, next, facing)) {
          IBlockState top = world.getBlockState(topOf(world, next, facing));
          if (isTop(top)) {
            return top.getValue(LIT);
          }
        }
      }
    }
    return true;
  }

  /**
   * The height a new column takes: that of a shelter of this block, facing the same way, whose
   * bottom stands beside one of {@code bases}, so the roof carries on level; else the least.
   */
  private int joinedHeight(World world, List<BlockPos> bases, EnumFacing facing) {
    for (BlockPos base : bases) {
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = base.offset(side);
        if (bases.contains(next)) {
          continue;
        }
        if (part(world, next, facing) && !isTop(world.getBlockState(next))
            && !below(world, next, facing)) {
          return Math.max(MIN_HEIGHT, topOf(world, next, facing).getY() - next.getY() + 1);
        }
      }
    }
    return MIN_HEIGHT;
  }

  /** Every column of the shelter whose bottom is {@code bottom}: the bottoms, joined. */
  private List<BlockPos> shelterBottoms(World world, BlockPos bottom, EnumFacing facing) {
    List<BlockPos> out = new ArrayList<>();
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> open = new ArrayDeque<>();
    open.add(bottom);
    seen.add(bottom);
    while (!open.isEmpty() && out.size() < REACH) {
      BlockPos at = open.poll();
      out.add(at);
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = at.offset(side);
        if (!seen.contains(next) && world.isBlockLoaded(next) && part(world, next, facing)
            && !isTop(world.getBlockState(next)) && !below(world, next, facing)) {
          seen.add(next);
          open.add(next);
        }
      }
    }
    return out;
  }

  /**
   * Makes every column of the shelter at {@code pos} one section taller or shorter.
   *
   * @return the message for the player
   */
  private TextComponentTranslation resize(World world, BlockPos pos, IBlockState state,
      boolean taller) {
    EnumFacing facing = state.getValue(FACING);
    List<BlockPos> bottoms = shelterBottoms(world, bottomOf(world, pos, facing), facing);
    List<BlockPos> tops = new ArrayList<>();
    int height = 0;
    int lowest = Integer.MAX_VALUE;
    for (BlockPos bottom : bottoms) {
      BlockPos top = topOf(world, bottom, facing);
      tops.add(top);
      int h = top.getY() - bottom.getY() + 1;
      height = Math.max(height, h);
      lowest = Math.min(lowest, h);
    }
    if (taller) {
      if (height >= MAX_HEIGHT) {
        return new TextComponentTranslation("csm.transit.shelter.tallest", MAX_HEIGHT);
      }
      for (BlockPos top : tops) {
        if (top.getY() + 1 >= world.getHeight() || !free(world, top.up())) {
          BlockPos at = top.up();
          return new TextComponentTranslation("csm.transit.shelter.blocked", at.getX(),
              at.getY(), at.getZ());
        }
      }
    } else if (lowest <= MIN_HEIGHT) {
      return new TextComponentTranslation("csm.transit.shelter.lowest", MIN_HEIGHT);
    }
    RESHAPING.set(true);
    try {
      for (BlockPos top : tops) {
        IBlockState roof = world.getBlockState(top);
        if (!isTop(roof)) {
          continue;
        }
        IBlockState plain = roof.withProperty(SECTION, BusShelterSection.BOTTOM)
            .withProperty(LIT, false).withProperty(POWERED, false);
        if (taller) {
          world.setBlockState(top.up(), roof, 3);
          world.setBlockState(top, plain, 3);
        } else {
          world.setBlockState(top.down(), roof, 3);
          world.setBlockToAir(top);
        }
      }
    } finally {
      RESHAPING.set(false);
    }
    return new TextComponentTranslation(
        taller ? "csm.transit.shelter.taller" : "csm.transit.shelter.shorter",
        taller ? height + 1 : height - 1);
  }

  /** Breaking any section takes its whole column; the section broken drops the item. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!RESHAPING.get()) {
      EnumFacing facing = state.getValue(FACING);
      List<BlockPos> column = new ArrayList<>();
      if (!isTop(state)) {
        BlockPos at = pos.up();
        for (int i = 0; i < MAX_HEIGHT * 2 && part(world, at, facing); i++) {
          column.add(at);
          if (isTop(world.getBlockState(at))) {
            break;
          }
          at = at.up();
        }
      }
      BlockPos at = pos.down();
      for (int i = 0; i < MAX_HEIGHT * 2 && part(world, at, facing)
          && !isTop(world.getBlockState(at)); i++) {
        column.add(at);
        at = at.down();
      }
      RESHAPING.set(true);
      try {
        for (BlockPos p : column) {
          world.setBlockToAir(p);
        }
      } finally {
        RESHAPING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  @Override
  @Nonnull
  protected ItemBlock createItemBlock() {
    return new ItemShelter(this);
  }

  /**
   * The columns a shelter placed from {@code pos} stands on, its front row in {@code pos}: a full
   * shelter, {@link #FOOT_LENGTH} long and {@link #FOOT_DEPTH} deep, from one block on the
   * placer's left to two on their right; or a compact one, {@link #COMPACT_LENGTH} long and one
   * deep, from one block on the left to one on the right.
   *
   * @param pos     the block clicked into
   * @param facing  the shelter's facing (its open front, toward the placer)
   * @param compact whether it is the compact footprint
   *
   * @return the bottoms of its columns
   */
  static List<BlockPos> footprint(BlockPos pos, EnumFacing facing, boolean compact) {
    EnumFacing away = facing.getOpposite();      // from the front row to the back
    EnumFacing along = away.rotateY();           // the placer's right
    int length = compact ? COMPACT_LENGTH : FOOT_LENGTH;
    int depth = compact ? 1 : FOOT_DEPTH;
    List<BlockPos> bases = new ArrayList<>();
    for (int i = -1; i < length - 1; i++) {
      for (int row = 0; row < depth; row++) {
        bases.add(pos.offset(along, i).offset(away, row));
      }
    }
    return bases;
  }

  /**
   * The shelter's item: places a whole shelter (one column, sneaking), and makes a shelter
   * taller or shorter when used on one. A sneaking click in the air switches what it places
   * between a full shelter and a compact one ({@link #COMPACT_LENGTH} x 1, for a narrow
   * pavement), kept in the stack's NBT ({@code compact}) only while compact, so a stack of full
   * shelters still stacks with any other.
   */
  public static class ItemShelter extends ItemBlock {

    private static final String TAG_COMPACT = "compact";

    public ItemShelter(BlockBusShelter block) {
      super(block);
    }

    /** Whether this stack places the compact footprint. */
    static boolean isCompact(ItemStack stack) {
      NBTTagCompound t = stack.getTagCompound();
      return t != null && t.getBoolean(TAG_COMPACT);
    }

    /** Sets the stack's mode, dropping the tag (and an emptied compound) for the full one. */
    static void setCompact(ItemStack stack, boolean compact) {
      if (compact) {
        if (!stack.hasTagCompound()) {
          stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setBoolean(TAG_COMPACT, true);
      } else if (stack.hasTagCompound()) {
        stack.getTagCompound().removeTag(TAG_COMPACT);
        if (stack.getTagCompound().isEmpty()) {
          stack.setTagCompound(null);
        }
      }
    }

    /** A sneaking click in the air switches between the full and the compact footprint. */
    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
        @Nonnull EnumHand hand) {
      ItemStack stack = player.getHeldItem(hand);
      if (!player.isSneaking()) {
        return super.onItemRightClick(world, player, hand);
      }
      if (!world.isRemote) {
        boolean compact = !isCompact(stack);
        setCompact(stack, compact);
        player.sendStatusMessage(new TextComponentTranslation("csm.transit.shelter.mode",
            new TextComponentTranslation(compact ? "csm.transit.shelter.mode_compact"
                : "csm.transit.shelter.mode_full")), true);
      }
      return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
        @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
      super.addInformation(stack, world, tooltip, flag);
      tooltip.add(I18n.format("csm.transit.shelter.mode", I18n.format(isCompact(stack)
          ? "csm.transit.shelter.mode_compact" : "csm.transit.shelter.mode_full")));
      tooltip.add(I18n.format("csm.transit.shelter.mode_hint"));
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
        EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
      IBlockState clicked = world.getBlockState(pos);
      if (clicked.getBlock() == block) {
        if (!world.isRemote) {
          player.sendStatusMessage(((BlockBusShelter) block)
              .resize(world, pos, clicked, !player.isSneaking()), true);
        }
        return EnumActionResult.SUCCESS;
      }
      return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      BlockBusShelter shelter = (BlockBusShelter) block;
      EnumFacing facing = newState.getValue(FACING);
      List<BlockPos> bases = new ArrayList<>();
      boolean creative = player.capabilities.isCreativeMode;
      List<BlockPos> whole = footprint(pos, facing, isCompact(stack));
      int footprint = whole.size();
      if (!player.isSneaking() && (creative || stack.getCount() >= footprint)) {
        bases.addAll(whole);
        int height = shelter.joinedHeight(world, bases, facing);
        for (BlockPos base : bases) {
          if (!shelter.columnFits(world, base, height, player, stack)) {
            if (!world.isRemote) {
              player.sendStatusMessage(new TextComponentTranslation(
                  "csm.transit.shelter.no_room", base.getX(), base.getY(), base.getZ()), true);
            }
            return false;
          }
        }
      } else {
        bases.add(pos);
        if (!shelter.columnFits(world, pos, shelter.joinedHeight(world, bases, facing), player,
            stack)) {
          return false;
        }
        if (!player.isSneaking() && !world.isRemote) {
          player.sendStatusMessage(new TextComponentTranslation(
              "csm.transit.shelter.one_column", footprint), true);
        }
      }
      if (world.isRemote) {
        return true;
      }
      boolean lit = shelter.joinedLight(world, bases, facing);
      int height = shelter.joinedHeight(world, bases, facing);
      for (BlockPos base : bases) {
        shelter.placeColumn(world, base, facing, lit, height);
      }
      if (!creative && bases.size() > 1) {
        stack.shrink(bases.size() - 1);  // onItemUse takes the last one
      }
      return true;
    }
  }

  // --- the roof light ------------------------------------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return isTop(state) && state.getValue(LIT) ? LIGHT : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  /** A click with an empty hand switches the shelter's light. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing facing = state.getValue(FACING);
      BlockPos top = topOf(world, bottomOf(world, pos, facing), facing);
      IBlockState roof = world.getBlockState(top);
      boolean lit = roof.getBlock() == this && isTop(roof) && !roof.getValue(LIT);
      switchShelter(world, top, lit);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, lit ? 0.6F : 0.5F);
    }
    return true;
  }

  /**
   * A change of redstone power at any section switches the shelter: on when power comes, off
   * when it goes. The bottom remembers the power it last saw.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote || RESHAPING.get()) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    BlockPos bottomPos = bottomOf(world, pos, facing);
    IBlockState bottom = world.getBlockState(bottomPos);
    if (bottom.getBlock() != this || isTop(bottom)) {
      return;
    }
    BlockPos topPos = topOf(world, bottomPos, facing);
    boolean powered = false;
    for (BlockPos at = bottomPos; at.getY() <= topPos.getY() && !powered; at = at.up()) {
      powered = world.isBlockPowered(at);
    }
    if (powered != bottom.getValue(POWERED)) {
      world.setBlockState(bottomPos, bottom.withProperty(POWERED, powered), 2);
      switchShelter(world, topPos, powered);
    }
  }

  /**
   * Sets the light of every top of the shelter that {@code top} is part of: the same block,
   * facing the same way, joined side to side or front to back, up to {@link #REACH}.
   */
  private void switchShelter(World world, BlockPos top, boolean lit) {
    IBlockState start = world.getBlockState(top);
    if (start.getBlock() != this || !isTop(start)) {
      return;
    }
    EnumFacing facing = start.getValue(FACING);
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> open = new ArrayDeque<>();
    open.add(top);
    seen.add(top);
    while (!open.isEmpty() && seen.size() <= REACH) {
      BlockPos at = open.poll();
      IBlockState here = world.getBlockState(at);
      if (here.getValue(LIT) != lit) {
        world.setBlockState(at, here.withProperty(LIT, lit), 3);
      }
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = at.offset(side);
        if (!seen.contains(next) && world.isBlockLoaded(next)
            && continues(world, next, facing, true)) {
          seen.add(next);
          open.add(next);
        }
      }
    }
  }

  // --- shape -------------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  /** The walls, bench, posts and roof of this section, as {@link BusShelterStyle} lists them. */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(world, pos);
    EnumFacing facing = actual.getValue(FACING);
    for (AxisAlignedBB box : style.collision(actual.getValue(SECTION), actual.getValue(LEFT),
        actual.getValue(RIGHT), actual.getValue(AHEAD), actual.getValue(BEHIND))) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(box, facing));
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
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
    return style != null ? style.getLayer() : BlockRenderLayer.TRANSLUCENT;
  }

  /**
   * A glass shelter draws its glass apart from its frame and seat.
   */
  @Override
  public boolean isGlassFronted() {
    return getBlockRenderLayer() == BlockRenderLayer.TRANSLUCENT;
  }
}
