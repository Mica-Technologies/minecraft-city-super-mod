package com.micatechnologies.minecraft.csm.furniture.appliance;

import net.minecraft.block.properties.PropertyBool;

/**
 * A block that is a working appliance: it holds a {@link TileEntityAppliance}, whose screen
 * (the {@link #GUI_ID}) opens on right-click, and it says what kind of appliance it is through
 * its {@link ApplianceSpec}. {@link ApplianceHelper} does the parts every such block shares
 * (right-click, water, drops, comparator).
 *
 * <p>A block that has a {@link #RUNNING} property is told when it starts and stops working, so
 * its blockstate can light a window or a lamp; it should store the property in its metadata and
 * give {@link ApplianceSpec#getLight()} while it is true.</p>
 *
 * @since 2026.9
 */
public interface IAppliance {

  /** Whether the appliance is working: its oven lamp on, its toaster glowing. */
  PropertyBool RUNNING = PropertyBool.create("running");

  /** The appliance screen's GUI id, unique across the mod. */
  int GUI_ID = 36;

  /** The tile entity's registered name. */
  String TILE_ENTITY_NAME = "tileentityappliance";

  /**
   * What kind of appliance this is.
   *
   * @return the spec, shared by every block of the kind
   */
  ApplianceSpec getApplianceSpec();
}
