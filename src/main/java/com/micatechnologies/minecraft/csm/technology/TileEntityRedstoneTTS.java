package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSInvokePacket;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSUpdatePacket;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityRedstoneTTS extends AbstractTileEntity {

  private static final String TTS_STRING_KEY = "tts";
  private static final String LEGACY_TTS_STRING_KEY = "ttsString";
  private static final String TTS_STRING_DEFAULT = "Setup is Required!";
  private static final String TTS_RADIUS_KEY = "ttR";
  private static final String LEGACY_TTS_RADIUS_KEY = "ttsRadius";
  private static final String TTS_VOICE_KEY = "ttV";
  private static final String TTS_VOICE_DEFAULT = "cmu-slt-hsmm";
  private static final long COOLDOWN_DURATION = 2000;

  private AtomicLong lastTtsInvocationTime = new AtomicLong(0);
  private String ttsString = TTS_STRING_DEFAULT;
  private double ttsRadius = 32.0;
  private String ttsVoice = TTS_VOICE_DEFAULT;

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(TTS_STRING_KEY)) {
      ttsString = compound.getString(TTS_STRING_KEY);
    } else if (compound.hasKey(LEGACY_TTS_STRING_KEY)) {
      ttsString = compound.getString(LEGACY_TTS_STRING_KEY);
    }

    if (compound.hasKey(TTS_RADIUS_KEY)) {
      ttsRadius = compound.getDouble(TTS_RADIUS_KEY);
    } else if (compound.hasKey(LEGACY_TTS_RADIUS_KEY)) {
      ttsRadius = compound.getDouble(LEGACY_TTS_RADIUS_KEY);
    }

    if (compound.hasKey(TTS_VOICE_KEY)) {
      ttsVoice = compound.getString(TTS_VOICE_KEY);
    }

    compound.removeTag(LEGACY_TTS_STRING_KEY);
    compound.removeTag(LEGACY_TTS_RADIUS_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(TTS_STRING_KEY, ttsString);
    compound.setDouble(TTS_RADIUS_KEY, ttsRadius);
    compound.setString(TTS_VOICE_KEY, ttsVoice);
    return compound;
  }

  public void readTtsString() {
    if (world.isRemote) {
      System.err.println(
          "Attempted to directly invoke TTS string read on client side! This is a bug!");
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastTtsInvocationTime.get() < COOLDOWN_DURATION) {
      return;
    }
    lastTtsInvocationTime.set(currentTime);

    CsmNetwork.sendToAllAround(new TileEntityRedstoneTTSInvokePacket(ttsString, ttsVoice),
        new TargetPoint(
            world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), ttsRadius));
  }

  public String getTtsString() {
    return ttsString;
  }

  public String getTtsVoice() {
    return ttsVoice;
  }

  public void setTtsConfigFromGui(String newTtsString, String newTtsVoice) {
    if (world.isRemote) {
      CsmNetwork.sendToServer(
          new TileEntityRedstoneTTSUpdatePacket(this.pos, newTtsString, newTtsVoice));
    } else {
      setTtsString(newTtsString);
      setTtsVoice(newTtsVoice);
    }
  }

  public void setTtsString(String ttsString) {
    if (world.isRemote) {
      System.err.println("Attempted to set TTS string on client side! This is a bug!");
    }
    this.ttsString = ttsString;
    markDirtySync(getWorld(), getPos(), true);
  }

  public void setTtsVoice(String ttsVoice) {
    if (world.isRemote) {
      System.err.println("Attempted to set TTS voice on client side! This is a bug!");
    }
    this.ttsVoice = ttsVoice;
    markDirtySync(getWorld(), getPos(), true);
  }
}
