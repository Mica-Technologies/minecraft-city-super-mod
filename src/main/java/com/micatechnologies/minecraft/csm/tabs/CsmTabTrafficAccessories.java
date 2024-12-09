package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxLarge;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxLargeBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxLargeMatteWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxLargeSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxLargeTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxLargeWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxSmallBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxSmallMatteWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxSmallMetal;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxSmallSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxSmallTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockControlBoxSmallWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockDMPTBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockDMPTSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockDMPTTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockDMPTUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockDMPTWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockFreewayCallBox;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockMetalWireCenter;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockMetalWireCenterTop;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockMetalWireOffset;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockMetalWireOffsetTop;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockSignalPoleMount2;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnBlackBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnBlackBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnBlackPink;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnBlueBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnGrayGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnPinkBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorder5AddOnYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnBlackBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnBlackBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnBlackPink;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnBlueBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnGrayGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnPinkBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderAddOnYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackBlack8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackBlack8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackPink;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlueBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlueBlack8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderBlueBlack8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderGrayGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderGrayGray8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderGrayGray8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderPinkBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderPinkBlack8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderPinkBlack8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleBlackBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleBlackBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleBlackPink;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleBlueBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleGrayGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSinglePinkBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderSingleYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderWhiteBlack8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderWhiteBlack8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderYellowBlack8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderYellowBlack8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLController;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLControllerBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLControllerMatteWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLControllerSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLControllerTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLControllerWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDCover;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderBlackBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderBlackBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderBlackPink;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderBlueBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderGrayGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderPinkBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLDoghouseBorderYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHBorderBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHBorderTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHBorderWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHBorderYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHCover;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderBlackBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderBlackBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderBlackPink;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderBlueBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderGrayGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderPinkBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLHawkBorderYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLPMblack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLPMsilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLPMtan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLPMwhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVABorderBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVABorderBlackWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVABorderBlackYellow;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVABorderTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVCover;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVMountKit8812Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVMountKit8Inch;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVTall90LMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVTall90RMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLVTallMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLiteHorzWireMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLiteVertWireMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTlpmunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightLeftAngleBorderBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightLeftAngleBorderTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightLeftAngleBorderWhiteBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightLeftAngleBorderYellowBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleBaseBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleBaseSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleBaseTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleBaseUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleBaseWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizSignMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizSignMountSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizSignMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizSignMountWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount1Black;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount1Silver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount1Tan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount1Unpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount1White;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount2Black;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount2Silver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount2Tan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount2Unpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount2White;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount3Black;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount3Silver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount3Tan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount3Unpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleMount3White;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDouble;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDoubleBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDoubleTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDoubleWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMountWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnector;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorAngledBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorAngledSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorAngledTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorAngledUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorAngledWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorDoubleBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorDoubleSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorDoubleTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorDoubleUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorDoubleWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalConnectorWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnector;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorDoubleGuyBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorDoubleGuySilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorDoubleGuyTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorDoubleGuyUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorDoubleGuyWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalCurveConnectorWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalDoubleGuyMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalDoubleGuyMountSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalDoubleGuyMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalDoubleGuyMountUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalDoubleGuyMountWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalLightMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalLightMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalLightMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalLightMountUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalLightMountWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalQuadMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalQuadMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalQuadMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalQuadMountWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalSignalMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalSignalMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalSignalMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleVerticalSignalMountWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator1;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator2;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator3;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator4;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalHangMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalInterconnectModule1;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalInterconnectModule2;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalPreemptionBeacon;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalSnowBeacon;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSign;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignDouble;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdblblack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdblconcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdblsilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdbltan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdblunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdblwhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdoublemountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzsignmountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzsinglemountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolevertdblblack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolevertdblconcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolevertdblsilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolevertdbltan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolevertdblunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolevertdblwhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalconnectorconcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalconnectorunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalcurveconnectorunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalquadmountconcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalquadmountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalsignalmountconcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpoleverticalsignalmountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeUnpainted;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic accessory blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 9)
public class CsmTabTrafficAccessories extends CsmTab {

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
    return CsmRegistry.getBlock("tlborderyellowblack");
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
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockControlBoxLarge.class, fmlPreInitializationEvent); // ControlBoxLarge
    initTabBlock(BlockControlBoxLargeBlack.class,
        fmlPreInitializationEvent); // ControlBoxLargeBlack
    initTabBlock(BlockControlBoxLargeMatteWhite.class,
        fmlPreInitializationEvent); // ControlBoxLargeMatteWhite
    initTabBlock(BlockControlBoxLargeSilver.class,
        fmlPreInitializationEvent); // ControlBoxLargeSilver
    initTabBlock(BlockControlBoxLargeTan.class, fmlPreInitializationEvent); // ControlBoxLargeTan
    initTabBlock(BlockControlBoxLargeWhite.class,
        fmlPreInitializationEvent); // ControlBoxLargeWhite
    initTabBlock(BlockControlBoxSmallBlack.class,
        fmlPreInitializationEvent); // ControlBoxSmallBlack
    initTabBlock(BlockControlBoxSmallMatteWhite.class,
        fmlPreInitializationEvent); // ControlBoxSmallMatteWhite
    initTabBlock(BlockControlBoxSmallMetal.class,
        fmlPreInitializationEvent); // ControlBoxSmallMetal
    initTabBlock(BlockControlBoxSmallSilver.class,
        fmlPreInitializationEvent); // ControlBoxSmallSilver
    initTabBlock(BlockControlBoxSmallTan.class, fmlPreInitializationEvent); // ControlBoxSmallTan
    initTabBlock(BlockControlBoxSmallWhite.class,
        fmlPreInitializationEvent); // ControlBoxSmallWhite
    initTabBlock(BlockDMPTBlack.class, fmlPreInitializationEvent); // DMPTBlack
    initTabBlock(BlockDMPTSilver.class, fmlPreInitializationEvent); // DMPTSilver
    initTabBlock(BlockDMPTTan.class, fmlPreInitializationEvent); // DMPTTan
    initTabBlock(BlockDMPTUnpainted.class, fmlPreInitializationEvent); // DMPTUnpainted
    initTabBlock(BlockDMPTWhite.class, fmlPreInitializationEvent); // DMPTWhite
    initTabBlock(BlockFreewayCallBox.class, fmlPreInitializationEvent); // FreewayCallBox
    initTabBlock(BlockMetalWireCenter.class, fmlPreInitializationEvent); // MetalWireCenter
    initTabBlock(BlockMetalWireCenterTop.class, fmlPreInitializationEvent); // MetalWireCenterTop
    initTabBlock(BlockMetalWireOffset.class, fmlPreInitializationEvent); // MetalWireOffset
    initTabBlock(BlockMetalWireOffsetTop.class, fmlPreInitializationEvent); // MetalWireOffsetTop
    initTabBlock(BlockSignalPoleMount2.class, fmlPreInitializationEvent); // SignalPoleMount2
    initTabBlock(BlockTLBorder5AddOnBlackBlack.class,
        fmlPreInitializationEvent); // TLBorder5AddOnBlackBlack
    initTabBlock(BlockTLBorder5AddOnBlackWhite.class,
        fmlPreInitializationEvent); // TLBorder5AddOnBlackWhite
    initTabBlock(BlockTLBorder5AddOnBlackYellow.class,
        fmlPreInitializationEvent); // TLBorder5AddOnBlackYellow
    initTabBlock(BlockTLBorder5AddOnGrayGray.class,
        fmlPreInitializationEvent); // TLBorder5AddOnGrayGray
    initTabBlock(BlockTLBorder5AddOnBlueBlack.class,
        fmlPreInitializationEvent); // TLBorder5AddOnBlueBlack
    initTabBlock(BlockTLBorder5AddOnPinkBlack.class,
        fmlPreInitializationEvent); // TLBorder5AddOnPinkBlack
    initTabBlock(BlockTLBorder5AddOnBlackBlue.class,
        fmlPreInitializationEvent); // TLBorder5AddOnBlackBlue
    initTabBlock(BlockTLBorder5AddOnBlackPink.class,
        fmlPreInitializationEvent); // TLBorder5AddOnBlackPink
    initTabBlock(BlockTLBorder5AddOnWhiteBlack.class,
        fmlPreInitializationEvent); // TLBorder5AddOnWhiteBlack
    initTabBlock(BlockTLBorder5AddOnYellowBlack.class,
        fmlPreInitializationEvent); // TLBorder5AddOnYellowBlack
    initTabBlock(BlockTLBorderAddOnBlackBlack.class,
        fmlPreInitializationEvent); // TLBorderAddOnBlackBlack
    initTabBlock(BlockTLBorderAddOnBlackWhite.class,
        fmlPreInitializationEvent); // TLBorderAddOnBlackWhite
    initTabBlock(BlockTLBorderAddOnBlackYellow.class,
        fmlPreInitializationEvent); // TLBorderAddOnBlackYellow
    initTabBlock(BlockTLBorderAddOnGrayGray.class,
        fmlPreInitializationEvent); // TLBorderAddOnGrayGray
    initTabBlock(BlockTLBorderAddOnBlueBlack.class,
        fmlPreInitializationEvent); // TLBorderAddOnBlueBlack
    initTabBlock(BlockTLBorderAddOnPinkBlack.class,
        fmlPreInitializationEvent); // TLBorderAddOnPinkBlack
    initTabBlock(BlockTLBorderAddOnBlackBlue.class,
        fmlPreInitializationEvent); // TLBorderAddOnBlackBlue
    initTabBlock(BlockTLBorderAddOnBlackPink.class,
        fmlPreInitializationEvent); // TLBorderAddOnBlackPink
    initTabBlock(BlockTLBorderAddOnWhiteBlack.class,
        fmlPreInitializationEvent); // TLBorderAddOnWhiteBlack
    initTabBlock(BlockTLBorderAddOnYellowBlack.class,
        fmlPreInitializationEvent); // TLBorderAddOnYellowBlack
    initTabBlock(BlockTLBorderBlackBlack.class, fmlPreInitializationEvent); // TLBorderBlackBlack
    initTabBlock(BlockTLBorderBlackBlack8812Inch.class,
        fmlPreInitializationEvent); // TLBorderBlackBlack8812Inch
    initTabBlock(BlockTLBorderBlackBlack8Inch.class,
        fmlPreInitializationEvent); // TLBorderBlackBlack8Inch
    initTabBlock(BlockTLBorderBlackWhite.class, fmlPreInitializationEvent); // TLBorderBlackWhite
    initTabBlock(BlockTLBorderBlackYellow.class, fmlPreInitializationEvent); // TLBorderBlackYellow
    initTabBlock(BlockTLBorderGrayGray.class, fmlPreInitializationEvent); // TLBorderGrayGray
    initTabBlock(BlockTLBorderBlueBlack.class, fmlPreInitializationEvent); // TLBorderBlueBlack
    initTabBlock(BlockTLBorderPinkBlack.class, fmlPreInitializationEvent); // TLBorderPinkBlack
    initTabBlock(BlockTLBorderBlackBlue.class, fmlPreInitializationEvent); // TLBorderBlackBlue
    initTabBlock(BlockTLBorderBlackPink.class, fmlPreInitializationEvent); // TLBorderBlackPink
    initTabBlock(BlockTLBorderSingleBlackBlack.class,
        fmlPreInitializationEvent); // TLBorderSingleBlackBlack
    initTabBlock(BlockTLBorderSingleBlackWhite.class,
        fmlPreInitializationEvent); // TLBorderSingleBlackWhite
    initTabBlock(BlockTLBorderSingleBlackYellow.class,
        fmlPreInitializationEvent); // TLBorderSingleBlackYellow
    initTabBlock(BlockTLBorderSingleGrayGray.class,
        fmlPreInitializationEvent); // TLBorderSingleGrayGray
    initTabBlock(BlockTLBorderSingleWhiteBlack.class,
        fmlPreInitializationEvent); // TLBorderSingleWhiteBlack
    initTabBlock(BlockTLBorderSingleYellowBlack.class,
        fmlPreInitializationEvent); // TLBorderSingleYellowBlack
    initTabBlock(BlockTLBorderSingleBlackBlue.class,
        fmlPreInitializationEvent); // TLBorderSingleBlackBlue
    initTabBlock(BlockTLBorderSingleBlackPink.class,
        fmlPreInitializationEvent); // TLBorderSingleBlackPink
    initTabBlock(BlockTLBorderSingleBlueBlack.class,
        fmlPreInitializationEvent); // TLBorderSingleBlueBlack
    initTabBlock(BlockTLBorderSinglePinkBlack.class,
        fmlPreInitializationEvent); // TLBorderSinglePinkBlack
    initTabBlock(BlockTLBorderWhiteBlack.class, fmlPreInitializationEvent); // TLBorderWhiteBlack
    initTabBlock(BlockTLBorderWhiteBlack8812Inch.class,
        fmlPreInitializationEvent); // TLBorderWhiteBlack8812Inch
    initTabBlock(BlockTLBorderWhiteBlack8Inch.class,
        fmlPreInitializationEvent); // TLBorderWhiteBlack8Inch
    initTabBlock(BlockTLBorderYellowBlack.class, fmlPreInitializationEvent); // TLBorderYellowBlack
    initTabBlock(BlockTLBorderYellowBlack8812Inch.class,
        fmlPreInitializationEvent); // TLBorderYellowBlack8812Inch
    initTabBlock(BlockTLBorderYellowBlack8Inch.class,
        fmlPreInitializationEvent); // TLBorderYellowBlack8Inch
    initTabBlock(BlockTLBorderBlueBlack8Inch.class,
        fmlPreInitializationEvent); // TLBorderBlueBlack8Inch
    initTabBlock(BlockTLBorderPinkBlack8Inch.class,
        fmlPreInitializationEvent); // TLBorderPinkBlack8Inch
    initTabBlock(BlockTLBorderGrayGray8Inch.class,
        fmlPreInitializationEvent); // TLBorderGrayGray8Inch
    initTabBlock(BlockTLBorderBlueBlack8812Inch.class,
        fmlPreInitializationEvent); // TLBorderBlueBlack8812Inch
    initTabBlock(BlockTLBorderPinkBlack8812Inch.class,
        fmlPreInitializationEvent); // TLBorderPinkBlack8812Inch
    initTabBlock(BlockTLBorderGrayGray8812Inch.class,
        fmlPreInitializationEvent); // TLBorderGrayGray1288Inch
    initTabBlock(BlockTLController.class, fmlPreInitializationEvent); // TLController
    initTabBlock(BlockTLControllerBlack.class, fmlPreInitializationEvent); // TLControllerBlack
    initTabBlock(BlockTLControllerMatteWhite.class,
        fmlPreInitializationEvent); // TLControllerMatteWhite
    initTabBlock(BlockTLControllerSilver.class, fmlPreInitializationEvent); // TLControllerSilver
    initTabBlock(BlockTLControllerTan.class, fmlPreInitializationEvent); // TLControllerTan
    initTabBlock(BlockTLControllerWhite.class, fmlPreInitializationEvent); // TLControllerWhite
    initTabBlock(BlockTLDCover.class, fmlPreInitializationEvent); // TLDCover
    initTabBlock(BlockTLDoghouseBorderBlackBlack.class,
        fmlPreInitializationEvent); // TLDoghouseBorderBlackBlack
    initTabBlock(BlockTLDoghouseBorderBlackWhite.class,
        fmlPreInitializationEvent); // TLDoghouseBorderBlackWhite
    initTabBlock(BlockTLDoghouseBorderBlackYellow.class,
        fmlPreInitializationEvent); // TLDoghouseBorderBlackYellow
    initTabBlock(BlockTLDoghouseBorderGrayGray.class,
        fmlPreInitializationEvent); // TLDoghouseBorderGrayGray
    initTabBlock(BlockTLDoghouseBorderBlueBlack.class,
        fmlPreInitializationEvent); // TLDoghouseBorderBlueBlack
    initTabBlock(BlockTLDoghouseBorderPinkBlack.class,
        fmlPreInitializationEvent); // TLDoghouseBorderPinkBlack
    initTabBlock(BlockTLDoghouseBorderBlackBlue.class,
        fmlPreInitializationEvent); // TLDoghouseBorderBlackBlue
    initTabBlock(BlockTLDoghouseBorderBlackPink.class,
        fmlPreInitializationEvent); // TLDoghouseBorderBlackPink
    initTabBlock(BlockTLDoghouseBorderWhiteBlack.class,
        fmlPreInitializationEvent); // TLDoghouseBorderWhiteBlack
    initTabBlock(BlockTLDoghouseBorderYellowBlack.class,
        fmlPreInitializationEvent); // TLDoghouseBorderYellowBlack
    initTabBlock(BlockTLHawkBorderBlackBlack.class,
        fmlPreInitializationEvent); // TLHawkBorderBlackBlack
    initTabBlock(BlockTLHawkBorderBlackWhite.class,
        fmlPreInitializationEvent); // TLHawkBorderBlackWhite
    initTabBlock(BlockTLHawkBorderBlackYellow.class,
        fmlPreInitializationEvent); // TLHawkBorderBlackYellow
    initTabBlock(BlockTLHawkBorderGrayGray.class,
        fmlPreInitializationEvent); // TLHawkBorderGrayGray
    initTabBlock(BlockTLHawkBorderBlueBlack.class,
        fmlPreInitializationEvent); // TLHawkBorderBlueBlack
    initTabBlock(BlockTLHawkBorderPinkBlack.class,
        fmlPreInitializationEvent); // TLHawkBorderPinkBlack
    initTabBlock(BlockTLHawkBorderBlackBlue.class,
        fmlPreInitializationEvent); // TLHawkBorderBlackBlue
    initTabBlock(BlockTLHawkBorderBlackPink.class,
        fmlPreInitializationEvent); // TLHawkBorderBlackPink
    initTabBlock(BlockTLHawkBorderWhiteBlack.class,
        fmlPreInitializationEvent); // TLHawkBorderWhiteBlack
    initTabBlock(BlockTLHawkBorderYellowBlack.class,
        fmlPreInitializationEvent); // TLHawkBorderYellowBlack
    initTabBlock(BlockTLHBorderBlack.class, fmlPreInitializationEvent); // TLHBorderBlack
    initTabBlock(BlockTLHBorderTan.class, fmlPreInitializationEvent); // TLHBorderTan
    initTabBlock(BlockTLHBorderWhite.class, fmlPreInitializationEvent); // TLHBorderWhite
    initTabBlock(BlockTLHBorderYellow.class, fmlPreInitializationEvent); // TLHBorderYellow
    initTabBlock(BlockTLHCover.class, fmlPreInitializationEvent); // TLHCover
    initTabBlock(BlockTLHMountKit.class, fmlPreInitializationEvent); // TLHMountKit
    initTabBlock(BlockTLPMblack.class, fmlPreInitializationEvent); // TLPMblack
    initTabBlock(BlockTLPMsilver.class, fmlPreInitializationEvent); // TLPMsilver
    initTabBlock(BlockTLPMtan.class, fmlPreInitializationEvent); // TLPMtan
    initTabBlock(BlockTLPMwhite.class, fmlPreInitializationEvent); // TLPMwhite
    initTabBlock(BlockTLVABorderBlack.class, fmlPreInitializationEvent); // TLVABorderBlack
    initTabBlock(BlockTLVABorderBlackWhite.class,
        fmlPreInitializationEvent); // TLVABorderBlackWhite
    initTabBlock(BlockTLVABorderBlackYellow.class,
        fmlPreInitializationEvent); // TLVABorderBlackYellow
    initTabBlock(BlockTLVABorderTan.class, fmlPreInitializationEvent); // TLVABorderTan
    initTabBlock(BlockTLVCover.class, fmlPreInitializationEvent); // TLVCover
    initTabBlock(BlockTLVMountKit.class, fmlPreInitializationEvent); // TLVMountKit
    initTabBlock(BlockTLVMountKit8812Inch.class, fmlPreInitializationEvent); // TLVMountKit8812Inch
    initTabBlock(BlockTLVMountKit8Inch.class, fmlPreInitializationEvent); // TLVMountKit8Inch
    initTabBlock(BlockTLVTall90LMountKit.class, fmlPreInitializationEvent); // TLVTall90LMountKit
    initTabBlock(BlockTLVTall90RMountKit.class, fmlPreInitializationEvent); // TLVTall90RMountKit
    initTabBlock(BlockTLVTallMountKit.class, fmlPreInitializationEvent); // TLVTallMountKit
    initTabBlock(BlockTLiteHorzWireMount.class, fmlPreInitializationEvent); // TLiteHorzWireMount
    initTabBlock(BlockTLiteVertWireMount.class, fmlPreInitializationEvent); // TLiteVertWireMount
    initTabBlock(BlockTlpmunpainted.class, fmlPreInitializationEvent); // Tlpmunpainted
    initTabBlock(BlockTrafficLightLeftAngleBorderBlack.class,
        fmlPreInitializationEvent); // TrafficLightLeftAngleBorderBlack
    initTabBlock(BlockTrafficLightLeftAngleBorderTan.class,
        fmlPreInitializationEvent); // TrafficLightLeftAngleBorderTan
    initTabBlock(BlockTrafficLightLeftAngleBorderWhiteBlack.class,
        fmlPreInitializationEvent); // TrafficLightLeftAngleBorderWhiteBlack
    initTabBlock(BlockTrafficLightLeftAngleBorderYellowBlack.class,
        fmlPreInitializationEvent); // TrafficLightLeftAngleBorderYellowBlack
    initTabBlock(BlockTrafficPoleBaseBlack.class,
        fmlPreInitializationEvent); // TrafficPoleBaseBlack
    initTabBlock(BlockTrafficPoleBaseSilver.class,
        fmlPreInitializationEvent); // TrafficPoleBaseSilver
    initTabBlock(BlockTrafficPoleBaseTan.class, fmlPreInitializationEvent); // TrafficPoleBaseTan
    initTabBlock(BlockTrafficPoleBaseUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleBaseUnpainted
    initTabBlock(BlockTrafficPoleBaseWhite.class,
        fmlPreInitializationEvent); // TrafficPoleBaseWhite
    initTabBlock(BlockTrafficPoleHorizSignMountBlack.class,
        fmlPreInitializationEvent); // TrafficPoleHorizSignMountBlack
    initTabBlock(BlockTrafficPoleHorizSignMountSilver.class,
        fmlPreInitializationEvent); // TrafficPoleHorizSignMountSilver
    initTabBlock(BlockTrafficPoleHorizSignMountTan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizSignMountTan
    initTabBlock(BlockTrafficPoleHorizSignMountWhite.class,
        fmlPreInitializationEvent); // TrafficPoleHorizSignMountWhite
    initTabBlock(BlockTrafficPoleSmallGray.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontal
    initTabBlock(BlockTrafficPoleHorizontalAngleBlack.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleBlack
    initTabBlock(BlockTrafficPoleHorizontalAngleMount1Black.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount1Black
    initTabBlock(BlockTrafficPoleHorizontalAngleMount1Silver.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount1Silver
    initTabBlock(BlockTrafficPoleHorizontalAngleMount1Tan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount1Tan
    initTabBlock(BlockTrafficPoleHorizontalAngleMount1Unpainted.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount1Unpainted
    initTabBlock(BlockTrafficPoleHorizontalAngleMount1White.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount1White
    initTabBlock(BlockTrafficPoleHorizontalAngleMount2Black.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount2Black
    initTabBlock(BlockTrafficPoleHorizontalAngleMount2Silver.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount2Silver
    initTabBlock(BlockTrafficPoleHorizontalAngleMount2Tan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount2Tan
    initTabBlock(BlockTrafficPoleHorizontalAngleMount2Unpainted.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount2Unpainted
    initTabBlock(BlockTrafficPoleHorizontalAngleMount2White.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount2White
    initTabBlock(BlockTrafficPoleHorizontalAngleMount3Black.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount3Black
    initTabBlock(BlockTrafficPoleHorizontalAngleMount3Silver.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount3Silver
    initTabBlock(BlockTrafficPoleHorizontalAngleMount3Tan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount3Tan
    initTabBlock(BlockTrafficPoleHorizontalAngleMount3Unpainted.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount3Unpainted
    initTabBlock(BlockTrafficPoleHorizontalAngleMount3White.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleMount3White
    initTabBlock(BlockTrafficPoleHorizontalAngleSilver.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleSilver
    initTabBlock(BlockTrafficPoleHorizontalAngleTan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleTan
    initTabBlock(BlockTrafficPoleHorizontalAngleUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleUnpainted
    initTabBlock(BlockTrafficPoleHorizontalAngleWhite.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalAngleWhite
    initTabBlock(BlockTrafficPoleSmallBlack.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalBlack
    initTabBlock(BlockTrafficPoleHorizontalMountDouble.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalMountDouble
    initTabBlock(BlockTrafficPoleHorizontalMountDoubleBlack.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalMountDoubleBlack
    initTabBlock(BlockTrafficPoleHorizontalMountDoubleTan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalMountDoubleTan
    initTabBlock(BlockTrafficPoleHorizontalMountDoubleWhite.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalMountDoubleWhite
    initTabBlock(BlockTrafficPoleHorizontalSingleMount.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalSingleMount
    initTabBlock(BlockTrafficPoleHorizontalSingleMountBlack.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalSingleMountBlack
    initTabBlock(BlockTrafficPoleHorizontalSingleMountTan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalSingleMountTan
    initTabBlock(BlockTrafficPoleHorizontalSingleMountWhite.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalSingleMountWhite
    initTabBlock(BlockTrafficPoleSmallTan.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalTan
    initTabBlock(BlockTrafficPoleSmallWhite.class,
        fmlPreInitializationEvent); // TrafficPoleHorizontalWhite
    initTabBlock(BlockTrafficPoleLargeGray.class, fmlPreInitializationEvent); // TrafficPoleVertical
    initTabBlock(BlockTrafficPoleLargeBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalBlack
    initTabBlock(BlockTrafficPoleVerticalConnector.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnector
    initTabBlock(BlockTrafficPoleVerticalConnectorAngledBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorAngledBlack
    initTabBlock(BlockTrafficPoleVerticalConnectorAngledSilver.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorAngledSilver
    initTabBlock(BlockTrafficPoleVerticalConnectorAngledTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorAngledTan
    initTabBlock(BlockTrafficPoleVerticalConnectorAngledUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorAngledUnpainted
    initTabBlock(BlockTrafficPoleVerticalConnectorAngledWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorAngledWhite
    initTabBlock(BlockTrafficPoleVerticalConnectorBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorBlack
    initTabBlock(BlockTrafficPoleVerticalConnectorDoubleBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorDoubleBlack
    initTabBlock(BlockTrafficPoleVerticalConnectorDoubleSilver.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorDoubleSilver
    initTabBlock(BlockTrafficPoleVerticalConnectorDoubleTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorDoubleTan
    initTabBlock(BlockTrafficPoleVerticalConnectorDoubleUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorDoubleUnpainted
    initTabBlock(BlockTrafficPoleVerticalConnectorDoubleWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorDoubleWhite
    initTabBlock(BlockTrafficPoleVerticalConnectorTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorTan
    initTabBlock(BlockTrafficPoleVerticalConnectorWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalConnectorWhite
    initTabBlock(BlockTrafficPoleVerticalCurveConnector.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnector
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorBlack
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorDoubleGuyBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorDoubleGuyBlack
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorDoubleGuySilver.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorDoubleGuySilver
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorDoubleGuyTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorDoubleGuyTan
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorDoubleGuyUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorDoubleGuyUnpainted
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorDoubleGuyWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorDoubleGuyWhite
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorTan
    initTabBlock(BlockTrafficPoleVerticalCurveConnectorWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalCurveConnectorWhite
    initTabBlock(BlockTrafficPoleVerticalDoubleGuyMountBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalDoubleGuyMountBlack
    initTabBlock(BlockTrafficPoleVerticalDoubleGuyMountSilver.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalDoubleGuyMountSilver
    initTabBlock(BlockTrafficPoleVerticalDoubleGuyMountTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalDoubleGuyMountTan
    initTabBlock(BlockTrafficPoleVerticalDoubleGuyMountUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalDoubleGuyMountUnpainted
    initTabBlock(BlockTrafficPoleVerticalDoubleGuyMountWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalDoubleGuyMountWhite
    initTabBlock(BlockTrafficPoleVerticalLightMount.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalLightMount
    initTabBlock(BlockTrafficPoleVerticalLightMountBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalLightMountBlack
    initTabBlock(BlockTrafficPoleVerticalLightMountTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalLightMountTan
    initTabBlock(BlockTrafficPoleVerticalLightMountUnpainted.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalLightMountUnpainted
    initTabBlock(BlockTrafficPoleVerticalLightMountWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalLightMountWhite
    initTabBlock(BlockTrafficPoleVerticalQuadMount.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalQuadMount
    initTabBlock(BlockTrafficPoleVerticalQuadMountBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalQuadMountBlack
    initTabBlock(BlockTrafficPoleVerticalQuadMountTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalQuadMountTan
    initTabBlock(BlockTrafficPoleVerticalQuadMountWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalQuadMountWhite
    initTabBlock(BlockTrafficPoleVerticalSignalMount.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalSignalMount
    initTabBlock(BlockTrafficPoleVerticalSignalMountBlack.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalSignalMountBlack
    initTabBlock(BlockTrafficPoleVerticalSignalMountTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalSignalMountTan
    initTabBlock(BlockTrafficPoleVerticalSignalMountWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalSignalMountWhite
    initTabBlock(BlockTrafficPoleLargeTan.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalTan
    initTabBlock(BlockTrafficPoleLargeWhite.class,
        fmlPreInitializationEvent); // TrafficPoleVerticalWhite
    initTabBlock(BlockTrafficSignalFatigueMitigator1.class,
        fmlPreInitializationEvent); // TrafficSignalFatigueMitigator1
    initTabBlock(BlockTrafficSignalFatigueMitigator2.class,
        fmlPreInitializationEvent); // TrafficSignalFatigueMitigator2
    initTabBlock(BlockTrafficSignalFatigueMitigator3.class,
        fmlPreInitializationEvent); // TrafficSignalFatigueMitigator3
    initTabBlock(BlockTrafficSignalFatigueMitigator4.class,
        fmlPreInitializationEvent); // TrafficSignalFatigueMitigator4
    initTabBlock(BlockTrafficSignalHangMount.class,
        fmlPreInitializationEvent); // TrafficSignalHangMount
    initTabBlock(BlockTrafficSignalInterconnectModule1.class,
        fmlPreInitializationEvent); // TrafficSignalInterconnectModule1
    initTabBlock(BlockTrafficSignalInterconnectModule2.class,
        fmlPreInitializationEvent); // TrafficSignalInterconnectModule2
    initTabBlock(BlockTrafficSignalPreemptionBeacon.class,
        fmlPreInitializationEvent); // TrafficSignalPreemptionBeacon
    initTabBlock(BlockTrafficSignalSnowBeacon.class,
        fmlPreInitializationEvent); // TrafficSignalSnowBeacon
    initTabBlock(BlockTrafficStreetNameSign.class,
        fmlPreInitializationEvent); // TrafficStreetNameSign
    initTabBlock(BlockTrafficStreetNameSignDouble.class,
        fmlPreInitializationEvent); // TrafficStreetNameSignDouble
    initTabBlock(BlockTrafficStreetNameSignMount.class,
        fmlPreInitializationEvent); // TrafficStreetNameSignMount
    initTabBlock(BlockTrafficpolehorzdblblack.class,
        fmlPreInitializationEvent); // Trafficpolehorzdblblack
    initTabBlock(BlockTrafficpolehorzdblconcrete.class,
        fmlPreInitializationEvent); // Trafficpolehorzdblconcrete
    initTabBlock(BlockTrafficpolehorzdblsilver.class,
        fmlPreInitializationEvent); // Trafficpolehorzdblsilver
    initTabBlock(BlockTrafficpolehorzdbltan.class,
        fmlPreInitializationEvent); // Trafficpolehorzdbltan
    initTabBlock(BlockTrafficpolehorzdblunpainted.class,
        fmlPreInitializationEvent); // Trafficpolehorzdblunpainted
    initTabBlock(BlockTrafficpolehorzdblwhite.class,
        fmlPreInitializationEvent); // Trafficpolehorzdblwhite
    initTabBlock(BlockTrafficpolehorzdoublemountunpainted.class,
        fmlPreInitializationEvent); // Trafficpolehorzdoublemountunpainted
    initTabBlock(BlockTrafficpolehorzsignmountunpainted.class,
        fmlPreInitializationEvent); // Trafficpolehorzsignmountunpainted
    initTabBlock(BlockTrafficpolehorzsinglemountunpainted.class,
        fmlPreInitializationEvent); // Trafficpolehorzsinglemountunpainted
    initTabBlock(BlockTrafficPoleSmallUnpainted.class,
        fmlPreInitializationEvent); // Trafficpolehorzunpainted
    initTabBlock(BlockTrafficpolevertdblblack.class,
        fmlPreInitializationEvent); // Trafficpolevertdblblack
    initTabBlock(BlockTrafficpolevertdblconcrete.class,
        fmlPreInitializationEvent); // Trafficpolevertdblconcrete
    initTabBlock(BlockTrafficpolevertdblsilver.class,
        fmlPreInitializationEvent); // Trafficpolevertdblsilver
    initTabBlock(BlockTrafficpolevertdbltan.class,
        fmlPreInitializationEvent); // Trafficpolevertdbltan
    initTabBlock(BlockTrafficpolevertdblunpainted.class,
        fmlPreInitializationEvent); // Trafficpolevertdblunpainted
    initTabBlock(BlockTrafficpolevertdblwhite.class,
        fmlPreInitializationEvent); // Trafficpolevertdblwhite
    initTabBlock(BlockTrafficpoleverticalconnectorconcrete.class,
        fmlPreInitializationEvent); // Trafficpoleverticalconnectorconcrete
    initTabBlock(BlockTrafficpoleverticalconnectorunpainted.class,
        fmlPreInitializationEvent); // Trafficpoleverticalconnectorunpainted
    initTabBlock(BlockTrafficpoleverticalcurveconnectorunpainted.class,
        fmlPreInitializationEvent); // Trafficpoleverticalcurveconnectorunpainted
    initTabBlock(BlockTrafficpoleverticalquadmountconcrete.class,
        fmlPreInitializationEvent); // Trafficpoleverticalquadmountconcrete
    initTabBlock(BlockTrafficpoleverticalquadmountunpainted.class,
        fmlPreInitializationEvent); // Trafficpoleverticalquadmountunpainted
    initTabBlock(BlockTrafficpoleverticalsignalmountconcrete.class,
        fmlPreInitializationEvent); // Trafficpoleverticalsignalmountconcrete
    initTabBlock(BlockTrafficpoleverticalsignalmountunpainted.class,
        fmlPreInitializationEvent); // Trafficpoleverticalsignalmountunpainted
    initTabBlock(BlockTrafficPoleLargeUnpainted.class,
        fmlPreInitializationEvent); // Trafficpoleverticalunpainted
  }
}
