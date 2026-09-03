package com.micatechnologies.minecraft.csm.roads;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Client proxy for the Roads & Traffic module: binds this module's tile-entity special renderers (and any
 * client-only overlays) once Forge reaches initialization. These bindings used to live in Core's
 * client proxy; they are here so Core never names a class that belongs to a module.
 */
public class CsmRoadsClientProxy extends CsmRoadsCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    // Bind the TESR to the TileEntity
    ClientRegistry.bindTileEntitySpecialRenderer(com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead.class, new com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHeadRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntitySignalBackplate.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntitySignalBackplateRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignal.class, new com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNew.class, new com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNewRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox.class, new com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBoxRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityLaneControlSignal.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityLaneControlSignalRenderer());
    // Span wire: both ends of every cable segment draw their own piece, so both attachment
    // kinds get the same renderer behaviour bound to their own tile entity type.
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireAnchor.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireAnchorRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireHanger.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireHangerRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireClusterMount.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireClusterMountRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireDisconnectBox.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireHangerRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficLightMountKit.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficLightMountKitRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficLightCover.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficLightCoverRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPortableMessageSign.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPortableMessageSignRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadMessageSign.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadMessageSignRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityVariableSpeedLimit.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPortableSpeedLimitRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadSpeedLimit.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadSpeedLimitRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPoleMountSpeedLimit.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPoleMountSpeedLimitRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicGuideSign.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicGuideSignRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicStreetSign.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicStreetSignRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficBeacon.class,
        new com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityTrafficBeaconRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTattleTaleBeacon.class,
        new com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTattleTaleBeaconRenderer());
  }
}
