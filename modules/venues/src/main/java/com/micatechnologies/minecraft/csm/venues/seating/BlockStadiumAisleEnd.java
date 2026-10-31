package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The seat at the aisle end of a row, with the row's letter on a plate on its end standard
 * ({@code stadium_seat_aisle_end} rows A to P, {@code stadium_seat_aisle_end_2} rows Q to Z and
 * AA to FF).
 *
 * <p>It takes everything but the letter from the stadium seat beside it: which way it faces,
 * which side the aisle is on (the side away from that seat), its shape, whether it is folded and,
 * through the cushion's tint, its colour. So the whole of its metadata is the row letter, and a
 * generator places it in a row's end cell instead of a seat. Without a seat beside it, it faces
 * north as a charcoal 2-deep seat with its standard on the left.</p>
 *
 * <p><b>Metadata</b>: the row letter's index within the block, 0 to 15. A click sits (when the
 * row is down); a sneak-click steps the letter on.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumAisleEnd extends BlockVenue implements IStadiumSeat {

  /** Every row letter, in order; each block holds sixteen of them from its offset. */
  public static final String[] LABELS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
      "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "BB", "CC",
      "DD", "EE", "FF"};

  /** The row letter's index within the block. Stored, the whole metadata. */
  public static final PropertyInteger LABEL = PropertyInteger.create("label", 0, 15);
  /** The end standard is on the right (the seat beside is on the left). Actual. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** Folded up, as the seat beside it is. Actual. */
  public static final PropertyBool UP = BlockStadiumSeat.UP;
  /** The 1-deep shape, as the seat beside it has. Actual. */
  public static final PropertyBool RISER = BlockStadiumSeat.RISER;

  private static final AxisAlignedBB SELECT = new AxisAlignedBB(0, 0, 1 / 16.0, 1, 1, 1);

  private final int labelOffset;

  /**
   * Constructs an aisle-end block.
   *
   * @param registryName its registry name
   * @param labelOffset  the index in {@link #LABELS} of its metadata 0
   */
  public BlockStadiumAisleEnd(String registryName, int labelOffset) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    this.labelOffset = labelOffset;
    setDefaultState(blockState.getBaseState().withProperty(LABEL, 0)
        .withProperty(FACING, EnumFacing.NORTH).withProperty(RIGHT, false)
        .withProperty(UP, false).withProperty(RISER, false));
  }

  /** The row letter at metadata {@code meta} of this block. */
  public String label(int meta) {
    return LABELS[labelOffset + (meta & 15)];
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, LABEL, FACING, RIGHT, UP, RISER);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(LABEL, meta & 15);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(LABEL);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState();
  }

  /** What an aisle-end block takes from the seat beside it. */
  static final class Look {

    final EnumFacing facing;
    final boolean right;
    final boolean up;
    final boolean riser;
    final BlockPos source;

    Look(EnumFacing facing, boolean right, boolean up, boolean riser, BlockPos source) {
      this.facing = facing;
      this.right = right;
      this.up = up;
      this.riser = riser;
      this.source = source;
    }
  }

  /**
   * Finds the stadium seat beside {@code pos} that this block ends the row of: a
   * {@link BlockStadiumSeat} to the side of it, facing across the line between them.
   *
   * @param world the world
   * @param pos   the aisle-end block
   *
   * @return what it takes from that seat, or null if there is none
   */
  @Nullable
  static Look look(IBlockAccess world, BlockPos pos) {
    for (EnumFacing d : EnumFacing.HORIZONTALS) {
      BlockPos n = pos.offset(d);
      IBlockState s = world.getBlockState(n);
      if (!(s.getBlock() instanceof BlockStadiumSeat)) {
        continue;
      }
      EnumFacing f = s.getValue(FACING);
      boolean right;
      if (d == f.rotateY()) {
        right = false;      // the row runs on to the right; the aisle and standard on the left
      } else if (d == f.rotateYCCW()) {
        right = true;
      } else {
        continue;
      }
      return new Look(f, right, s.getValue(BlockStadiumSeat.UP),
          s.getValue(BlockStadiumSeat.RISER), n);
    }
    return null;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    Look look = look(world, pos);
    if (look == null) {
      return state.withProperty(FACING, EnumFacing.NORTH).withProperty(RIGHT, false)
          .withProperty(UP, false).withProperty(RISER, false);
    }
    return state.withProperty(FACING, look.facing).withProperty(RIGHT, look.right)
        .withProperty(UP, look.up).withProperty(RISER, look.riser);
  }

  @Override
  public EnumFacing seatFacing(IBlockAccess world, BlockPos pos, IBlockState state) {
    Look look = look(world, pos);
    return look != null ? look.facing : EnumFacing.NORTH;
  }

  @Override
  public int seatColour(IBlockAccess world, BlockPos pos, IBlockState state) {
    Look look = look(world, pos);
    if (look == null) {
      return DEFAULT_COLOUR;
    }
    IBlockState s = world.getBlockState(look.source);
    return ((BlockStadiumSeat) s.getBlock()).getColour();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SELECT;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = getActualState(state, world, pos);
    BlockStadiumSeat.addSeatBoxes(pos, entityBox, boxes, actual.getValue(FACING),
        actual.getValue(RISER), actual.getValue(UP));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      if (!world.isRemote) {
        int next = (state.getValue(LABEL) + 1) & 15;
        world.setBlockState(pos, state.withProperty(LABEL, next), 3);
        player.sendStatusMessage(new TextComponentTranslation("csm.venues.aisle_end.row",
            label(next)), true);
      }
      return true;
    }
    Look look = look(world, pos);
    boolean riser = look != null && look.riser;
    if (look != null && look.up) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing front = look != null ? look.facing : EnumFacing.NORTH;
      StadiumSeating.sit(world, pos, front, BlockStadiumSeat.SEAT_TOP,
          riser ? BlockStadiumSeat.FORWARD_RISER : BlockStadiumSeat.FORWARD_FLOOR, player);
    }
    return true;
  }
}
