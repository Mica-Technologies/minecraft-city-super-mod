package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.api.CsmDoorControl;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A holding cell's sliding barred door, a block high: stack two for a door. Click either and the
 * whole stack slides open or shut; a redstone signal holds it open, as a control desk's switch
 * would. Open, the bars have slid aside into the wall beside the doorway and the way is clear.
 *
 * <p>With Building installed, a keypad, push button or control station links to it like any door
 * (sneak-click the control, then the cell door). A linked keypad locks it from both sides: a cell
 * has no inside a prisoner may open it from, so it then opens only by the code, the control or
 * redstone ({@link CsmDoorControl}, #336).</p>
 *
 * <p>Stored in metadata: the facing, {@link #OPEN} and {@link #POWERED}.</p>
 *
 * @since 2026.9
 */
public class BlockCellDoor extends BlockFireProtectionProp implements CsmDoorControl.Door {

  public static final PropertyBool OPEN = PropertyBool.create("open");
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** The closed bars, facing north: a plane across the middle of the cell. */
  private static final AxisAlignedBB CLOSED = new AxisAlignedBB(0, 0, 7 / 16.0, 1, 1, 9 / 16.0);

  public BlockCellDoor(String registryName, int[] box) {
    super(registryName, box, true);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(OPEN, false).withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN, POWERED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0)
        | (state.getValue(POWERED) ? 8 : 0);
  }

  /**
   * The door's two edges are a pane's edge, so iron bars and glass panes beside it join it the way
   * they join each other; anything else left a half-block gap each side of the door.
   */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return face.getAxis() != EnumFacing.Axis.Y
        && face.getAxis() != state.getValue(FACING).getAxis()
        ? BlockFaceShape.MIDDLE_POLE_THIN : BlockFaceShape.UNDEFINED;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0)
        .withProperty(POWERED, (meta & 8) != 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actual) {
    if (state.getValue(OPEN)) {
      return;
    }
    AxisAlignedBB box = state.getValue(FACING).getAxis() == EnumFacing.Axis.Z ? CLOSED
        : new AxisAlignedBB(CLOSED.minZ, 0, CLOSED.minX, CLOSED.maxZ, 1, CLOSED.maxX);
    addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getValue(OPEN);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    BlockPos key = doorKey(world, pos);
    // A sneak-click while linking a keypad, button or control station links it to this door.
    if (player.isSneaking() && CsmDoorControl.finishLink(player, world, key)) {
      return true;
    }
    if (!world.isRemote) {
      // A locked cell opens from neither side by hand: only its keypad's code, its control or a
      // redstone signal opens it. Shutting it by hand is always allowed.
      if (!state.getValue(OPEN) && CsmDoorControl.isLocked(world, key)) {
        player.sendStatusMessage(new TextComponentTranslation("gui.csm.door.locked"), true);
        world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS,
            0.4F, 1.6F);
        return true;
      }
      setStack(world, pos, !state.getValue(OPEN));
    }
    return true;
  }

  /** The lowest door of the stack: what a control links to and a keypad locks. */
  @Override
  public BlockPos doorKey(World world, BlockPos pos) {
    BlockPos bottom = pos;
    while (world.getBlockState(bottom.down()).getBlock() == this) {
      bottom = bottom.down();
    }
    return bottom.toImmutable();
  }

  @Override
  public void doorCommand(World world, BlockPos key, CsmDoorControl.Command command) {
    if (world.getBlockState(key).getBlock() != this) {
      return;
    }
    switch (command) {
      case OPEN:
        setStack(world, key, true);
        break;
      case CLOSE:
        setStack(world, key, false);
        break;
      case TOGGLE:
        setStack(world, key, !world.getBlockState(key).getValue(OPEN));
        break;
      default:
        break;
    }
  }

  /** The bottom door of a stack going takes its lock with it, so a door put back is not locked. */
  @Override
  public void breakBlock(World world, BlockPos pos, IBlockState state) {
    if (!world.isRemote && world.getBlockState(pos.down()).getBlock() != this) {
      CsmDoorControl.doorRemoved(world, pos);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      setStack(world, pos, powered);
    }
  }

  /** Opens or shuts this door and every door of the same kind stacked with it. */
  private void setStack(World world, BlockPos pos, boolean open) {
    BlockPos bottom = pos;
    while (world.getBlockState(bottom.down()).getBlock() == this) {
      bottom = bottom.down();
    }
    boolean changed = false;
    for (BlockPos p = bottom; world.getBlockState(p).getBlock() == this; p = p.up()) {
      IBlockState s = world.getBlockState(p);
      if (s.getValue(OPEN) != open) {
        world.setBlockState(p, s.withProperty(OPEN, open), 3);
        changed = true;
      }
    }
    if (changed) {
      world.playSound(null, pos,
          open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
          SoundCategory.BLOCKS, 1.0F, 0.7F);
    }
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}
