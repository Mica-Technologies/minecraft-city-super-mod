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
  LINDEN("linden");

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
