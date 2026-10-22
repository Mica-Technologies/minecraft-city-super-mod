package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.api.CsmFireAlarmLink;
import com.micatechnologies.minecraft.csm.codeutils.ICsmBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.IHasModel;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The fare line's service gate: a vanilla fence gate in every way -- the station railings join
 * it, a click opens it away from whoever clicks and closes it, redstone power opens it, and open
 * it is walked through -- drawn as a stainless leaf with a SERVICE GATE plate between a hinge post
 * and a latch post, swung a quarter about its hinge when open ({@code gen_transit_stations.py}).
 * Nobody checks a fare at it: which side is paid is the builder's business.
 *
 * @since 2026.9
 */
public class BlockStationGate extends BlockFenceGate
    implements IHasModel, ICsmBlock, ICsmTileEntityProvider, CsmFireAlarmLink.Follower {

  /** How often a gate following a panel looks at it, in ticks. */
  private static final int ALARM_POLL_TICKS = 20;

  private final String registryName;

  /**
   * Constructs a service gate.
   *
   * @param registryName its registry name
   */
  public BlockStationGate(String registryName) {
    super(BlockPlanks.EnumType.OAK);
    this.registryName = registryName;
    setTranslationKey(registryName);
    setRegistryName(CsmConstants.MOD_NAMESPACE, registryName);
    setSoundType(SoundType.METAL);
    setHardness(3.0F);
    setResistance(10.0F);
    setHarvestLevel("pickaxe", 1);
    // A random tick runs updateTick, which restarts the alarm look after a reload
    setTickRandomly(true);
    CsmRegistry.registerBlock(this);
    CsmRegistry.registerItem(
        new ItemBlock(this).setRegistryName(Objects.requireNonNull(getRegistryName())));
  }

  // --- fire alarm release (#301) -------------------------------------------------------------

  /**
   * Links the gate to a fire alarm control panel: while that panel is in alarm the gate stands
   * open, and on reset it shuts again if the alarm opened it.
   */
  @Override
  public boolean followFireAlarmPanel(World world, BlockPos pos, BlockPos panel) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityStationGate)
        || !((TileEntityStationGate) te).setAlarmPanel(panel)) {
      return false;
    }
    world.scheduleUpdate(pos, this, ALARM_POLL_TICKS);
    return true;
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    TileEntity te = world.getTileEntity(pos);
    if (world.isRemote || !(te instanceof TileEntityStationGate)) {
      return;
    }
    TileEntityStationGate gate = (TileEntityStationGate) te;
    if (gate.getAlarmPanel() == null) {
      return;
    }
    boolean alarm = CsmFireAlarmLink.isPanelInAlarm(world, gate.getAlarmPanel());
    boolean open = state.getValue(OPEN);
    if (alarm && !open) {
      world.setBlockState(pos, state.withProperty(OPEN, true), 10);
      world.playEvent(null, 1008, pos, 0);
      gate.setAlarmOpened(true);
    } else if (!alarm && gate.isAlarmOpened()) {
      if (open) {
        world.setBlockState(pos, state.withProperty(OPEN, false), 10);
        world.playEvent(null, 1014, pos, 0);
      }
      gate.setAlarmOpened(false);
    }
    // a scheduled update already pending is not duplicated, so a random tick costs nothing more
    world.scheduleUpdate(pos, this, ALARM_POLL_TICKS);
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStationGate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystationgate";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStationGate();
  }

  @Override
  public void registerModels() {
    Csm.proxy.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, "inventory");
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  /** Unused: a fence gate's own boxes are kept. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return null;
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
