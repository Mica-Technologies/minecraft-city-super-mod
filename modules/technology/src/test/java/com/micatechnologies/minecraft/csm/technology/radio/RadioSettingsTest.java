package com.micatechnologies.minecraft.csm.technology.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class RadioSettingsTest {

  @Test
  void settingsRoundTripThroughNbt() {
    RadioSettings in = new RadioSettings(true, 7, 9, 2, 12, true, "ENG-51");
    RadioSettings out = RadioSettings.read(in.write(new NBTTagCompound()));
    assertEquals(in, out);
    assertTrue(out.isOn());
    assertEquals(7, out.getChannel());
    assertEquals(9, out.getVolume());
    assertEquals(2, out.getSquelch());
    assertEquals(12, out.getPrivacyCode());
    assertTrue(out.isScan());
    assertEquals("ENG-51", out.getUnitId());
  }

  @Test
  void aRadioNeverSetReadsAsTheDefaults() {
    assertEquals(RadioSettings.DEFAULT, RadioSettings.read(null));
    assertEquals(RadioSettings.DEFAULT, RadioSettings.read(new NBTTagCompound()));
    assertFalse(RadioSettings.DEFAULT.isOn());
  }

  /** NBT is written by scripts and editors as well as by the game; nothing in it is trusted. */
  @Test
  void valuesOutsideTheRangeAreBroughtIntoIt() {
    NBTTagCompound tag = new NBTTagCompound();
    tag.setInteger("ch", 4000);
    tag.setInteger("vol", 99);
    tag.setInteger("sq", -3);
    tag.setInteger("pc", 1000);
    tag.setString("id", "car 54, where are you?");
    RadioSettings s = RadioSettings.read(tag);
    assertEquals(0, s.getChannel());
    assertEquals(RadioSettings.MAX_VOLUME, s.getVolume());
    assertEquals(0, s.getSquelch());
    assertEquals(RadioModel.MAX_PRIVACY_CODE, s.getPrivacyCode());
    assertEquals("CAR 54 W", s.getUnitId());

    tag.setInteger("ch", -1);
    tag.setInteger("vol", -1);
    tag.setInteger("pc", -1);
    s = RadioSettings.read(tag);
    assertEquals(0, s.getChannel());
    assertEquals(0, s.getVolume());
    assertEquals(0, s.getPrivacyCode());
  }

  @Test
  void aMakeOnlyKeepsWhatItHas() {
    RadioSettings all = new RadioSettings(true, 21, 5, 3, 20, true, "UNIT 9");

    // The consumer radio: 22 channels and a privacy code, no scan, no unit ID.
    RadioSettings consumer = all.forModel(RadioModel.MICAROLLA_EM1000);
    assertEquals(21, consumer.getChannel());
    assertEquals(20, consumer.getPrivacyCode());
    assertFalse(consumer.isScan());
    assertEquals("", consumer.getUnitId());

    // The public-safety radios: ten talkgroups, scan and a unit ID, no privacy code.
    for (RadioModel model : new RadioModel[]{RadioModel.HARROW_XG100,
        RadioModel.MICAROLLA_APEX8000}) {
      RadioSettings safety = all.forModel(model);
      assertEquals(0, safety.getChannel(), "a preset the make lacks falls back to its first");
      assertEquals(0, safety.getPrivacyCode());
      assertTrue(safety.isScan());
      assertEquals("UNIT 9", safety.getUnitId());
    }
  }

  @Test
  void everyPresetOfEveryMakeIsStorable() {
    for (RadioModel model : RadioModel.values()) {
      int count = model.getChannels().size();
      assertTrue(count > 0 && count <= RadioSettings.MAX_CHANNELS, model.name());
      RadioSettings last = RadioSettings.DEFAULT.withChannel(count - 1);
      assertEquals(count - 1,
          RadioSettings.read(last.write(new NBTTagCompound())).forModel(model).getChannel(),
          model.name());
    }
  }

  @Test
  void unitIdIsCleaned() {
    assertEquals("", RadioSettings.cleanUnitId(null));
    assertEquals("MEDIC-7", RadioSettings.cleanUnitId("medic-7"));
    assertEquals("AB", RadioSettings.cleanUnitId("  a§b!  "));
    assertEquals(RadioSettings.MAX_UNIT_ID_LENGTH,
        RadioSettings.cleanUnitId("ABCDEFGHIJKLMNOP").length());
  }

  @Test
  void steppingPastAnEndStops() {
    RadioSettings s = RadioSettings.DEFAULT.withVolume(RadioSettings.MAX_VOLUME + 1);
    assertEquals(RadioSettings.MAX_VOLUME, s.getVolume());
    assertEquals(0, s.withVolume(-1).getVolume());
    assertEquals(RadioSettings.MAX_SQUELCH, s.withSquelch(50).getSquelch());
    assertEquals(0, s.withPrivacyCode(-1).getPrivacyCode());
  }

  @Test
  void tileEntityKeepsItsSettings() {
    RadioSettings in = new RadioSettings(true, 3, 8, 1, 0, true, "TRUCK 2");
    TileEntityRadio te = new TileEntityRadio();
    te.readNBT(in.write(new NBTTagCompound()));
    assertEquals(in, te.getSettings());

    TileEntityRadio copy = new TileEntityRadio();
    copy.readNBT(te.writeNBT(new NBTTagCompound()));
    assertEquals(in, copy.getSettings());
  }

  @Test
  void packetRoundTripsForAHandAndForABlock() {
    RadioSettings in = new RadioSettings(true, 9, 10, 9, 38, true, "K9-1");

    ByteBuf buf = Unpooled.buffer();
    new RadioSettingsPacket(EnumHand.OFF_HAND, in).toBytes(buf);
    RadioSettingsPacket held = new RadioSettingsPacket();
    held.fromBytes(buf);
    assertTrue(held.isHeld());
    assertEquals(EnumHand.OFF_HAND, held.getHand());
    assertEquals(in, held.getSettings());

    buf = Unpooled.buffer();
    BlockPos pos = new BlockPos(-120, 64, 3000);
    new RadioSettingsPacket(pos, in).toBytes(buf);
    RadioSettingsPacket placed = new RadioSettingsPacket();
    placed.fromBytes(buf);
    assertFalse(placed.isHeld());
    assertEquals(pos, placed.getPos());
    assertEquals(in, placed.getSettings());
  }

  /** A packet's bytes are the sender's to choose: whatever they say, the settings are in range. */
  @Test
  void aPacketOfNonsenseDecodesIntoRange() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeBoolean(true);
    buf.writeBoolean(false);
    buf.writeLong(0L);
    buf.writeBoolean(true);
    buf.writeByte(255);
    buf.writeByte(255);
    buf.writeByte(255);
    buf.writeByte(255);
    buf.writeBoolean(true);
    byte[] id = "§k§k§klong long long long".getBytes(
        java.nio.charset.StandardCharsets.UTF_8);
    buf.writeInt(id.length);
    buf.writeBytes(id);
    RadioSettingsPacket packet = new RadioSettingsPacket();
    packet.fromBytes(buf);
    RadioSettings s = packet.getSettings();
    assertEquals(0, s.getChannel());
    assertEquals(RadioSettings.MAX_VOLUME, s.getVolume());
    assertEquals(RadioSettings.MAX_SQUELCH, s.getSquelch());
    assertEquals(RadioModel.MAX_PRIVACY_CODE, s.getPrivacyCode());
    assertTrue(s.getUnitId().length() <= RadioSettings.MAX_UNIT_ID_LENGTH);
    assertFalse(s.getUnitId().contains("§"));
  }

  @Test
  void channelsAreNamedOnceAndWrittenAsMegahertz() {
    for (RadioModel model : RadioModel.values()) {
      Set<String> names = new HashSet<>();
      Set<Integer> frequencies = new HashSet<>();
      for (RadioChannel channel : model.getChannels()) {
        assertTrue(names.add(channel.getName()), model + " " + channel.getName());
        assertTrue(frequencies.add(channel.getFrequencyHz()), model + " " + channel.getName());
      }
    }
    assertEquals("462.5625",
        RadioModel.MICAROLLA_EM1000.getChannels().get(0).getFrequencyText());
    assertEquals("146.5200", RadioModel.BOWFENG_BF5.getChannels().get(3).getFrequencyText());
    assertEquals("851.0125", RadioModel.HARROW_XG100.getChannels().get(0).getFrequencyText());
  }

  /** What lets a transmission cross makes later: presets meet on frequency, not on index. */
  @Test
  void makesShareChannelsByFrequency() {
    assertEquals(RadioModel.MICAROLLA_EM1000.getChannels().get(0).getFrequencyHz(),
        RadioModel.BOWFENG_BF5.getChannels().get(0).getFrequencyHz());
    for (int i = 0; i < RadioModel.HARROW_XG100.getChannels().size(); i++) {
      assertEquals(RadioModel.HARROW_XG100.getChannels().get(i).getFrequencyHz(),
          RadioModel.MICAROLLA_APEX8000.getChannels().get(i).getFrequencyHz());
    }
  }
}
