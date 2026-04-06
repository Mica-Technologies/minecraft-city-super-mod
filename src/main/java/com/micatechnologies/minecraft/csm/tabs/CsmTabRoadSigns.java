package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockSignpost;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockSignpostmount;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockTrafficSign;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for road sign blocks. Most signs are registered using {@link BlockTrafficSign}, a
 * parameterized factory class that replaces the hundreds of single-method subclasses that
 * previously only overrode {@code getBlockRegistryName()}.
 *
 * @version 2.0
 */
@CsmTab.Load(order = 7)
public class CsmTabRoadSigns extends CsmTab {

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
    return CsmRegistry.getBlock("signpoststopsign");
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
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(new BlockTrafficSign("absolutelynothingsign"));
    initTabBlock(new BlockTrafficSign("buslaneahead"));
    initTabBlock(new BlockTrafficSign("cautiondriveways"));
    initTabBlock(new BlockTrafficSign("lhsstopsign"));
    initTabBlock(new BlockTrafficSign("landslidearea"));
    initTabBlock(new BlockTrafficSign("noparking830530"));
    initTabBlock(new BlockTrafficSign("noparkinglogo830530"));
    initTabBlock(new BlockTrafficSign("noparkingsundayholiday"));
    initTabBlock(new BlockTrafficSign("nostandingsign"));
    initTabBlock(new BlockTrafficSign("roadend"));
    initTabBlock(new BlockTrafficSign("rwrkbepreptostop"));
    initTabBlock(new BlockTrafficSign("rwrkflagger"));
    initTabBlock(new BlockTrafficSign("rwrklowshoulder"));
    initTabBlock(new BlockTrafficSign("rwrknewtrafficpatternsign"));
    initTabBlock(new BlockTrafficSign("rwrknoshouldersign"));
    initTabBlock(new BlockTrafficSign("rwrkshiftleft2lanes"));
    initTabBlock(new BlockTrafficSign("rwrkshiftright2lanes"));
    initTabBlock(new BlockTrafficSign("rwrksignalahead"));
    initTabBlock(new BlockTrafficSign("rwrkstopahead"));
    initTabBlock(new BlockTrafficSign("sign"));
    initTabBlock(new BlockTrafficSign("sign14_4"));
    initTabBlock(new BlockTrafficSign("sign24hrparking"));
    initTabBlock(new BlockTrafficSign("sign3left"));
    initTabBlock(new BlockTrafficSign("sign3right"));
    initTabBlock(new BlockTrafficSign("sign3wayt"));
    initTabBlock(new BlockTrafficSign("sign4way"));
    initTabBlock(new BlockTrafficSign("signbikelaneplaque"));
    initTabBlock(new BlockTrafficSign("signbikesignal"));
    initTabBlock(new BlockTrafficSign("signbikesignaldoublesided"));
    initTabBlock(new BlockTrafficSign("signbuslane"));
    initTabBlock(new BlockTrafficSign("signdontblockthebox"));
    initTabBlock(new BlockTrafficSign("signexceptbicycle"));
    initTabBlock(new BlockTrafficSign("signexceptbicycleicon"));
    initTabBlock(new BlockTrafficSign("signexceptbus"));
    initTabBlock(new BlockTrafficSign("signexceptbusbicycle"));
    initTabBlock(new BlockTrafficSign("signltyofy"));
    initTabBlock(new BlockTrafficSign("signoncominghasextendedgreen"));
    initTabBlock(new BlockTrafficSign("signoncomingmayextendedgreen"));
    initTabBlock(new BlockTrafficSign("signonecarpergreen"));
    initTabBlock(new BlockTrafficSign("signonecarpergreeneachlane"));
    initTabBlock(new BlockTrafficSign("signr1016"));
    initTabBlock(new BlockTrafficSign("signr105"));
    initTabBlock(new BlockTrafficSign("signr105a"));
    initTabBlock(new BlockTrafficSign("signbikelane"));
    initTabBlock(new BlockTrafficSign("signbikelanelarge"));
    initTabBlock(new BlockTrafficSign("signbikesallowedusefulllane"));
    initTabBlock(new BlockTrafficSign("signbikesallowedusefulllanelarge"));
    initTabBlock(new BlockTrafficSign("signbeginplaque"));
    initTabBlock(new BlockTrafficSign("signendplaque"));
    initTabBlock(new BlockTrafficSign("signendsplaque"));
    initTabBlock(new BlockTrafficSign("signaheadplaque"));
    initTabBlock(new BlockTrafficSign("signaheadplaquefloyellow"));
    initTabBlock(new BlockTrafficSign("signarrowplaquefloyellowdownleft"));
    initTabBlock(new BlockTrafficSign("signarrowplaquefloyellowdownright"));
    initTabBlock(new BlockTrafficSign("signradioradiation"));
    initTabBlock(new BlockTrafficSign("signyintersection"));
    initTabBlock(new BlockTrafficSign("signaddleft"));
    initTabBlock(new BlockTrafficSign("signaddright"));
    initTabBlock(new BlockTrafficSign("signahead"));
    initTabBlock(new BlockTrafficSign("signaheadbrown"));
    initTabBlock(new BlockTrafficSign("signaheadleft"));
    initTabBlock(new BlockTrafficSign("signaheadleftright"));
    initTabBlock(new BlockTrafficSign("signaheadonly"));
    initTabBlock(new BlockTrafficSign("signaheadright"));
    initTabBlock(new BlockTrafficSign("signaheadsharpleft"));
    initTabBlock(new BlockTrafficSign("signaheadsharpright"));
    initTabBlock(new BlockTrafficSign("signaheadslightleft"));
    initTabBlock(new BlockTrafficSign("signaheadslightright"));
    initTabBlock(new BlockTrafficSign("signairport"));
    initTabBlock(new BlockTrafficSign("signallmergeleft"));
    initTabBlock(new BlockTrafficSign("signallmergeright"));
    initTabBlock(new BlockTrafficSign("signalt"));
    initTabBlock(new BlockTrafficSign("signalternate"));
    initTabBlock(new BlockTrafficSign("signambulance"));
    initTabBlock(new BlockTrafficSign("signarchery"));
    initTabBlock(new BlockTrafficSign("signarrowdownleft"));
    initTabBlock(new BlockTrafficSign("signarrowdownright"));
    initTabBlock(new BlockTrafficSign("signarv"));
    initTabBlock(new BlockTrafficSign("signatv"));
    initTabBlock(new BlockTrafficSign("signaxle5tonlimit"));
    initTabBlock(new BlockTrafficSign("signbeginleftlaneyieldbikes"));
    initTabBlock(new BlockTrafficSign("signbeginrightlaneyieldbikes"));
    initTabBlock(new BlockTrafficSign("signbicycle"));
    initTabBlock(new BlockTrafficSign("signbikelaneahead"));
    initTabBlock(new BlockTrafficSign("signbikelaneends"));
    initTabBlock(new BlockTrafficSign("signblastingzone"));
    initTabBlock(new BlockTrafficSign("signbluestop"));
    initTabBlock(new BlockTrafficSign("signboats"));
    initTabBlock(new BlockTrafficSign("signbridgeice"));
    initTabBlock(new BlockTrafficSign("signbrownleft"));
    initTabBlock(new BlockTrafficSign("signbump"));
    initTabBlock(new BlockTrafficSign("signbusiness"));
    initTabBlock(new BlockTrafficSign("signbusstation"));
    initTabBlock(new BlockTrafficSign("signbusstopahead"));
    initTabBlock(new BlockTrafficSign("signbusstopnoparking"));
    initTabBlock(new BlockTrafficSign("signbustaxionly"));
    initTabBlock(new BlockTrafficSign("signbypass"));
    initTabBlock(new BlockTrafficSign("signcamper"));
    initTabBlock(new BlockTrafficSign("signcamping"));
    initTabBlock(new BlockTrafficSign("signcautiondriveslowly"));
    initTabBlock(new BlockTrafficSign("signcenterhov6a9a"));
    initTabBlock(new BlockTrafficSign("signcenterlanebusonly69"));
    initTabBlock(new BlockTrafficSign("signcenterlanenouse79"));
    initTabBlock(new BlockTrafficSign("signcenterlaneturnsonly"));
    initTabBlock(new BlockTrafficSign("signcityspeed35"));
    initTabBlock(new BlockTrafficSign("signcommercialexclude"));
    initTabBlock(new BlockTrafficSign("signcow"));
    initTabBlock(new BlockTrafficSign("signcrossatcrosswalks"));
    initTabBlock(new BlockTrafficSign("signcrossoverleft"));
    initTabBlock(new BlockTrafficSign("signcrossoverquartermile"));
    initTabBlock(new BlockTrafficSign("signcurve15"));
    initTabBlock(new BlockTrafficSign("signcurve25"));
    initTabBlock(new BlockTrafficSign("signcurve35"));
    initTabBlock(new BlockTrafficSign("signcurve45"));
    initTabBlock(new BlockTrafficSign("signdeadend"));
    initTabBlock(new BlockTrafficSign("signdeer"));
    initTabBlock(new BlockTrafficSign("signdiesel"));
    initTabBlock(new BlockTrafficSign("signdip"));
    initTabBlock(new BlockTrafficSign("signdivhw"));
    initTabBlock(new BlockTrafficSign("signdivhwend"));
    initTabBlock(new BlockTrafficSign("signdividedhw1"));
    initTabBlock(new BlockTrafficSign("signdividedhw2"));
    initTabBlock(new BlockTrafficSign("signdividedhwend"));
    initTabBlock(new BlockTrafficSign("signdividedhwstart"));
    initTabBlock(new BlockTrafficSign("signdividedroad"));
    initTabBlock(new BlockTrafficSign("signdog"));
    initTabBlock(new BlockTrafficSign("signdonotenter"));
    initTabBlock(new BlockTrafficSign("signdonotpass"));
    initTabBlock(new BlockTrafficSign("signdontthinkparking"));
    initTabBlock(new BlockTrafficSign("signdownleftupright"));
    initTabBlock(new BlockTrafficSign("signduststor"));
    initTabBlock(new BlockTrafficSign("signeast"));
    initTabBlock(new BlockTrafficSign("signeisenhower"));
    initTabBlock(new BlockTrafficSign("signemergencyparkingonly"));
    initTabBlock(new BlockTrafficSign("signemergencystoppingonly"));
    initTabBlock(new BlockTrafficSign("signend"));
    initTabBlock(new BlockTrafficSign("signendroadwork"));
    initTabBlock(new BlockTrafficSign("signendspeed35"));
    initTabBlock(new BlockTrafficSign("signesignal"));
    initTabBlock(new BlockTrafficSign("signexit25"));
    initTabBlock(new BlockTrafficSign("signexitclosed"));
    initTabBlock(new BlockTrafficSign("signfamily"));
    initTabBlock(new BlockTrafficSign("signfine400"));
    initTabBlock(new BlockTrafficSign("signfiretruck"));
    initTabBlock(new BlockTrafficSign("signfishing"));
    initTabBlock(new BlockTrafficSign("signfood"));
    initTabBlock(new BlockTrafficSign("signgas"));
    initTabBlock(new BlockTrafficSign("signgatecode"));
    initTabBlock(new BlockTrafficSign("signhairpinleft"));
    initTabBlock(new BlockTrafficSign("signhairpinright"));
    initTabBlock(new BlockTrafficSign("signhandicap"));
    initTabBlock(new BlockTrafficSign("signhandicapreservedparking"));
    initTabBlock(new BlockTrafficSign("signhangglider"));
    initTabBlock(new BlockTrafficSign("signhardleftshift"));
    initTabBlock(new BlockTrafficSign("signhardrightshift"));
    initTabBlock(new BlockTrafficSign("signhelicopter"));
    initTabBlock(new BlockTrafficSign("signhightideroadflood"));
    initTabBlock(new BlockTrafficSign("signhiking"));
    initTabBlock(new BlockTrafficSign("signhikingbrown"));
    initTabBlock(new BlockTrafficSign("signhill"));
    initTabBlock(new BlockTrafficSign("signhm"));
    initTabBlock(new BlockTrafficSign("signhospital"));
    initTabBlock(new BlockTrafficSign("signhov2onlyoverhead"));
    initTabBlock(new BlockTrafficSign("signhov2ormorepervehicle"));
    initTabBlock(new BlockTrafficSign("signhov6a9a"));
    initTabBlock(new BlockTrafficSign("signhovahead"));
    initTabBlock(new BlockTrafficSign("signhovends"));
    initTabBlock(new BlockTrafficSign("signhovlaneahead"));
    initTabBlock(new BlockTrafficSign("signhovlaneends"));
    initTabBlock(new BlockTrafficSign("signhovrules"));
    initTabBlock(new BlockTrafficSign("signhurricane"));
    initTabBlock(new BlockTrafficSign("signhurricaneleft"));
    initTabBlock(new BlockTrafficSign("signhurricaneright"));
    initTabBlock(new BlockTrafficSign("signhwintersection"));
    initTabBlock(new BlockTrafficSign("signinformation"));
    initTabBlock(new BlockTrafficSign("signjct"));
    initTabBlock(new BlockTrafficSign("signkayak"));
    initTabBlock(new BlockTrafficSign("signkeepoffmedian"));
    initTabBlock(new BlockTrafficSign("signkeepright1"));
    initTabBlock(new BlockTrafficSign("signkeepright2"));
    initTabBlock(new BlockTrafficSign("signlaundry"));
    initTabBlock(new BlockTrafficSign("signleft"));
    initTabBlock(new BlockTrafficSign("signleftahead"));
    initTabBlock(new BlockTrafficSign("signleftarrow"));
    initTabBlock(new BlockTrafficSign("signleftbikerightpark"));
    initTabBlock(new BlockTrafficSign("signleftchevron"));
    initTabBlock(new BlockTrafficSign("signleftcurve"));
    initTabBlock(new BlockTrafficSign("signleftends"));
    initTabBlock(new BlockTrafficSign("signleftlaneends"));
    initTabBlock(new BlockTrafficSign("signleftmustturnleft"));
    initTabBlock(new BlockTrafficSign("signleftongreenarrow"));
    initTabBlock(new BlockTrafficSign("signleftonly"));
    initTabBlock(new BlockTrafficSign("signleftright"));
    initTabBlock(new BlockTrafficSign("signleftrightarrow"));
    initTabBlock(new BlockTrafficSign("signleftshift"));
    initTabBlock(new BlockTrafficSign("signleftturn"));
    initTabBlock(new BlockTrafficSign("signleftturnsignal"));
    initTabBlock(new BlockTrafficSign("signlibrary"));
    initTabBlock(new BlockTrafficSign("signlitteringillegal"));
    initTabBlock(new BlockTrafficSign("signloadzonenoparking"));
    initTabBlock(new BlockTrafficSign("signlodging"));
    initTabBlock(new BlockTrafficSign("signloookbothways"));
    initTabBlock(new BlockTrafficSign("signloopright"));
    initTabBlock(new BlockTrafficSign("signlowaircraft"));
    initTabBlock(new BlockTrafficSign("signmergeleft"));
    initTabBlock(new BlockTrafficSign("signmergeleftlanends"));
    initTabBlock(new BlockTrafficSign("signmergeright"));
    initTabBlock(new BlockTrafficSign("signmetalpost"));
    initTabBlock(new BlockTrafficSign("signmetro"));
    initTabBlock(new BlockTrafficSign("signmotorbike"));
    initTabBlock(new BlockTrafficSign("signmotorcycleprohibit"));
    initTabBlock(new BlockTrafficSign("signnarrowbridge"));
    initTabBlock(new BlockTrafficSign("signnarrowbridgeimg"));
    initTabBlock(new BlockTrafficSign("signnewsignal"));
    initTabBlock(new BlockTrafficSign("signnobikes"));
    initTabBlock(new BlockTrafficSign("signnobridgefishing"));
    initTabBlock(new BlockTrafficSign("signnodumping"));
    initTabBlock(new BlockTrafficSign("signnohitchhiker"));
    initTabBlock(new BlockTrafficSign("signnohitchhiking"));
    initTabBlock(new BlockTrafficSign("signnohm"));
    initTabBlock(new BlockTrafficSign("signnoleftred"));
    initTabBlock(new BlockTrafficSign("signnoleftturn"));
    initTabBlock(new BlockTrafficSign("signnomotorvehicles"));
    initTabBlock(new BlockTrafficSign("signnonmotorprohibit"));
    initTabBlock(new BlockTrafficSign("signnooutlet"));
    initTabBlock(new BlockTrafficSign("signnoovernightparking"));
    initTabBlock(new BlockTrafficSign("signnoparking"));
    initTabBlock(new BlockTrafficSign("signnoparkinganytime"));
    initTabBlock(new BlockTrafficSign("signnoparkingexceptshoulder"));
    initTabBlock(new BlockTrafficSign("signnoparkingonpave"));
    initTabBlock(new BlockTrafficSign("signnoparkingtext"));
    initTabBlock(new BlockTrafficSign("signnopedestrians"));
    initTabBlock(new BlockTrafficSign("signnorightred"));
    initTabBlock(new BlockTrafficSign("signnorightturn"));
    initTabBlock(new BlockTrafficSign("signnorth"));
    initTabBlock(new BlockTrafficSign("signnosigns"));
    initTabBlock(new BlockTrafficSign("signnostoppingexceptshoulder"));
    initTabBlock(new BlockTrafficSign("signnostoppingpavement"));
    initTabBlock(new BlockTrafficSign("signnotrucks"));
    initTabBlock(new BlockTrafficSign("signnotrucksleftlane"));
    initTabBlock(new BlockTrafficSign("signnotrucksover7000"));
    initTabBlock(new BlockTrafficSign("signnoturnred"));
    initTabBlock(new BlockTrafficSign("signnoturns"));
    initTabBlock(new BlockTrafficSign("signnoturnsofficialonly"));
    initTabBlock(new BlockTrafficSign("signnouturn"));
    initTabBlock(new BlockTrafficSign("signoffroad"));
    initTabBlock(new BlockTrafficSign("signonbridge"));
    initTabBlock(new BlockTrafficSign("signonehrparking97"));
    initTabBlock(new BlockTrafficSign("signonelanebridge"));
    initTabBlock(new BlockTrafficSign("signonewayleft"));
    initTabBlock(new BlockTrafficSign("signonewayright"));
    initTabBlock(new BlockTrafficSign("signonpavement"));
    initTabBlock(new BlockTrafficSign("signoturnonred"));
    initTabBlock(new BlockTrafficSign("signparkingahead"));
    initTabBlock(new BlockTrafficSign("signparkingarea1mile"));
    initTabBlock(new BlockTrafficSign("signparkingarearight"));
    initTabBlock(new BlockTrafficSign("signparkingl"));
    initTabBlock(new BlockTrafficSign("signparkingleft"));
    initTabBlock(new BlockTrafficSign("signparkingnoarrow"));
    initTabBlock(new BlockTrafficSign("signparkingr"));
    initTabBlock(new BlockTrafficSign("signparkingright"));
    initTabBlock(new BlockTrafficSign("signpasswithcare"));
    initTabBlock(new BlockTrafficSign("signpavementends"));
    initTabBlock(new BlockTrafficSign("signpeddetourleft"));
    initTabBlock(new BlockTrafficSign("signpeddetourright"));
    initTabBlock(new BlockTrafficSign("signpedestrian"));
    initTabBlock(new BlockTrafficSign("signpedestrianprohibit"));
    initTabBlock(new BlockTrafficSign("signphone"));
    initTabBlock(new BlockTrafficSign("signphotoenforced"));
    initTabBlock(new BlockTrafficSign("signpicnic"));
    initTabBlock(new BlockTrafficSign("signplayground"));
    initTabBlock(new BlockTrafficSign("signpolice"));
    initTabBlock(BlockSignpost.class, fmlPreInitializationEvent);
    initTabBlock(new BlockTrafficSign("signpost4way"));
    initTabBlock(new BlockTrafficSign("signpost50min30"));
    initTabBlock(new BlockTrafficSign("signpostallway"));
    initTabBlock(new BlockTrafficSign("signpostbackcircle"));
    initTabBlock(new BlockTrafficSign("signpostbackdiamond"));
    initTabBlock(new BlockTrafficSign("signpostbackhalf"));
    initTabBlock(new BlockTrafficSign("signpostbackhalfwide"));
    initTabBlock(new BlockTrafficSign("signpostbackoctagon"));
    initTabBlock(new BlockTrafficSign("signpostbacktall"));
    initTabBlock(new BlockTrafficSign("signpostca_pch"));
    initTabBlock(new BlockTrafficSign("signpostcurvyroad"));
    initTabBlock(new BlockTrafficSign("signpostdonotblock"));
    initTabBlock(new BlockTrafficSign("signpostkeepright"));
    initTabBlock(new BlockTrafficSign("signpostleftturnyieldgreen"));
    initTabBlock(new BlockTrafficSign("signpostmbtalogo"));
    initTabBlock(new BlockTrafficSign("signpostmin40"));
    initTabBlock(BlockSignpostmount.class, fmlPreInitializationEvent);
    initTabBlock(new BlockTrafficSign("signpostonewayleft"));
    initTabBlock(new BlockTrafficSign("signpostonewayright"));
    initTabBlock(new BlockTrafficSign("signpostreduced30"));
    initTabBlock(new BlockTrafficSign("signpostreducedspeedahead"));
    initTabBlock(new BlockTrafficSign("signpostroadwork"));
    initTabBlock(new BlockTrafficSign("signpostsidewalkclosed"));
    initTabBlock(new BlockTrafficSign("signpostspeed30"));
    initTabBlock(new BlockTrafficSign("signpostspeed50"));
    initTabBlock(new BlockTrafficSign("signpostspeed55"));
    initTabBlock(new BlockTrafficSign("signpostspeedzoneahead"));
    initTabBlock(new BlockTrafficSign("signpoststopsign"));
    initTabBlock(new BlockTrafficSign("signpoststreetnamesignmount1"));
    initTabBlock(new BlockTrafficSign("signpoststreetnamesignmount2"));
    initTabBlock(new BlockTrafficSign("signposttruck40"));
    initTabBlock(new BlockTrafficSign("signpostwallmountbottom1"));
    initTabBlock(new BlockTrafficSign("signpostwallmountbottom2"));
    initTabBlock(new BlockTrafficSign("signpostwallmounttop1"));
    initTabBlock(new BlockTrafficSign("signpostwallmounttop2"));
    initTabBlock(new BlockTrafficSign("signpostweightlimit"));
    initTabBlock(new BlockTrafficSign("signpsotstopahead"));
    initTabBlock(new BlockTrafficSign("signramp15"));
    initTabBlock(new BlockTrafficSign("signramp25"));
    initTabBlock(new BlockTrafficSign("signramp35"));
    initTabBlock(new BlockTrafficSign("signrampclosedahead"));
    initTabBlock(new BlockTrafficSign("signrampsignalahead"));
    initTabBlock(new BlockTrafficSign("signrdclosed"));
    initTabBlock(new BlockTrafficSign("signrdclosedthrutraffic"));
    initTabBlock(new BlockTrafficSign("signredlightphoto"));
    initTabBlock(new BlockTrafficSign("signresidentlarge"));
    initTabBlock(new BlockTrafficSign("signresidentnormal"));
    initTabBlock(new BlockTrafficSign("signrestarea1mile"));
    initTabBlock(new BlockTrafficSign("signrestarearight"));
    initTabBlock(new BlockTrafficSign("signright"));
    initTabBlock(new BlockTrafficSign("signrightahead"));
    initTabBlock(new BlockTrafficSign("signrightarrow"));
    initTabBlock(new BlockTrafficSign("signrightarrowbrown"));
    initTabBlock(new BlockTrafficSign("signrightchevron"));
    initTabBlock(new BlockTrafficSign("signrightcurve"));
    initTabBlock(new BlockTrafficSign("signrightlanebikeonly"));
    initTabBlock(new BlockTrafficSign("signrightlaneends"));
    initTabBlock(new BlockTrafficSign("signrightonly"));
    initTabBlock(new BlockTrafficSign("signrightshift"));
    initTabBlock(new BlockTrafficSign("signrightturn"));
    initTabBlock(new BlockTrafficSign("signroadends"));
    initTabBlock(new BlockTrafficSign("signroadsplit"));
    initTabBlock(new BlockTrafficSign("signroundabout"));
    initTabBlock(new BlockTrafficSign("signrworkfinesdouble"));
    initTabBlock(new BlockTrafficSign("signrwrkshiftleftsingle"));
    initTabBlock(new BlockTrafficSign("signrwrkshiftrightsingle"));
    initTabBlock(new BlockTrafficSign("signscenicoverlook2miles"));
    initTabBlock(new BlockTrafficSign("signscenicoverlookright"));
    initTabBlock(new BlockTrafficSign("signseaplane"));
    initTabBlock(new BlockTrafficSign("signseverestorm"));
    initTabBlock(new BlockTrafficSign("signshareroad"));
    initTabBlock(new BlockTrafficSign("signshelter"));
    initTabBlock(new BlockTrafficSign("signsignalahead"));
    initTabBlock(new BlockTrafficSign("signsignalremovalstudy"));
    initTabBlock(new BlockTrafficSign("signsignalworkahead"));
    initTabBlock(new BlockTrafficSign("signskilift"));
    initTabBlock(new BlockTrafficSign("signslightleft"));
    initTabBlock(new BlockTrafficSign("signslightright"));
    initTabBlock(new BlockTrafficSign("signslippery"));
    initTabBlock(new BlockTrafficSign("signslowdangerousintersection"));
    initTabBlock(new BlockTrafficSign("signslowdownpedestriantraffic"));
    initTabBlock(new BlockTrafficSign("signslowertraffickeepright"));
    initTabBlock(new BlockTrafficSign("signslowschool"));
    initTabBlock(new BlockTrafficSign("signsnowflake"));
    initTabBlock(new BlockTrafficSign("signsoftshoulder"));
    initTabBlock(new BlockTrafficSign("signsouth"));
    initTabBlock(new BlockTrafficSign("signspeed0"));
    initTabBlock(new BlockTrafficSign("signspeed15"));
    initTabBlock(new BlockTrafficSign("signspeed20"));
    initTabBlock(new BlockTrafficSign("signspeed25"));
    initTabBlock(new BlockTrafficSign("signspeed35"));
    initTabBlock(new BlockTrafficSign("signspeed40"));
    initTabBlock(new BlockTrafficSign("signspeed45"));
    initTabBlock(new BlockTrafficSign("signspeed5"));
    initTabBlock(new BlockTrafficSign("signspeed65"));
    initTabBlock(new BlockTrafficSign("signspeed75"));
    initTabBlock(new BlockTrafficSign("signspeedhump"));
    initTabBlock(new BlockTrafficSign("signstatepropertynotrasspassing"));
    initTabBlock(new BlockTrafficSign("signstophereflashing"));
    initTabBlock(new BlockTrafficSign("signstophereflashred2"));
    initTabBlock(new BlockTrafficSign("signstopherepedleft"));
    initTabBlock(new BlockTrafficSign("signstopherepedright"));
    initTabBlock(new BlockTrafficSign("signstopherered"));
    initTabBlock(new BlockTrafficSign("signstopherered2"));
    initTabBlock(new BlockTrafficSign("signstreetworkahead"));
    initTabBlock(new BlockTrafficSign("signswimming"));
    initTabBlock(new BlockTrafficSign("signtemporary"));
    initTabBlock(new BlockTrafficSign("signto"));
    initTabBlock(new BlockTrafficSign("signtowawayzone"));
    initTabBlock(new BlockTrafficSign("signtractor"));
    initTabBlock(new BlockTrafficSign("signtrafficctlpoint"));
    initTabBlock(new BlockTrafficSign("signtrafficislands"));
    initTabBlock(new BlockTrafficSign("signtrainleft"));
    initTabBlock(new BlockTrafficSign("signtrainright"));
    initTabBlock(new BlockTrafficSign("signtrainstation"));
    initTabBlock(new BlockTrafficSign("signtrolley"));
    initTabBlock(new BlockTrafficSign("signtruck"));
    initTabBlock(new BlockTrafficSign("signtruck8grade"));
    initTabBlock(new BlockTrafficSign("signtruckcrossing"));
    initTabBlock(new BlockTrafficSign("signtruckhalf"));
    initTabBlock(new BlockTrafficSign("signtruckhill"));
    initTabBlock(new BlockTrafficSign("signtrucklane500ft"));
    initTabBlock(new BlockTrafficSign("signtruckroll"));
    initTabBlock(new BlockTrafficSign("signtrucksuserightlanes"));
    initTabBlock(new BlockTrafficSign("signturnflashred"));
    initTabBlock(new BlockTrafficSign("signturnleftyieldped"));
    initTabBlock(new BlockTrafficSign("signturnleftyieldpedbike"));
    initTabBlock(new BlockTrafficSign("signturnoff2way"));
    initTabBlock(new BlockTrafficSign("signturnrightyieldped"));
    initTabBlock(new BlockTrafficSign("signturnrightyieldpedbike"));
    initTabBlock(new BlockTrafficSign("signturnsonly"));
    initTabBlock(new BlockTrafficSign("signtwowaytraffic"));
    initTabBlock(new BlockTrafficSign("signunevenlanes"));
    initTabBlock(new BlockTrafficSign("signunmarkedpavement"));
    initTabBlock(new BlockTrafficSign("signupleft"));
    initTabBlock(new BlockTrafficSign("signupleftdownright"));
    initTabBlock(new BlockTrafficSign("signupright"));
    initTabBlock(new BlockTrafficSign("signupslightleft"));
    initTabBlock(new BlockTrafficSign("signupslightright"));
    initTabBlock(new BlockTrafficSign("signusecrosswalkleft"));
    initTabBlock(new BlockTrafficSign("signusecrosswalkright"));
    initTabBlock(new BlockTrafficSign("signuselanewithgreenarrow"));
    initTabBlock(new BlockTrafficSign("signvehiclelugsprohibit"));
    initTabBlock(new BlockTrafficSign("signvisitornolongtermparking"));
    initTabBlock(new BlockTrafficSign("signwalkleft"));
    initTabBlock(new BlockTrafficSign("signwatchemergency"));
    initTabBlock(new BlockTrafficSign("signweighstation1mile"));
    initTabBlock(new BlockTrafficSign("signweighstationnextright"));
    initTabBlock(new BlockTrafficSign("signweighstationright"));
    initTabBlock(new BlockTrafficSign("signweightlimit10ton"));
    initTabBlock(new BlockTrafficSign("signweightlimit2peraxle"));
    initTabBlock(new BlockTrafficSign("signwest"));
    initTabBlock(new BlockTrafficSign("signwindsurf"));
    initTabBlock(new BlockTrafficSign("signworkdetouerleft"));
    initTabBlock(new BlockTrafficSign("signworkdetourright"));
    initTabBlock(new BlockTrafficSign("signworkexitleft"));
    initTabBlock(new BlockTrafficSign("signworkexitright"));
    initTabBlock(new BlockTrafficSign("signworkpulloffleft"));
    initTabBlock(new BlockTrafficSign("signworkpulloffright"));
    initTabBlock(new BlockTrafficSign("signworkturnlaneleft"));
    initTabBlock(new BlockTrafficSign("signworkturnlaneright"));
    initTabBlock(new BlockTrafficSign("signwrongway"));
    initTabBlock(new BlockTrafficSign("signyieldahead"));
    initTabBlock(new BlockTrafficSign("signyleft"));
    initTabBlock(new BlockTrafficSign("signyright"));
    initTabBlock(new BlockTrafficSign("streetsweepfri"));
    initTabBlock(new BlockTrafficSign("streetsweepfrischool"));
    initTabBlock(new BlockTrafficSign("streetsweepmon"));
    initTabBlock(new BlockTrafficSign("streetsweepmonschool"));
    initTabBlock(new BlockTrafficSign("streetsweepthurs"));
    initTabBlock(new BlockTrafficSign("streetsweepthursschool"));
    initTabBlock(new BlockTrafficSign("streetsweeptues"));
    initTabBlock(new BlockTrafficSign("streetsweeptuesschool"));
    initTabBlock(new BlockTrafficSign("streetsweepwed"));
    initTabBlock(new BlockTrafficSign("streetsweepwedschool"));
    initTabBlock(new BlockTrafficSign("twohourpark830530"));
    initTabBlock(new BlockTrafficSign("beachclosedsign"));
    initTabBlock(new BlockTrafficSign("falloutsheltersign"));
    initTabBlock(new BlockTrafficSign("falloutsheltersignalt"));
    initTabBlock(new BlockTrafficSign("schoolsafetyzonesign"));
    initTabBlock(new BlockTrafficSign("seniorsafetyzonesign"));
    initTabBlock(new BlockTrafficSign("bikesusepedsignalsign"));
    initTabBlock(new BlockTrafficSign("signbikeyieldtopeds"));
    initTabBlock(new BlockTrafficSign("signsharedpathway"));
    initTabBlock(new BlockTrafficSign("signturningvehiclesyieldtoped"));
    initTabBlock(new BlockTrafficSign("signturningvehiclesyieldtopedbike"));
    initTabBlock(new BlockTrafficSign("signfreewayentrance"));
    initTabBlock(new BlockTrafficSign("signnoparkingbikelane"));
    initTabBlock(new BlockTrafficSign("signfdcstandpipe"));
    initTabBlock(new BlockTrafficSign("signleftplaque"));
    initTabBlock(new BlockTrafficSign("signrightplaque"));
    initTabBlock(new BlockTrafficSign("signstripyellow"));
    initTabBlock(new BlockTrafficSign("signstripyellowgreen"));
    initTabBlock(new BlockTrafficSign("signstripgreen"));
    initTabBlock(new BlockTrafficSign("signstripred"));
    initTabBlock(new BlockTrafficSign("signstripwhite"));
    initTabBlock(new BlockTrafficSign("signdoubleoneway"));
    initTabBlock(new BlockTrafficSign("signdoubleonewayb"));
  }
}
