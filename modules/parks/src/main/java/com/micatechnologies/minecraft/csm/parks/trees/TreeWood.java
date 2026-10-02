package com.micatechnologies.minecraft.csm.parks.trees;

/**
 * A tree species' wood: the bark its logs wear. The species' leaves are separate blocks.
 *
 * <p>The {@link #id} names the log blocks ({@code tree_log_<id>_<width>}) and the bark texture
 * ({@code csm:blocks/parks/bark_<id>}); both, and the lang lines, are written by
 * {@code dev-env-utils/scripts/gen_trees.py} from the same catalogue, which must list the same
 * woods in the same order.</p>
 *
 * @since 2026.9
 */
public enum TreeWood {
  LIVE_OAK("liveoak"),
  ELM("elm"),
  PLANE("plane"),
  HONEY_LOCUST("honeylocust"),
  CYPRESS("cypress"),
  GINKGO("ginkgo"),
  PALM("palm"),
  JACARANDA("jacaranda"),
  PEPPER("pepper"),
  POPLAR("poplar"),
  SWEETGUM("sweetgum"),
  HORNBEAM("hornbeam"),
  GUM("gum"),
  WILLOW("willow"),
  LINDEN("linden"),
  BIRCH("birch"),
  MAPLE("maple"),
  SPRUCE("spruce"),
  PINE("pine"),
  BEECH("beech"),
  SABAL("sabal"),
  REDWOOD("redwood"),
  WHITE_PINE("whitepine"),
  OAK("oak"),
  CAMPHOR("camphor"),
  SEQUOIA("sequoia"),
  JOSHUA("joshua"),
  CANARY("canary"),
  /** Coconut and king palms: smooth pale grey, ringed with leaf scars. */
  PALM_GREY("palmgrey"),
  DOUGLAS_FIR("douglasfir"),
  BRISTLECONE("bristlecone"),
  SYCAMORE("sycamore"),
  BLUE_GUM("bluegum"),
  CITRUS("citrus"),
  AVOCADO("avocado"),
  OLIVE("olive"),
  APPLE("apple"),
  MULBERRY("mulberry"),
  /** A banana's pseudostem: not wood at all, but its leaf sheaths rolled one round another. */
  BANANA("banana"),
  SUGAR_MAPLE("sugarmaple"),
  MAGNOLIA("magnolia"),
  MAHOGANY("mahogany"),
  CRAPE_MYRTLE("crapemyrtle"),
  CHESTNUT("chestnut"),
  TRIDENT_MAPLE("tridentmaple"),
  PLUMERIA("plumeria"),
  CHERRY("cherry");

  private final String id;

  TreeWood(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  /** The bark sprite, as a texture resource location. */
  public String getBarkTexture() {
    return "csm:blocks/parks/bark_" + id;
  }
}
