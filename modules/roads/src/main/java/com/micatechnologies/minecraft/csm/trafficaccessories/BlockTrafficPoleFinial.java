package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole.TRAFFIC_POLE_COLOR;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * The decorative finial that caps a pedestal traffic pole: the cast ornament screwed onto the top
 * of a decorative post, in six styles from a small ball to a plain flat cap.
 *
 * <p><b>Why it is a block and not a state of the pole.</b> Six ornaments is six models, and a
 * per-property Forge blockstate has to name every property of the block it draws, so carrying the
 * choice on the pole would multiply its 864 states by seven for something only the top block of a
 * stack ever shows. A block of its own is also a thing a player can place, break and pick like
 * any other, and it leaves the pole with a rule it already had: something of its family sitting
 * beyond an end means end the tube flush, with no cap.
 *
 * <p>It lives in the air block <em>above</em> a pedestal pole's top, and its collar reaches back
 * down into that pole's block, over the tube, so the joint reads as one casting. That is also why
 * it only goes on a pedestal pole: the collar is cut to the 6-across pedestal tube, and on the
 * 8-across thin pole or the 12-across signal pole it would swallow nothing and float.
 *
 * <p>The style is stored (metadata 0-5). The colour is not: it is read from the pole below every
 * frame, the same way {@code AbstractBrightLightPoleColored} does it, so a finial always wears the
 * finish of the post it stands on and repainting the post repaints the finial.
 *
 * <p>Geometry, the blockstate and the lang and tab fragments come from
 * {@code dev-env-utils/scripts/gen_pole_finials.py}; nothing under {@code shared_models/
 * polefinial_*} is hand edited.
 *
 * @author Mica Technologies
 * @since 2026.9.17
 */
public class BlockTrafficPoleFinial extends AbstractBlock implements ICsmTrafficPoleIgnored {

  /**
   * The ornaments, in the order the Street Light Configuration Tool steps through them. Each
   * carries the height of its own casting, in sixteenths, for the selection box; the shapes
   * themselves live in the generator.
   */
  public enum Style implements IStringSerializable {
    /** A modest ball, the width of the tube itself. */
    BALL_SMALL("ball_small", "Small Ball", 9.5),
    /** The ball that reads from across the street. */
    BALL_LARGE("ball_large", "Large Ball", 11.5),
    /** Cup and nut, closing to a blunt tip. */
    ACORN("acorn", "Acorn", 10.5),
    /** A footed vase with a fluted belly and a flared lip. */
    URN("urn", "Fluted Urn", 10.5),
    /** A bead and a long spear point: the tallest of them. */
    SPIRE("spire", "Spire", 13.0),
    /** A wide, shallow disc, for a plain pedestrian post. */
    DISC("disc", "Flat Cap", 3.5);

    private final String name;
    private final String friendlyName;
    private final AxisAlignedBB boundingBox;

    Style(String name, String friendlyName, double height) {
      this.name = name;
      this.friendlyName = friendlyName;
      // Wide enough for the flat cap's rim, which is the widest casting of the six.
      this.boundingBox = new AxisAlignedBB(3.0 / 16.0, 0.0, 3.0 / 16.0,
          13.0 / 16.0, height / 16.0, 13.0 / 16.0);
    }

    @Override
    public String getName() {
      return name;
    }

    /** @return the name the configuration tool says in chat */
    public String getFriendlyName() {
      return friendlyName;
    }

    /** @return the selection box of this ornament */
    public AxisAlignedBB getBoundingBox() {
      return boundingBox;
    }

    /**
     * The style after this one, wrapping round to the first.
     *
     * @return the next style in the cycle
     */
    public Style next() {
      return values()[(ordinal() + 1) % values().length];
    }

    /**
     * The style stored in a block's metadata.
     *
     * @param meta the metadata value
     *
     * @return the style, or the first one if the value is out of range
     */
    public static Style fromMeta(int meta) {
      return meta >= 0 && meta < values().length ? values()[meta] : values()[0];
    }
  }

  /** Which ornament this finial is. Stored; metadata holds nothing else. */
  public static final PropertyEnum<Style> STYLE = PropertyEnum.create("style", Style.class);

  /**
   * The finish, taken from the pole below. Actual-state only, so it is never stored and never
   * disagrees with the post it stands on.
   */
  public static final PropertyEnum<TRAFFIC_POLE_COLOR> COLOR =
      PropertyEnum.create("color", TRAFFIC_POLE_COLOR.class);

  /** The finish a finial wears with no pole under it, which only the inventory icon sees. */
  private static final TRAFFIC_POLE_COLOR DEFAULT_COLOR = TRAFFIC_POLE_COLOR.SILVER;

  /**
   * Constructs a pole finial block.
   */
  public BlockTrafficPoleFinial() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    setDefaultState(getDefaultState().withProperty(STYLE, Style.BALL_SMALL)
        .withProperty(COLOR, DEFAULT_COLOR));
  }

  @Override
  public String getBlockRegistryName() {
    return "trafficpolefinial";
  }

  @Override
  @NotNull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, STYLE, COLOR);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(STYLE).ordinal();
  }

  @Override
  @NotNull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(STYLE, Style.fromMeta(meta));
  }

  /**
   * Takes the finish of the pole below, so the ornament matches its post without being told.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn, @NotNull BlockPos pos) {
    Block below = worldIn.getBlockState(pos.down()).getBlock();
    TRAFFIC_POLE_COLOR color = below instanceof AbstractBlockTrafficPole
        ? ((AbstractBlockTrafficPole) below).getTrafficPoleColor()
        : DEFAULT_COLOR;
    return state.withProperty(COLOR, color);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(STYLE).getBoundingBox();
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
  @NotNull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * A finial exists only on top of a pedestal pole.
   *
   * @param worldIn the world
   * @param pos     the position the finial would occupy
   *
   * @return {@code true} if an upright pedestal pole tops out directly below {@code pos}
   */
  @Override
  public boolean canPlaceBlockAt(@NotNull World worldIn, @NotNull BlockPos pos) {
    return super.canPlaceBlockAt(worldIn, pos) && canStandOn(worldIn, pos);
  }

  /**
   * Whether the block below {@code pos} is a pedestal pole standing upright, which is the only
   * thing a finial's collar fits over.
   *
   * @param worldIn the world
   * @param pos     the finial's position
   *
   * @return {@code true} if a finial belongs at {@code pos}
   */
  public static boolean canStandOn(IBlockAccess worldIn, BlockPos pos) {
    IBlockState below = worldIn.getBlockState(pos.down());
    return below.getBlock() instanceof BlockTrafficPolePedestal
        && below.getValue(AbstractBlockTrafficPole.FACING).getAxis() == EnumFacing.Axis.Y;
  }

  /**
   * Falls off as an item when the pole under it goes away, the way a torch or a sign does. The
   * casting has nothing to bolt to once the post is gone, and leaving it floating would be the
   * one way to have a finial without a pole.
   */
  @Override
  public void neighborChanged(@NotNull IBlockState state, @NotNull World worldIn,
      @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos) {
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    if (!worldIn.isRemote && !canStandOn(worldIn, pos)) {
      dropBlockAsItem(worldIn, pos, state, 0);
      worldIn.setBlockToAir(pos);
    }
  }
}
