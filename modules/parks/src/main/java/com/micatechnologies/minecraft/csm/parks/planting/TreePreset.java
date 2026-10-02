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
          .clearance(5).spread(1.6).fork(0.6)),
  /** London plane: a tall trunk and a broad, rounded crown. */
  PLANE("plane", Shape.LIMB, TreeWood.PLANE, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_plane", null,
      p -> p.trunk(5, 6).lean(0, 1).limbs(4, 4).reach(3, 5).rise(3, 5).cluster(3.0, 2.4)
          .clearance(5).spread(2.2).fork(0.3)),
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
          .clearance(4).spread(2.6).fork(0.4)),
  /** California pepper tree: a gnarled leaning trunk and weeping tips. */
  PEPPER_TREE("peppertree", Shape.LIMB, TreeWood.PEPPER, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_pepper", null,
      p -> p.trunk(2, 3).lean(1, 2).limbs(4, 5).reach(3, 5).rise(1, 3).cluster(2.8, 1.8)
          .clearance(3).spread(1.8).fork(0.3)),
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
      p -> p.trunk(4, 5).limbs(4, 5).cluster(1.4, 1.1)),
  /** Paper birch, New Hampshire's state tree: a slim white trunk, often forked, a light crown. */
  PAPER_BIRCH("paperbirch", Shape.LIMB, TreeWood.BIRCH, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_birch", null,
      p -> p.trunk(5, 6).lean(0, 1).limbs(3, 4).reach(2, 3).rise(3, 5).cluster(2.2, 1.8)
          .clearance(4).spread(1.4).fork(0.5)),
  /** Colorado blue spruce: a dense, silver-blue cone from nearly the ground up. */
  BLUE_SPRUCE("bluespruce", Shape.PROFILE, TreeWood.SPRUCE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_spruce_blue", null,
      p -> p.trunk(1, 2).height(12, 15).cluster(2.6, 0)),
  /**
   * Cabbage palm (sabal), Florida's state tree: a stout trunk criss-crossed with old frond bases
   * and a dense round head of big fans, more often than not still wearing its dead fronds.
   */
  CABBAGE_PALM("cabbagepalm", Shape.PALM, TreeWood.SABAL, TreeLogWidth.MEDIUM,
      TreeLogWidth.MEDIUM, "tree_crown_palm_cabbage_skirt", "tree_crown_palm_cabbage",
      p -> p.height(9, 13).lean(0, 2)),
  /** Japanese maple: a low, many-limbed dome of red leaves, for a garden rather than a street. */
  JAPANESE_MAPLE("japanesemaple", Shape.LIMB, TreeWood.MAPLE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_maple_japanese", null,
      p -> p.trunk(1, 2).lean(0, 1).limbs(4, 5).reach(2, 4).rise(1, 2).cluster(2.4, 1.2)
          .clearance(2).spread(2.6).fork(0.5)),
  /** Scots pine: a tall bare orange trunk and a flat-topped crown of blue-green clumps. */
  SCOTS_PINE("scotspine", Shape.LIMB, TreeWood.PINE, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_pine", null,
      p -> p.trunk(7, 9).lean(0, 2).limbs(3, 4).reach(2, 3).rise(1, 3).cluster(2.4, 1.2)
          .clearance(7).spread(2.4)),
  /** European beech, Denmark's national tree: a smooth grey trunk and a broad, dense dome. */
  EUROPEAN_BEECH("beech", Shape.LIMB, TreeWood.BEECH, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_beech", null,
      p -> p.trunk(3, 4).lean(0, 1).limbs(4, 5).reach(3, 5).rise(3, 5).cluster(3.0, 2.2)
          .clearance(4).spread(2.2).fork(0.3)),
  /**
   * Coast redwood, California: the tallest tree there is, a full-width trunk clear for a long way
   * and a narrow spire of dark foliage above it.
   */
  COAST_REDWOOD("coastredwood", Shape.PROFILE, TreeWood.REDWOOD, TreeLogWidth.FULL,
      TreeLogWidth.THICK, "tree_leaves_redwood", null,
      p -> p.trunk(7, 9).height(30, 36).cluster(3.2, 0)),
  /**
   * Eastern white pine, New Hampshire's state tree: a tall straight trunk with its limbs in
   * whorls, each tipped with a flat pad of soft needles, the layers shorter toward the top.
   */
  EASTERN_WHITE_PINE("whitepine", Shape.TIERED, TreeWood.WHITE_PINE, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_pine_white", null,
      p -> p.trunk(7, 9).height(22, 27).lean(0, 1).limbs(3, 4).reach(4, 6).rise(0, 1)
          .cluster(3.0, 1.2).clearance(6)),
  /**
   * An old English oak, the kind Denmark's Kongeegen and Sweden's hundreds-of-years oaks are: a
   * full-width trunk, heavy limbs reaching far out and a vast, broad crown.
   */
  ENGLISH_OAK("englishoak", Shape.LIMB, TreeWood.OAK, TreeLogWidth.FULL, TreeLogWidth.THICK,
      "tree_leaves_oak", null,
      p -> p.trunk(5, 6).lean(0, 1).limbs(6, 7).reach(5, 7).rise(4, 6).cluster(4.2, 2.8)
          .clearance(5).spread(2.8).backLimb(true).fork(0.4)),
  /**
   * Camphor tree (kusunoki), Japan's great shrine trees: a massive trunk forking low into limbs
   * that climb to a high, dense, glossy dome.
   */
  CAMPHOR_TREE("camphor", Shape.LIMB, TreeWood.CAMPHOR, TreeLogWidth.FULL, TreeLogWidth.THICK,
      "tree_leaves_camphor", null,
      p -> p.trunk(4, 5).lean(0, 1).limbs(6, 7).reach(4, 6).rise(5, 7).cluster(4.4, 3.4)
          .clearance(5).spread(3.0).fork(0.6)),

  // GitHub #250: trees of the American West and its deserts.

  /**
   * Giant sequoia, the Sierra Nevada's: the most massive tree there is, a cinnamon trunk three
   * blocks across flaring into buttresses at its foot and hardly narrowing up to a rounded crown
   * of clumped foliage high up. Not the coast redwood's slender spire.
   */
  GIANT_SEQUOIA("giantsequoia", Shape.GIANT, TreeWood.SEQUOIA, TreeLogWidth.FULL,
      TreeLogWidth.MEDIUM, "tree_leaves_sequoia", null,
      p -> p.trunk(11, 14).height(28, 33).limbs(2, 3).reach(4, 6).rise(0, 2)
          .cluster(2.0, 1.3)),
  /**
   * Joshua tree (Yucca brevifolia), the Mojave's: a shaggy trunk branching again and again into
   * thick, angular arms, each ending in a rosette of stiff dagger leaves. The limbs are how many
   * times it branches.
   */
  JOSHUA_TREE("joshuatree", Shape.BRANCHING, TreeWood.JOSHUA, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_crown_joshua", null,
      p -> p.trunk(3, 4).lean(0, 1).limbs(3, 3).reach(1, 1).rise(1, 2)),
  /** A young Joshua tree: one trunk, branched once if at all, one to three rosettes. */
  YOUNG_JOSHUA_TREE("youngjoshua", Shape.BRANCHING, TreeWood.JOSHUA, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_crown_joshua", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(0, 1).reach(1, 1).rise(1, 2)),
  /**
   * Canary Island date palm, the "pineapple palm" of California avenues: a very thick straight
   * trunk patterned with diamond leaf bases, the pineapple knob, and a huge, dense round crown.
   */
  CANARY_PALM("canarypalm", Shape.PALM, TreeWood.CANARY, TreeLogWidth.THICK, TreeLogWidth.THICK,
      "tree_crown_palm_canary", null,
      p -> p.height(7, 11).lean(0, 0)),
  /** Coconut palm: a slender grey ringed trunk curving out from a swollen foot, drooping fronds. */
  COCONUT_PALM("coconutpalm", Shape.PALM, TreeWood.PALM_GREY, TreeLogWidth.THIN,
      TreeLogWidth.THIN, "tree_crown_palm_coconut", null,
      p -> p.height(9, 13).lean(1, 2).base(TreeLogWidth.MEDIUM).planar()),
  /** King palm: a slender smooth grey trunk, a bright green crownshaft and arching fronds. */
  KING_PALM("kingpalm", Shape.PALM, TreeWood.PALM_GREY, TreeLogWidth.THIN, TreeLogWidth.THIN,
      "tree_crown_palm_king", null,
      p -> p.height(10, 14).lean(0, 0).planar()),
  /**
   * Douglas fir, of the Pacific Northwest: a tall, narrow cone of soft, dark foliage in drooping
   * layers nearly to the ground, on thick, deeply furrowed bark.
   */
  DOUGLAS_FIR("douglasfir", Shape.PROFILE, TreeWood.DOUGLAS_FIR, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_douglasfir", null,
      p -> p.trunk(2, 4).height(22, 27).cluster(3.6, 0).cone(0.35)),
  /**
   * Great Basin bristlecone pine, the oldest trees alive: low and wide rather than tall, a squat
   * twisted trunk and gnarled limbs, many of them bare silver deadwood, the living ones tipped
   * with short foxtails of needles.
   */
  BRISTLECONE_PINE("bristlecone", Shape.GNARLED, TreeWood.BRISTLECONE, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_bristlecone", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(4, 6).reach(3, 5).cluster(1.0, 1.1).clearance(1)
          .spread(2.6).dead(0.55)),
  /**
   * California sycamore (Platanus racemosa): leaning, often several trunks from one foot,
   * mottled white bark, and an irregular, open crown of big maple-like leaves.
   */
  CALIFORNIA_SYCAMORE("sycamore", Shape.LIMB, TreeWood.SYCAMORE, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_plane", null,
      p -> p.trunk(3, 5).lean(1, 3).limbs(5, 6).reach(2, 5).rise(4, 7).cluster(2.0, 1.7)
          .clearance(4).spread(2.2).stems(2, 3)),
  /**
   * Blue gum eucalyptus, planted all over California: very tall and straight, bark peeling in
   * long streaks, a few long limbs rising steeply from high up and diverging, and an open crown
   * of hanging sickle leaves in clumps. Few limbs and no forked leader, or the steep limbs stand
   * side by side like the rungs of a ladder.
   */
  BLUE_GUM("bluegum", Shape.LIMB, TreeWood.BLUE_GUM, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_bluegum", null,
      p -> p.trunk(10, 13).lean(0, 1).limbs(3, 4).reach(4, 6).rise(5, 8).cluster(2.0, 1.7)
          .clearance(10).spread(2.8).fork(0.15)),

  // GitHub #250: fruit trees. Each bears its fruit on its leaves block (a fruiting set), so the
  // fruit shows wherever the crown does, at no cost in quads.

  /**
   * Orange: a small, dense, rounded evergreen on a short trunk, glossy dark leaves, oranges all
   * over it. Citrus are the head shape: a grove's trees are a dome nearly to the ground.
   */
  ORANGE("orange", Shape.HEAD, TreeWood.CITRUS, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_citrus_orange", null,
      p -> p.trunk(1, 2).cluster(2.3, 2.3)),
  /** Lemon: a little smaller than the orange, yellow lemons. */
  LEMON("lemon", Shape.HEAD, TreeWood.CITRUS, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_citrus_lemon", null,
      p -> p.trunk(1, 2).cluster(2.1, 2.1)),
  /** Lime: the smallest citrus, small green limes. */
  LIME("lime", Shape.HEAD, TreeWood.CITRUS, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_citrus_lime", null,
      p -> p.trunk(1, 1).cluster(2.0, 2.0)),
  /** Grapefruit: the largest citrus, on a taller trunk, big pale yellow-pink fruit. */
  GRAPEFRUIT("grapefruit", Shape.HEAD, TreeWood.CITRUS, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_citrus_grapefruit", null,
      p -> p.trunk(2, 2).cluster(2.7, 2.7)),
  /** Avocado: taller, a broad dense crown of big leathery leaves, dark fruit hanging inside. */
  AVOCADO("avocado", Shape.LIMB, TreeWood.AVOCADO, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_avocado", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(5, 5).reach(1, 3).rise(4, 6).cluster(2.4, 2.3)
          .clearance(2).spread(2.0).fork(0.4)),
  /**
   * Olive: a short, gnarled, twisted grey trunk, often split into several from the foot, and an
   * open, silvery grey-green crown.
   */
  OLIVE("olive", Shape.LIMB, TreeWood.OLIVE, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_olive", null,
      p -> p.trunk(3, 3).lean(1, 2).limbs(4, 5).reach(2, 3).rise(2, 3).cluster(2.2, 1.5)
          .clearance(2).spread(2.6).stems(2, 3)),
  /**
   * Banana: not a tree. A pseudostem of leaf sheaths with a crown of huge torn paddle leaves and
   * a hanging bunch with its purple bell, and younger suckers round it from the same corm.
   */
  BANANA("banana", Shape.CLUMP, TreeWood.BANANA, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_crown_banana_fruit", "tree_crown_banana",
      p -> p.height(3, 4).stems(2, 3)),
  /** Honeycrisp apple: an orchard tree, short trunk and wide low limbs, red-blushed apples. */
  HONEYCRISP_APPLE("applehoneycrisp", Shape.LIMB, TreeWood.APPLE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_apple_honeycrisp", null,
      p -> p.trunk(2, 2).lean(0, 1).limbs(4, 5).reach(2, 3).rise(2, 3).cluster(1.8, 1.4)
          .clearance(2).spread(2.8)),
  /** Granny Smith apple: the same orchard tree, green apples. */
  GRANNY_SMITH_APPLE("applegranny", Shape.LIMB, TreeWood.APPLE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_apple_granny", null,
      p -> p.trunk(2, 2).lean(0, 1).limbs(4, 5).reach(2, 3).rise(2, 3).cluster(1.8, 1.4)
          .clearance(2).spread(2.8)),
  /** Golden Delicious apple: the same orchard tree, yellow apples. */
  GOLDEN_DELICIOUS_APPLE("applegolden", Shape.LIMB, TreeWood.APPLE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_apple_golden", null,
      p -> p.trunk(2, 2).lean(0, 1).limbs(4, 5).reach(2, 3).rise(2, 3).cluster(1.8, 1.4)
          .clearance(2).spread(2.8)),
  /** Mulberry: a broad, rounded, dense crown of big glossy leaves, dark and red berries. */
  MULBERRY("mulberry", Shape.LIMB, TreeWood.MULBERRY, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_mulberry", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(5, 6).reach(2, 3).rise(2, 3).cluster(2.5, 2.0)
          .clearance(2).spread(2.8).fork(0.3)),

  // GitHub #250: ornamental trees.

  /** Sugar maple: the big street and forest maple, a broad dense oval crown, five-lobed leaves. */
  SUGAR_MAPLE("sugarmaple", Shape.LIMB, TreeWood.SUGAR_MAPLE, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_sugarmaple", null,
      p -> p.trunk(3, 4).lean(0, 1).limbs(5, 6).reach(2, 3).rise(5, 7).cluster(2.8, 3.0)
          .clearance(3).spread(2.2).fork(0.5)),
  /**
   * Southern magnolia: an evergreen of big glossy dark leaves, rusty beneath, in a dense
   * pyramid to oval nearly to the ground, big white cup flowers over its outside.
   */
  MAGNOLIA("magnolia", Shape.PROFILE, TreeWood.MAGNOLIA, TreeLogWidth.MEDIUM, TreeLogWidth.THIN,
      "tree_leaves_magnolia", null,
      p -> p.trunk(2, 3).height(12, 15).cluster(3.6, 0)),
  /** White willow: upright and broad, an irregular crown of narrow silvery leaves, not weeping. */
  WHITE_WILLOW("whitewillow", Shape.LIMB, TreeWood.WILLOW, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_whitewillow", null,
      p -> p.trunk(3, 4).lean(0, 1).limbs(5, 6).reach(2, 4).rise(5, 7).cluster(2.4, 2.4)
          .clearance(4).spread(1.8).fork(0.6)),
  /**
   * West Indies mahogany: a tall straight trunk of dark reddish-brown furrowed bark and a broad,
   * umbrella-like crown of small pinnate leaves.
   */
  MAHOGANY("mahogany", Shape.LIMB, TreeWood.MAHOGANY, TreeLogWidth.THICK, TreeLogWidth.MEDIUM,
      "tree_leaves_mahogany", null,
      p -> p.trunk(7, 9).lean(0, 1).limbs(5, 6).reach(4, 6).rise(2, 4).cluster(3.0, 1.8)
          .clearance(7).spread(2.8).fork(0.4)),
  /**
   * Crape myrtle: three to five smooth, mottled, peeling stems from one foot in a vase, the crown
   * covered in crinkled pink flowers.
   */
  CRAPE_MYRTLE("crapemyrtle", Shape.LIMB, TreeWood.CRAPE_MYRTLE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_crapemyrtle", null,
      p -> p.trunk(4, 5).lean(0, 1).limbs(4, 5).reach(1, 2).rise(2, 3).cluster(1.9, 2.0)
          .clearance(3).spread(2.0).stems(4, 5).footStems()),
  /** A dwarf crape myrtle: a shrubby ball of flowers two or three blocks high. */
  DWARF_CRAPE_MYRTLE("crapemyrtledwarf", Shape.HEAD, TreeWood.CRAPE_MYRTLE, TreeLogWidth.THIN,
      TreeLogWidth.THIN, "tree_leaves_crapemyrtle", null,
      p -> p.trunk(1, 1).cluster(1.3, 1.3)),
  /**
   * Chinese chestnut: low, broad and rounded, spreading, long glossy leaves and spiny green burs
   * dotted through the crown.
   */
  CHINESE_CHESTNUT("chinesechestnut", Shape.LIMB, TreeWood.CHESTNUT, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_chestnut", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(5, 6).reach(3, 4).rise(2, 3).cluster(2.8, 1.9)
          .clearance(3).spread(2.8).fork(0.5)),
  /** Trident maple: a small rounded tree, three-lobed leaves, peeling orange-brown bark. */
  TRIDENT_MAPLE("tridentmaple", Shape.LIMB, TreeWood.TRIDENT_MAPLE, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_tridentmaple", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(4, 5).reach(2, 3).rise(2, 3).cluster(2.3, 1.9)
          .clearance(2).spread(2.6).fork(0.4)),
  /** A dwarf jacaranda: the jacaranda's purple umbrella on a tree three to five blocks high. */
  DWARF_JACARANDA("dwarfjacaranda", Shape.LIMB, TreeWood.JACARANDA, TreeLogWidth.MEDIUM,
      TreeLogWidth.THIN, "tree_leaves_jacaranda_blossom", null,
      p -> p.trunk(2, 2).lean(0, 1).limbs(4, 5).reach(2, 3).rise(1, 2).cluster(2.2, 1.0)
          .clearance(2).spread(2.6).fork(0.4)),
  /**
   * Plumeria (frangipani): thick, blunt grey branches forking like a candelabra, bare but for a
   * tuft of big leaves and flowers on the end of each.
   */
  PLUMERIA("plumeria", Shape.BRANCHING, TreeWood.PLUMERIA, TreeLogWidth.MEDIUM,
      TreeLogWidth.MEDIUM, "tree_crown_plumeria", null,
      p -> p.trunk(2, 2).lean(0, 1).limbs(2, 3).reach(1, 1).rise(1, 2)),
  /**
   * Yoshino cherry (Somei-yoshino), the cherry of Japan's hanami and Washington's Tidal Basin: a
   * short dark trunk, limbs arching out and a little up into a wide, spreading umbrella, a soft
   * cloud of pale blush blossom with hardly a leaf, and its petals fallen on the ground beneath.
   */
  YOSHINO_CHERRY("cherryyoshino", Shape.LIMB, TreeWood.CHERRY, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_cherry_yoshino", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(5, 6).reach(3, 5).rise(2, 3).cluster(3.0, 1.7)
          .clearance(2).spread(2.8).fork(0.6).ground("ground_cherry_petals", 0.4)),
  /**
   * Kanzan cherry: a more upright vase of limbs carrying full pom-poms of double, deeper pink
   * flowers.
   */
  KANZAN_CHERRY("cherrykanzan", Shape.LIMB, TreeWood.CHERRY, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_cherry_kanzan", null,
      p -> p.trunk(2, 3).lean(0, 1).limbs(5, 6).reach(2, 3).rise(4, 5).cluster(2.5, 2.0)
          .clearance(3).spread(2.0).fork(0.7).ground("ground_cherry_petals", 0.4)),
  /**
   * Weeping cherry (Shidare-zakura): limbs arching out and over, the blossom hanging from them
   * in curtains of pink.
   */
  WEEPING_CHERRY("cherryweeping", Shape.LIMB, TreeWood.CHERRY, TreeLogWidth.THICK,
      TreeLogWidth.MEDIUM, "tree_leaves_cherry_weeping", null,
      p -> p.trunk(3, 4).lean(0, 1).limbs(5, 6).reach(2, 4).rise(2, 3).cluster(2.6, 1.8)
          .clearance(3).spread(2.8).fork(0.4).ground("ground_cherry_petals", 0.35));

  /** The generator shapes. */
  public enum Shape {
    PROFILE, LIMB, PALM, HEAD, BOX, POLLARD, TIERED, GIANT, BRANCHING, GNARLED, CLUMP
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
  /** The chance a limbed tree's trunk forks into two leaders, as an elm's does. */
  double forkChance;
  /** The chance a long limb puts out a side branch with its own cluster. */
  double branchChance = 0.45;
  /** Limbed trees: how many trunks grow from the one foot. */
  int stemsMin = 1;
  int stemsMax = 1;
  /** Palms: the trunk's width at its foot, or null to keep it the same all the way up. */
  TreeLogWidth baseWidth;
  /** Profile trees: a straight cone rather than a rounded spire, and how deep its layers are. */
  boolean cone;
  double layering;
  /** Palms: whether the trunk leans in one plane (along an axis), so its curve is smooth. */
  boolean planar;
  /** Limbed trees: whether the extra stems rise from round the foot (a vase), not from it. */
  boolean footStems;
  /** A ground cover scattered under the crown (fallen petals), and the share of cells it takes. */
  String groundCover;
  double groundChance;
  /** Gnarled trees: the share of limbs that are bare deadwood. */
  double deadChance;

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

  private TreePreset fork(double chance) {
    forkChance = chance;
    return this;
  }

  private TreePreset stems(int min, int max) {
    stemsMin = min;
    stemsMax = max;
    return this;
  }

  private TreePreset base(TreeLogWidth width) {
    baseWidth = width;
    return this;
  }

  /** A straight cone, its outline stepped in drooping layers by {@code layers} (0 smooth). */
  private TreePreset cone(double layers) {
    cone = true;
    layering = layers;
    return this;
  }

  private TreePreset planar() {
    planar = true;
    return this;
  }

  private TreePreset footStems() {
    footStems = true;
    return this;
  }

  private TreePreset ground(String block, double chance) {
    groundCover = block;
    groundChance = chance;
    return this;
  }

  private TreePreset dead(double chance) {
    deadChance = chance;
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
