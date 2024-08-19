package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

public enum TrafficSignalVisorType implements IStringSerializable {
  CIRCLE("circle", "Circle",
      new ModelResourceLocation("csm:block/trafficsignals/visors/visor_circle")),
  TUNNEL("tunnel", "Tunnel",
      new ModelResourceLocation("csm:block/trafficsignals/visors/visor_tunnel")),
  CUTAWAY("cutaway", "Cutaway",
      new ModelResourceLocation("csm:block/trafficsignals/visors/visor_cap")),
  VERTICAL_LOUVERED("louvered_vertical", "Vertical Louvered",
      new ModelResourceLocation("csm:block/trafficsignals/visors/visor_louvered_vertical")),
  HORIZONTAL_LOUVERED("louvered_horizontal", "Horizontal Louvered",
      new ModelResourceLocation("csm:block/trafficsignals/visors/visor_louvered_horizontal")),
  BOTH_LOUVERED("louvered_both", "Vertical and Horizontal Louvered",
      new ModelResourceLocation("csm:block/trafficsignals/visors/visor_louvered_both")),
  NONE("none", "None", new ModelResourceLocation("csm:block/trafficsignals/visors/visor_none"));

  // Instance fields
  private final String name;          // The identifier used for serialization
  private final String friendlyName;  // The friendly name for display purposes
  private final ModelResourceLocation modelLocation; // The model location for rendering

  // Constructor
  TrafficSignalVisorType(String name, String friendlyName, ModelResourceLocation modelLocation) {
    this.name = name;
    this.friendlyName = friendlyName;
    this.modelLocation = modelLocation;
  }

  // Method to get the enum from NBT data
  public static TrafficSignalVisorType fromNBT(int ordinal) {
    int finalOrdinal = ordinal;
    if (ordinal < 0 || ordinal >= values().length) {
      finalOrdinal = 0;
    }
    return values()[finalOrdinal];
  }

  // Getter for the friendly name
  public String getFriendlyName() {
    return friendlyName;
  }

  // Getter for the model location
  public ModelResourceLocation getModelLocation() {
    return modelLocation;
  }

  // Method to get the next enum value in the sequence
  public TrafficSignalVisorType getNextVisorType() {
    int ordinal = ordinal();
    int nextOrdinal = ordinal + 1;
    if (nextOrdinal >= values().length) {
      nextOrdinal = 0;
    }
    return values()[nextOrdinal];
  }

  // Method to convert the enum to its ordinal value for NBT storage
  public int toNBT() {
    return ordinal();
  }

  // Overriding the getName method from IStringSerializable
  @Override
  public @NotNull String getName() {
    return this.name;
  }
}