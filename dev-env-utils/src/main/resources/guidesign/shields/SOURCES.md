# Route shield sources

Every SVG in this folder is rendered into the guide sign atlas (`sign_atlas.png`) by
`GuideSignAtlasTool`. All of them are route markers from Wikimedia Commons whose file pages
mark them public domain: a highway marker design is simple shapes and lettering that carry
no copyright, or the drawing was released by its author or has expired. Nothing here is
CC-BY-SA or of unclear licence. The licence column names the Commons licence template on
the file page (`PD MUTCD` / `PD shape` / `PD ineligible`: not eligible for copyright;
`PD-self` / `PD-author` / `PD-user`: released by the author; `PD Canada`: expired).

Every marker file was simplified the same way before it was committed: Inkscape,
Sodipodi and RDF metadata, comments, titles and empty text placeholders were removed, a
`viewBox` was added where the file had none, and any route number was taken out, since
the sign renderer draws the number. Where the marker has a numbered file but no blank,
or a file needed more than that, the note says so. The geometry and colours are the
source's.

## Generic shields

| File | Source | Author | Licence |
|---|---|---|---|
| `interstate.svg` | [I-blank.svg](https://commons.wikimedia.org/wiki/File:I-blank.svg) | Ltljltlj | PD MUTCD |
| `interstate_business.svg` | [Business Loop blank.svg](https://commons.wikimedia.org/wiki/File:Business_Loop_blank.svg) | not given | PD MUTCD |
| `us_route.svg` | [US blank.svg](https://commons.wikimedia.org/wiki/File:US_blank.svg) | SPUI | PD-user |
| `state_circle.svg` | [Circle sign blank.svg](https://commons.wikimedia.org/wiki/File:Circle_sign_blank.svg) | not given | PD-self |
| `county_route.svg` | [County Blank.svg](https://commons.wikimedia.org/wiki/File:County_Blank.svg) | Middlesex_County_Route_650_NJ.svg: Northenglish derivative work: Beao | PD MUTCD |

`shieldalto*.png` are the Alto route markers, artwork supplied for the mod.

## State and DC markers

| Marker | File | Source | Author | Licence | Notes |
|---|---|---|---|---|---|
| Alabama | `alabama.svg` | [Alabama blank.svg](https://commons.wikimedia.org/wiki/File:Alabama_blank.svg) | Ltljltlj | PD shape |  |
| Alaska | `alaska.svg` | [Alaska blank shield.svg](https://commons.wikimedia.org/wiki/File:Alaska_blank_shield.svg) | Alaska Department of Transportation & Public Facilities | PD MUTCD AK |  |
| Arizona | `arizona.svg` | [Arizona blank.svg](https://commons.wikimedia.org/wiki/File:Arizona_blank.svg) | Ltljltlj | PD MUTCD AZ |  |
| Arkansas | `arkansas.svg` | [Arkansas blank.svg](https://commons.wikimedia.org/wiki/File:Arkansas_blank.svg) | Ltljltlj | PD shape |  |
| California | `california.svg` | [California blank.svg](https://commons.wikimedia.org/wiki/File:California_blank.svg) | SPUI | PD MUTCD CA |  |
| Colorado | `colorado.svg` | [Colorado blank.svg](https://commons.wikimedia.org/wiki/File:Colorado_blank.svg) | not given | PD MUTCD CO |  |
| Connecticut | `connecticut.svg` | [Connecticut Highway blank.svg](https://commons.wikimedia.org/wiki/File:Connecticut_Highway_blank.svg) | Mr. Matté | PD shape |  |
| Delaware | `state_circle.svg` | shared with the generic circle | | | Posts the plain MUTCD M1-5 circle |
| District of Columbia | `district_of_columbia.svg` | [DC-blank.svg](https://commons.wikimedia.org/wiki/File:DC-blank.svg) | TwinsMetsFan | PD shape |  |
| Florida | `florida.svg` | [Florida blank.svg](https://commons.wikimedia.org/wiki/File:Florida_blank.svg) | SPUI | PD shape |  |
| Georgia | `georgia.svg` | [Georgia blank.svg](https://commons.wikimedia.org/wiki/File:Georgia_blank.svg) | Fredddie, originally created by Pedriana | PD MUTCD GA |  |
| Hawaii | `hawaii.svg` | [HI-blank.svg](https://commons.wikimedia.org/wiki/File:HI-blank.svg) | TwinsMetsFan | PD shape |  |
| Idaho | `idaho.svg` | [Idaho blank.svg](https://commons.wikimedia.org/wiki/File:Idaho_blank.svg) | Idaho Transportation Department | PD MUTCD ID | The ITD marker to its April 2020 specification. |
| Illinois | `illinois.svg` | [Illinois blank.svg](https://commons.wikimedia.org/wiki/File:Illinois_blank.svg) | not given | PD MUTCD IL |  |
| Indiana | `indiana.svg` | [Indiana blank.svg](https://commons.wikimedia.org/wiki/File:Indiana_blank.svg) | Fredddie, originally by Holderca1 | PD MUTCD IN, PD ineligible (road signs) |  |
| Iowa | `state_circle.svg` | shared with the generic circle | | | Posts the plain MUTCD M1-5 circle |
| Kansas | `kansas.svg` | [K-blank.svg](https://commons.wikimedia.org/wiki/File:K-blank.svg) | Scott Nazelrod, Jonathan N. Winkler | PD-self |  |
| Kentucky | `state_circle.svg` | shared with the generic circle | | | Posts the plain MUTCD M1-5 circle |
| Louisiana | `louisiana.svg` | [Louisiana blank (2008).svg](https://commons.wikimedia.org/wiki/File:Louisiana_blank_(2008).svg) | Minh Nguyen | PD MUTCD | The 2008 marker. |
| Maine | `maine.svg` | [Maine blank.svg](https://commons.wikimedia.org/wiki/File:Maine_blank.svg) | Fredddie | PD shape |  |
| Maryland | `maryland.svg` | [MD blank.svg](https://commons.wikimedia.org/wiki/File:MD_blank.svg) | Jeff02 | PD MUTCD MD |  |
| Massachusetts | `massachusetts.svg` | [MA Route blank.svg](https://commons.wikimedia.org/wiki/File:MA_Route_blank.svg) | SPUI | PD MUTCD MA |  |
| Michigan | `michigan.svg` | [M-Blank.svg](https://commons.wikimedia.org/wiki/File:M-Blank.svg) | IW4 | PD-author |  |
| Minnesota | `minnesota.svg` | [MN-blank.svg](https://commons.wikimedia.org/wiki/File:MN-blank.svg) | Minnesota Department of Transportation | PD MUTCD MN |  |
| Mississippi | `state_circle.svg` | shared with the generic circle | | | Posts the plain MUTCD M1-5 circle |
| Missouri | `missouri.svg` | [MO-blank.svg](https://commons.wikimedia.org/wiki/File:MO-blank.svg) | O | PD MUTCD MO |  |
| Montana | `montana.svg` | [MT-blank.svg](https://commons.wikimedia.org/wiki/File:MT-blank.svg) | Fredddie, originally created by Master son | PD shape |  |
| Nebraska | `nebraska.svg` | [Nebraska state highway marker.svg](https://commons.wikimedia.org/wiki/File:Nebraska_state_highway_marker.svg) | Fredddie, originally created by Scott Onson | PD MUTCD NE |  |
| Nevada | `nevada.svg` | [Nevada blank.svg](https://commons.wikimedia.org/wiki/File:Nevada_blank.svg) | Geopgeop; NevadaDOT | PD MUTCD NV |  |
| New Hampshire | `new_hampshire.svg` | [NH Route blank.svg](https://commons.wikimedia.org/wiki/File:NH_Route_blank.svg) | SPUI | PD shape |  |
| New Jersey | `state_circle.svg` | shared with the generic circle | | | Posts the plain MUTCD M1-5 circle |
| New Mexico | `new_mexico.svg` | [New Mexico blank.svg](https://commons.wikimedia.org/wiki/File:New_Mexico_blank.svg) | not given | PD MUTCD NM |  |
| New York | `new_york.svg` | [NY-blank.svg](https://commons.wikimedia.org/wiki/File:NY-blank.svg) | TwinsMetsFan | PD MUTCD NY |  |
| North Carolina | `north_carolina.svg` | [NC blank.svg](https://commons.wikimedia.org/wiki/File:NC_blank.svg) | TwinsMetsFan | PD MUTCD NC |  |
| North Dakota | `north_dakota.svg` | [ND-blank.svg](https://commons.wikimedia.org/wiki/File:ND-blank.svg) | North Dakota Department of Transportation | PD shape | The 2015 state-outline marker. |
| Ohio | `ohio.svg` | [OH-blank.svg](https://commons.wikimedia.org/wiki/File:OH-blank.svg) | Holderca1 | PD-self |  |
| Oklahoma | `oklahoma.svg` | [Oklahoma State Highway blank.svg](https://commons.wikimedia.org/wiki/File:Oklahoma_State_Highway_blank.svg) | SPUI | PD shape |  |
| Oregon | `oregon.svg` | [OR blank.svg](https://commons.wikimedia.org/wiki/File:OR_blank.svg) | Northenglish | PD MUTCD OR |  |
| Pennsylvania | `pennsylvania.svg` | [PA-blank2di.svg](https://commons.wikimedia.org/wiki/File:PA-blank2di.svg) | TwinsMetsFan | PD MUTCD PA |  |
| Rhode Island | `rhode_island.svg` | [Rhode Island blank.svg](https://commons.wikimedia.org/wiki/File:Rhode_Island_blank.svg) | TwinsMetsFan | PD shape |  |
| South Carolina | `south_carolina.svg` | [South Carolina blank.svg](https://commons.wikimedia.org/wiki/File:South_Carolina_blank.svg) | Mr. Matté | PD MUTCD SC | Dropped a `<use>` that points at an outline in another file (it renders nothing); the state's white rim it drew is a hairline at atlas size. |
| South Dakota | `south_dakota.svg` | [SD Blank-2d.svg](https://commons.wikimedia.org/wiki/File:SD_Blank-2d.svg) | Fredddie | PD shape |  |
| Tennessee | `tennessee.svg` | [Tennessee blank.svg](https://commons.wikimedia.org/wiki/File:Tennessee_blank.svg) | Fredddie, state design by SPUI | PD MUTCD TN |  |
| Texas | `texas.svg` | [Texas blank.svg](https://commons.wikimedia.org/wiki/File:Texas_blank.svg) | not given | PD MUTCD TX |  |
| Utah | `utah.svg` | [Utah blank.svg](https://commons.wikimedia.org/wiki/File:Utah_blank.svg) | Fredddie | PD MUTCD UT |  |
| Vermont | `vermont.svg` | [Vermont blank.svg](https://commons.wikimedia.org/wiki/File:Vermont_blank.svg) | SPUI | PD shape |  |
| Virginia | `virginia.svg` | [Virginia blank.svg](https://commons.wikimedia.org/wiki/File:Virginia_blank.svg) | not given | PD MUTCD VA |  |
| Washington | `washington.svg` | [WA-blank.svg](https://commons.wikimedia.org/wiki/File:WA-blank.svg) | The original uploader was PHenry at English Wikipedia. | PD MUTCD WA |  |
| West Virginia | `west_virginia.svg` | [WV-blank.svg](https://commons.wikimedia.org/wiki/File:WV-blank.svg) | TwinsMetsFan | PD shape |  |
| Wisconsin | `wisconsin.svg` | [WIS blank.svg](https://commons.wikimedia.org/wiki/File:WIS_blank.svg) | SPUI | PD MUTCD WI |  |
| Wyoming | `wyoming.svg` | [WY-blank.svg](https://commons.wikimedia.org/wiki/File:WY-blank.svg) | not given | PD MUTCD WY |  |

## Province markers

| Marker | File | Source | Author | Licence | Notes |
|---|---|---|---|---|---|
| New Brunswick | `new_brunswick.svg` | [NB blank green.svg](https://commons.wikimedia.org/wiki/File:NB_blank_green.svg) | Fredddie and Michael J (NB shape) | PD shape | Arterial (green) variant. Dropped an embedded Illustrator `foreignObject` blob. |
| Newfoundland and Labrador | `newfoundland.svg` | [NL Route blank.svg](https://commons.wikimedia.org/wiki/File:NL_Route_blank.svg) | Fredddie | PD shape |  |
| Nova Scotia | `nova_scotia.svg` | [Nova Scotia 7.svg](https://commons.wikimedia.org/wiki/File:Nova_Scotia_7.svg) | Fredddie | PD ineligible | Commons has no blank of the trunk highway marker; the numeral group was removed from the Trunk 7 file. |
| Ontario | `ontario.svg` | [Ontario blank.svg](https://commons.wikimedia.org/wiki/File:Ontario_blank.svg) | Imzadi1979 | PD Canada |  |
| Prince Edward Island | `prince_edward_island.svg` | [PEI Highway blank.svg](https://commons.wikimedia.org/wiki/File:PEI_Highway_blank.svg) | Svgalbertian | PD ineligible |  |
| Quebec | `quebec.svg` | [Qc shield.svg](https://commons.wikimedia.org/wiki/File:Qc_shield.svg) | 京市 | PD shape | Provincial route marker (not the blue Autoroute marker). |
