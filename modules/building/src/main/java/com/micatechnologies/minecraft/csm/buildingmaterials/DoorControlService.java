package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.api.CsmDoorControl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Building's side of {@link CsmDoorControl}: other modules' doors (a holding cell door) link to the
 * door controls and are locked by a keypad through it, just as Building's own doors are.
 *
 * @since 2026.11
 */
final class DoorControlService implements CsmDoorControl.Service {

  private DoorControlService() {
  }

  static void register() {
    CsmDoorControl.setService(new DoorControlService());
  }

  @Override
  public boolean finishLink(EntityPlayer player, World world, BlockPos key) {
    return GarageDoorLinks.finish(player, world, key);
  }

  @Override
  public boolean isLocked(World world, BlockPos key) {
    return DoorLocks.holds(world, key);
  }

  @Override
  public void doorRemoved(World world, BlockPos key) {
    DoorLocks.get(world).unlock(key);
  }

  /** A control's command as the API names it. */
  static CsmDoorControl.Command of(BlockGarageDoor.Command command) {
    switch (command) {
      case OPEN:
        return CsmDoorControl.Command.OPEN;
      case CLOSE:
        return CsmDoorControl.Command.CLOSE;
      case STOP:
        return CsmDoorControl.Command.STOP;
      default:
        return CsmDoorControl.Command.TOGGLE;
    }
  }
}
