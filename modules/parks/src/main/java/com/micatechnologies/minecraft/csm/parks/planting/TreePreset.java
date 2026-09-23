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
      p -> p.trunk(4, 5).cluster(2.3, 2.3)),
  /** Jacaranda: a low, wide umbrella of arching limbs, in its purple blossom. */
  JACARANDA("jacaranda", Shape.LIMB, TreeWood.JACARANDA, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_jacaranda_blossom", null,
      p -> p.trunk(3, 4).lean(0, 1).limbs(5, 6).reach(3, 5).rise(2, 3).cluster(3.0, 1.4)
          .clearance(4).spread(2.6)),
  /** California pepper tree: a gnarled leaning trunk and weeping tips. */
  PEPPER_TREE("peppertree", Shape.LIMB, TreeWood.PEPPER, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_pepper", null,
      p -> p.trunk(2, 3).lean(1, 2).limbs(4, 5).reach(3, 5).rise(1, 3).cluster(2.8, 1.8)
          .clearance(3).spread(1.8)),
  /** Coast live oak: low and twisting, limbs reaching out close to the ground. */
  COAST_LIVE_OAK("coastliveoak", Shape.LIMB, TreeWood.LIVE_OAK, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_liveoak", null,
      p -> p.trunk(2, 3).lean(1, 2).limbs(4, 5).reach(4, 6).rise(1, 2).cluster(2.8, 1.6)
          .clearance(3).spread(2.2).backLimb(true)),
  /** Weeping willow, for parks rather than streets: a curtain of leaves to the ground. */
  WEEPING_WILLOW("weepingwillow", Shape.LIMB, TreeWood.WILLOW, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_willow", "willow_strands",
      p -> p.trunk(3, 4).lean(0, 1).limbs(5, 6).reach(3, 4).rise(3, 4).cluster(3.2, 2.2)
          .clearance(4).spread(2.8).hang(0.8, 4)),
  /** Lombardy poplar: a tall, narrow column. */
  LOMBARDY_POPLAR("poplar", Shape.PROFILE, TreeWood.POPLAR, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_poplar", null,
      p -> p.trunk(3, 3).height(16, 20).cluster(1.3, 0)),
  /** 'Slender Silhouette' sweetgum: barely wider than its trunk. */
  SWEETGUM("sweetgum", Shape.PROFILE, TreeWood.SWEETGUM, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_sweetgum", null,
      p -> p.trunk(3, 3).height(12, 14).cluster(1.1, 0)),
  /** Columnar hornbeam: a formal upright oval. */
  HORNBEAM("hornbeam", Shape.PROFILE, TreeWood.HORNBEAM, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_hornbeam", null,
      p -> p.trunk(2, 3).height(10, 12).cluster(1.8, 0)),
  /** Queen palm: straight or nearly, a feathery crown. */
  QUEEN_PALM("queenpalm", Shape.PALM, TreeWood.PALM, TreeLogWidth.THIN, TreeLogWidth.THIN,
      "tree_crown_palm_feather", null,
      p -> p.height(10, 12).lean(0, 1)),
  /** Lemon-scented gum: a tall smooth white trunk and a sparse crown high up. */
  LEMON_GUM("lemongum", Shape.LIMB, TreeWood.GUM, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_gum", null,
      p -> p.trunk(8, 10).lean(0, 1).limbs(3, 4).reach(2, 3).rise(3, 5).cluster(2.4, 1.6)
          .clearance(8).spread(2.4)),
  /** Emerald arborvitae: a small green column, for screening. */
  ARBORVITAE("arborvitae", Shape.PROFILE, TreeWood.CYPRESS, TreeLogWidth.THIN, TreeLogWidth.THIN,
      "tree_leaves_arborvitae", null,
      p -> p.trunk(1, 1).height(4, 5).cluster(0.4, 0)),
  /**
   * Pleached linden: a box-clipped crown on a clear trunk, wide across the way the planter
   * faces, so a row planted a few blocks apart joins into a hedge on stilts.
   */
  PLEACHED_LINDEN("pleachedlinden", Shape.BOX, TreeWood.LINDEN, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_linden_clipped", null,
      p -> p.trunk(4, 5).cluster(2.5, 3)),
  /** Pollarded plane: a stout trunk cut back to knuckles, each with a tight tuft of shoots. */
  POLLARDED_PLANE("pollardedplane", Shape.POLLARD, TreeWood.PLANE, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_plane", null,
      p -> p.trunk(4, 5).limbs(4, 5).cluster(1.4, 1.1));

  /** The generator shapes. */
  public enum Shape {
    PROFILE, LIMB, PALM, HEAD, BOX, POLLARD
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
  /** Of the crown's outer undersides, the share that get a hanging block, and its longest drop. */
  double hangChance = 0.22;
  int hangMax = 3;

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

  private TreePreset hang(double chance, int max) {
    hangChance = chance;
    hangMax = max;
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
