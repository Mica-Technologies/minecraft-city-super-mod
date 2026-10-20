package com.micatechnologies.minecraft.csm.trafficsignals;

import com.google.common.base.Predicate;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficEntitySelectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants;

/**
 * The {@link TileEntityTrafficSignalSensor} is a {@link TileEntity} which is paired with an
 * {@link AbstractBlockTrafficSignalSensor} implementation. The
 * {@link TileEntityTrafficSignalSensor} is used to detect entities within its detection zones and
 * report the number of entities detected to the paired {@link TileEntityTrafficSignalController}.
 *
 * <p>A sensor has four zone types (standard/through, left, protected, right), and each type is
 * made of up to {@link #MAX_BOXES_PER_ZONE} boxes, each defined by two corner positions. A type's
 * count is the number of distinct eligible entities across all of its boxes, so a vehicle in two
 * overlapping boxes is one vehicle, and a single-lane zone and an L-shaped or two-lane zone call
 * their phase the same way.</p>
 *
 * @version 2.0
 * @see AbstractBlockTrafficSignalSensor
 * @see TileEntityTrafficSignalController
 * @see TileEntity
 * @since 2023.2.0
 */
public class TileEntityTrafficSignalSensor extends AbstractTileEntity {

  /**
   * Matches the entities a traffic signal sensor counts as demand: players and villagers.
   *
   * <p>The definition itself is {@link TrafficEntitySelectors#VEHICLE}, shared with the other
   * road blocks that have to agree on what a vehicle is. It keeps a short alias here because
   * this class names it in several places and the long form reads badly at each of them.</p>
   *
   * @since 2026.8
   */
  private static final Predicate<Entity> ELIGIBLE_ENTITY = TrafficEntitySelectors.VEHICLE;

  /**
   * The most boxes one zone type may hold. Every box is one entity query each time the controller
   * polls the sensor, so the cap keeps a sensor's poll cost bounded.
   */
  public static final int MAX_BOXES_PER_ZONE = 8;

  /** Returned by {@link #addBox} when the zone type already holds {@link #MAX_BOXES_PER_ZONE}. */
  public static final int ADD_REFUSED_FULL = -1;

  /** Returned by {@link #addBox} when the zone type already holds the same box. */
  public static final int ADD_REFUSED_DUPLICATE = -2;

  /** The four detection zone types a sensor reports separately. */
  public enum ZoneType {
    STANDARD("m1", "m2", "blockPos1", "blockPos2", "mx", "Through"),
    LEFT("l1", "l2", "leftBlockPos1", "leftBlockPos2", "lx", "Left"),
    PROTECTED("p1", "p2", "protectedBlockPos1", "protectedBlockPos2", "px", "Protected"),
    RIGHT("r1", "r2", "rightBlockPos1", "rightBlockPos2", "rx", "Right");

    // Box 0 keeps the original short keys (and their legacy long-form fallbacks), so a sensor
    // with one box per type saves exactly as it always has. Boxes 1 and up go in the extra list
    // key: an NBTTagList of int arrays, each [x1, y1, z1, x2, y2, z2] (the two corners).
    // Readable and writable by hand with /blockdata, e.g. {mx:[[I;10,64,20,14,68,30]]}.
    private final String corner1Key;
    private final String corner2Key;
    private final String legacyCorner1Key;
    private final String legacyCorner2Key;
    private final String extraBoxesKey;
    private final String displayName;

    ZoneType(String corner1Key, String corner2Key, String legacyCorner1Key,
        String legacyCorner2Key, String extraBoxesKey, String displayName) {
      this.corner1Key = corner1Key;
      this.corner2Key = corner2Key;
      this.legacyCorner1Key = legacyCorner1Key;
      this.legacyCorner2Key = legacyCorner2Key;
      this.extraBoxesKey = extraBoxesKey;
      this.displayName = displayName;
    }

    /** @return the short name shown to players, e.g. "Through". */
    public String getDisplayName() {
      return displayName;
    }
  }

  /**
   * One zone type's boxes, as corner pairs in the order they were drawn, plus the boxes'
   * {@link AxisAlignedBB}s, built once on first scan after a change rather than on every poll.
   */
  private static final class Zone {

    private final List<BlockPos[]> boxes = new ArrayList<>(1);
    private AxisAlignedBB[] scanBoxes;

    AxisAlignedBB[] scanBoxes() {
      AxisAlignedBB[] cached = scanBoxes;
      if (cached == null) {
        cached = new AxisAlignedBB[boxes.size()];
        for (int i = 0; i < cached.length; i++) {
          BlockPos[] box = boxes.get(i);
          cached[i] = new AxisAlignedBB(box[0], box[1]);
        }
        scanBoxes = cached;
      }
      return cached;
    }

    void changed() {
      scanBoxes = null;
    }
  }

  /** The sensor's zones, indexed by {@link ZoneType#ordinal()}. */
  private final Zone[] zones = new Zone[ZoneType.values().length];

  {
    for (int i = 0; i < zones.length; i++) {
      zones[i] = new Zone();
    }
  }

  private Zone zone(ZoneType type) {
    return zones[type.ordinal()];
  }

  /**
   * Processes the reading of the {@link TileEntityTrafficSignalSensor}'s NBT data from the supplied
   * NBT tag compound.
   *
   * @param compound the NBT tag compound to read the {@link TileEntityTrafficSignalSensor}'s NBT
   *                 data from
   *
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    for (ZoneType type : ZoneType.values()) {
      Zone zone = zone(type);
      zone.boxes.clear();
      zone.changed();

      BlockPos corner1 = readPos(compound, type.corner1Key, type.legacyCorner1Key);
      BlockPos corner2 = readPos(compound, type.corner2Key, type.legacyCorner2Key);
      if (corner1 != null && corner2 != null) {
        zone.boxes.add(new BlockPos[]{corner1, corner2});
      }

      // Extra boxes. If box 0 is missing, the first of these becomes box 0, so box 0 is always
      // the first box and the zone stays compact.
      NBTTagList extra = compound.getTagList(type.extraBoxesKey, Constants.NBT.TAG_INT_ARRAY);
      for (int i = 0; i < extra.tagCount() && zone.boxes.size() < MAX_BOXES_PER_ZONE; i++) {
        int[] c = extra.getIntArrayAt(i);
        if (c.length == 6) {
          zone.boxes.add(new BlockPos[]{new BlockPos(c[0], c[1], c[2]),
              new BlockPos(c[3], c[4], c[5])});
        }
      }

      // Strip legacy long-form keys so the next save produces only short-form output
      compound.removeTag(type.legacyCorner1Key);
      compound.removeTag(type.legacyCorner2Key);
    }
  }

  private static BlockPos readPos(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) {
      return SerializationUtils.getBlockPosFromNBTOrNull(compound, key);
    }
    return SerializationUtils.getBlockPosFromNBTOrNull(compound, legacyKey);
  }

  /**
   * Returns the specified NBT tag compound with the {@link TileEntityTrafficSignalSensor}'s NBT
   * data.
   *
   * @param compound the NBT tag compound to write the {@link TileEntityTrafficSignalSensor}'s NBT
   *                 data to
   *
   * @return the NBT tag compound with the {@link TileEntityTrafficSignalSensor}'s NBT data
   *
   * @since 2.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    for (ZoneType type : ZoneType.values()) {
      List<BlockPos[]> boxes = zone(type).boxes;

      // Box 0 in the original keys
      BlockPos[] first = boxes.isEmpty() ? null : boxes.get(0);
      SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, type.corner1Key,
          first == null ? null : first[0]);
      SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, type.corner2Key,
          first == null ? null : first[1]);

      // Boxes 1 and up in the extra list, written only when there are any
      if (boxes.size() > 1) {
        NBTTagList extra = new NBTTagList();
        for (int i = 1; i < boxes.size(); i++) {
          BlockPos a = boxes.get(i)[0];
          BlockPos b = boxes.get(i)[1];
          extra.appendTag(new NBTTagIntArray(
              new int[]{a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()}));
        }
        compound.setTag(type.extraBoxesKey, extra);
      } else {
        compound.removeTag(type.extraBoxesKey);
      }
    }

    // Return the NBT tag compound
    return compound;
  }

  /**
   * Marks the sensor dirty and sends it to the client, so the configuration GUI's box counts are
   * current. Skipped when the tile entity is not in a world (unit tests).
   */
  private void zonesChanged() {
    if (world != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  // --- Boxes per zone type ---

  /**
   * Adds a box to a zone type.
   *
   * @param type      the zone type
   * @param blockPos1 the first corner of the box
   * @param blockPos2 the second corner of the box
   *
   * @return the new box's 1-based number within its type, {@link #ADD_REFUSED_FULL} if the type
   *     already holds {@link #MAX_BOXES_PER_ZONE} boxes, or {@link #ADD_REFUSED_DUPLICATE} if it
   *     already holds a box with the same extent (corners in either order)
   */
  public int addBox(ZoneType type, BlockPos blockPos1, BlockPos blockPos2) {
    Zone zone = zone(type);
    AxisAlignedBB added = new AxisAlignedBB(blockPos1, blockPos2);
    for (AxisAlignedBB existing : zone.scanBoxes()) {
      if (existing.equals(added)) {
        return ADD_REFUSED_DUPLICATE;
      }
    }
    if (zone.boxes.size() >= MAX_BOXES_PER_ZONE) {
      return ADD_REFUSED_FULL;
    }
    zone.boxes.add(new BlockPos[]{blockPos1, blockPos2});
    zone.changed();
    zonesChanged();
    return zone.boxes.size();
  }

  /**
   * Removes the most recently added box of a zone type.
   *
   * @param type the zone type
   *
   * @return {@code true} if a box was removed, {@code false} if the type had none
   */
  public boolean removeLastBox(ZoneType type) {
    Zone zone = zone(type);
    if (zone.boxes.isEmpty()) {
      return false;
    }
    zone.boxes.remove(zone.boxes.size() - 1);
    zone.changed();
    zonesChanged();
    return true;
  }

  /**
   * Removes every box of a zone type.
   *
   * @param type the zone type
   */
  public void clearZone(ZoneType type) {
    Zone zone = zone(type);
    zone.boxes.clear();
    zone.changed();
    zonesChanged();
  }

  /**
   * @param type the zone type
   *
   * @return how many boxes the zone type holds, 0 to {@link #MAX_BOXES_PER_ZONE}
   */
  public int getBoxCount(ZoneType type) {
    return zone(type).boxes.size();
  }

  /**
   * @param type the zone type
   *
   * @return a copy of the zone type's boxes in the order they were drawn, each a two-element
   *     array of its corners
   */
  public List<BlockPos[]> getBoxes(ZoneType type) {
    List<BlockPos[]> copy = new ArrayList<>(zone(type).boxes.size());
    for (BlockPos[] box : zone(type).boxes) {
      copy.add(new BlockPos[]{box[0], box[1]});
    }
    return Collections.unmodifiableList(copy);
  }

  /**
   * Adds a box to the standard (through) zone. See {@link #addBox}.
   *
   * @since 1.0
   */
  public int setScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    return addBox(ZoneType.STANDARD, blockPos1, blockPos2);
  }

  /**
   * Adds a box to the left turn lane zone. See {@link #addBox}.
   *
   * @since 1.0
   */
  public int setLeftScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    return addBox(ZoneType.LEFT, blockPos1, blockPos2);
  }

  /**
   * Adds a box to the protected lane zone. See {@link #addBox}.
   *
   * @since 1.0
   */
  public int setProtectedScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    return addBox(ZoneType.PROTECTED, blockPos1, blockPos2);
  }

  /**
   * Adds a box to the right turn lane zone. See {@link #addBox}.
   *
   * @since 1.0
   */
  public int setRightScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    return addBox(ZoneType.RIGHT, blockPos1, blockPos2);
  }

  // --- Scanning ---

  /**
   * Counts the distinct eligible entities across a zone's boxes. Eligible entities are those
   * matched by {@link TrafficEntitySelectors#VEHICLE}.
   *
   * @param zone the zone to scan
   *
   * @return the number of distinct eligible entities in any of the zone's boxes
   */
  private int countEntities(Zone zone) {
    if (world == null) {
      return 0;
    }
    // One sweep with a predicate rather than one per entity class. The class argument to
    // getEntitiesWithinAABB filters the results, it does not narrow the search: each call
    // still walks every chunk overlapping the box and every entity list within it. Since a
    // sensor scans four zones and the controller polls every sensor on its circuit, a second
    // sweep per box would double the cost of the whole demand poll for no extra information.
    return countDistinct(zone.scanBoxes(),
        box -> world.getEntitiesWithinAABB(Entity.class, box, ELIGIBLE_ENTITY));
  }

  /**
   * Counts the distinct results of one query per box. With one box this is that query's size,
   * with no set allocated; with several, a result found by two of them (overlapping boxes, or an
   * entity straddling a shared edge) is counted once. Entities are equal by entity id.
   *
   * @param boxes the boxes to query
   * @param query the query run once for each box
   * @param <T>   the result type
   *
   * @return the number of distinct results across all of the boxes
   */
  static <T> int countDistinct(AxisAlignedBB[] boxes, Function<AxisAlignedBB, List<T>> query) {
    if (boxes.length == 0) {
      return 0;
    }
    if (boxes.length == 1) {
      return query.apply(boxes[0]).size();
    }
    Set<T> seen = new HashSet<>();
    for (AxisAlignedBB box : boxes) {
      seen.addAll(query.apply(box));
    }
    return seen.size();
  }

  /**
   * @param type the zone type
   *
   * @return the boxes a scan of the zone type queries, one per box, in the order they were drawn
   */
  AxisAlignedBB[] getScanBoxes(ZoneType type) {
    return zone(type).scanBoxes().clone();
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s standard
   * (through) zone and returns the number of distinct entities found across its boxes. Eligible
   * entities are those matched by {@link TrafficEntitySelectors#VEHICLE}.
   *
   * @return the number of eligible entities found within the standard zone
   *
   * @since 1.0
   */
  public int scanEntities() {
    return countEntities(zone(ZoneType.STANDARD));
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s left turn lane
   * zone and returns the number of distinct entities found across its boxes.
   *
   * @return the number of eligible entities found within the left turn lane zone
   *
   * @since 1.0
   */
  public int scanLeftEntities() {
    return countEntities(zone(ZoneType.LEFT));
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s protected lane
   * zone and returns the number of distinct entities found across its boxes.
   *
   * @return the number of eligible entities found within the protected lane zone
   *
   * @since 1.0
   */
  public int scanProtectedEntities() {
    return countEntities(zone(ZoneType.PROTECTED));
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s right turn lane
   * zone and returns the number of distinct entities found across its boxes.
   *
   * @return the number of eligible entities found within the right turn lane zone
   *
   * @since 1.0
   */
  public int scanRightEntities() {
    return countEntities(zone(ZoneType.RIGHT));
  }

  // --- Zone status + clearing (used by the sensor configuration GUI) ---

  /** @return {@code true} if the standard (through) zone has at least one box. */
  public boolean hasStandardZone() {
    return getBoxCount(ZoneType.STANDARD) > 0;
  }

  /** @return {@code true} if the left-turn zone has at least one box. */
  public boolean hasLeftZone() {
    return getBoxCount(ZoneType.LEFT) > 0;
  }

  /** @return {@code true} if the right-turn zone has at least one box. */
  public boolean hasRightZone() {
    return getBoxCount(ZoneType.RIGHT) > 0;
  }

  /** @return {@code true} if the protected zone has at least one box. */
  public boolean hasProtectedZone() {
    return getBoxCount(ZoneType.PROTECTED) > 0;
  }

  /** Clears every box of the standard (through) zone. */
  public void clearStandardZone() {
    clearZone(ZoneType.STANDARD);
  }

  /** Clears every box of the left-turn zone. */
  public void clearLeftZone() {
    clearZone(ZoneType.LEFT);
  }

  /** Clears every box of the right-turn zone. */
  public void clearRightZone() {
    clearZone(ZoneType.RIGHT);
  }

  /** Clears every box of the protected zone. */
  public void clearProtectedZone() {
    clearZone(ZoneType.PROTECTED);
  }

  /** Clears all four zones in a single sync. */
  public void clearAllZones() {
    for (Zone zone : zones) {
      zone.boxes.clear();
      zone.changed();
    }
    zonesChanged();
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s standard zone
   * and returns a list of tuples containing each entity's ID and current position, each entity
   * once however many of the zone's boxes it is in. This method is used by the wrong way detection
   * system to track entity movement across ticks.
   *
   * @return a list of {@link Tuple}s where the first element is the entity ID ({@link Integer}) and
   *     the second element is the entity's current position ({@link Vec3d}), or an empty list if no
   *     eligible entities are found or the standard zone has no boxes
   *
   * @since 1.0
   */
  public List<Tuple<Integer, Vec3d>> scanEntitiesWithPositions() {
    List<Tuple<Integer, Vec3d>> results = new ArrayList<>();
    if (world == null) {
      return results;
    }
    AxisAlignedBB[] boxes = zone(ZoneType.STANDARD).scanBoxes();
    // Single predicate sweep per box -- see countEntities for why two class-filtered calls
    // cost twice as much as one predicate call for the same set of entities.
    Set<Integer> seen = boxes.length > 1 ? new HashSet<>() : null;
    for (AxisAlignedBB box : boxes) {
      for (Entity entity : world.getEntitiesWithinAABB(Entity.class, box, ELIGIBLE_ENTITY)) {
        if (seen == null || seen.add(entity.getEntityId())) {
          results.add(new Tuple<>(entity.getEntityId(), entity.getPositionVector()));
        }
      }
    }
    return results;
  }

  /**
   * No baked model reads this tile entity -- only its special renderer, which reads it every
   * frame -- so a sync never needs the chunk section rebuilt.
   */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}
