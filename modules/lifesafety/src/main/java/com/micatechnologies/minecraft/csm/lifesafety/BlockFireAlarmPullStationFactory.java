package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Pull stations that differ only in registry name and box, each wearing an existing pull
 * station's body with its own face texture. A click trips the linked panel exactly as
 * {@link BlockFireAlarmGenericPullStation} does.
 *
 * @since 2026.9
 */
public class BlockFireAlarmPullStationFactory extends AbstractBlockFireAlarmActivator {

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  public BlockFireAlarmPullStationFactory(String registryName, AxisAlignedBB boundingBox) {
    this(initRegistryName(registryName), registryName, boundingBox);
  }

  private BlockFireAlarmPullStationFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos blockPos, IBlockState blockState,
      EntityPlayer entityPlayer, EnumHand enumHand, EnumFacing enumFacing, float hitX,
      float hitY, float hitZ) {
    if (entityPlayer.inventory.getCurrentItem().getItem() instanceof ItemFireAlarmLinker) {
      return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand,
          enumFacing, hitX, hitY, hitZ);
    }
    boolean activated = activateLinkedPanel(world, blockPos, entityPlayer);
    if (!activated && !world.isRemote) {
      entityPlayer.sendMessage(
          new TextComponentString("WARNING: This pull station has lost connection, " +
              "has failed or is otherwise not functional."));
    }
    return true;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public void onTick(World world, BlockPos blockPos, IBlockState blockState) {
    // Do nothing
  }

  @Override
  public int getBlockTickRate() {
    return 20;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }
}
