package com.micatechnologies.minecraft.csm.parks.planting;

import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import com.micatechnologies.minecraft.csm.parks.trees.TreeWood;
import java.util.function.Consumer;

/**
 * The trees the Tree Planting Tool grows: a generator shape plus the species' parameters. Each
 * planting draws its height, lean and limbs within these ranges, so a planted row is alike but
 * not cloned.
 *
 * <p>The order is the tool's mode order, and the ordinal is saved on the tool; add presets at the
 * end.</p>
 *
 * @since 2026.9
 */
public enum TreePreset {

  /** Southern live oak: a short leaning trunk, long level limbs arching over a street, moss. */
  LIVE_OAK("liveoak", Shape.LIMB, TreeWood.LIVE_OAK, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_liveoak", "spanish_moss",
      p -> p.trunk(3, 4).lean(1, 2).limbs(4, 5).reach(6, 9).rise(3, 5).cluster(3.2, 1.6)
          .clearance(5).backLimb(true)),
  /** American elm: a trunk forking into limbs that rise steeply and arch out, a vase. */
  ELM("elm", Shape.LIMB, TreeWood.ELM, TreeLogWidth.THICK, TreeLogWidth.MEDIUM, "tree_leaves_elm",
      null,
      p -> p.trunk(4, 5).lean(0, 1).limbs(3, 4).reach(3, 5).rise(6, 8).cluster(3.0, 2.0)
          .clearance(5).spread(1.6)),
  /** London plane: a tall trunk and a broad, rounded crown. */
  PLANE("plane", Shape.LIMB, TreeWood.PLANE, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_plane", null,
      p -> p.trunk(5, 6).lean(0, 1).limbs(4, 4).reach(3, 5).rise(3, 5).cluster(3.0, 2.4)
          .clearance(5).spread(2.2)),
  /** Honey locust: a slender leaning trunk and an airy, one-sided head. */
  HONEY_LOCUST("honeylocust", Shape.LIMB, TreeWood.HONEY_LOCUST, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_honeylocust", null,
      p -> p.trunk(5, 6).lean(1, 2).limbs(3, 3).reach(3, 4).rise(2, 4)
          .cluster(2.6, 1.6).clearance(4)),
  /** Italian cypress: a one-wide column of foliage. */
  CYPRESS("cypress", Shape.PROFILE, TreeWood.CYPRESS, TreeLogWidth.THIN, TreeLogWidth.THIN,
      "tree_leaves_cypress", null,
      p -> p.trunk(2, 2).height(10, 14).cluster(0.4, 0)),
  /** Ginkgo 'Princeton Sentry': a narrow upright cone. */
  GINKGO("ginkgo", Shape.PROFILE, TreeWood.GINKGO, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_ginkgo", null,
      p -> p.trunk(3, 3).height(11, 13).cluster(1.7, 0)),
  /** Mexican fan palm: very tall and thin, a small crown, sometimes a skirt. */
  FAN_PALM("fanpalm", Shape.PALM, TreeWood.PALM, TreeLogWidth.THIN, TreeLogWidth.THIN,
      "tree_crown_palm_fan", "tree_crown_palm_fan_skirt",
      p -> p.height(14, 20).lean(0, 0)),
  /** A feather palm leaning out on a curving trunk. */
  LEANING_PALM("leaningpalm", Shape.PALM, TreeWood.PALM, TreeLogWidth.THIN, TreeLogWidth.THIN,
      "tree_crown_palm_feather", null,
      p -> p.height(8, 11).lean(2, 4)),
  /** A clipped round head on a clear trunk, the European street tree. */
  LOLLIPOP_PLANE("lollipopplane", Shape.HEAD, TreeWood.PLANE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_plane", null,
      p -> p.trunk(4, 5).cluster(2.3, 2.3));

  /** The four generator shapes. */
  public enum Shape {
    PROFILE, LIMB, PALM, HEAD
  }

  public final String id;
  public final Shape shape;
  public final TreeWood wood;
  public final TreeLogWidth trunkWidth;
  public final TreeLogWidth limbWidth;
  /** The leaves block, or the palm crown. */
  public final String leaves;
  /** Moss hung under the crown; for a palm, the alternative (skirted) crown. May be null. */
  public final String extra;

  int trunkMin = 4;
  int trunkMax = 5;
  int heightMin;
  int heightMax;
  int leanMin;
  int leanMax;
  int limbsMin;
  int limbsMax;
  int reachMin;
  int reachMax;
  int riseMin;
  int riseMax;
  double clusterRx;
  double clusterRy;
  int clearance;
  boolean backLimb;
  double spread = 0.9;

  TreePreset(String id, Shape shape, TreeWood wood, TreeLogWidth trunkWidth,
      TreeLogWidth limbWidth, String leaves, String extra, Consumer<TreePreset> setup) {
    this.id = id;
    this.shape = shape;
    this.wood = wood;
    this.trunkWidth = trunkWidth;
    this.limbWidth = limbWidth;
    this.leaves = leaves;
    this.extra = extra;
    setup.accept(this);
  }

  private TreePreset trunk(int min, int max) {
    trunkMin = min;
    trunkMax = max;
    return this;
  }

  private TreePreset height(int min, int max) {
    heightMin = min;
    heightMax = max;
    return this;
  }

  private TreePreset lean(int min, int max) {
    leanMin = min;
    leanMax = max;
    return this;
  }

  private TreePreset limbs(int min, int max) {
    limbsMin = min;
    limbsMax = max;
    return this;
  }

  private TreePreset reach(int min, int max) {
    reachMin = min;
    reachMax = max;
    return this;
  }

  private TreePreset rise(int min, int max) {
    riseMin = min;
    riseMax = max;
    return this;
  }

  private TreePreset cluster(double rx, double ry) {
    clusterRx = rx;
    clusterRy = ry;
    return this;
  }

  private TreePreset clearance(int blocks) {
    clearance = blocks;
    return this;
  }

  private TreePreset backLimb(boolean value) {
    backLimb = value;
    return this;
  }

  /** How widely the limbs fan out around the lean direction, in radians either side. */
  private TreePreset spread(double radians) {
    spread = radians;
    return this;
  }

  /** The lang key of the preset's name. */
  public String getTranslationKey() {
    return "csm.parks.preset." + id;
  }
}
