package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockSV4;
import com.micatechnologies.minecraft.csm.trafficaccessories.*;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockSignpoststopsign;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic accessory blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order=9)
public class CsmTabTrafficAccessories extends CsmTab
{
    /**
     * Gets the ID (unique identifier) of the tab.
     *
     * @return the ID of the tab
     *
     * @since 1.0
     */
    @Override
    public String getTabId() {
        return "tabtrafficaccessories";
    }

    /**
     * Gets the block to use as the icon of the tab
     *
     * @return the block to use as the icon of the tab
     *
     * @since 1.0
     */
    @Override
    public Block getTabIcon() {
        return CsmRegistry.getBlock( "tlborderyellowblack" );
    }

    /**
     * Gets a boolean indicating if the tab is searchable (has its own search bar).
     *
     * @return {@code true} if the tab is searchable, otherwise {@code false}
     *
     * @since 1.0
     */
    @Override
    public boolean getTabSearchable() {
        return false;
    }

    /**
     * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
     *
     * @return {@code true} if the tab is hidden, otherwise {@code false}
     *
     * @since 1.0
     */
    @Override
    public boolean getTabHidden() {
        return false;
    }

    /**
     * Initializes all the items belonging to the tab.
     *
     * @since 1.0
     */
    @Override
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockControlBoxLarge.class, fmlPreInitializationEvent ); // ControlBoxLarge
        initTabBlock( BlockControlBoxLargeBlack.class, fmlPreInitializationEvent ); // ControlBoxLargeBlack
        initTabBlock( BlockControlBoxLargeMatteWhite.class, fmlPreInitializationEvent ); // ControlBoxLargeMatteWhite
        initTabBlock( BlockControlBoxLargeSilver.class, fmlPreInitializationEvent ); // ControlBoxLargeSilver
        initTabBlock( BlockControlBoxLargeTan.class, fmlPreInitializationEvent ); // ControlBoxLargeTan
        initTabBlock( BlockControlBoxLargeWhite.class, fmlPreInitializationEvent ); // ControlBoxLargeWhite
        initTabBlock( BlockControlBoxSmallBlack.class, fmlPreInitializationEvent ); // ControlBoxSmallBlack
        initTabBlock( BlockControlBoxSmallMatteWhite.class, fmlPreInitializationEvent ); // ControlBoxSmallMatteWhite
        initTabBlock( BlockControlBoxSmallMetal.class, fmlPreInitializationEvent ); // ControlBoxSmallMetal
        initTabBlock( BlockControlBoxSmallSilver.class, fmlPreInitializationEvent ); // ControlBoxSmallSilver
        initTabBlock( BlockControlBoxSmallTan.class, fmlPreInitializationEvent ); // ControlBoxSmallTan
        initTabBlock( BlockControlBoxSmallWhite.class, fmlPreInitializationEvent ); // ControlBoxSmallWhite
        initTabBlock( BlockDMPTBlack.class, fmlPreInitializationEvent ); // DMPTBlack
        initTabBlock( BlockDMPTSilver.class, fmlPreInitializationEvent ); // DMPTSilver
        initTabBlock( BlockDMPTTan.class, fmlPreInitializationEvent ); // DMPTTan
        initTabBlock( BlockDMPTUnpainted.class, fmlPreInitializationEvent ); // DMPTUnpainted
        initTabBlock( BlockDMPTWhite.class, fmlPreInitializationEvent ); // DMPTWhite
        initTabBlock( BlockFreewayCallBox.class, fmlPreInitializationEvent ); // FreewayCallBox
        initTabBlock( BlockMetalWireCenter.class, fmlPreInitializationEvent ); // MetalWireCenter
        initTabBlock( BlockMetalWireCenterTop.class, fmlPreInitializationEvent ); // MetalWireCenterTop
        initTabBlock( BlockMetalWireOffset.class, fmlPreInitializationEvent ); // MetalWireOffset
        initTabBlock( BlockMetalWireOffsetTop.class, fmlPreInitializationEvent ); // MetalWireOffsetTop
        initTabBlock( BlockSignalPoleMount2.class, fmlPreInitializationEvent ); // SignalPoleMount2
        initTabBlock( BlockTLBorder5AddOnBlackBlack.class, fmlPreInitializationEvent ); // TLBorder5AddOnBlackBlack
        initTabBlock( BlockTLBorder5AddOnBlackWhite.class, fmlPreInitializationEvent ); // TLBorder5AddOnBlackWhite
        initTabBlock( BlockTLBorder5AddOnBlackYellow.class, fmlPreInitializationEvent ); // TLBorder5AddOnBlackYellow
        initTabBlock( BlockTLBorder5AddOnLargeGray.class, fmlPreInitializationEvent ); // TLBorder5AddOnLargeGray
        initTabBlock( BlockTLBorder5AddOnTan.class, fmlPreInitializationEvent ); // TLBorder5AddOnTan
        initTabBlock( BlockTLBorder5AddOnWhiteBlack.class, fmlPreInitializationEvent ); // TLBorder5AddOnWhiteBlack
        initTabBlock( BlockTLBorder5AddOnYellowBlack.class, fmlPreInitializationEvent ); // TLBorder5AddOnYellowBlack
        initTabBlock( BlockTLBorderAddOnBlackBlack.class, fmlPreInitializationEvent ); // TLBorderAddOnBlackBlack
        initTabBlock( BlockTLBorderAddOnBlackWhite.class, fmlPreInitializationEvent ); // TLBorderAddOnBlackWhite
        initTabBlock( BlockTLBorderAddOnBlackYellow.class, fmlPreInitializationEvent ); // TLBorderAddOnBlackYellow
        initTabBlock( BlockTLBorderAddOnLargeGray.class, fmlPreInitializationEvent ); // TLBorderAddOnLargeGray
        initTabBlock( BlockTLBorderAddOnTan.class, fmlPreInitializationEvent ); // TLBorderAddOnTan
        initTabBlock( BlockTLBorderAddOnWhiteBlack.class, fmlPreInitializationEvent ); // TLBorderAddOnWhiteBlack
        initTabBlock( BlockTLBorderAddOnYellowBlack.class, fmlPreInitializationEvent ); // TLBorderAddOnYellowBlack
        initTabBlock( BlockTLBorderBlackBlack.class, fmlPreInitializationEvent ); // TLBorderBlackBlack
        initTabBlock( BlockTLBorderBlackBlack1288Inch.class, fmlPreInitializationEvent ); // TLBorderBlackBlack1288Inch
        initTabBlock( BlockTLBorderBlackBlack8812Inch.class, fmlPreInitializationEvent ); // TLBorderBlackBlack8812Inch
        initTabBlock( BlockTLBorderBlackBlack8Inch.class, fmlPreInitializationEvent ); // TLBorderBlackBlack8Inch
        initTabBlock( BlockTLBorderBlackWhite.class, fmlPreInitializationEvent ); // TLBorderBlackWhite
        initTabBlock( BlockTLBorderBlackYellow.class, fmlPreInitializationEvent ); // TLBorderBlackYellow
        initTabBlock( BlockTLBorderLArgeGray.class, fmlPreInitializationEvent ); // TLBorderLArgeGray
        initTabBlock( BlockTLBorderSingleBlackBlack.class, fmlPreInitializationEvent ); // TLBorderSingleBlackBlack
        initTabBlock( BlockTLBorderSingleBlackWhite.class, fmlPreInitializationEvent ); // TLBorderSingleBlackWhite
        initTabBlock( BlockTLBorderSingleBlackYellow.class, fmlPreInitializationEvent ); // TLBorderSingleBlackYellow
        initTabBlock( BlockTLBorderSingleTan.class, fmlPreInitializationEvent ); // TLBorderSingleTan
        initTabBlock( BlockTLBorderSingleWhiteBlack.class, fmlPreInitializationEvent ); // TLBorderSingleWhiteBlack
        initTabBlock( BlockTLBorderSingleYellowBlack.class, fmlPreInitializationEvent ); // TLBorderSingleYellowBlack
        initTabBlock( BlockTLBorderTan.class, fmlPreInitializationEvent ); // TLBorderTan
        initTabBlock( BlockTLBorderWhiteBlack.class, fmlPreInitializationEvent ); // TLBorderWhiteBlack
        initTabBlock( BlockTLBorderWhiteBlack1288Inch.class, fmlPreInitializationEvent ); // TLBorderWhiteBlack1288Inch
        initTabBlock( BlockTLBorderWhiteBlack8812Inch.class, fmlPreInitializationEvent ); // TLBorderWhiteBlack8812Inch
        initTabBlock( BlockTLBorderWhiteBlack8Inch.class, fmlPreInitializationEvent ); // TLBorderWhiteBlack8Inch
        initTabBlock( BlockTLBorderYellowBlack.class, fmlPreInitializationEvent ); // TLBorderYellowBlack
        initTabBlock( BlockTLBorderYellowBlack1288Inch.class,
                      fmlPreInitializationEvent ); // TLBorderYellowBlack1288Inch
        initTabBlock( BlockTLBorderYellowBlack8812Inch.class,
                      fmlPreInitializationEvent ); // TLBorderYellowBlack8812Inch
        initTabBlock( BlockTLBorderYellowBlack8Inch.class, fmlPreInitializationEvent ); // TLBorderYellowBlack8Inch
        initTabBlock( BlockTLController.class, fmlPreInitializationEvent ); // TLController
        initTabBlock( BlockTLControllerBlack.class, fmlPreInitializationEvent ); // TLControllerBlack
        initTabBlock( BlockTLControllerMatteWhite.class, fmlPreInitializationEvent ); // TLControllerMatteWhite
        initTabBlock( BlockTLControllerSilver.class, fmlPreInitializationEvent ); // TLControllerSilver
        initTabBlock( BlockTLControllerTan.class, fmlPreInitializationEvent ); // TLControllerTan
        initTabBlock( BlockTLControllerWhite.class, fmlPreInitializationEvent ); // TLControllerWhite
        initTabBlock( BlockTLDoghouseBorderBlackBlack.class, fmlPreInitializationEvent ); // TLDoghouseBorderBlackBlack
        initTabBlock( BlockTLDoghouseBorderBlackWhite.class, fmlPreInitializationEvent ); // TLDoghouseBorderBlackWhite
        initTabBlock( BlockTLDoghouseBorderBlackYellow.class,
                      fmlPreInitializationEvent ); // TLDoghouseBorderBlackYellow
        initTabBlock( BlockTLDoghouseBorderTan.class, fmlPreInitializationEvent ); // TLDoghouseBorderTan
        initTabBlock( BlockTLDoghouseBorderWhiteBlack.class, fmlPreInitializationEvent ); // TLDoghouseBorderWhiteBlack
        initTabBlock( BlockTLDoghouseBorderYellowBlack.class,
                      fmlPreInitializationEvent ); // TLDoghouseBorderYellowBlack
        initTabBlock( BlockTLHBorderBlack.class, fmlPreInitializationEvent ); // TLHBorderBlack
        initTabBlock( BlockTLHBorderTan.class, fmlPreInitializationEvent ); // TLHBorderTan
        initTabBlock( BlockTLHBorderWhite.class, fmlPreInitializationEvent ); // TLHBorderWhite
        initTabBlock( BlockTLHBorderYellow.class, fmlPreInitializationEvent ); // TLHBorderYellow
        initTabBlock( BlockTLHCover.class, fmlPreInitializationEvent ); // TLHCover
        initTabBlock( BlockTLHMountKit.class, fmlPreInitializationEvent ); // TLHMountKit
        initTabBlock( BlockTLPMblack.class, fmlPreInitializationEvent ); // TLPMblack
        initTabBlock( BlockTLPMsilver.class, fmlPreInitializationEvent ); // TLPMsilver
        initTabBlock( BlockTLPMtan.class, fmlPreInitializationEvent ); // TLPMtan
        initTabBlock( BlockTLPMwhite.class, fmlPreInitializationEvent ); // TLPMwhite
        initTabBlock( BlockTLVABorderBlack.class, fmlPreInitializationEvent ); // TLVABorderBlack
        initTabBlock( BlockTLVABorderBlackWhite.class, fmlPreInitializationEvent ); // TLVABorderBlackWhite
        initTabBlock( BlockTLVABorderBlackYellow.class, fmlPreInitializationEvent ); // TLVABorderBlackYellow
        initTabBlock( BlockTLVABorderTan.class, fmlPreInitializationEvent ); // TLVABorderTan
        initTabBlock( BlockTLVCover.class, fmlPreInitializationEvent ); // TLVCover
        initTabBlock( BlockTLVMountKit.class, fmlPreInitializationEvent ); // TLVMountKit
        initTabBlock( BlockTLVMountKit8812Inch.class, fmlPreInitializationEvent ); // TLVMountKit8812Inch
        initTabBlock( BlockTLVMountKit8Inch.class, fmlPreInitializationEvent ); // TLVMountKit8Inch
        initTabBlock( BlockTLVTall90LMountKit.class, fmlPreInitializationEvent ); // TLVTall90LMountKit
        initTabBlock( BlockTLVTall90RMountKit.class, fmlPreInitializationEvent ); // TLVTall90RMountKit
        initTabBlock( BlockTLVTallMountKit.class, fmlPreInitializationEvent ); // TLVTallMountKit
        initTabBlock( BlockTLiteHorzWireMount.class, fmlPreInitializationEvent ); // TLiteHorzWireMount
        initTabBlock( BlockTLiteVertWireMount.class, fmlPreInitializationEvent ); // TLiteVertWireMount
        initTabBlock( BlockTlpmunpainted.class, fmlPreInitializationEvent ); // Tlpmunpainted
        initTabBlock( BlockTrafficLightLeftAngleBorderBlack.class,
                      fmlPreInitializationEvent ); // TrafficLightLeftAngleBorderBlack
        initTabBlock( BlockTrafficLightLeftAngleBorderTan.class,
                      fmlPreInitializationEvent ); // TrafficLightLeftAngleBorderTan
        initTabBlock( BlockTrafficLightLeftAngleBorderWhiteBlack.class,
                      fmlPreInitializationEvent ); // TrafficLightLeftAngleBorderWhiteBlack
        initTabBlock( BlockTrafficLightLeftAngleBorderYellowBlack.class,
                      fmlPreInitializationEvent ); // TrafficLightLeftAngleBorderYellowBlack
        initTabBlock( BlockTrafficPoleBaseBlack.class, fmlPreInitializationEvent ); // TrafficPoleBaseBlack
        initTabBlock( BlockTrafficPoleBaseSilver.class, fmlPreInitializationEvent ); // TrafficPoleBaseSilver
        initTabBlock( BlockTrafficPoleBaseTan.class, fmlPreInitializationEvent ); // TrafficPoleBaseTan
        initTabBlock( BlockTrafficPoleBaseUnpainted.class, fmlPreInitializationEvent ); // TrafficPoleBaseUnpainted
        initTabBlock( BlockTrafficPoleBaseWhite.class, fmlPreInitializationEvent ); // TrafficPoleBaseWhite
        initTabBlock( BlockTrafficPoleHorizSignMountBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizSignMountBlack
        initTabBlock( BlockTrafficPoleHorizSignMountSilver.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizSignMountSilver
        initTabBlock( BlockTrafficPoleHorizSignMountTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizSignMountTan
        initTabBlock( BlockTrafficPoleHorizSignMountWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizSignMountWhite
        initTabBlock( BlockTrafficPoleHorizontal.class, fmlPreInitializationEvent ); // TrafficPoleHorizontal
        initTabBlock( BlockTrafficPoleHorizontalAngleBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleBlack
        initTabBlock( BlockTrafficPoleHorizontalAngleMount1Black.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount1Black
        initTabBlock( BlockTrafficPoleHorizontalAngleMount1Silver.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount1Silver
        initTabBlock( BlockTrafficPoleHorizontalAngleMount1Tan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount1Tan
        initTabBlock( BlockTrafficPoleHorizontalAngleMount1Unpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount1Unpainted
        initTabBlock( BlockTrafficPoleHorizontalAngleMount1White.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount1White
        initTabBlock( BlockTrafficPoleHorizontalAngleMount2Black.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount2Black
        initTabBlock( BlockTrafficPoleHorizontalAngleMount2Silver.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount2Silver
        initTabBlock( BlockTrafficPoleHorizontalAngleMount2Tan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount2Tan
        initTabBlock( BlockTrafficPoleHorizontalAngleMount2Unpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount2Unpainted
        initTabBlock( BlockTrafficPoleHorizontalAngleMount2White.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount2White
        initTabBlock( BlockTrafficPoleHorizontalAngleMount3Black.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount3Black
        initTabBlock( BlockTrafficPoleHorizontalAngleMount3Silver.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount3Silver
        initTabBlock( BlockTrafficPoleHorizontalAngleMount3Tan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount3Tan
        initTabBlock( BlockTrafficPoleHorizontalAngleMount3Unpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount3Unpainted
        initTabBlock( BlockTrafficPoleHorizontalAngleMount3White.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleMount3White
        initTabBlock( BlockTrafficPoleHorizontalAngleSilver.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleSilver
        initTabBlock( BlockTrafficPoleHorizontalAngleTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleTan
        initTabBlock( BlockTrafficPoleHorizontalAngleUnpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleUnpainted
        initTabBlock( BlockTrafficPoleHorizontalAngleWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalAngleWhite
        initTabBlock( BlockTrafficPoleHorizontalBlack.class, fmlPreInitializationEvent ); // TrafficPoleHorizontalBlack
        initTabBlock( BlockTrafficPoleHorizontalMountDouble.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalMountDouble
        initTabBlock( BlockTrafficPoleHorizontalMountDoubleBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalMountDoubleBlack
        initTabBlock( BlockTrafficPoleHorizontalMountDoubleTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalMountDoubleTan
        initTabBlock( BlockTrafficPoleHorizontalMountDoubleWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalMountDoubleWhite
        initTabBlock( BlockTrafficPoleHorizontalSingleMount.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalSingleMount
        initTabBlock( BlockTrafficPoleHorizontalSingleMountBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalSingleMountBlack
        initTabBlock( BlockTrafficPoleHorizontalSingleMountTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalSingleMountTan
        initTabBlock( BlockTrafficPoleHorizontalSingleMountWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleHorizontalSingleMountWhite
        initTabBlock( BlockTrafficPoleHorizontalTan.class, fmlPreInitializationEvent ); // TrafficPoleHorizontalTan
        initTabBlock( BlockTrafficPoleHorizontalWhite.class, fmlPreInitializationEvent ); // TrafficPoleHorizontalWhite
        initTabBlock( BlockTrafficPoleVertical.class, fmlPreInitializationEvent ); // TrafficPoleVertical
        initTabBlock( BlockTrafficPoleVerticalBlack.class, fmlPreInitializationEvent ); // TrafficPoleVerticalBlack
        initTabBlock( BlockTrafficPoleVerticalConnector.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnector
        initTabBlock( BlockTrafficPoleVerticalConnectorAngledBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorAngledBlack
        initTabBlock( BlockTrafficPoleVerticalConnectorAngledSilver.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorAngledSilver
        initTabBlock( BlockTrafficPoleVerticalConnectorAngledTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorAngledTan
        initTabBlock( BlockTrafficPoleVerticalConnectorAngledUnpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorAngledUnpainted
        initTabBlock( BlockTrafficPoleVerticalConnectorAngledWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorAngledWhite
        initTabBlock( BlockTrafficPoleVerticalConnectorBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorBlack
        initTabBlock( BlockTrafficPoleVerticalConnectorDoubleBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorDoubleBlack
        initTabBlock( BlockTrafficPoleVerticalConnectorDoubleSilver.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorDoubleSilver
        initTabBlock( BlockTrafficPoleVerticalConnectorDoubleTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorDoubleTan
        initTabBlock( BlockTrafficPoleVerticalConnectorDoubleUnpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorDoubleUnpainted
        initTabBlock( BlockTrafficPoleVerticalConnectorDoubleWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorDoubleWhite
        initTabBlock( BlockTrafficPoleVerticalConnectorTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorTan
        initTabBlock( BlockTrafficPoleVerticalConnectorWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalConnectorWhite
        initTabBlock( BlockTrafficPoleVerticalCurveConnector.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnector
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorBlack
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorDoubleGuyBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorDoubleGuyBlack
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorDoubleGuySilver.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorDoubleGuySilver
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorDoubleGuyTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorDoubleGuyTan
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorDoubleGuyUnpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorDoubleGuyUnpainted
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorDoubleGuyWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorDoubleGuyWhite
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorTan
        initTabBlock( BlockTrafficPoleVerticalCurveConnectorWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalCurveConnectorWhite
        initTabBlock( BlockTrafficPoleVerticalDoubleGuyMountBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalDoubleGuyMountBlack
        initTabBlock( BlockTrafficPoleVerticalDoubleGuyMountSilver.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalDoubleGuyMountSilver
        initTabBlock( BlockTrafficPoleVerticalDoubleGuyMountTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalDoubleGuyMountTan
        initTabBlock( BlockTrafficPoleVerticalDoubleGuyMountUnpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalDoubleGuyMountUnpainted
        initTabBlock( BlockTrafficPoleVerticalDoubleGuyMountWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalDoubleGuyMountWhite
        initTabBlock( BlockTrafficPoleVerticalLightMount.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalLightMount
        initTabBlock( BlockTrafficPoleVerticalLightMountBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalLightMountBlack
        initTabBlock( BlockTrafficPoleVerticalLightMountTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalLightMountTan
        initTabBlock( BlockTrafficPoleVerticalLightMountUnpainted.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalLightMountUnpainted
        initTabBlock( BlockTrafficPoleVerticalLightMountWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalLightMountWhite
        initTabBlock( BlockTrafficPoleVerticalQuadMount.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalQuadMount
        initTabBlock( BlockTrafficPoleVerticalQuadMountBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalQuadMountBlack
        initTabBlock( BlockTrafficPoleVerticalQuadMountTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalQuadMountTan
        initTabBlock( BlockTrafficPoleVerticalQuadMountWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalQuadMountWhite
        initTabBlock( BlockTrafficPoleVerticalSignalMount.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalSignalMount
        initTabBlock( BlockTrafficPoleVerticalSignalMountBlack.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalSignalMountBlack
        initTabBlock( BlockTrafficPoleVerticalSignalMountTan.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalSignalMountTan
        initTabBlock( BlockTrafficPoleVerticalSignalMountWhite.class,
                      fmlPreInitializationEvent ); // TrafficPoleVerticalSignalMountWhite
        initTabBlock( BlockTrafficPoleVerticalTan.class, fmlPreInitializationEvent ); // TrafficPoleVerticalTan
        initTabBlock( BlockTrafficPoleVerticalWhite.class, fmlPreInitializationEvent ); // TrafficPoleVerticalWhite
        initTabBlock( BlockTrafficSignalFatigueMitigator1.class,
                      fmlPreInitializationEvent ); // TrafficSignalFatigueMitigator1
        initTabBlock( BlockTrafficSignalFatigueMitigator2.class,
                      fmlPreInitializationEvent ); // TrafficSignalFatigueMitigator2
        initTabBlock( BlockTrafficSignalFatigueMitigator3.class,
                      fmlPreInitializationEvent ); // TrafficSignalFatigueMitigator3
        initTabBlock( BlockTrafficSignalFatigueMitigator4.class,
                      fmlPreInitializationEvent ); // TrafficSignalFatigueMitigator4
        initTabBlock( BlockTrafficSignalHangMount.class, fmlPreInitializationEvent ); // TrafficSignalHangMount
        initTabBlock( BlockTrafficSignalInterconnectModule1.class,
                      fmlPreInitializationEvent ); // TrafficSignalInterconnectModule1
        initTabBlock( BlockTrafficSignalInterconnectModule2.class,
                      fmlPreInitializationEvent ); // TrafficSignalInterconnectModule2
        initTabBlock( BlockTrafficSignalPreemptionBeacon.class,
                      fmlPreInitializationEvent ); // TrafficSignalPreemptionBeacon
        initTabBlock( BlockTrafficSignalSnowBeacon.class, fmlPreInitializationEvent ); // TrafficSignalSnowBeacon
        initTabBlock( BlockTrafficStreetNameSign.class, fmlPreInitializationEvent ); // TrafficStreetNameSign
        initTabBlock( BlockTrafficStreetNameSignDouble.class,
                      fmlPreInitializationEvent ); // TrafficStreetNameSignDouble
        initTabBlock( BlockTrafficStreetNameSignMount.class, fmlPreInitializationEvent ); // TrafficStreetNameSignMount
        initTabBlock( BlockTrafficpolehorzdblblack.class, fmlPreInitializationEvent ); // Trafficpolehorzdblblack
        initTabBlock( BlockTrafficpolehorzdblconcrete.class, fmlPreInitializationEvent ); // Trafficpolehorzdblconcrete
        initTabBlock( BlockTrafficpolehorzdblsilver.class, fmlPreInitializationEvent ); // Trafficpolehorzdblsilver
        initTabBlock( BlockTrafficpolehorzdbltan.class, fmlPreInitializationEvent ); // Trafficpolehorzdbltan
        initTabBlock( BlockTrafficpolehorzdblunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpolehorzdblunpainted
        initTabBlock( BlockTrafficpolehorzdblwhite.class, fmlPreInitializationEvent ); // Trafficpolehorzdblwhite
        initTabBlock( BlockTrafficpolehorzdoublemountunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpolehorzdoublemountunpainted
        initTabBlock( BlockTrafficpolehorzsignmountunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpolehorzsignmountunpainted
        initTabBlock( BlockTrafficpolehorzsinglemountunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpolehorzsinglemountunpainted
        initTabBlock( BlockTrafficpolehorzunpainted.class, fmlPreInitializationEvent ); // Trafficpolehorzunpainted
        initTabBlock( BlockTrafficpolevertdblblack.class, fmlPreInitializationEvent ); // Trafficpolevertdblblack
        initTabBlock( BlockTrafficpolevertdblconcrete.class, fmlPreInitializationEvent ); // Trafficpolevertdblconcrete
        initTabBlock( BlockTrafficpolevertdblsilver.class, fmlPreInitializationEvent ); // Trafficpolevertdblsilver
        initTabBlock( BlockTrafficpolevertdbltan.class, fmlPreInitializationEvent ); // Trafficpolevertdbltan
        initTabBlock( BlockTrafficpolevertdblunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpolevertdblunpainted
        initTabBlock( BlockTrafficpolevertdblwhite.class, fmlPreInitializationEvent ); // Trafficpolevertdblwhite
        initTabBlock( BlockTrafficpoleverticalconnectorconcrete.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalconnectorconcrete
        initTabBlock( BlockTrafficpoleverticalconnectorunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalconnectorunpainted
        initTabBlock( BlockTrafficpoleverticalcurveconnectorunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalcurveconnectorunpainted
        initTabBlock( BlockTrafficpoleverticalquadmountconcrete.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalquadmountconcrete
        initTabBlock( BlockTrafficpoleverticalquadmountunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalquadmountunpainted
        initTabBlock( BlockTrafficpoleverticalsignalmountconcrete.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalsignalmountconcrete
        initTabBlock( BlockTrafficpoleverticalsignalmountunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalsignalmountunpainted
        initTabBlock( BlockTrafficpoleverticalunpainted.class,
                      fmlPreInitializationEvent ); // Trafficpoleverticalunpainted
    }
}
