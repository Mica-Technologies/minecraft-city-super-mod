package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;

/**
 * The Alto Springs Main St corridor's real ADVANCED plans, as read from the eight live
 * controllers on 2026-11-09 ({@code src/test/resources/alto/main_corridor_tcadv_20261109.json}):
 * each controller's {@code tcAdv} after the day's {@code /blockdata} writes, each one's phase list
 * from before the {@code tcAdv:{ph:[...]}} write that set ped recall and rest in walk, and Elm's
 * whole {@code tcAdv} from before its {@code tcAdv:{co:{...}}} merge. The dump is JSON, so the NBT
 * types are rebuilt the way the controller's readers accept them: numbers as longs (every
 * numeric getter reads any primitive), number lists as int arrays, lists of objects as lists.
 */
final class AltoCorridorPlans {

  /** The eight junctions, in the order the dump lists them. */
  static final String[] JUNCTIONS =
      {"elm", "academy", "church", "pine", "sierra", "yaney", "hammond", "keough"};

  private static JsonObject root;

  private AltoCorridorPlans() {
  }

  private static synchronized JsonObject root() throws Exception {
    if (root == null) {
      File f = new File("modules/roads/src/test/resources/alto/main_corridor_tcadv_20261109.json");
      if (!f.isFile()) {
        f = new File(System.getProperty("user.dir"), f.getPath());
      }
      root = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()),
          StandardCharsets.UTF_8)).getAsJsonObject();
    }
    return root;
  }

  /** A junction's {@code tcAdv} as it is now (after every write). */
  static NBTTagCompound after(String junction) throws Exception {
    return (NBTTagCompound) nbt(root().getAsJsonObject("after").get(junction));
  }

  /** A junction's {@code tcAdv} as it was before the ph-only write set ped recall and rest in walk. */
  static NBTTagCompound beforePedRecall(String junction) throws Exception {
    NBTTagCompound adv = after(junction);
    adv.setTag("ph", nbt(root().getAsJsonObject("phBefore").get(junction)));
    return adv;
  }

  /** Elm's {@code tcAdv} before its co-only merge (a FREE coordination plan). */
  static NBTTagCompound elmBeforeCoordination() throws Exception {
    return (NBTTagCompound) nbt(root().get("elmBefore"));
  }

  static TrafficSignalProgrammedPhasePlan plan(NBTTagCompound adv) {
    return TrafficSignalProgrammedPhasePlan.fromNBT(adv);
  }

  static NBTBase nbt(JsonElement e) {
    if (e.isJsonObject()) {
      NBTTagCompound c = new NBTTagCompound();
      for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
        c.setTag(en.getKey(), nbt(en.getValue()));
      }
      return c;
    }
    if (e.isJsonArray()) {
      JsonArray a = e.getAsJsonArray();
      boolean numbers = true;
      for (JsonElement x : a) {
        numbers &= x.isJsonPrimitive() && x.getAsJsonPrimitive().isNumber();
      }
      if (numbers) {
        int[] v = new int[a.size()];
        for (int i = 0; i < v.length; i++) {
          v[i] = a.get(i).getAsInt();
        }
        return new NBTTagIntArray(v);
      }
      NBTTagList list = new NBTTagList();
      for (JsonElement x : a) {
        list.appendTag(nbt(x));
      }
      return list;
    }
    if (e.getAsJsonPrimitive().isBoolean()) {
      return new NBTTagByte((byte) (e.getAsBoolean() ? 1 : 0));
    }
    if (e.getAsJsonPrimitive().isNumber()) {
      return new NBTTagLong(e.getAsLong());
    }
    return new NBTTagString(e.getAsString());
  }

  /** The number of circuits a plan refers to (phases and overlaps), plus one spare. */
  static int circuitCount(TrafficSignalProgrammedPhasePlan plan) {
    int max = 0;
    for (TrafficSignalProgrammedPhase p : plan.getPhases()) {
      max = Math.max(max, p.getCircuitIndex());
    }
    for (TrafficSignalProgrammedOverlap ov : plan.getVehicleOverlaps()) {
      max = Math.max(max, ov.getOutputCircuitIndex());
    }
    for (TrafficSignalPreempt pe : plan.getPreempts()) {
      max = Math.max(max, pe.getTriggerCircuitIndex());
    }
    return max + 2;
  }
}
