package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSInvokePacket;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSUpdatePacket;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityRedstoneTTS extends AbstractTileEntity {

  private static final String TTS_STRING_KEY = "ttsString";
  private static final String TTS_STRING_DEFAULT = "Setup is Required!";
  private static final String TTS_RADIUS_KEY = "ttsRadius";
  private static final long COOLDOWN_DURATION = 2000; // Cooldown duration in milliseconds
  private AtomicLong lastTtsInvocationTime = new AtomicLong(0);
  private String ttsString = TTS_STRING_DEFAULT;
  private double ttsRadius = 32.0;

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(TTS_STRING_KEY)) {
      ttsString = compound.getString(TTS_STRING_KEY);
    }

    if (compound.hasKey(TTS_RADIUS_KEY)) {
      ttsRadius = compound.getDouble(TTS_RADIUS_KEY);
    }
  }

  /**
   * Returns the NBT tag compound with the tile entity's NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(TTS_STRING_KEY, ttsString);
    compound.setDouble(TTS_RADIUS_KEY, ttsRadius);
    return compound;
  }

  public void readTtsString() {
    if (world.isRemote) {
      System.err.println(
          "Attempted to directly invoke TTS string read on client side! This is a bug!");
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastTtsInvocationTime.get() < COOLDOWN_DURATION) {
      // Method was called within the cooldown period, ignore this invocation
      return;
    }
    lastTtsInvocationTime.set(currentTime); // Update last invocation time

    // Send packet to trigger TTS reading on nearby clients
    CsmNetwork.sendToAllAround(new TileEntityRedstoneTTSInvokePacket(ttsString), new TargetPoint(
        world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), ttsRadius));
  }

  public String getTtsString() {
    return ttsString;
  }

  public void setTtsStringFromGui(String newTtsString) {
    if (world.isRemote) { // Check if we're on the client side
      // Send packet to server with the new TTS string and tile entity position
      CsmNetwork.sendToServer(new TileEntityRedstoneTTSUpdatePacket(this.pos, newTtsString));
    } else {
      // Update the TTS string on the server side
      setTtsString(newTtsString);
    }
  }

  public void setTtsString(String ttsString) {
    if (world.isRemote) {
      System.err.println("Attempted to set TTS string on client side! This is a bug!");
    }
    this.ttsString = ttsString;
    markDirtySync(getWorld(), getPos(), true);
  }
}

