package com.micatechnologies.minecraft.csm.venues.courts;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A net laid as a run of blocks across a court ({@code tennis_net}, {@code pickleball_net},
 * {@code volleyball_net}), the net in the middle of its cells: a post where the run stops, and
 * what lies between worked out from the run. A tennis net sags from 1.07 m at the posts to
 * 0.914 m in the middle, where its centre strap is; a pickleball net from 36 to 34 in; a
 * volleyball net is a 1 m band, its top at 2.43 m (men's) or 2.24 m (women's), with an antenna
 * over each sideline, the edge of the block next to each post.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3; the run goes across it, the net
 * faces it); for the volleyball net bit 2 the women's height. A sneak-click with an empty hand on
 * a volleyball net switches its height. Actual state: {@code end_l}, {@code end_r} (the run stops
 * there: a post), {@code sag} (0 at the posts to 3 in the middle; 4 the middle block, with the
 * strap) and, for the volleyball net, {@code ant_l} and {@code ant_r}.</p>
 *
 * <p>Posts stand where the rules put them when the run is laid as {@code --layout} lays it:
 * tennis 13 blocks with the posts 0.914 m outside the doubles sidelines, pickleball 7 blocks,
 * volleyball 11 (indoor) or 10 (sand) blocks with the posts in the end blocks' middles.</p>
 *
 * @since 2026.10
 */
public class BlockCourtNet extends BlockVenue {

  /** Which net. */
  public enum Kind {
    TENNIS(4, 17.2, false),
    PICKLEBALL(2, 14.7, false),
    VOLLEYBALL(1, 39.0, true);

    final int sags;
    final double top;
    final boolean tall;

    Kind(int sags, double top, boolean tall) {
      this.sags = sags;
      this.top = top;
      this.tall = tall;
    }
  }

  public static final PropertyBool END_L = PropertyBool.create("end_l");
  public static final PropertyBool END_R = PropertyBool.create("end_r");
  public static final PropertyBool ANT_L = PropertyBool.create("ant_l");
  public static final PropertyBool ANT_R = PropertyBool.create("ant_r");
  public static final PropertyBool WOMEN = PropertyBool.create("women");
  public static final PropertyInteger SAG = PropertyInteger.create("sag", 0, 4);

  private static final ThreadLocal<Kind> PENDING = new ThreadLocal<>();
  /** How far a run is followed each way. */
  private static final int SCAN = 16;

  private final Kind kind;

  public BlockCourtNet(String registryName, Kind kind) {
    super(stash(registryName, kind), Material.CLOTH, SoundType.CLOTH, 1.0F);
    this.kind = kind;
    PENDING.remove();
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(END_L, false).withProperty(END_R, false);
    if (kind == Kind.VOLLEYBALL) {
      s = s.withProperty(ANT_L, false).withProperty(ANT_R, false).withProperty(WOMEN, false);
    } else {
      s = s.withProperty(SAG, 0);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, Kind kind) {
    PENDING.set(kind);
    return registryName;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    if (PENDING.get() == Kind.VOLLEYBALL) {
      return new CsmBlockStateContainer(this, FACING, END_L, END_R, ANT_L, ANT_R, WOMEN);
    }
    return new CsmBlockStateContainer(this, FACING, END_L, END_R, SAG);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    if (kind == Kind.VOLLEYBALL) {
      s = s.withProperty(WOMEN, (meta & 4) != 0);
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int m = state.getValue(FACING).getHorizontalIndex();
    if (kind == Kind.VOLLEYBALL && state.getValue(WOMEN)) {
      m |= 4;
    }
    return m;
  }

  /** How many net blocks of the same run lie that way, up to {@link #SCAN}. */
  private int count(IBlockAccess world, BlockPos pos, EnumFacing facing, EnumFacing way) {
    int n = 0;
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
    for (int k = 1; k <= SCAN; k++) {
      at.setPos(pos.getX() + way.getXOffset() * k, pos.getY(), pos.getZ() + way.getZOffset() * k);
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() != this || s.getValue(FACING).getAxis() != facing.getAxis()) {
        break;
      }
      n++;
    }
    return n;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    int left = count(world, pos, f, f.rotateYCCW());
    int right = count(world, pos, f, f.rotateY());
    IBlockState s = state.withProperty(END_L, left == 0).withProperty(END_R, right == 0);
    if (kind == Kind.VOLLEYBALL) {
      return s.withProperty(ANT_L, left == 1 && right > 0).withProperty(ANT_R,
          right == 1 && left > 0);
    }
    int d = Math.min(left, right);
    int half = (left + right) / 2;
    int sag;
    if (left == right && left > 0) {
      sag = 4;
    } else if (half <= 0) {
      sag = 0;
    } else {
      sag = (int) Math.round((double) d / half * (kind.sags - 1));
      sag = Math.max(0, Math.min(kind.sags - 1, sag));
    }
    return s.withProperty(SAG, sag);
  }

  /** The selection box, drawn facing north: the base class turns it with the facing. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double top = Math.min(1.0, (kind.tall ? 40.0 : kind.top) / 16.0);
    return new AxisAlignedBB(0, 0, 0.44, 1, top, 0.56);
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    double top = kind.tall ? 1.5 : kind.top / 16.0;
    return RotationUtils.rotateBoundingBoxByFacing(new AxisAlignedBB(0, 0, 0.47, 1, top, 0.53),
        state.getValue(FACING));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (kind != Kind.VOLLEYBALL || !player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      boolean women = !state.getValue(WOMEN);
      world.setBlockState(pos, state.withProperty(WOMEN, women), 3);
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.venues.courts.net_height." + (women ? 1 : 0)), true);
    }
    return true;
  }
}
