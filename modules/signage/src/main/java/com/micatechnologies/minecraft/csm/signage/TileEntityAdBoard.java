package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An advertising board's controller: how big the board is, where the controller sits in it, and
 * what it shows. Nothing ticks -- which ad is up is worked out from the world time by
 * {@link AdRotation} -- and nothing here feeds a baked model, so a sync never rebuilds a chunk.
 */
public class TileEntityAdBoard extends AbstractTileEntity {

  /** The longest ad id or category a board keeps; the library's are far shorter. */
  static final int MAX_ID_LENGTH = 64;

  private static final String KEY_WIDTH = "w";
  private static final String KEY_HEIGHT = "h";
  private static final String KEY_ALIGN = "al";
  private static final String KEY_AD = "ad";
  private static final String KEY_ROTATION = "ro";
  private static final String KEY_CATEGORY = "ca";
  private static final String KEY_INTERVAL = "iv";
  private static final String KEY_FIT = "fi";
  private static final String KEY_LIGHT = "li";
  private static final String KEY_POWERED = "pw";
  private static final String KEY_BACK = "bk";
  private static final String KEY_TRANSITION = "tr";

  private int width = 1;
  private int height = 1;
  private AdBoardAlign align = AdBoardAlign.CENTRE;
  private String adId = "";
  private AdRotation rotation = AdRotation.SINGLE;
  private String category = "";
  private int interval = 20;
  private AdFit fit = AdFit.COVER;
  private AdLight light = AdLight.UNLIT;
  private boolean powered;
  private AdBack back = AdBack.NONE;
  private AdTransition transition = AdTransition.CUT;

  /** The ads the rotation draws from, rebuilt when the rotation or category changes. */
  private transient List<AdEntry> pool;
  private transient int poolGeneration;
  private transient AxisAlignedBB renderBox;

  /** The last shuffle worked out, for the round it is good for. */
  private transient long shuffleRound = Long.MIN_VALUE;
  private transient int[] shuffleOrder;

  /** The world's light in front of the face, looked up at most once a second. */
  @SideOnly(Side.CLIENT)
  private transient int lightmap;
  private transient long lightmapTick;
  private transient boolean lightmapKnown;

  /**
   * Gives a newly placed board an ad of its own: which one follows from where it is, so a street
   * of new boards is not one ad over and over, and the client that placed it guesses the same ad
   * the server picks.
   */
  void initialise(AdBoardKind kind, BlockPos at) {
    if (!adId.isEmpty()) {
      return;
    }
    List<AdEntry> ads = AdLibrary.get().rotation();
    adId = ads.isEmpty() ? AdLibrary.HOUSE_AD
        : ads.get((int) Math.floorMod(seed(at), (long) ads.size())).getId();
    // Paper is lit by the day; a screen, and a printed billboard's floodlights, by themselves.
    light = kind == AdBoardKind.WALL_POSTER ? AdLight.UNLIT
        : kind == AdBoardKind.BILLBOARD ? AdLight.NIGHT : AdLight.LIT;
    // A kiosk shows an ad each way along the pavement, and scrolls through them.
    back = kind.isFixedSize() ? AdBack.NEXT : AdBack.NONE;
    if (kind.isFixedSize()) {
      rotation = AdRotation.ALL_SHUFFLED;
    }
    // A screen changes ads often and fades between them; a kiosk scrolls, as a scroller does.
    if (kind == AdBoardKind.DIGITAL_BILLBOARD) {
      rotation = AdRotation.ALL_SHUFFLED;
      interval = 10;
      transition = AdTransition.FADE;
    } else if (kind.isFixedSize()) {
      transition = AdTransition.SLIDE;
    }
    pool = null;
  }

  /**
   * The board's own number, from its position: the SplitMix64 finaliser, which moves every bit.
   * A plain multiply was tried first and left the low bits alone for positions a power of two
   * apart, so two boards eight blocks apart started on the same ad.
   */
  static long seed(BlockPos at) {
    long z = at.toLong();
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return (z ^ (z >>> 31)) & Long.MAX_VALUE;
  }

  // --- size ---------------------------------------------------------------------------------

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public AdBoardAlign getAlign() {
    return align;
  }

  /** Which column of the board, from the left seen from the front, the controller is in. */
  public int getControllerColumn() {
    return align.controllerColumn(width);
  }

  /** Called by {@link AdBoards} once the blocks for a new size are in place. */
  void setSize(int width, int height, AdBoardAlign align) {
    this.width = width;
    this.height = height;
    this.align = align;
    renderBox = null;
  }

  // --- what it shows ------------------------------------------------------------------------

  public String getAdId() {
    return adId;
  }

  public AdRotation getRotation() {
    return rotation;
  }

  public String getCategory() {
    return category;
  }

  public int getInterval() {
    return interval;
  }

  public AdFit getFit() {
    return fit;
  }

  public AdLight getLight() {
    return light;
  }

  public boolean isPowered() {
    return powered;
  }

  public AdBack getBack() {
    return back;
  }

  public AdTransition getTransition() {
    return transition;
  }

  /** Called on the server with a setting its packet handler has already checked. */
  void setTransition(AdTransition transition) {
    this.transition = transition;
  }

  /** How long each ad is up, in ticks. */
  public long stepTicks() {
    return AdRotation.clampInterval(interval) * 20L;
  }

  /** Called on the server with a setting its packet handler has already checked. */
  void setBack(AdBack back) {
    this.back = back;
  }

  /**
   * The ad on the back at {@code worldTime}: the front's, or the one the front shows next -- for a
   * board showing a single ad, the ad after it in the library, so the two sides still differ.
   */
  public AdEntry showingBack(long worldTime) {
    if (back != AdBack.NEXT) {
      return showing(worldTime);
    }
    if (rotation == AdRotation.SINGLE) {
      List<AdEntry> ads = AdLibrary.get().rotation();
      int at = ads.indexOf(AdLibrary.get().resolve(adId));
      return ads.isEmpty() ? showing(worldTime) : ads.get((at + 1) % ads.size());
    }
    return showing(worldTime + AdRotation.clampInterval(interval) * 20L);
  }

  /** Called on the server with a configuration its packet handler has already checked. */
  void setAds(String adId, AdRotation rotation, String category, int interval, AdFit fit,
      AdLight light) {
    this.adId = adId;
    this.rotation = rotation;
    this.category = category;
    this.interval = AdRotation.clampInterval(interval);
    this.fit = fit;
    this.light = light;
    this.pool = null;
  }

  void setPowered(boolean powered) {
    if (this.powered != powered) {
      this.powered = powered;
      if (world != null) {
        markDirtySync(world, pos, true);
      }
    }
  }

  /** The ads this board's rotation draws from; the house ad if there are none. */
  List<AdEntry> pool() {
    AdLibrary library = AdLibrary.get();
    if (pool == null || poolGeneration != library.generation()) {
      poolGeneration = library.generation();
      List<AdEntry> ads;
      if (rotation == AdRotation.SINGLE) {
        ads = Collections.singletonList(library.resolve(adId));
      } else if (rotation.usesCategory()) {
        ads = library.inCategory(category);
      } else {
        ads = library.rotation();
      }
      if (ads.isEmpty()) {
        ads = Collections.singletonList(library.resolve(AdLibrary.HOUSE_AD));
      }
      pool = ads;
    }
    return pool;
  }

  /** The ad showing at {@code worldTime}. */
  public AdEntry showing(long worldTime) {
    List<AdEntry> ads = pool();
    int n = ads.size();
    if (n == 1 || rotation == AdRotation.SINGLE) {
      return ads.get(0);
    }
    long seed = seed(pos);
    if (!rotation.isShuffled()) {
      return ads.get(rotation.select(ads, worldTime, interval, seed));
    }
    // The same arithmetic as AdRotation.select, with the shuffle kept for the round it is good
    // for rather than made afresh every frame.
    long step = Math.max(0, worldTime) / (AdRotation.clampInterval(interval) * 20L);
    long round = step / n;
    if (round != shuffleRound || shuffleOrder == null || shuffleOrder.length != n) {
      shuffleOrder = AdRotation.shuffle(n, seed * 31 + round);
      shuffleRound = round;
    }
    return ads.get(shuffleOrder[(int) (step % n)]);
  }

  /** The combined light in front of the face's middle, refreshed once a second. */
  @SideOnly(Side.CLIENT)
  int lightmap() {
    long now = world.getTotalWorldTime();
    // A flag rather than a sentinel tick: now - Long.MIN_VALUE overflows, and the light was never
    // looked up at all, which drew every unlit board black.
    if (!lightmapKnown || now - lightmapTick >= 20 || now < lightmapTick) {
      IBlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof AbstractBlockAdBoard) {
        EnumFacing facing = state.getValue(AbstractBlockAdBoard.FACING);
        EnumFacing right = AbstractBlockAdBoard.right(facing);
        BlockPos middle = pos.offset(right, width / 2 - getControllerColumn())
            .up(height / 2).offset(facing);
        lightmap = world.getCombinedLight(middle, 0);
      }
      lightmapTick = now;
      lightmapKnown = true;
    }
    return lightmap;
  }

  // --- rendering bounds ---------------------------------------------------------------------

  @Override
  @Nonnull
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    if (renderBox == null) {
      IBlockState state = world.getBlockState(pos);
      if (!(state.getBlock() instanceof AbstractBlockAdBoard)) {
        return new AxisAlignedBB(pos);
      }
      EnumFacing right = AbstractBlockAdBoard.right(state.getValue(AbstractBlockAdBoard.FACING));
      int column = getControllerColumn();
      BlockPos a = pos.offset(right, -column);
      BlockPos b = pos.offset(right, width - 1 - column).up(height - 1);
      renderBox = new AxisAlignedBB(a).union(new AxisAlignedBB(b));
    }
    return renderBox;
  }

  /**
   * The long range every CSM renderer shares, plus the board's own size. The distance is measured
   * to the controller alone -- a board's bottom corner -- so without the size, a 40-block
   * billboard's face vanished while most of it stood well inside the range; the benchmark scene
   * showed five of ten billboards blank.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    double reach = LONG_RANGE_RENDER_DISTANCE + width + height;
    return reach * reach;
  }

  @Override
  protected long getBakedModelKey() {
    // The frame is drawn from the neighbours, not from anything here.
    return 0L;
  }

  // --- NBT ----------------------------------------------------------------------------------

  @Override
  public void readNBT(NBTTagCompound compound) {
    width = clamp(compound.getInteger(KEY_WIDTH), 1, 64);
    height = clamp(compound.getInteger(KEY_HEIGHT), 1, 64);
    align = AdBoardAlign.fromOrdinal(compound.getByte(KEY_ALIGN));
    adId = trim(compound.getString(KEY_AD));
    rotation = AdRotation.fromOrdinal(compound.getByte(KEY_ROTATION));
    category = trim(compound.getString(KEY_CATEGORY));
    interval = compound.hasKey(KEY_INTERVAL)
        ? AdRotation.clampInterval(compound.getShort(KEY_INTERVAL)) : 20;
    fit = AdFit.fromOrdinal(compound.getByte(KEY_FIT));
    light = AdLight.fromOrdinal(compound.getByte(KEY_LIGHT));
    powered = compound.getBoolean(KEY_POWERED);
    back = AdBack.fromOrdinal(compound.getByte(KEY_BACK));
    transition = AdTransition.fromOrdinal(compound.getByte(KEY_TRANSITION));
    pool = null;
    renderBox = null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_WIDTH, width);
    compound.setInteger(KEY_HEIGHT, height);
    compound.setByte(KEY_ALIGN, (byte) align.ordinal());
    compound.setString(KEY_AD, adId);
    compound.setByte(KEY_ROTATION, (byte) rotation.ordinal());
    compound.setString(KEY_CATEGORY, category);
    compound.setShort(KEY_INTERVAL, (short) interval);
    compound.setByte(KEY_FIT, (byte) fit.ordinal());
    compound.setByte(KEY_LIGHT, (byte) light.ordinal());
    compound.setBoolean(KEY_POWERED, powered);
    compound.setByte(KEY_BACK, (byte) back.ordinal());
    compound.setByte(KEY_TRANSITION, (byte) transition.ordinal());
    return compound;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private static String trim(String s) {
    return s.length() > MAX_ID_LENGTH ? s.substring(0, MAX_ID_LENGTH) : s;
  }
}
