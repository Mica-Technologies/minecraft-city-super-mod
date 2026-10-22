package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A parking garage's barrier gate (#302): the cabinet, whose striped arm closes the lane to the
 * cabinet's right as a driver facing it sees it. The arm is {@link TileEntityParkingGateRenderer}'s;
 * the rules are {@link TileEntityParkingGate}'s.
 *
 * <ul>
 *   <li>Sneak and right-click with an empty hand: the next mode (automatic, ticket, redstone
 *   only).</li>
 *   <li>Right-click with an empty hand: the next arm length, 3 to 6 blocks.</li>
 * </ul>
 *
 * <p>Stored: the facing and {@code open}, the server's answer to whether the arm should be up
 * (metadata bit 2), which the client swings the arm toward. Redstone always lifts it.</p>
 *
 * @since 2026.10
 */
public class BlockParkingGate extends AbstractBlockRotatableNSEW implements ICsmTileEntityProvider {

  public static final PropertyBool OPEN = PropertyBool.create("open");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(3 / 16.0, 0, 4 / 16.0, 13 / 16.0,
      1, 12 / 16.0);

  public BlockParkingGate() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 0F, 0);
    setDefaultState(getDefaultState().withProperty(OPEN, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "parking_gate_arm";
  }

  /** The way the arm reaches: the cabinet's right, as a driver facing its front sees it. */
  public static EnumFacing armDirection(IBlockState state) {
    return state.getValue(FACING).rotateYCCW();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!world.isRemote && te instanceof TileEntityParkingGate) {
      TileEntityParkingGate gate = (TileEntityParkingGate) te;
      if (player.isSneaking()) {
        TileEntityParkingGate.Mode[] modes = TileEntityParkingGate.Mode.values();
        gate.setMode(modes[(gate.getMode().ordinal() + 1) % modes.length]);
        player.sendStatusMessage(new TextComponentString("Gate: " + gate.getMode().label), true);
      } else {
        int next = gate.getArmLength() + 1;
        gate.setArmLength(next > TileEntityParkingGate.MAX_LENGTH
            ? TileEntityParkingGate.MIN_LENGTH : next);
        player.sendStatusMessage(new TextComponentString(
            "Gate arm: " + gate.getArmLength() + " blocks"), true);
      }
    }
    return true;
  }

  /**
   * Lifts the gate nearest {@code from} within four blocks, for a ticket issued or paid there.
   *
   * @return whether there was a gate to lift
   */
  public static boolean grantNearest(World world, BlockPos from) {
    TileEntityParkingGate best = null;
    double bestDistance = Double.MAX_VALUE;
    for (BlockPos at : BlockPos.getAllInBoxMutable(from.add(-4, -2, -4), from.add(4, 2, 4))) {
      if (world.getBlockState(at).getBlock() instanceof BlockParkingGate) {
        TileEntity te = world.getTileEntity(at);
        double d = at.distanceSq(from);
        if (te instanceof TileEntityParkingGate && d < bestDistance) {
          best = (TileEntityParkingGate) te;
          bestDistance = d;
        }
      }
    }
    if (best != null) {
      best.grant();
    }
    return best != null;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
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
    return true;
  }

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityParkingGate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityparkinggate";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityParkingGate();
  }
}
