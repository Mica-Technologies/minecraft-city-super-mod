package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * A light-gauge steel stud wall: galvanised C-studs with punched knockouts, standing in top and
 * bottom track.
 *
 * <p>The first member of the framing family, and the one the rest are measured against. Its
 * framing kind is {@code steel}, so it joins the other steel members and meets wood framing at a
 * corner rather than running into it.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSteelStudWall extends BlockFramingWall {

  /** The framing system this block belongs to. */
  public static final String FRAMING_KIND = "steel";

  /**
   * Constructs a {@link BlockSteelStudWall}.
   *
   * @since 1.0
   */
  public BlockSteelStudWall() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 1.5F, 8F);
  }

  @Override
  public String getBlockRegistryName() {
    return "steel_stud_wall";
  }

  @Override
  public String getFramingKind() {
    return FRAMING_KIND;
  }
}
