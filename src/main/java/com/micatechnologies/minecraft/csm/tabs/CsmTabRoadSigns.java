package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficsigns.*;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for road sign blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabRoadSigns extends CsmTab
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
        return "tabroadsigns";
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
        return CsmRegistry.getBlock( BlockSignpoststopsign.class );
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
        return true;
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
        initTabBlock( BlockAbsolutelynothingsign.class, fmlPreInitializationEvent ); // Absolutelynothingsign
        initTabBlock( BlockBuslaneahead.class, fmlPreInitializationEvent ); // Buslaneahead
        initTabBlock( BlockCautiondriveways.class, fmlPreInitializationEvent ); // Cautiondriveways
        initTabBlock( BlockLHSStopSign.class, fmlPreInitializationEvent ); // LHSStopSign
        initTabBlock( BlockLandslidearea.class, fmlPreInitializationEvent ); // Landslidearea
        initTabBlock( BlockNoparking830530.class, fmlPreInitializationEvent ); // Noparking830530
        initTabBlock( BlockNoparkinglogo830530.class, fmlPreInitializationEvent ); // Noparkinglogo830530
        initTabBlock( BlockNoparkingsundayholiday.class, fmlPreInitializationEvent ); // Noparkingsundayholiday
        initTabBlock( BlockNostandingsign.class, fmlPreInitializationEvent ); // Nostandingsign
        initTabBlock( BlockRoadend.class, fmlPreInitializationEvent ); // Roadend
        initTabBlock( BlockRwrkbepreptostop.class, fmlPreInitializationEvent ); // Rwrkbepreptostop
        initTabBlock( BlockRwrkflagger.class, fmlPreInitializationEvent ); // Rwrkflagger
        initTabBlock( BlockRwrklowshoulder.class, fmlPreInitializationEvent ); // Rwrklowshoulder
        initTabBlock( BlockRwrknewtrafficpatternsign.class, fmlPreInitializationEvent ); // Rwrknewtrafficpatternsign
        initTabBlock( BlockRwrknoshouldersign.class, fmlPreInitializationEvent ); // Rwrknoshouldersign
        initTabBlock( BlockRwrkshiftleft2lanes.class, fmlPreInitializationEvent ); // Rwrkshiftleft2lanes
        initTabBlock( BlockRwrkshiftright2lanes.class, fmlPreInitializationEvent ); // Rwrkshiftright2lanes
        initTabBlock( BlockRwrksignalahead.class, fmlPreInitializationEvent ); // Rwrksignalahead
        initTabBlock( BlockRwrkstopahead.class, fmlPreInitializationEvent ); // Rwrkstopahead
        initTabBlock( BlockSign.class, fmlPreInitializationEvent ); // Sign
        initTabBlock( BlockSign14_4.class, fmlPreInitializationEvent ); // Sign14_4
        initTabBlock( BlockSign24hrparking.class, fmlPreInitializationEvent ); // Sign24hrparking
        initTabBlock( BlockSign3left.class, fmlPreInitializationEvent ); // Sign3left
        initTabBlock( BlockSign3right.class, fmlPreInitializationEvent ); // Sign3right
        initTabBlock( BlockSign3wayt.class, fmlPreInitializationEvent ); // Sign3wayt
        initTabBlock( BlockSign4way.class, fmlPreInitializationEvent ); // Sign4way
        initTabBlock( BlockSignBikeLane.class, fmlPreInitializationEvent ); // SignBikeLane
        initTabBlock( BlockSignBikeSignal.class, fmlPreInitializationEvent ); // SignBikeSignal
        initTabBlock( BlockSignBikeSignalDoubleSided.class, fmlPreInitializationEvent ); // SignBikeSignalDoubleSided
        initTabBlock( BlockSignBusLane.class, fmlPreInitializationEvent ); // SignBusLane
        initTabBlock( BlockSignDontBlockTheBox.class, fmlPreInitializationEvent ); // SignDontBlockTheBox
        initTabBlock( BlockSignExceptBicycle.class, fmlPreInitializationEvent ); // SignExceptBicycle
        initTabBlock( BlockSignExceptBicycleIcon.class, fmlPreInitializationEvent ); // SignExceptBicycleIcon
        initTabBlock( BlockSignExceptBus.class, fmlPreInitializationEvent ); // SignExceptBus
        initTabBlock( BlockSignExceptBusBicycle.class, fmlPreInitializationEvent ); // SignExceptBusBicycle
        initTabBlock( BlockSignLTYOFY.class, fmlPreInitializationEvent ); // SignLTYOFY
        initTabBlock( BlockSignOncomingHasExtendedGreen.class,
                      fmlPreInitializationEvent ); // SignOncomingHasExtendedGreen
        initTabBlock( BlockSignOncomingMayExtendedGreen.class,
                      fmlPreInitializationEvent ); // SignOncomingMayExtendedGreen
        initTabBlock( BlockSignOneCarPerGreen.class, fmlPreInitializationEvent ); // SignOneCarPerGreen
        initTabBlock( BlockSignOneCarPerGreenEachLane.class, fmlPreInitializationEvent ); // SignOneCarPerGreenEachLane
        initTabBlock( BlockSignR1016.class, fmlPreInitializationEvent ); // SignR1016
        initTabBlock( BlockSignR105.class, fmlPreInitializationEvent ); // SignR105
        initTabBlock( BlockSignR105A.class, fmlPreInitializationEvent ); // SignR105A
        initTabBlock( BlockSignRadioRadiation.class, fmlPreInitializationEvent ); // SignRadioRadiation
        initTabBlock( BlockSignYintersection.class, fmlPreInitializationEvent ); // SignYintersection
        initTabBlock( BlockSignaddleft.class, fmlPreInitializationEvent ); // Signaddleft
        initTabBlock( BlockSignaddright.class, fmlPreInitializationEvent ); // Signaddright
        initTabBlock( BlockSignahead.class, fmlPreInitializationEvent ); // Signahead
        initTabBlock( BlockSignaheadbrown.class, fmlPreInitializationEvent ); // Signaheadbrown
        initTabBlock( BlockSignaheadleft.class, fmlPreInitializationEvent ); // Signaheadleft
        initTabBlock( BlockSignaheadleftright.class, fmlPreInitializationEvent ); // Signaheadleftright
        initTabBlock( BlockSignaheadonly.class, fmlPreInitializationEvent ); // Signaheadonly
        initTabBlock( BlockSignaheadright.class, fmlPreInitializationEvent ); // Signaheadright
        initTabBlock( BlockSignaheadsharpleft.class, fmlPreInitializationEvent ); // Signaheadsharpleft
        initTabBlock( BlockSignaheadsharpright.class, fmlPreInitializationEvent ); // Signaheadsharpright
        initTabBlock( BlockSignaheadslightleft.class, fmlPreInitializationEvent ); // Signaheadslightleft
        initTabBlock( BlockSignaheadslightright.class, fmlPreInitializationEvent ); // Signaheadslightright
        initTabBlock( BlockSignairport.class, fmlPreInitializationEvent ); // Signairport
        initTabBlock( BlockSignallmergeleft.class, fmlPreInitializationEvent ); // Signallmergeleft
        initTabBlock( BlockSignallmergeright.class, fmlPreInitializationEvent ); // Signallmergeright
        initTabBlock( BlockSignalt.class, fmlPreInitializationEvent ); // Signalt
        initTabBlock( BlockSignalternate.class, fmlPreInitializationEvent ); // Signalternate
        initTabBlock( BlockSignambulance.class, fmlPreInitializationEvent ); // Signambulance
        initTabBlock( BlockSignarchery.class, fmlPreInitializationEvent ); // Signarchery
        initTabBlock( BlockSignarrowdownleft.class, fmlPreInitializationEvent ); // Signarrowdownleft
        initTabBlock( BlockSignarrowdownright.class, fmlPreInitializationEvent ); // Signarrowdownright
        initTabBlock( BlockSignarv.class, fmlPreInitializationEvent ); // Signarv
        initTabBlock( BlockSignatv.class, fmlPreInitializationEvent ); // Signatv
        initTabBlock( BlockSignaxle5tonlimit.class, fmlPreInitializationEvent ); // Signaxle5tonlimit
        initTabBlock( BlockSignbicycle.class, fmlPreInitializationEvent ); // Signbicycle
        initTabBlock( BlockSignbikelaneahead.class, fmlPreInitializationEvent ); // Signbikelaneahead
        initTabBlock( BlockSignbikelaneends.class, fmlPreInitializationEvent ); // Signbikelaneends
        initTabBlock( BlockSignblastingzone.class, fmlPreInitializationEvent ); // Signblastingzone
        initTabBlock( BlockSignbluestop.class, fmlPreInitializationEvent ); // Signbluestop
        initTabBlock( BlockSignboats.class, fmlPreInitializationEvent ); // Signboats
        initTabBlock( BlockSignbridgeice.class, fmlPreInitializationEvent ); // Signbridgeice
        initTabBlock( BlockSignbrownleft.class, fmlPreInitializationEvent ); // Signbrownleft
        initTabBlock( BlockSignbump.class, fmlPreInitializationEvent ); // Signbump
        initTabBlock( BlockSignbusiness.class, fmlPreInitializationEvent ); // Signbusiness
        initTabBlock( BlockSignbusstation.class, fmlPreInitializationEvent ); // Signbusstation
        initTabBlock( BlockSignbusstopahead.class, fmlPreInitializationEvent ); // Signbusstopahead
        initTabBlock( BlockSignbusstopnoparking.class, fmlPreInitializationEvent ); // Signbusstopnoparking
        initTabBlock( BlockSignbustaxionly.class, fmlPreInitializationEvent ); // Signbustaxionly
        initTabBlock( BlockSignbypass.class, fmlPreInitializationEvent ); // Signbypass
        initTabBlock( BlockSigncamper.class, fmlPreInitializationEvent ); // Signcamper
        initTabBlock( BlockSigncamping.class, fmlPreInitializationEvent ); // Signcamping
        initTabBlock( BlockSigncautiondriveslowly.class, fmlPreInitializationEvent ); // Signcautiondriveslowly
        initTabBlock( BlockSigncenterhov6a9a.class, fmlPreInitializationEvent ); // Signcenterhov6a9a
        initTabBlock( BlockSigncenterlanebusonly69.class, fmlPreInitializationEvent ); // Signcenterlanebusonly69
        initTabBlock( BlockSigncenterlanenouse79.class, fmlPreInitializationEvent ); // Signcenterlanenouse79
        initTabBlock( BlockSigncenterlaneturnsonly.class, fmlPreInitializationEvent ); // Signcenterlaneturnsonly
        initTabBlock( BlockSigncityspeed35.class, fmlPreInitializationEvent ); // Signcityspeed35
        initTabBlock( BlockSigncommercialexclude.class, fmlPreInitializationEvent ); // Signcommercialexclude
        initTabBlock( BlockSigncow.class, fmlPreInitializationEvent ); // Signcow
        initTabBlock( BlockSigncrossatcrosswalks.class, fmlPreInitializationEvent ); // Signcrossatcrosswalks
        initTabBlock( BlockSigncrossoverleft.class, fmlPreInitializationEvent ); // Signcrossoverleft
        initTabBlock( BlockSigncrossoverquartermile.class, fmlPreInitializationEvent ); // Signcrossoverquartermile
        initTabBlock( BlockSigncurve15.class, fmlPreInitializationEvent ); // Signcurve15
        initTabBlock( BlockSigncurve25.class, fmlPreInitializationEvent ); // Signcurve25
        initTabBlock( BlockSigncurve35.class, fmlPreInitializationEvent ); // Signcurve35
        initTabBlock( BlockSigncurve45.class, fmlPreInitializationEvent ); // Signcurve45
        initTabBlock( BlockSigndeadend.class, fmlPreInitializationEvent ); // Signdeadend
        initTabBlock( BlockSigndeer.class, fmlPreInitializationEvent ); // Signdeer
        initTabBlock( BlockSigndiesel.class, fmlPreInitializationEvent ); // Signdiesel
        initTabBlock( BlockSigndip.class, fmlPreInitializationEvent ); // Signdip
        initTabBlock( BlockSigndivhw.class, fmlPreInitializationEvent ); // Signdivhw
        initTabBlock( BlockSigndivhwend.class, fmlPreInitializationEvent ); // Signdivhwend
        initTabBlock( BlockSigndividedhw1.class, fmlPreInitializationEvent ); // Signdividedhw1
        initTabBlock( BlockSigndividedhw2.class, fmlPreInitializationEvent ); // Signdividedhw2
        initTabBlock( BlockSigndividedhwend.class, fmlPreInitializationEvent ); // Signdividedhwend
        initTabBlock( BlockSigndividedhwstart.class, fmlPreInitializationEvent ); // Signdividedhwstart
        initTabBlock( BlockSigndividedroad.class, fmlPreInitializationEvent ); // Signdividedroad
        initTabBlock( BlockSigndog.class, fmlPreInitializationEvent ); // Signdog
        initTabBlock( BlockSigndonotenter.class, fmlPreInitializationEvent ); // Signdonotenter
        initTabBlock( BlockSigndonotpass.class, fmlPreInitializationEvent ); // Signdonotpass
        initTabBlock( BlockSigndontthinkparking.class, fmlPreInitializationEvent ); // Signdontthinkparking
        initTabBlock( BlockSigndownleftupright.class, fmlPreInitializationEvent ); // Signdownleftupright
        initTabBlock( BlockSignduststor.class, fmlPreInitializationEvent ); // Signduststor
        initTabBlock( BlockSigneast.class, fmlPreInitializationEvent ); // Signeast
        initTabBlock( BlockSigneisenhower.class, fmlPreInitializationEvent ); // Signeisenhower
        initTabBlock( BlockSignemergencyparkingonly.class, fmlPreInitializationEvent ); // Signemergencyparkingonly
        initTabBlock( BlockSignemergencystoppingonly.class, fmlPreInitializationEvent ); // Signemergencystoppingonly
        initTabBlock( BlockSignend.class, fmlPreInitializationEvent ); // Signend
        initTabBlock( BlockSignendroadwork.class, fmlPreInitializationEvent ); // Signendroadwork
        initTabBlock( BlockSignendspeed35.class, fmlPreInitializationEvent ); // Signendspeed35
        initTabBlock( BlockSignesignal.class, fmlPreInitializationEvent ); // Signesignal
        initTabBlock( BlockSignexit25.class, fmlPreInitializationEvent ); // Signexit25
        initTabBlock( BlockSignexitclosed.class, fmlPreInitializationEvent ); // Signexitclosed
        initTabBlock( BlockSignfamily.class, fmlPreInitializationEvent ); // Signfamily
        initTabBlock( BlockSignfine400.class, fmlPreInitializationEvent ); // Signfine400
        initTabBlock( BlockSignfiretruck.class, fmlPreInitializationEvent ); // Signfiretruck
        initTabBlock( BlockSignfishing.class, fmlPreInitializationEvent ); // Signfishing
        initTabBlock( BlockSignfood.class, fmlPreInitializationEvent ); // Signfood
        initTabBlock( BlockSigngas.class, fmlPreInitializationEvent ); // Signgas
        initTabBlock( BlockSigngatecode.class, fmlPreInitializationEvent ); // Signgatecode
        initTabBlock( BlockSignhairpinleft.class, fmlPreInitializationEvent ); // Signhairpinleft
        initTabBlock( BlockSignhairpinright.class, fmlPreInitializationEvent ); // Signhairpinright
        initTabBlock( BlockSignhandicap.class, fmlPreInitializationEvent ); // Signhandicap
        initTabBlock( BlockSignhandicapreservedparking.class,
                      fmlPreInitializationEvent ); // Signhandicapreservedparking
        initTabBlock( BlockSignhangglider.class, fmlPreInitializationEvent ); // Signhangglider
        initTabBlock( BlockSignhardleftshift.class, fmlPreInitializationEvent ); // Signhardleftshift
        initTabBlock( BlockSignhardrightshift.class, fmlPreInitializationEvent ); // Signhardrightshift
        initTabBlock( BlockSignhelicopter.class, fmlPreInitializationEvent ); // Signhelicopter
        initTabBlock( BlockSignhightideroadflood.class, fmlPreInitializationEvent ); // Signhightideroadflood
        initTabBlock( BlockSignhiking.class, fmlPreInitializationEvent ); // Signhiking
        initTabBlock( BlockSignhikingbrown.class, fmlPreInitializationEvent ); // Signhikingbrown
        initTabBlock( BlockSignhill.class, fmlPreInitializationEvent ); // Signhill
        initTabBlock( BlockSignhm.class, fmlPreInitializationEvent ); // Signhm
        initTabBlock( BlockSignhospital.class, fmlPreInitializationEvent ); // Signhospital
        initTabBlock( BlockSignhov2onlyoverhead.class, fmlPreInitializationEvent ); // Signhov2onlyoverhead
        initTabBlock( BlockSignhov2ormorepervehicle.class, fmlPreInitializationEvent ); // Signhov2ormorepervehicle
        initTabBlock( BlockSignhov6a9a.class, fmlPreInitializationEvent ); // Signhov6a9a
        initTabBlock( BlockSignhovahead.class, fmlPreInitializationEvent ); // Signhovahead
        initTabBlock( BlockSignhovends.class, fmlPreInitializationEvent ); // Signhovends
        initTabBlock( BlockSignhovlaneahead.class, fmlPreInitializationEvent ); // Signhovlaneahead
        initTabBlock( BlockSignhovlaneends.class, fmlPreInitializationEvent ); // Signhovlaneends
        initTabBlock( BlockSignhovrules.class, fmlPreInitializationEvent ); // Signhovrules
        initTabBlock( BlockSignhurricane.class, fmlPreInitializationEvent ); // Signhurricane
        initTabBlock( BlockSignhurricaneleft.class, fmlPreInitializationEvent ); // Signhurricaneleft
        initTabBlock( BlockSignhurricaneright.class, fmlPreInitializationEvent ); // Signhurricaneright
        initTabBlock( BlockSignhwintersection.class, fmlPreInitializationEvent ); // Signhwintersection
        initTabBlock( BlockSigninformation.class, fmlPreInitializationEvent ); // Signinformation
        initTabBlock( BlockSignjct.class, fmlPreInitializationEvent ); // Signjct
        initTabBlock( BlockSignkayak.class, fmlPreInitializationEvent ); // Signkayak
        initTabBlock( BlockSignkeepoffmedian.class, fmlPreInitializationEvent ); // Signkeepoffmedian
        initTabBlock( BlockSignkeepright1.class, fmlPreInitializationEvent ); // Signkeepright1
        initTabBlock( BlockSignkeepright2.class, fmlPreInitializationEvent ); // Signkeepright2
        initTabBlock( BlockSignlaundry.class, fmlPreInitializationEvent ); // Signlaundry
        initTabBlock( BlockSignleft.class, fmlPreInitializationEvent ); // Signleft
        initTabBlock( BlockSignleftahead.class, fmlPreInitializationEvent ); // Signleftahead
        initTabBlock( BlockSignleftarrow.class, fmlPreInitializationEvent ); // Signleftarrow
        initTabBlock( BlockSignleftbikerightpark.class, fmlPreInitializationEvent ); // Signleftbikerightpark
        initTabBlock( BlockSignleftchevron.class, fmlPreInitializationEvent ); // Signleftchevron
        initTabBlock( BlockSignleftcurve.class, fmlPreInitializationEvent ); // Signleftcurve
        initTabBlock( BlockSignleftends.class, fmlPreInitializationEvent ); // Signleftends
        initTabBlock( BlockSignleftlaneends.class, fmlPreInitializationEvent ); // Signleftlaneends
        initTabBlock( BlockSignleftmustturnleft.class, fmlPreInitializationEvent ); // Signleftmustturnleft
        initTabBlock( BlockSignleftongreenarrow.class, fmlPreInitializationEvent ); // Signleftongreenarrow
        initTabBlock( BlockSignleftonly.class, fmlPreInitializationEvent ); // Signleftonly
        initTabBlock( BlockSignleftright.class, fmlPreInitializationEvent ); // Signleftright
        initTabBlock( BlockSignleftrightarrow.class, fmlPreInitializationEvent ); // Signleftrightarrow
        initTabBlock( BlockSignleftshift.class, fmlPreInitializationEvent ); // Signleftshift
        initTabBlock( BlockSignleftturn.class, fmlPreInitializationEvent ); // Signleftturn
        initTabBlock( BlockSignleftturnsignal.class, fmlPreInitializationEvent ); // Signleftturnsignal
        initTabBlock( BlockSignlibrary.class, fmlPreInitializationEvent ); // Signlibrary
        initTabBlock( BlockSignlitteringillegal.class, fmlPreInitializationEvent ); // Signlitteringillegal
        initTabBlock( BlockSignloadzonenoparking.class, fmlPreInitializationEvent ); // Signloadzonenoparking
        initTabBlock( BlockSignlodging.class, fmlPreInitializationEvent ); // Signlodging
        initTabBlock( BlockSignloookbothways.class, fmlPreInitializationEvent ); // Signloookbothways
        initTabBlock( BlockSignloopright.class, fmlPreInitializationEvent ); // Signloopright
        initTabBlock( BlockSignlowaircraft.class, fmlPreInitializationEvent ); // Signlowaircraft
        initTabBlock( BlockSignmergeleft.class, fmlPreInitializationEvent ); // Signmergeleft
        initTabBlock( BlockSignmergeleftlanends.class, fmlPreInitializationEvent ); // Signmergeleftlanends
        initTabBlock( BlockSignmergeright.class, fmlPreInitializationEvent ); // Signmergeright
        initTabBlock( BlockSignmetalpost.class, fmlPreInitializationEvent ); // Signmetalpost
        initTabBlock( BlockSignmetro.class, fmlPreInitializationEvent ); // Signmetro
        initTabBlock( BlockSignmotorbike.class, fmlPreInitializationEvent ); // Signmotorbike
        initTabBlock( BlockSignmotorcycleprohibit.class, fmlPreInitializationEvent ); // Signmotorcycleprohibit
        initTabBlock( BlockSignnarrowbridge.class, fmlPreInitializationEvent ); // Signnarrowbridge
        initTabBlock( BlockSignnarrowbridgeimg.class, fmlPreInitializationEvent ); // Signnarrowbridgeimg
        initTabBlock( BlockSignnewsignal.class, fmlPreInitializationEvent ); // Signnewsignal
        initTabBlock( BlockSignnobikes.class, fmlPreInitializationEvent ); // Signnobikes
        initTabBlock( BlockSignnobridgefishing.class, fmlPreInitializationEvent ); // Signnobridgefishing
        initTabBlock( BlockSignnodumping.class, fmlPreInitializationEvent ); // Signnodumping
        initTabBlock( BlockSignnohitchhiker.class, fmlPreInitializationEvent ); // Signnohitchhiker
        initTabBlock( BlockSignnohitchhiking.class, fmlPreInitializationEvent ); // Signnohitchhiking
        initTabBlock( BlockSignnohm.class, fmlPreInitializationEvent ); // Signnohm
        initTabBlock( BlockSignnoleftred.class, fmlPreInitializationEvent ); // Signnoleftred
        initTabBlock( BlockSignnoleftturn.class, fmlPreInitializationEvent ); // Signnoleftturn
        initTabBlock( BlockSignnomotorvehicles.class, fmlPreInitializationEvent ); // Signnomotorvehicles
        initTabBlock( BlockSignnonmotorprohibit.class, fmlPreInitializationEvent ); // Signnonmotorprohibit
        initTabBlock( BlockSignnooutlet.class, fmlPreInitializationEvent ); // Signnooutlet
        initTabBlock( BlockSignnoovernightparking.class, fmlPreInitializationEvent ); // Signnoovernightparking
        initTabBlock( BlockSignnoparking.class, fmlPreInitializationEvent ); // Signnoparking
        initTabBlock( BlockSignnoparkinganytime.class, fmlPreInitializationEvent ); // Signnoparkinganytime
        initTabBlock( BlockSignnoparkingexceptshoulder.class,
                      fmlPreInitializationEvent ); // Signnoparkingexceptshoulder
        initTabBlock( BlockSignnoparkingonpave.class, fmlPreInitializationEvent ); // Signnoparkingonpave
        initTabBlock( BlockSignnoparkingtext.class, fmlPreInitializationEvent ); // Signnoparkingtext
        initTabBlock( BlockSignnopedestrians.class, fmlPreInitializationEvent ); // Signnopedestrians
        initTabBlock( BlockSignnorightred.class, fmlPreInitializationEvent ); // Signnorightred
        initTabBlock( BlockSignnorightturn.class, fmlPreInitializationEvent ); // Signnorightturn
        initTabBlock( BlockSignnorth.class, fmlPreInitializationEvent ); // Signnorth
        initTabBlock( BlockSignnosigns.class, fmlPreInitializationEvent ); // Signnosigns
        initTabBlock( BlockSignnostoppingexceptshoulder.class,
                      fmlPreInitializationEvent ); // Signnostoppingexceptshoulder
        initTabBlock( BlockSignnostoppingpavement.class, fmlPreInitializationEvent ); // Signnostoppingpavement
        initTabBlock( BlockSignnotrucks.class, fmlPreInitializationEvent ); // Signnotrucks
        initTabBlock( BlockSignnotrucksleftlane.class, fmlPreInitializationEvent ); // Signnotrucksleftlane
        initTabBlock( BlockSignnotrucksover7000.class, fmlPreInitializationEvent ); // Signnotrucksover7000
        initTabBlock( BlockSignnoturnred.class, fmlPreInitializationEvent ); // Signnoturnred
        initTabBlock( BlockSignnoturns.class, fmlPreInitializationEvent ); // Signnoturns
        initTabBlock( BlockSignnoturnsofficialonly.class, fmlPreInitializationEvent ); // Signnoturnsofficialonly
        initTabBlock( BlockSignnouturn.class, fmlPreInitializationEvent ); // Signnouturn
        initTabBlock( BlockSignoffroad.class, fmlPreInitializationEvent ); // Signoffroad
        initTabBlock( BlockSignonbridge.class, fmlPreInitializationEvent ); // Signonbridge
        initTabBlock( BlockSignonehrparking97.class, fmlPreInitializationEvent ); // Signonehrparking97
        initTabBlock( BlockSignonelanebridge.class, fmlPreInitializationEvent ); // Signonelanebridge
        initTabBlock( BlockSignonewayleft.class, fmlPreInitializationEvent ); // Signonewayleft
        initTabBlock( BlockSignonewayright.class, fmlPreInitializationEvent ); // Signonewayright
        initTabBlock( BlockSignonpavement.class, fmlPreInitializationEvent ); // Signonpavement
        initTabBlock( BlockSignoturnonred.class, fmlPreInitializationEvent ); // Signoturnonred
        initTabBlock( BlockSignparkingahead.class, fmlPreInitializationEvent ); // Signparkingahead
        initTabBlock( BlockSignparkingarea1mile.class, fmlPreInitializationEvent ); // Signparkingarea1mile
        initTabBlock( BlockSignparkingarearight.class, fmlPreInitializationEvent ); // Signparkingarearight
        initTabBlock( BlockSignparkingl.class, fmlPreInitializationEvent ); // Signparkingl
        initTabBlock( BlockSignparkingleft.class, fmlPreInitializationEvent ); // Signparkingleft
        initTabBlock( BlockSignparkingnoarrow.class, fmlPreInitializationEvent ); // Signparkingnoarrow
        initTabBlock( BlockSignparkingr.class, fmlPreInitializationEvent ); // Signparkingr
        initTabBlock( BlockSignparkingright.class, fmlPreInitializationEvent ); // Signparkingright
        initTabBlock( BlockSignpasswithcare.class, fmlPreInitializationEvent ); // Signpasswithcare
        initTabBlock( BlockSignpavementends.class, fmlPreInitializationEvent ); // Signpavementends
        initTabBlock( BlockSignpeddetourleft.class, fmlPreInitializationEvent ); // Signpeddetourleft
        initTabBlock( BlockSignpeddetourright.class, fmlPreInitializationEvent ); // Signpeddetourright
        initTabBlock( BlockSignpedestrian.class, fmlPreInitializationEvent ); // Signpedestrian
        initTabBlock( BlockSignpedestrianprohibit.class, fmlPreInitializationEvent ); // Signpedestrianprohibit
        initTabBlock( BlockSignphone.class, fmlPreInitializationEvent ); // Signphone
        initTabBlock( BlockSignphotoenforced.class, fmlPreInitializationEvent ); // Signphotoenforced
        initTabBlock( BlockSignpicnic.class, fmlPreInitializationEvent ); // Signpicnic
        initTabBlock( BlockSignplayground.class, fmlPreInitializationEvent ); // Signplayground
        initTabBlock( BlockSignpolice.class, fmlPreInitializationEvent ); // Signpolice
        initTabBlock( BlockSignpost.class, fmlPreInitializationEvent ); // Signpost
        initTabBlock( BlockSignpost4way.class, fmlPreInitializationEvent ); // Signpost4way
        initTabBlock( BlockSignpost50min30.class, fmlPreInitializationEvent ); // Signpost50min30
        initTabBlock( BlockSignpostallway.class, fmlPreInitializationEvent ); // Signpostallway
        initTabBlock( BlockSignpostbackcircle.class, fmlPreInitializationEvent ); // Signpostbackcircle
        initTabBlock( BlockSignpostbackdiamond.class, fmlPreInitializationEvent ); // Signpostbackdiamond
        initTabBlock( BlockSignpostbackhalf.class, fmlPreInitializationEvent ); // Signpostbackhalf
        initTabBlock( BlockSignpostbackhalfwide.class, fmlPreInitializationEvent ); // Signpostbackhalfwide
        initTabBlock( BlockSignpostbackoctagon.class, fmlPreInitializationEvent ); // Signpostbackoctagon
        initTabBlock( BlockSignpostbacktall.class, fmlPreInitializationEvent ); // Signpostbacktall
        initTabBlock( BlockSignpostca_pch.class, fmlPreInitializationEvent ); // Signpostca_pch
        initTabBlock( BlockSignpostcurvyroad.class, fmlPreInitializationEvent ); // Signpostcurvyroad
        initTabBlock( BlockSignpostdonotblock.class, fmlPreInitializationEvent ); // Signpostdonotblock
        initTabBlock( BlockSignpostkeepright.class, fmlPreInitializationEvent ); // Signpostkeepright
        initTabBlock( BlockSignpostleftturnyieldgreen.class, fmlPreInitializationEvent ); // Signpostleftturnyieldgreen
        initTabBlock( BlockSignpostmbtalogo.class, fmlPreInitializationEvent ); // Signpostmbtalogo
        initTabBlock( BlockSignpostmin40.class, fmlPreInitializationEvent ); // Signpostmin40
        initTabBlock( BlockSignpostmount.class, fmlPreInitializationEvent ); // Signpostmount
        initTabBlock( BlockSignpostonewayleft.class, fmlPreInitializationEvent ); // Signpostonewayleft
        initTabBlock( BlockSignpostreduced30.class, fmlPreInitializationEvent ); // Signpostreduced30
        initTabBlock( BlockSignpostreducedspeedahead.class, fmlPreInitializationEvent ); // Signpostreducedspeedahead
        initTabBlock( BlockSignpostroadwork.class, fmlPreInitializationEvent ); // Signpostroadwork
        initTabBlock( BlockSignpostsidewalkclosed.class, fmlPreInitializationEvent ); // Signpostsidewalkclosed
        initTabBlock( BlockSignpostspeed30.class, fmlPreInitializationEvent ); // Signpostspeed30
        initTabBlock( BlockSignpostspeed50.class, fmlPreInitializationEvent ); // Signpostspeed50
        initTabBlock( BlockSignpostspeed55.class, fmlPreInitializationEvent ); // Signpostspeed55
        initTabBlock( BlockSignpostspeedzoneahead.class, fmlPreInitializationEvent ); // Signpostspeedzoneahead
        initTabBlock( BlockSignpoststopsign.class, fmlPreInitializationEvent ); // Signpoststopsign
        initTabBlock( BlockSignpoststreetnamesignmount1.class,
                      fmlPreInitializationEvent ); // Signpoststreetnamesignmount1
        initTabBlock( BlockSignpoststreetnamesignmount2.class,
                      fmlPreInitializationEvent ); // Signpoststreetnamesignmount2
        initTabBlock( BlockSignposttruck40.class, fmlPreInitializationEvent ); // Signposttruck40
        initTabBlock( BlockSignpostwallmountbottom1.class, fmlPreInitializationEvent ); // Signpostwallmountbottom1
        initTabBlock( BlockSignpostwallmountbottom2.class, fmlPreInitializationEvent ); // Signpostwallmountbottom2
        initTabBlock( BlockSignpostwallmounttop1.class, fmlPreInitializationEvent ); // Signpostwallmounttop1
        initTabBlock( BlockSignpostwallmounttop2.class, fmlPreInitializationEvent ); // Signpostwallmounttop2
        initTabBlock( BlockSignpostweightlimit.class, fmlPreInitializationEvent ); // Signpostweightlimit
        initTabBlock( BlockSignpsotstopahead.class, fmlPreInitializationEvent ); // Signpsotstopahead
        initTabBlock( BlockSignramp15.class, fmlPreInitializationEvent ); // Signramp15
        initTabBlock( BlockSignramp25.class, fmlPreInitializationEvent ); // Signramp25
        initTabBlock( BlockSignramp35.class, fmlPreInitializationEvent ); // Signramp35
        initTabBlock( BlockSignrampclosedahead.class, fmlPreInitializationEvent ); // Signrampclosedahead
        initTabBlock( BlockSignrampsignalahead.class, fmlPreInitializationEvent ); // Signrampsignalahead
        initTabBlock( BlockSignrdclosed.class, fmlPreInitializationEvent ); // Signrdclosed
        initTabBlock( BlockSignrdclosedthrutraffic.class, fmlPreInitializationEvent ); // Signrdclosedthrutraffic
        initTabBlock( BlockSignredlightphoto.class, fmlPreInitializationEvent ); // Signredlightphoto
        initTabBlock( BlockSignresidentlarge.class, fmlPreInitializationEvent ); // Signresidentlarge
        initTabBlock( BlockSignresidentnormal.class, fmlPreInitializationEvent ); // Signresidentnormal
        initTabBlock( BlockSignrestarea1mile.class, fmlPreInitializationEvent ); // Signrestarea1mile
        initTabBlock( BlockSignrestarearight.class, fmlPreInitializationEvent ); // Signrestarearight
        initTabBlock( BlockSignright.class, fmlPreInitializationEvent ); // Signright
        initTabBlock( BlockSignrightahead.class, fmlPreInitializationEvent ); // Signrightahead
        initTabBlock( BlockSignrightarrow.class, fmlPreInitializationEvent ); // Signrightarrow
        initTabBlock( BlockSignrightarrowbrown.class, fmlPreInitializationEvent ); // Signrightarrowbrown
        initTabBlock( BlockSignrightchevron.class, fmlPreInitializationEvent ); // Signrightchevron
        initTabBlock( BlockSignrightcurve.class, fmlPreInitializationEvent ); // Signrightcurve
        initTabBlock( BlockSignrightlanebikeonly.class, fmlPreInitializationEvent ); // Signrightlanebikeonly
        initTabBlock( BlockSignrightlaneends.class, fmlPreInitializationEvent ); // Signrightlaneends
        initTabBlock( BlockSignrightonly.class, fmlPreInitializationEvent ); // Signrightonly
        initTabBlock( BlockSignrightshift.class, fmlPreInitializationEvent ); // Signrightshift
        initTabBlock( BlockSignrightturn.class, fmlPreInitializationEvent ); // Signrightturn
        initTabBlock( BlockSignroadends.class, fmlPreInitializationEvent ); // Signroadends
        initTabBlock( BlockSignroadsplit.class, fmlPreInitializationEvent ); // Signroadsplit
        initTabBlock( BlockSignroundabout.class, fmlPreInitializationEvent ); // Signroundabout
        initTabBlock( BlockSignrworkfinesdouble.class, fmlPreInitializationEvent ); // Signrworkfinesdouble
        initTabBlock( BlockSignrwrkshiftleftsingle.class, fmlPreInitializationEvent ); // Signrwrkshiftleftsingle
        initTabBlock( BlockSignrwrkshiftrightsingle.class, fmlPreInitializationEvent ); // Signrwrkshiftrightsingle
        initTabBlock( BlockSignscenicoverlook2miles.class, fmlPreInitializationEvent ); // Signscenicoverlook2miles
        initTabBlock( BlockSignscenicoverlookright.class, fmlPreInitializationEvent ); // Signscenicoverlookright
        initTabBlock( BlockSignseaplane.class, fmlPreInitializationEvent ); // Signseaplane
        initTabBlock( BlockSignseverestorm.class, fmlPreInitializationEvent ); // Signseverestorm
        initTabBlock( BlockSignshareroad.class, fmlPreInitializationEvent ); // Signshareroad
        initTabBlock( BlockSignshelter.class, fmlPreInitializationEvent ); // Signshelter
        initTabBlock( BlockSignsignalahead.class, fmlPreInitializationEvent ); // Signsignalahead
        initTabBlock( BlockSignsignalremovalstudy.class, fmlPreInitializationEvent ); // Signsignalremovalstudy
        initTabBlock( BlockSignsignalworkahead.class, fmlPreInitializationEvent ); // Signsignalworkahead
        initTabBlock( BlockSignskilift.class, fmlPreInitializationEvent ); // Signskilift
        initTabBlock( BlockSignslightleft.class, fmlPreInitializationEvent ); // Signslightleft
        initTabBlock( BlockSignslightright.class, fmlPreInitializationEvent ); // Signslightright
        initTabBlock( BlockSignslippery.class, fmlPreInitializationEvent ); // Signslippery
        initTabBlock( BlockSignslowdangerousintersection.class,
                      fmlPreInitializationEvent ); // Signslowdangerousintersection
        initTabBlock( BlockSignslowdownpedestriantraffic.class,
                      fmlPreInitializationEvent ); // Signslowdownpedestriantraffic
        initTabBlock( BlockSignslowertraffickeepright.class, fmlPreInitializationEvent ); // Signslowertraffickeepright
        initTabBlock( BlockSignslowschool.class, fmlPreInitializationEvent ); // Signslowschool
        initTabBlock( BlockSignsnowflake.class, fmlPreInitializationEvent ); // Signsnowflake
        initTabBlock( BlockSignsoftshoulder.class, fmlPreInitializationEvent ); // Signsoftshoulder
        initTabBlock( BlockSignsouth.class, fmlPreInitializationEvent ); // Signsouth
        initTabBlock( BlockSignspeed0.class, fmlPreInitializationEvent ); // Signspeed0
        initTabBlock( BlockSignspeed15.class, fmlPreInitializationEvent ); // Signspeed15
        initTabBlock( BlockSignspeed20.class, fmlPreInitializationEvent ); // Signspeed20
        initTabBlock( BlockSignspeed25.class, fmlPreInitializationEvent ); // Signspeed25
        initTabBlock( BlockSignspeed35.class, fmlPreInitializationEvent ); // Signspeed35
        initTabBlock( BlockSignspeed40.class, fmlPreInitializationEvent ); // Signspeed40
        initTabBlock( BlockSignspeed45.class, fmlPreInitializationEvent ); // Signspeed45
        initTabBlock( BlockSignspeed5.class, fmlPreInitializationEvent ); // Signspeed5
        initTabBlock( BlockSignspeed65.class, fmlPreInitializationEvent ); // Signspeed65
        initTabBlock( BlockSignspeed75.class, fmlPreInitializationEvent ); // Signspeed75
        initTabBlock( BlockSignspeedhump.class, fmlPreInitializationEvent ); // Signspeedhump
        initTabBlock( BlockSignstatepropertynotrasspassing.class,
                      fmlPreInitializationEvent ); // Signstatepropertynotrasspassing
        initTabBlock( BlockSignstophereflashing.class, fmlPreInitializationEvent ); // Signstophereflashing
        initTabBlock( BlockSignstophereflashred2.class, fmlPreInitializationEvent ); // Signstophereflashred2
        initTabBlock( BlockSignstopherepedleft.class, fmlPreInitializationEvent ); // Signstopherepedleft
        initTabBlock( BlockSignstopherepedright.class, fmlPreInitializationEvent ); // Signstopherepedright
        initTabBlock( BlockSignstopherered.class, fmlPreInitializationEvent ); // Signstopherered
        initTabBlock( BlockSignstopherered2.class, fmlPreInitializationEvent ); // Signstopherered2
        initTabBlock( BlockSignstreetworkahead.class, fmlPreInitializationEvent ); // Signstreetworkahead
        initTabBlock( BlockSignswimming.class, fmlPreInitializationEvent ); // Signswimming
        initTabBlock( BlockSigntemporary.class, fmlPreInitializationEvent ); // Signtemporary
        initTabBlock( BlockSignto.class, fmlPreInitializationEvent ); // Signto
        initTabBlock( BlockSigntowawayzone.class, fmlPreInitializationEvent ); // Signtowawayzone
        initTabBlock( BlockSigntractor.class, fmlPreInitializationEvent ); // Signtractor
        initTabBlock( BlockSigntrafficctlpoint.class, fmlPreInitializationEvent ); // Signtrafficctlpoint
        initTabBlock( BlockSigntrafficislands.class, fmlPreInitializationEvent ); // Signtrafficislands
        initTabBlock( BlockSigntrainleft.class, fmlPreInitializationEvent ); // Signtrainleft
        initTabBlock( BlockSigntrainright.class, fmlPreInitializationEvent ); // Signtrainright
        initTabBlock( BlockSigntrainstation.class, fmlPreInitializationEvent ); // Signtrainstation
        initTabBlock( BlockSigntrolley.class, fmlPreInitializationEvent ); // Signtrolley
        initTabBlock( BlockSigntruck.class, fmlPreInitializationEvent ); // Signtruck
        initTabBlock( BlockSigntruck8grade.class, fmlPreInitializationEvent ); // Signtruck8grade
        initTabBlock( BlockSigntruckcrossing.class, fmlPreInitializationEvent ); // Signtruckcrossing
        initTabBlock( BlockSigntruckhalf.class, fmlPreInitializationEvent ); // Signtruckhalf
        initTabBlock( BlockSigntruckhill.class, fmlPreInitializationEvent ); // Signtruckhill
        initTabBlock( BlockSigntrucklane500ft.class, fmlPreInitializationEvent ); // Signtrucklane500ft
        initTabBlock( BlockSigntruckroll.class, fmlPreInitializationEvent ); // Signtruckroll
        initTabBlock( BlockSigntrucksuserightlanes.class, fmlPreInitializationEvent ); // Signtrucksuserightlanes
        initTabBlock( BlockSignturnflashred.class, fmlPreInitializationEvent ); // Signturnflashred
        initTabBlock( BlockSignturnleftyieldped.class, fmlPreInitializationEvent ); // Signturnleftyieldped
        initTabBlock( BlockSignturnleftyieldpedbike.class, fmlPreInitializationEvent ); // Signturnleftyieldpedbike
        initTabBlock( BlockSignturnoff2way.class, fmlPreInitializationEvent ); // Signturnoff2way
        initTabBlock( BlockSignturnrightyieldped.class, fmlPreInitializationEvent ); // Signturnrightyieldped
        initTabBlock( BlockSignturnrightyieldpedbike.class, fmlPreInitializationEvent ); // Signturnrightyieldpedbike
        initTabBlock( BlockSignturnsonly.class, fmlPreInitializationEvent ); // Signturnsonly
        initTabBlock( BlockSigntwowaytraffic.class, fmlPreInitializationEvent ); // Signtwowaytraffic
        initTabBlock( BlockSignunevenlanes.class, fmlPreInitializationEvent ); // Signunevenlanes
        initTabBlock( BlockSignunmarkedpavement.class, fmlPreInitializationEvent ); // Signunmarkedpavement
        initTabBlock( BlockSignupleft.class, fmlPreInitializationEvent ); // Signupleft
        initTabBlock( BlockSignupleftdownright.class, fmlPreInitializationEvent ); // Signupleftdownright
        initTabBlock( BlockSignupright.class, fmlPreInitializationEvent ); // Signupright
        initTabBlock( BlockSignupslightleft.class, fmlPreInitializationEvent ); // Signupslightleft
        initTabBlock( BlockSignupslightright.class, fmlPreInitializationEvent ); // Signupslightright
        initTabBlock( BlockSignusecrosswalkleft.class, fmlPreInitializationEvent ); // Signusecrosswalkleft
        initTabBlock( BlockSignusecrosswalkright.class, fmlPreInitializationEvent ); // Signusecrosswalkright
        initTabBlock( BlockSignuselanewithgreenarrow.class, fmlPreInitializationEvent ); // Signuselanewithgreenarrow
        initTabBlock( BlockSignvehiclelugsprohibit.class, fmlPreInitializationEvent ); // Signvehiclelugsprohibit
        initTabBlock( BlockSignvisitornolongtermparking.class,
                      fmlPreInitializationEvent ); // Signvisitornolongtermparking
        initTabBlock( BlockSignwalkleft.class, fmlPreInitializationEvent ); // Signwalkleft
        initTabBlock( BlockSignwatchemergency.class, fmlPreInitializationEvent ); // Signwatchemergency
        initTabBlock( BlockSignweighstation1mile.class, fmlPreInitializationEvent ); // Signweighstation1mile
        initTabBlock( BlockSignweighstationnextright.class, fmlPreInitializationEvent ); // Signweighstationnextright
        initTabBlock( BlockSignweighstationright.class, fmlPreInitializationEvent ); // Signweighstationright
        initTabBlock( BlockSignweightlimit10ton.class, fmlPreInitializationEvent ); // Signweightlimit10ton
        initTabBlock( BlockSignweightlimit2peraxle.class, fmlPreInitializationEvent ); // Signweightlimit2peraxle
        initTabBlock( BlockSignwest.class, fmlPreInitializationEvent ); // Signwest
        initTabBlock( BlockSignwindsurf.class, fmlPreInitializationEvent ); // Signwindsurf
        initTabBlock( BlockSignworkdetouerleft.class, fmlPreInitializationEvent ); // Signworkdetouerleft
        initTabBlock( BlockSignworkdetourright.class, fmlPreInitializationEvent ); // Signworkdetourright
        initTabBlock( BlockSignworkexitleft.class, fmlPreInitializationEvent ); // Signworkexitleft
        initTabBlock( BlockSignworkexitright.class, fmlPreInitializationEvent ); // Signworkexitright
        initTabBlock( BlockSignworkpulloffleft.class, fmlPreInitializationEvent ); // Signworkpulloffleft
        initTabBlock( BlockSignworkpulloffright.class, fmlPreInitializationEvent ); // Signworkpulloffright
        initTabBlock( BlockSignworkturnlaneleft.class, fmlPreInitializationEvent ); // Signworkturnlaneleft
        initTabBlock( BlockSignworkturnlaneright.class, fmlPreInitializationEvent ); // Signworkturnlaneright
        initTabBlock( BlockSignwrongway.class, fmlPreInitializationEvent ); // Signwrongway
        initTabBlock( BlockSignyieldahead.class, fmlPreInitializationEvent ); // Signyieldahead
        initTabBlock( BlockSignyleft.class, fmlPreInitializationEvent ); // Signyleft
        initTabBlock( BlockSignyright.class, fmlPreInitializationEvent ); // Signyright
        initTabBlock( BlockStreetsweepfri.class, fmlPreInitializationEvent ); // Streetsweepfri
        initTabBlock( BlockStreetsweepfrischool.class, fmlPreInitializationEvent ); // Streetsweepfrischool
        initTabBlock( BlockStreetsweepmon.class, fmlPreInitializationEvent ); // Streetsweepmon
        initTabBlock( BlockStreetsweepmonschool.class, fmlPreInitializationEvent ); // Streetsweepmonschool
        initTabBlock( BlockStreetsweepthurs.class, fmlPreInitializationEvent ); // Streetsweepthurs
        initTabBlock( BlockStreetsweepthursschool.class, fmlPreInitializationEvent ); // Streetsweepthursschool
        initTabBlock( BlockStreetsweeptues.class, fmlPreInitializationEvent ); // Streetsweeptues
        initTabBlock( BlockStreetsweeptuesschool.class, fmlPreInitializationEvent ); // Streetsweeptuesschool
        initTabBlock( BlockStreetsweepwed.class, fmlPreInitializationEvent ); // Streetsweepwed
        initTabBlock( BlockStreetsweepwedschool.class, fmlPreInitializationEvent ); // Streetsweepwedschool
        initTabBlock( BlockTwohourpark830530.class, fmlPreInitializationEvent ); // Twohourpark830530
    }
}
