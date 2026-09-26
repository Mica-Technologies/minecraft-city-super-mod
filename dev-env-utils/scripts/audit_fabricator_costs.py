#!/usr/bin/env python3
"""Audit the Fabricator cost model statically, without launching the game.

Replicates the rules in materials/CsmFabricatorCosts.java against the block index, resolving full
ancestry chains so the Java `instanceof` checks are reproduced faithfully rather than approximated
by direct superclass.

Use it to sanity check that costs make sense for what each block actually is:

    python3 dev-env-utils/scripts/audit_fabricator_costs.py            # summary by cost
    python3 dev-env-utils/scripts/audit_fabricator_costs.py --list POLE_SECTION
    python3 dev-env-utils/scripts/audit_fabricator_costs.py --grep pole
"""

import collections
import io
import sys

import csm_block_index as index_mod

# Tabs whose blocks are never fabricable.
NON_FABRICABLE_TABS = {"tabroadshidden", "tablightinghidden", "tabfurnishingshidden",
                       "tabmaterials"}


def build_ancestry(classes):
    """Return {class_name: set_of_all_ancestor_class_names_including_itself}."""
    ancestry = {}

    def resolve(name, seen):
        if name in ancestry:
            return ancestry[name]
        if name in seen:  # defensive: a cycle would otherwise recurse forever
            return {name}
        seen.add(name)
        chain = {name}
        info = classes.get(name)
        if info and info.get("extends"):
            chain |= resolve(info["extends"], seen)
        ancestry[name] = chain
        return chain

    for class_name in list(classes):
        resolve(class_name, set())
    return ancestry


import re

LANG_FILES = index_mod.lang_files("en_us")

POLE_NOUNS = {"pole", "mast", "crossarm", "standard", "post"}
MOUNT_NOUNS = {"mount", "bracket", "backplate", "cover", "visor", "clamp", "hanger", "adapter",
               "coupler", "cap", "top", "base", "plate", "arm"}
OPTICAL_WORDS = ["camera", "alpr", "radar", "lidar"]
METAL_FURNITURE_WORDS = ["hydrant", "chain", "chains", "barbed", "radiator", "grill", "grate",
                         "rail"]
ELECTRONIC_NOVELTY_WORDS = ["record", "player", "jukebox", "radio", "television", "tv"]


def load_display_names():
    """Mirror of CsmBlockDisplayNames: registry name -> normalized display name."""
    names = {}
    for lang in LANG_FILES:
        with io.open(lang, encoding="utf-8") as handle:
            for line in handle:
                match = re.match(r"tile\.([^=]+)\.name=(.*)", line.strip())
                if match:
                    names[match.group(1)] = re.sub(r"\([^)]*\)|\[[^\]]*\]", " ",
                                                  match.group(2)).strip().lower()
    return names


DISPLAY = load_display_names()


def last_word(registry):
    words = re.findall(r"[a-z]+", DISPLAY.get(registry, ""))
    return words[-1] if words else ""


def words_of(registry):
    """Whole words of the display name.

    Token matching rather than substring, mirroring CsmBlockDisplayNames.hasWord: it is what
    keeps "alarm" from matching "arm" and "christmas" from matching "mast".
    """
    return set(re.findall(r"[a-z]+", DISPLAY.get(registry, "")))


def has_word(registry, word):
    return word in words_of(registry)


def has_any(registry, words):
    return any(has_word(registry, w) for w in words)


def _metal_dye(registry):
    if has_word(registry, "iridescent"):
        return "prismarine_crystals"
    if has_word(registry, "light") and has_word(registry, "blue"):
        return "dye:lightblue"
    for colour in ("black", "red", "green", "blue", "purple", "silver", "pink", "lime",
                   "yellow", "magenta", "orange", "copper"):
        if has_word(registry, colour):
            return "dye:" + colour
    return "dye:white"


def cost_for(registry, info, ancestors):
    """Mirror of CsmFabricatorCosts.getCost, returning a tuple of ingredient labels or None."""
    tab = info["tab"]
    if tab in NON_FABRICABLE_TABS:
        return None

    def has(*names):
        return any(n in ancestors for n in names)

    noun = last_word(registry)

    if noun in POLE_NOUNS:
        if has_word(registry, "concrete"):
            return ("POLE_SECTION", "CONCRETE_MIX")
        if has_word(registry, "wood") or has_word(registry, "wooden") or noun == "crossarm":
            return ("planks x2", "FASTENER_KIT")
        return ("POLE_SECTION x2",)

    if tab == "tabroadsigns":
        return ("SIGN_BLANK",)

    if tab == "tabbuildingmaterials":
        if (has_word(registry, "door") and not has_word(registry, "garage")
                and not has_word(registry, "keypad")):
            if has_word(registry, "storefront"):
                return ("glass_pane x2", "SHEET_METAL")
            if has_word(registry, "metal") or has_word(registry, "fire") or has_word(registry, "exit"):
                return ("SHEET_METAL x2", "FASTENER_KIT")
            return ("planks x3",)
        if has_word(registry, "metal"):
            dye = _metal_dye(registry)
            if has("AbstractBlockSlab"):
                return ("SHEET_METAL", dye)
            if has("AbstractBlockStairs"):
                return ("SHEET_METAL x2", dye)
            if has("AbstractBlockFence"):
                return ("SHEET_METAL", "FASTENER_KIT", dye)
            return ("SHEET_METAL x2", dye)
        if has_word(registry, "cladding"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if has_word(registry, "veneer") and not has_word(registry, "cast"):
            return ("cobblestone", "CONCRETE_MIX")
        if has_word(registry, "siding"):
            if has_word(registry, "cement"):
                return ("CONCRETE_MIX", "paper")
            if has_word(registry, "vinyl"):
                return ("paper x2", "dye")
            return ("planks x2",)
        if has_word(registry, "opener"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD", "WIRING_HARNESS")
        if has_word(registry, "keypad"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD")
        if has_word(registry, "station"):
            return ("ENCLOSURE_SHELL", "WIRING_HARNESS")
        if has_word(registry, "button"):
            return ("WIRING_HARNESS",)
        if has_word(registry, "hanger"):
            return ("FASTENER_KIT",)
        if has_word(registry, "door"):
            return ("SHEET_METAL", "FASTENER_KIT")
        if has_word(registry, "grille"):
            return ("iron_ingot", "FASTENER_KIT")
        if has_word(registry, "glass"):
            glass = "glass_pane" if has_word(registry, "pane") else "glass"
            if has_word(registry, "bullet"):
                return (glass + " x3",)
            if has_word(registry, "wired"):
                return (glass, "FASTENER_KIT")
            if has_word(registry, "clear"):
                return (glass,)
            return (glass, "dye")
        if has_word(registry, "chain"):
            return ("iron_ingot", "FASTENER_KIT")
        return ("CONCRETE_MIX", "clay_ball")

    if tab == "tabconstructionsite":
        if has_word(registry, "scaffold"):
            return ("POLE_SECTION", "planks")
        if has_word(registry, "formwork"):
            return ("planks x2", "FASTENER_KIT")
        if has_word(registry, "shore"):
            return ("POLE_SECTION", "FASTENER_KIT")
        if has_word(registry, "rebar"):
            return ("iron_ingot",)
        if has_word(registry, "trench"):
            if has_word(registry, "box"):
                return ("SHEET_METAL x2", "POLE_SECTION")
            return ("SHEET_METAL x2",)
        if has_word(registry, "stockpile"):
            if has_word(registry, "gravel"):
                return ("gravel",)
            if has_word(registry, "sand"):
                return ("sand",)
            return ("dirt",)
        if has_word(registry, "silt"):
            return ("planks", "paper")
        if has_word(registry, "pallet"):
            if has_word(registry, "brick"):
                return ("planks", "clay_ball x2")
            if has_word(registry, "drywall"):
                return ("planks", "paper x2")
            if has_word(registry, "block"):
                return ("planks", "CONCRETE_MIX")
            return ("planks", "CONCRETE_MIX x2")
        if has_word(registry, "lumber"):
            return ("planks x3",)
        if has_word(registry, "insulation") or has_word(registry, "pvc"):
            return ("paper x2", "dye")
        if has_word(registry, "conduit"):
            return ("POLE_SECTION",)
        if has_word(registry, "spool"):
            return ("planks", "WIRING_HARNESS")
        if has_word(registry, "container") or has_word(registry, "dumpster"):
            return ("SHEET_METAL x2",)
        if has_word(registry, "trailer"):
            return ("SHEET_METAL", "planks")
        if has_word(registry, "toilet"):
            return ("SHEET_METAL", "dye")
        if has_word(registry, "gang"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if has_word(registry, "washout"):
            return ("SHEET_METAL", "paper")
        if has_word(registry, "crane"):
            return ("POLE_SECTION x4", "CONTROL_BOARD", "WIRING_HARNESS")
        return ("SHEET_METAL", "FASTENER_KIT")

    if tab == "tabinteriorfinishes":
        if has_word(registry, "venetian"):
            return ("SHEET_METAL",)
        if has_word(registry, "shade") or has_word(registry, "vertical"):
            return ("paper x2",)
        if has_word(registry, "curtain"):
            return ("paper x2", "dye")
        if has_word(registry, "carpet"):
            return ("wool",)
        if has_word(registry, "vinyl"):
            return ("paper", "dye")
        if has_word(registry, "ceramic"):
            return ("clay_ball x2",)
        if has_word(registry, "hardwood"):
            return ("planks x2",)
        if has_word(registry, "polished"):
            return ("CONCRETE_MIX",)
        if has_word(registry, "rubber"):
            return ("slime_ball",)
        if has_word(registry, "guard"):
            return ("iron_ingot",) if has_word(registry, "stainless") else ("paper", "dye")
        if has_word(registry, "drywall"):
            return ("paper", "dye")
        if has_word(registry, "acoustic"):
            return ("wool",)
        if has_word(registry, "beadboard") or has_word(registry, "slat"):
            return ("planks",)
        if has_word(registry, "carpet"):
            return ("wool",)
        if has_word(registry, "vinyl"):
            return ("paper", "dye")
        if has_word(registry, "ceramic"):
            return ("clay_ball x2",)
        if has_word(registry, "hardwood"):
            return ("planks x2",)
        if has_word(registry, "polished"):
            return ("CONCRETE_MIX",)
        if has_word(registry, "rubber"):
            return ("slime_ball",)
        return ("CONCRETE_MIX", "clay_ball")

    # Never in the framing tab: see the note in CsmFabricatorCosts. A base plate and a
    # sole plate are structure, not brackets.
    if tab not in ("tabstructureframing", "tabstreetscape") and noun in MOUNT_NOUNS:
        return ("SHEET_METAL", "FASTENER_KIT")

    if has_any(registry, OPTICAL_WORDS):
        return ("OPTICAL_SENSOR", "CONTROL_BOARD")

    if tab == "tabhvac":
        return ("DUCTING", "SHEET_METAL")
    if tab == "tablifesafety":
        if has_word(registry, "sprinkler"):
            return ("SHEET_METAL", "LENS_ASSEMBLY")
        # Most specific first: Detector extends Activator, VoiceEvac extends Sounder.
        if has("AbstractBlockFireAlarmDetector"):
            return ("OPTICAL_SENSOR", "CONTROL_BOARD")
        if has("AbstractBlockFireAlarmActivator"):
            return ("CONTROL_BOARD", "SHEET_METAL")
        if has("AbstractBlockFireAlarmSounder"):
            return ("SOUNDER_DRIVER", "ENCLOSURE_SHELL")
        return ("SHEET_METAL", "WIRING_HARNESS")
    # The two tabs split out of Life Safety (LifeSafetyFabricatorRules.priceExits and
    # priceFireProtection): what they cost in the one tab.
    if tab == "tabexitsemergency":
        return ("SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabfireprotection":
        if has_word(registry, "sprinkler"):
            return ("SHEET_METAL", "LENS_ASSEMBLY")
        if has("AbstractBlockFireAlarmDetector"):
            return ("OPTICAL_SENSOR", "CONTROL_BOARD")
        if has("AbstractBlockFireAlarmActivator"):
            return ("CONTROL_BOARD", "SHEET_METAL")
        if has("AbstractBlockFireAlarmSounder"):
            return ("SOUNDER_DRIVER", "ENCLOSURE_SHELL")
        if has_word(registry, "sign"):
            return ("SIGN_BLANK",)
        if has_word(registry, "aed"):
            return ("SHEET_METAL", "CONTROL_BOARD", "WIRING_HARNESS")
        if has_word(registry, "cabinet"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if has_word(registry, "extinguisher"):
            return ("SHEET_METAL x2",)
        if has_any(registry, ("valve", "connection", "riser", "pipe", "manifold", "outlet",
                              "preventer", "box")):
            return ("SHEET_METAL", "FASTENER_KIT")
        return ("SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabemergencyservices":
        if has_any(registry, ("sign", "plaque", "emblem")):
            return ("SIGN_BLANK",)
        if has_word(registry, "hose"):
            return ("wool x2",)
        if has_word(registry, "tape"):
            return ("paper", "dye")
        if has_any(registry, ("gong", "bell")):
            return ("SHEET_METAL", "SOUNDER_DRIVER")
        if has_any(registry, ("compressor", "extractor", "fill", "console", "siren", "controller",
                              "detector", "call", "radio", "monitor", "alerting", "speaker",
                              "light", "lamp", "scanner")):
            return ("SHEET_METAL", "CONTROL_BOARD", "WIRING_HARNESS")
        if has_any(registry, ("cylinder", "scba")):
            return ("SHEET_METAL x2",)
        return ("SHEET_METAL", "FASTENER_KIT")
    if tab == "tablighting":
        return ("LED_MODULE", "SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabnovelties":
        if has_any(registry, ELECTRONIC_NOVELTY_WORDS):
            return ("CONTROL_BOARD", "SHEET_METAL")
        return ("clay_ball x2", "dye:any")
    if tab == "tabgaming":
        if has_word(registry, "arcade"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "LED_MODULE")
        if noun in ("cards", "deck") or has_word(registry, "card"):
            return ("paper x3",)
        return ("planks x2", "FASTENER_KIT")
    if tab == "tabpowergrid":
        return ("POLE_SECTION", "WIRING_HARNESS")
    if tab == "tabstructureframing":
        if has_word(registry, "wood"):
            return ("planks x2", "FASTENER_KIT")
        return ("SHEET_METAL x2", "FASTENER_KIT")
    if tab == "tabtechnology":
        return ("CONTROL_BOARD", "SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabtransit":
        # Mirrors TransitFabricatorRules: the bus stops and the station fit-out by what they
        # are made of, and the fare equipment at the price it kept from Technology.
        if registry.startswith("bus_stop_flag_"):
            return ("SIGN_BLANK", "FASTENER_KIT")
        if registry.startswith("bus_stop_") and registry.endswith("_case"):
            return ("SIGN_BLANK", "SHEET_METAL")
        if registry == "bus_stop_arrival_display":
            return ("LED_MODULE", "CONTROL_BOARD", "SHEET_METAL")
        if registry in ("bus_departure_board", "bus_bay_display"):
            return ("LED_MODULE", "CONTROL_BOARD", "SHEET_METAL")
        if registry == "bus_stop_curb_plaque":
            return ("SHEET_METAL",)
        if registry.startswith("tactile_") or registry.startswith("station_tile_"):
            return ("CONCRETE_MIX",)
        if registry in ("platform_help_point", "station_emergency_point"):
            return ("CONTROL_BOARD", "SOUNDER_DRIVER", "SHEET_METAL")
        if registry.startswith("platform_cctv_"):
            return ("OPTICAL_SENSOR", "SHEET_METAL")
        if registry == "platform_validator":
            return ("CONTROL_BOARD", "LED_MODULE", "SHEET_METAL")
        if registry == "platform_clock":
            return ("CONTROL_BOARD", "SHEET_METAL")
        if registry == "platform_canopy":
            return ("SHEET_METAL x2", "LED_MODULE")
        if registry.startswith("platform_column_"):
            return ("POLE_SECTION", "CONCRETE_MIX")
        if (registry.startswith("platform_sign_") or registry.startswith("station_name_")
                or registry in ("platform_number_sign", "platform_gap_sign",
                                "station_network_map")):
            return ("SIGN_BLANK", "FASTENER_KIT")
        if registry.startswith("station_") and registry not in (
                "station_emergency_point", "station_network_map"):
            station = {
                "station_entrance_glass": ("SHEET_METAL", "glass_pane x2"),
                "station_entrance_globe": ("POLE_SECTION", "LENS_ASSEMBLY", "LED_MODULE"),
                "station_entrance_railing_sign": ("SHEET_METAL", "FASTENER_KIT", "SIGN_BLANK"),
                "station_fare_railing_sign": ("SHEET_METAL", "FASTENER_KIT", "SIGN_BLANK"),
                "station_service_gate": ("SHEET_METAL x2", "FASTENER_KIT"),
                "station_line_bullet": ("SIGN_BLANK",),
                "station_booth_counter": ("SHEET_METAL x2", "glass_pane x2", "FASTENER_KIT"),
            }
            if registry in station:
                return station[registry]
            if registry.startswith("station_entrance_roof_"):
                return ("SHEET_METAL x2", "LED_MODULE")
            if registry.endswith("_railing"):
                return ("SHEET_METAL", "FASTENER_KIT")
        if registry.startswith("platform_"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if registry.startswith("airport_"):
            airside = {
                "airport_runway_edge_light": ("LENS_ASSEMBLY", "LED_MODULE", "FASTENER_KIT"),
                "airport_approach_light_bar": ("LENS_ASSEMBLY x2", "LED_MODULE x2", "SHEET_METAL"),
                "airport_beacon": ("LENS_ASSEMBLY x2", "LED_MODULE", "CONTROL_BOARD"),
                "airport_wind_sock": ("POLE_SECTION", "LENS_ASSEMBLY", "FASTENER_KIT"),
                "airport_antenna_mast": ("POLE_SECTION", "WIRING_HARNESS", "LENS_ASSEMBLY"),
                "airport_taxiway_location_sign": ("SIGN_BLANK", "LED_MODULE", "FASTENER_KIT"),
                "airport_stand_sign": ("SIGN_BLANK", "FASTENER_KIT"),
                "airport_airfield_mast": ("POLE_SECTION", "FASTENER_KIT"),
                "airport_wheel_chocks": ("SHEET_METAL",),
                "airport_ground_power_unit": ("ENCLOSURE_SHELL", "CONTROL_BOARD",
                                              "WIRING_HARNESS x2"),
                "airport_baggage_tug": ("SHEET_METAL x3", "CONTROL_BOARD", "WIRING_HARNESS"),
                "airport_baggage_cart": ("SHEET_METAL x2", "FASTENER_KIT"),
                "airport_air_stairs": ("SHEET_METAL x3", "FASTENER_KIT"),
                "airport_jet_bridge_cab": ("SHEET_METAL x3", "CONTROL_BOARD", "FASTENER_KIT"),
                "airport_jet_bridge_rotunda": ("SHEET_METAL x4", "FASTENER_KIT x2"),
                "airport_jet_bridge_drive": ("POLE_SECTION x2", "WIRING_HARNESS"),
                "airport_jet_bridge_column": ("CONCRETE_MIX x2",),
            }
            for same, as_ in (("airport_taxiway_edge_light", "airport_runway_edge_light"),
                              ("airport_runway_threshold_light", "airport_runway_edge_light"),
                              ("airport_runway_centreline_light", "airport_runway_edge_light"),
                              ("airport_taxiway_centreline_light", "airport_runway_edge_light"),
                              ("airport_stop_bar_light", "airport_runway_edge_light"),
                              ("airport_taxiway_direction_sign", "airport_taxiway_location_sign"),
                              ("airport_runway_holding_sign", "airport_taxiway_location_sign"),
                              ("airport_runway_distance_sign", "airport_taxiway_location_sign"),
                              ("airport_jet_bridge_tunnel", "airport_air_stairs")):
                airside[same] = airside[as_]
            if registry in airside:
                return airside[registry]
            if registry in ("airport_checkin_desk", "airport_gate_desk"):
                return ("SHEET_METAL x2", "CONTROL_BOARD", "FASTENER_KIT")
            if registry == "airport_checkin_scale":
                return ("SHEET_METAL", "CONTROL_BOARD", "WIRING_HARNESS")
            if registry == "airport_self_checkin_kiosk":
                return ("CONTROL_BOARD", "LED_MODULE", "SHEET_METAL")
            if registry == "airport_xray_scanner":
                return ("ENCLOSURE_SHELL", "CONTROL_BOARD", "OPTICAL_SENSOR", "WIRING_HARNESS")
            if registry == "airport_boarding_pass_scanner":
                return ("CONTROL_BOARD", "OPTICAL_SENSOR", "SHEET_METAL")
            if registry.startswith("airport_flight_board_"):
                return ("LED_MODULE", "CONTROL_BOARD", "SHEET_METAL")
            if registry == "airport_baggage_carousel":
                return ("SHEET_METAL x2", "WIRING_HARNESS", "FASTENER_KIT")
            if registry.startswith("airport_queue_stanchion_"):
                return ("POLE_SECTION", "FASTENER_KIT")
            if registry in ("airport_security_trays", "airport_security_tray_items",
                            "airport_luggage_cart"):
                return ("SHEET_METAL", "FASTENER_KIT")
            if registry.startswith("airport_sign_") or registry == "airport_gate_sign":
                return ("SIGN_BLANK", "FASTENER_KIT")
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if registry.startswith("bus_shelter_glass_"):
            return ("POLE_SECTION", "SHEET_METAL x2", "glass_pane x4", "LED_MODULE")
        if registry.startswith("bus_shelter_cantilever_"):
            return ("POLE_SECTION", "SHEET_METAL x2", "LED_MODULE")
        if registry.startswith("bus_shelter_"):
            return ("POLE_SECTION", "SHEET_METAL", "LED_MODULE")
        return ("CONTROL_BOARD", "SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabtrafficaccessories":
        return ("SHEET_METAL", "FASTENER_KIT")
    if tab == "tabsignage":
        if has_word(registry, "shelter"):
            return ("SHEET_METAL", "LED_MODULE", "SIGN_BLANK")
        if has_word(registry, "kiosk"):
            return ("SHEET_METAL x2", "LED_MODULE", "SIGN_BLANK")
        return ("SIGN_BLANK", "SHEET_METAL")
    if tab == "tabtrafficsignals":
        if has("AbstractBlockTrafficSignalSensor", "AbstractBlockTrafficSignalSensorHZEight"):
            return ("OPTICAL_SENSOR", "CONTROL_BOARD")
        if has("BlockTrafficSignalController"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD x2", "WIRING_HARNESS")
        if has("AbstractBlockControllableSignal"):
            return ("LED_MODULE", "LENS_ASSEMBLY", "SHEET_METAL")
        return ("SHEET_METAL", "WIRING_HARNESS")
    if tab == "tabstreetscape" and has("BlockParkingMeter"):
        # Mirrors StreetscapeFabricatorRules: electronics unless it is clockwork.
        if "mechanical" in registry:
            return ("SHEET_METAL", "FASTENER_KIT")
        return ("CONTROL_BOARD", "SHEET_METAL")
    if tab == "tabstreetscape" and registry.startswith("mailbox_cluster"):
        return ("SHEET_METAL x2", "FASTENER_KIT")
    if tab == "tabstreetscape" and registry.startswith("bollard_sphere"):
        return ("CONCRETE_MIX x2",)
    if tab == "tabstreetscape" and registry == "parking_pay_by_phone_sign":
        return ("SIGN_BLANK",)
    if tab == "tabstreetscape" and has("BlockUtilityBox") and registry.startswith("transformer"):
        return ("SHEET_METAL x2", "WIRING_HARNESS", "CONCRETE_MIX")
    if tab == "tabstreetscape" and has("BlockStreetCover"):
        # Mirrors StreetscapeFabricatorRules.
        if registry.startswith("storm_drain_marker"):
            return ("SIGN_BLANK",)
        if registry.startswith(("vault_lid", "valve_box", "sewer_cleanout")):
            return ("iron_ingot", "CONCRETE_MIX")
        return ("iron_ingot x2",)
    if tab == "tabutilities":
        # Mirrors UtilitiesFabricatorRules (Utilities module).
        if registry.startswith("utility_label_"):
            return ("SIGN_BLANK",)
        utilities = {
            "electric_meter_digital": ("ENCLOSURE_SHELL", "CONTROL_BOARD"),
            "electric_meter_analog": ("ENCLOSURE_SHELL", "glass_pane"),
            "electric_meter_bank": ("ENCLOSURE_SHELL", "CONTROL_BOARD", "WIRING_HARNESS"),
            "electric_meter_socket": ("SHEET_METAL", "WIRING_HARNESS"),
            "service_disconnect": ("ENCLOSURE_SHELL", "WIRING_HARNESS"),
            "main_panel": ("ENCLOSURE_SHELL", "WIRING_HARNESS"),
            "electric_switchboard": ("ENCLOSURE_SHELL x2", "WIRING_HARNESS x2"),
            "gas_meter": ("SHEET_METAL", "iron_ingot x2"),
            "gas_meter_bank": ("SHEET_METAL", "iron_ingot x2"),
            "water_meter_setter": ("iron_ingot x2", "FASTENER_KIT"),
            # the water system
            "water_tower_leg": ("POLE_SECTION",),
            "water_tower_riser": ("POLE_SECTION",),
            "water_tower_brace": ("iron_ingot",),
            "water_tower_strut": ("iron_ingot",),
            "caged_ladder": ("iron_ingot",),
            "water_pipe": ("iron_ingot",),
            "water_pipe_support": ("iron_ingot",),
            "water_tower_pedestal": ("SHEET_METAL x2",),
            "water_tower_bowl_small": ("SHEET_METAL x8", "FASTENER_KIT x2"),
            "water_tower_spheroid_small": ("SHEET_METAL x8", "FASTENER_KIT x2"),
            "water_tower_bowl_medium": ("SHEET_METAL x16", "FASTENER_KIT x4"),
            "water_tower_spheroid_medium": ("SHEET_METAL x16", "FASTENER_KIT x4"),
            "ground_tank_small": ("SHEET_METAL x6", "CONCRETE_MIX"),
            "ground_tank_large": ("SHEET_METAL x12", "CONCRETE_MIX x2"),
            "water_gate_valve": ("iron_ingot x2", "FASTENER_KIT"),
            "water_butterfly_valve": ("iron_ingot x2", "FASTENER_KIT"),
            "water_check_valve": ("iron_ingot x2", "FASTENER_KIT"),
            "water_air_release_valve": ("iron_ingot", "FASTENER_KIT"),
            "water_flow_meter": ("iron_ingot", "CONTROL_BOARD"),
            "pump_split_case": ("iron_ingot x4", "WIRING_HARNESS"),
            "pump_vertical_inline": ("iron_ingot x3", "WIRING_HARNESS"),
            "water_hydropneumatic_tank": ("SHEET_METAL x3",),
            "pump_control_panel": ("ENCLOSURE_SHELL x2", "CONTROL_BOARD", "WIRING_HARNESS"),
            "air_release_enclosure": ("SHEET_METAL", "iron_ingot"),
            "air_release_vault": ("CONCRETE_MIX x2", "iron_ingot"),
            "backflow_enclosure": ("SHEET_METAL x2",),
            "chemical_feed_skid": ("ENCLOSURE_SHELL", "CONTROL_BOARD", "SHEET_METAL"),
            "chlorine_cylinder_scale": ("iron_ingot x4", "CONTROL_BOARD"),
        }
        if registry in utilities:
            return utilities[registry]
    if tab == "tabfurniture":
        if has_any(registry, METAL_FURNITURE_WORDS):
            return ("SHEET_METAL", "FASTENER_KIT")
        return ("planks x2", "FASTENER_KIT")
    if tab == "tabresidential":
        # Mirrors ResidentialFabricatorRules (Furniture & Novelties module).
        # The outdoor and backyard section comes first.
        frame = "iron_ingot" if registry.endswith("_black") else "planks"
        if registry.startswith("patio_table_"):
            return (frame + " x3", "FASTENER_KIT")
        if registry.startswith("patio_chair_"):
            return (frame + " x2", "FASTENER_KIT")
        if registry.startswith("patio_umbrella_"):
            return ("wool x3", "iron_ingot x2")
        if registry.startswith("table_umbrella_"):
            return ("wool x3", "iron_ingot")
        if registry.startswith("sun_lounger_"):
            return (frame + " x3", "wool x2")
        if registry.startswith("adirondack_chair_"):
            return ("planks x3",)
        if registry.startswith("outdoor_sofa_"):
            return ("planks x2", "wool x3")
        if registry.startswith("outdoor_rug_"):
            return ("wool x2",)
        if registry.startswith("gas_grill_"):
            return ("SHEET_METAL x3", "WIRING_HARNESS")
        if registry.startswith("kettle_grill_"):
            return ("SHEET_METAL x2", "iron_ingot")
        if registry.startswith("fire_pit_"):
            return ("SHEET_METAL x2",) if registry.endswith("_steel") else ("stone x4",)
        if registry.startswith(("cooler_", "kiddie_pool_")):
            return ("ENCLOSURE_SHELL x2",)
        if registry.startswith("pool_float_"):
            return ("ENCLOSURE_SHELL",)
        if registry.startswith("chimney_"):
            return ("brick x6",)
        if registry.startswith("picket_fence_"):
            return ("planks x2",)
        if registry.startswith("picket_gate_"):
            return ("planks x3", "FASTENER_KIT")
        if registry.startswith("stepping_stones_"):
            return ("stone x2",)
        if registry.startswith("string_lights_"):
            return ("LED_MODULE", "string x2")
        if registry.startswith("trampoline_"):
            return ("iron_ingot x2", "wool x2", "slime_ball")
        if registry.startswith("bounce_castle_"):
            return ("wool x8", "slime_ball x2")
        if registry.startswith("diving_board_"):
            return ("planks x2", "iron_ingot", "slime_ball")
        if registry.startswith("pet_bed_"):
            return ("wool x2",)
        if registry.startswith("pet_bowls_"):
            return ("clay_ball x2",) if registry.endswith("_red") else ("iron_ingot",)
        if registry.startswith("litter_box_"):
            return ("ENCLOSURE_SHELL", "sand")
        if registry.startswith("cat_tree_"):
            return ("planks x3", "wool x3", "string x2")
        if registry.startswith("hose_reel_"):
            return ("SHEET_METAL", "FASTENER_KIT")
        if registry.startswith("outdoor_wall_light_"):
            return ("LED_MODULE", "SHEET_METAL")
        # The living room's extras.
        if registry.startswith(("large_flat_screen_tv_", "large_wall_tv_")):
            return ("ENCLOSURE_SHELL x2", "CONTROL_BOARD", "LED_MODULE x2")
        if registry.startswith(("flat_screen_tv_", "wall_tv_")):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("crt_tv_"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD", "glass")
        if registry.startswith("stereo_"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD", "SOUNDER_DRIVER")
        if registry.startswith("bookshelf_speaker_"):
            return ("planks", "SOUNDER_DRIVER")
        if registry.startswith("subwoofer_"):
            return ("planks x2", "SOUNDER_DRIVER")
        if registry.startswith("upright_piano_"):
            return ("planks x6", "iron_ingot x2", "FASTENER_KIT")
        if registry.startswith("piano_bench_"):
            return ("planks x2", "wool")
        if registry.startswith("digital_clock_"):
            return ("ENCLOSURE_SHELL", "CONTROL_BOARD")
        if registry.startswith("wall_clock_"):
            return ("planks", "clock")
        if registry.startswith("photo_frame_"):
            return ("planks", "paper")
        if registry.startswith("wall_photo_frames_"):
            return ("planks x2", "paper x3")
        if registry.startswith("wide_wall_art_"):
            return ("painting x2",)
        if registry.startswith("wall_art_"):
            return ("painting",)
        if registry.startswith(("monstera_plant_", "snake_plant_", "fiddle_leaf_fig_")):
            return ("flower_pot", "sapling")
        if registry.startswith("succulent_pots_"):
            return ("flower_pot", "cactus")
        if registry.startswith("hanging_plant_"):
            return ("flower_pot", "vine", "string")
        if registry.startswith("fireplace_"):
            return ("stone x4", "planks x2", "iron_ingot")
        if registry.startswith("ceiling_fan_"):
            return ("SHEET_METAL", "WIRING_HARNESS", "LED_MODULE", "planks")
        if registry.startswith(("floor_lamp_", "table_lamp_")):
            return ("LED_MODULE", "iron_ingot", "wool")
        if registry.startswith("pillar_candles_"):
            return ("torch x3",)
        if registry.startswith("candlestick_"):
            return ("torch", "gold_nugget x2")
        if registry.startswith("door_mat_"):
            return ("wool",)
        if registry.startswith("light_switch_"):
            return ("lever", "WIRING_HARNESS")
        if registry.startswith("doorbell_"):
            return ("stone_button", "SOUNDER_DRIVER")
        if registry.startswith("storage_crate_"):
            return ("planks x4", "FASTENER_KIT")
        # The bathroom, restroom and laundry.
        if registry.startswith("toilet_paper_holder_"):
            return ("iron_ingot", "paper")
        if registry.startswith("toilet_brush_"):
            return ("iron_ingot",)
        if registry.startswith(("toilet_", "urinal_")):
            return ("clay_ball x4", "FASTENER_KIT")
        if registry.startswith(("pedestal_sink_", "laundry_tub_")):
            return ("clay_ball x3", "SHEET_METAL")
        if registry.startswith("bathroom_vanity_"):
            return ("planks x3", "clay_ball x2", "SHEET_METAL")
        if registry.startswith("mirror_cabinet_"):
            return ("planks x2", "glass_pane x2", "FASTENER_KIT")
        if registry.startswith("bathtub_"):
            return ("clay_ball x6", "SHEET_METAL")
        if registry.startswith("shower_head_"):
            return ("SHEET_METAL",)
        if registry.startswith("shower_"):
            return ("glass_pane x4", "clay_ball x2", "SHEET_METAL")
        if registry.startswith("towel_rail_"):
            return ("iron_ingot", "wool")
        if registry.startswith("heated_towel_rail_"):
            return ("SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith("bathroom_radiator_"):
            return ("SHEET_METAL x2",)
        if registry.startswith(("wastebasket_", "soap_dispenser_", "paper_towel_dispenser_")):
            return ("SHEET_METAL",)
        if registry.startswith("toiletries_tray_"):
            return ("clay_ball", "glass")
        if registry.startswith("bath_mat_"):
            return ("wool",)
        if registry.startswith("grab_bar_"):
            return ("iron_ingot x2",)
        if registry.startswith("baby_changing_station_"):
            return ("SHEET_METAL", "FASTENER_KIT")
        if registry.startswith(("washing_machine_", "dryer_")):
            return ("SHEET_METAL x3", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("steam_iron_"):
            return ("SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith("ironing_board_"):
            return ("iron_ingot x2", "wool")
        if registry.startswith("laundry_basket_"):
            return ("planks x2",)
        if registry.startswith("kitchen_range_"):
            return ("SHEET_METAL x4", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith(("wall_oven_", "dishwasher_")):
            return ("SHEET_METAL x3", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("microwave_"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith(("air_fryer_", "coffee_machine_")):
            return ("SHEET_METAL", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("blender_"):
            return ("SHEET_METAL", "WIRING_HARNESS", "glass")
        if registry.startswith(("toaster_", "kettle_", "stand_mixer_")):
            return ("SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith(("cookie_jar_", "plate_stack_")):
            return ("clay_ball x3",)
        if registry.startswith(("dinner_plate_", "coffee_mug_")):
            return ("clay_ball",)
        if registry.startswith("drinking_glass_"):
            return ("glass",)
        if registry.startswith("cake_stand_"):
            return ("glass", "cake")
        if registry.startswith("chopping_board_"):
            return ("planks",)
        if registry.startswith(("kitchen_corner_cabinet_", "kitchen_island_")):
            return ("planks x5", "stone x2", "FASTENER_KIT")
        if registry.startswith("kitchen_cooktop_cabinet_"):
            return ("planks x4", "stone", "WIRING_HARNESS")
        if registry.startswith("kitchen_sink_cabinet_"):
            return ("planks x3", "stone", "SHEET_METAL")
        if registry.startswith("kitchen_wall_shelf_"):
            return ("planks x2",)
        if registry.startswith("kitchen_wall_cabinet_"):
            return ("planks x3", "FASTENER_KIT")
        if registry.startswith("kitchen_"):
            return ("planks x4", "stone", "FASTENER_KIT")
        if registry.startswith("range_hood_"):
            return ("SHEET_METAL x2", "DUCTING", "LED_MODULE")
        if registry.startswith("under_cabinet_light_"):
            return ("LED_MODULE", "SHEET_METAL")
        if registry.startswith("refrigerator_"):
            return ("SHEET_METAL x4", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("chest_freezer_"):
            return ("SHEET_METAL x3", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("bed_single_"):
            return ("planks x3", "wool x3")
        if registry.startswith(("bed_double_", "bed_king_", "bunk_bed_")):
            return ("planks x6", "wool x6")
        if registry.startswith("day_bed_"):
            return ("planks x4", "wool x3")
        if registry.startswith(("wardrobe_", "closet_")):
            return ("planks x8", "FASTENER_KIT")
        if registry.startswith("dresser_mirror_"):
            return ("planks x6", "glass_pane x2", "FASTENER_KIT")
        if registry.startswith("dresser_"):
            return ("planks x6", "FASTENER_KIT")
        if registry.startswith("vanity_stool_"):
            return ("planks", "wool")
        if registry.startswith(("vanity_", "standing_mirror_")):
            return ("planks x3", "glass_pane x3", "FASTENER_KIT")
        if registry.startswith("desk_chair_"):
            return ("iron_ingot", "wool x2")
        if registry.startswith(("desk_", "blanket_chest_")):
            return ("planks x4", "FASTENER_KIT")
        if registry.startswith(("crib_", "cradle_with_drawers_", "changing_table_")):
            return ("planks x4", "wool", "FASTENER_KIT")
        if registry.startswith("rocking_chair_"):
            return ("planks x3",)
        if registry.startswith("rug_"):
            return ("wool x2",)
        if registry.startswith("sofa_"):
            return ("planks x2", "wool x3")
        if registry.startswith("armchair_"):
            return ("planks x2", "wool x2")
        if registry.startswith("bookcase_"):
            return ("planks x3", "book x2")
        if registry.startswith("sideboard_"):
            return ("planks x6", "FASTENER_KIT")
        if registry.startswith("tv_stand_"):
            return ("planks x4", "FASTENER_KIT")
        if registry.startswith("dining_table_"):
            return ("planks x3", "FASTENER_KIT")
        if registry.startswith("cafe_table_"):
            return ("planks", "iron_ingot")
        return ("planks x2", "FASTENER_KIT")
    if tab == "tabcommercialoffice":
        # Mirrors OfficeFabricatorRules (Furniture & Novelties module).
        if registry.startswith("copier_"):
            return ("SHEET_METAL x3", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith(("desktop_computer_", "laptop_")):
            return ("CONTROL_BOARD", "LED_MODULE", "WIRING_HARNESS")
        if registry.startswith("retro_computer_"):
            return ("CONTROL_BOARD", "glass", "WIRING_HARNESS")
        if registry.startswith("computer_tower_"):
            return ("CONTROL_BOARD", "SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith(("desk_phone_", "fax_machine_")):
            return ("CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("desk_lamp_"):
            return ("LED_MODULE", "SHEET_METAL")
        if registry.startswith("ring_light_"):
            return ("LED_MODULE x2", "iron_ingot")
        if registry.startswith("studio_camera_"):
            return ("LENS_ASSEMBLY", "CONTROL_BOARD", "iron_ingot")
        if registry.startswith("pen_holder_"):
            return ("iron_ingot",)
        if registry.startswith("paper_tray_"):
            return ("SHEET_METAL", "paper")
        if registry.startswith(("task_chair_", "conference_chair_", "gaming_chair_")):
            return ("iron_ingot", "wool x2")
        if registry.startswith("guest_chair_"):
            return ("iron_ingot", "wool")
        if registry.startswith("waiting_bench_"):
            return ("iron_ingot x2", "wool")
        if registry.startswith("school_desk_"):
            return ("planks x2", "iron_ingot")
        if registry.startswith(("office_desk_pedestal_", "teacher_desk_")):
            return ("planks x4", "iron_ingot", "FASTENER_KIT")
        if registry.startswith("office_desk_"):
            return ("planks x3", "iron_ingot", "FASTENER_KIT")
        if registry.startswith("reception_desk_"):
            return ("planks x5", "FASTENER_KIT")
        if registry.startswith("filing_cabinet_"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if registry.startswith("office_shelving_"):
            return ("planks x3", "paper x2")
        if registry.startswith("conference_table_"):
            return ("planks x3", "iron_ingot")
        if registry.startswith("cubicle_panel_half_"):
            return ("wool", "iron_ingot")
        if registry.startswith("cubicle_panel_"):
            return ("wool x2", "iron_ingot")
        if registry.startswith("whiteboard_"):
            return ("SHEET_METAL", "FASTENER_KIT")
        if registry.startswith("chalkboard_"):
            return ("planks x2", "coal")
        if registry.startswith("cork_board_"):
            return ("planks x2", "paper")
        if registry.startswith("projector_screen_"):
            return ("SHEET_METAL", "wool")
        if registry.startswith("locker_"):
            return ("SHEET_METAL x3", "FASTENER_KIT")
        if registry.startswith("green_screen_"):
            return ("wool x2", "iron_ingot")
        return ("planks x2", "FASTENER_KIT")
    if tab == "tabmarketstore":
        # Mirrors MarketFabricatorRules (Furniture & Novelties module). The produce
        # crates keep the Furniture tab's cost, the Verifone the Technology tab's.
        if registry in ("applecrate", "bananacrate", "beetcrate", "carrotbarrel", "carrotcrate",
                        "corncrate", "goldenapples", "greenapplecrate", "largecrate",
                        "lettucecrate", "onioncrate", "orangecrate", "pearcrate", "potatoecrate",
                        "tomatoecrate"):
            return ("planks x2", "FASTENER_KIT")
        if registry == "vf915":
            return ("CONTROL_BOARD", "SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith(("reach_in_cooler_", "reach_in_freezer_")):
            return ("SHEET_METAL x3", "glass_pane x3", "CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("dairy_case_"):
            return ("SHEET_METAL x4", "CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("island_freezer_"):
            return ("SHEET_METAL x3", "glass_pane x2", "CONTROL_BOARD")
        if registry.startswith(("ice_cream_case_", "deli_case_")):
            return ("SHEET_METAL x2", "glass_pane x3", "CONTROL_BOARD")
        if registry.startswith(("butcher_case_", "seafood_case_")):
            return ("SHEET_METAL x2", "glass_pane x3", "CONTROL_BOARD")
        if registry.startswith("hot_food_case_"):
            return ("SHEET_METAL x2", "glass_pane x2", "WIRING_HARNESS", "LED_MODULE")
        if registry.startswith("floral_cooler_"):
            return ("SHEET_METAL x3", "glass_pane x3", "CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("bread_rack_"):
            return ("planks x3", "FASTENER_KIT")
        if registry.startswith("pastry_case_"):
            return ("planks x2", "glass_pane x2", "FASTENER_KIT")
        if registry.startswith("rotisserie_oven_"):
            return ("SHEET_METAL x2", "glass_pane", "WIRING_HARNESS")
        if registry.startswith("flower_stand_"):
            return ("iron_ingot x2", "red_flower x3")
        if registry.startswith(("coffee_bar_", "cup_counter_")):
            return ("planks x3", "FASTENER_KIT")
        if registry.startswith("coffee_brewer_"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "WIRING_HARNESS")
        if registry.startswith("fountain_machine_"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("bakery_case_"):
            return ("planks x2", "glass_pane x3", "LED_MODULE")
        if registry.startswith("gondola_shelf_"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if registry.startswith("produce_stand_"):
            return ("planks x3", "FASTENER_KIT")
        if registry.startswith("produce_scale_"):
            return ("SHEET_METAL", "iron_ingot")
        if registry.startswith("bulk_bins_"):
            return ("planks x2", "glass_pane x2")
        if registry.startswith("checkout_belt_"):
            return ("planks x3", "SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith("checkout_scanner_"):
            return ("planks x3", "SHEET_METAL", "OPTICAL_SENSOR")
        if registry.startswith("checkout_bagging_"):
            return ("planks x3", "SHEET_METAL")
        if registry.startswith("pos_terminal_"):
            return ("CONTROL_BOARD", "LED_MODULE", "SHEET_METAL")
        if registry.startswith("cash_register_"):
            return ("SHEET_METAL x2", "gold_nugget x2", "FASTENER_KIT")
        if registry.startswith("receipt_printer_"):
            return ("CONTROL_BOARD", "paper")
        if registry.startswith("card_terminal_stand_"):
            return ("CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("bag_carousel_"):
            return ("iron_ingot", "paper x2")
        if registry.startswith("self_checkout_"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "LED_MODULE", "OPTICAL_SENSOR")
        if registry.startswith("service_desk_"):
            return ("planks x5", "FASTENER_KIT")
        if registry.startswith("impulse_rack_"):
            return ("iron_ingot x2", "paper")
        if registry.startswith("shopping_cart_"):
            return ("iron_ingot x3",)
        if registry.startswith("cart_corral_"):
            return ("iron_ingot x3",)
        if registry.startswith("basket_stack_"):
            return ("paper x2", "dye", "iron_ingot")
        if registry.startswith("security_gate_"):
            return ("SHEET_METAL", "WIRING_HARNESS")
        if registry.startswith("aisle_sign_"):
            return ("SIGN_BLANK",)
        if registry.startswith("magazine_rack_"):
            return ("iron_ingot", "paper x3")
        if registry.startswith("bottle_return_machine_"):
            return ("SHEET_METAL x3", "CONTROL_BOARD", "OPTICAL_SENSOR")
        if registry.startswith(("pharmacy_dropoff_", "pharmacy_pickup_")):
            return ("planks x5", "FASTENER_KIT", "SIGN_BLANK")
        if registry.startswith("pharmacy_shelf_"):
            return ("SHEET_METAL x2", "FASTENER_KIT")
        if registry.startswith(("pharmacy_sign_", "consultation_sign_")):
            return ("SIGN_BLANK",)
        if registry.startswith("tobacco_case_"):
            return ("planks x2", "glass_pane x2", "FASTENER_KIT")
        if registry.startswith("lottery_dispenser_"):
            return ("glass_pane", "paper x2")
        if registry.startswith("lottery_terminal_"):
            return ("CONTROL_BOARD", "LED_MODULE", "paper")
        if registry.startswith("ice_merchandiser_"):
            return ("SHEET_METAL x3", "CONTROL_BOARD")
        if registry.startswith("propane_cage_"):
            return ("iron_ingot x3", "SHEET_METAL x2")
        if registry.startswith("firewood_rack_"):
            return ("iron_ingot x2", "log x2")
        if registry.startswith(("coin_kiosk_", "dvd_kiosk_")):
            return ("SHEET_METAL x3", "CONTROL_BOARD", "LED_MODULE")
        if registry.startswith("photo_kiosk_"):
            return ("SHEET_METAL x2", "CONTROL_BOARD", "LED_MODULE", "paper")
        if registry.startswith("end_cap_"):
            return ("SHEET_METAL x2", "FASTENER_KIT", "SIGN_BLANK")
        if registry.startswith("pallet_stack_"):
            return ("planks x2", "paper x2")
        if registry.startswith("bargain_bin_"):
            return ("iron_ingot x3", "SIGN_BLANK")
        if registry.startswith("dump_bin_"):
            return ("paper x4",)
        if registry.startswith("seasonal_table_"):
            return ("planks x2", "wool", "SIGN_BLANK")
        if registry.startswith("checkout_candy_"):
            return ("planks x3", "SHEET_METAL", "iron_ingot")
        if registry.startswith("department_sign_"):
            return ("SIGN_BLANK x2",)
        if registry.startswith(("sale_sign_", "deal_sign_")):
            return ("SIGN_BLANK", "iron_ingot")
        if registry.startswith(("clothing_rail_", "round_rack_")):
            return ("iron_ingot x2", "wool x2")
        if registry.startswith("apparel_table_"):
            return ("planks x3", "wool")
        if registry.startswith("mannequin_"):
            return ("SHEET_METAL", "wool")
        if registry.startswith("dress_form_"):
            return ("wool x2", "planks")
        if registry.startswith("fitting_room_"):
            return ("planks x4", "wool x2", "glass_pane")
        return ("planks x2", "FASTENER_KIT")
    # Any other tab is a module's own, priced by the rule the module registers
    # (e.g. ParksFabricatorRules), which this audit does not mirror; this is the generic cost
    # such a rule falls back to.
    return ("SHEET_METAL", "FASTENER_KIT")


def main():
    index, _tabs, classes = index_mod.build_index()
    ancestry = build_ancestry(classes)

    priced = {}
    for registry, info in index.items():
        ancestors = set(ancestry.get(info["class"], {info["class"]}))
        # AbstractBlockSetBasic generates its fence/slab/stairs variants as inner classes, so the
        # index maps those registry names to the outer set class. At runtime the block really is a
        # BlockSetVariant*, so add the base the game would actually see or the audit misprices them.
        if "AbstractBlockSetBasic" in ancestors:
            for suffix, base in (("_slab", "AbstractBlockSlab"),
                                 ("_stairs", "AbstractBlockStairs"),
                                 ("_fence", "AbstractBlockFence")):
                if registry.endswith(suffix):
                    ancestors.add(base)
        cost = cost_for(registry, info, ancestors)
        if cost is not None:
            priced[registry] = (cost, info)

    args = sys.argv[1:]

    if "--grep" in args:
        needle = args[args.index("--grep") + 1].lower()
        rows = sorted((r, c) for r, (c, _i) in priced.items() if needle in r.lower())
        print("{0} fabricable blocks matching '{1}':".format(len(rows), needle))
        for registry, cost in rows:
            print("  {0:52s} {1}".format(registry, " + ".join(cost)))
        return

    if "--list" in args:
        needle = args[args.index("--list") + 1]
        rows = sorted(r for r, (c, _i) in priced.items() if needle in " ".join(c))
        print("{0} blocks whose cost includes {1}:".format(len(rows), needle))
        for registry in rows:
            print("  {0}".format(registry))
        return

    counter = collections.Counter(cost for cost, _info in priced.values())
    print("Fabricable blocks: {0}".format(len(priced)))
    print()
    print("Cost recipes in use, by block count:")
    for cost, count in counter.most_common():
        print("  {0:5d}  {1}".format(count, " + ".join(cost)))
    print()
    print("Distinct cost recipes: {0}".format(len(counter)))

    # Blocks whose DISPLAY name says they are one thing while their cost says another.
    #
    # This checks the last word of the display name, the same signal the cost rules use, so it
    # only reports genuine disagreements. Checking registry names here would be worse than
    # useless: they are not descriptive and have been reused across different blocks, which is
    # exactly why the cost rules stopped consulting them.
    print()
    print("Possible mismatches (display-name noun vs cost):")
    hints = {
        "pole": "POLE_SECTION",
        "mast": "POLE_SECTION",
        "crossarm": "planks",
        "duct": "DUCTING",
        "camera": "OPTICAL_SENSOR",
    }
    found_any = False
    for noun, expected in sorted(hints.items()):
        bad = sorted(r for r, (c, _i) in priced.items()
                     if last_word(r) == noun and expected not in " ".join(c))
        if bad:
            found_any = True
            print("  '{0}' without {1}: {2} blocks (e.g. {3})".format(
                noun, expected, len(bad), ", ".join(bad[:4])))
    if not found_any:
        print("  none")


if __name__ == "__main__":
    main()
