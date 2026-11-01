# Healthcare

The Healthcare generators, sounds and demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

- `gen_healthcare_common.py` -- what the Healthcare generators share (not run on its own): the
  Life Safety catalogue pointed at `modules/healthcare` (writing, `--check`, `--fragments`, and
  `model_depth.py`'s separating and pruning over everything it writes), the hospital palette as
  shared swatch textures, the generic six-armed medical emblem (never a red cross), animated
  screen strips with their `.mcmeta`, Forge blockstates for the floor and wall facings, and
  `stamp_legend`, which sets a sign legend in the committed highway font
- `gen_healthcare_wards.py` -- patient rooms and wards (Healthcare): the hospital beds drawn whole
  and cut into a head and the shared foot (four blankets, ICU, bariatric, labour, and the
  transport and trauma stretchers on the same two cells), headwall,
  reading light, monitors, IV poles, overbed table, recliner, visitor chair, wardrobe, room TV,
  vitals and suction, the cubicle curtain track and panels (four cloths, mesh band, hem), the
  nurse call devices, dome light and master panel, corridor rails, dispensers, carts and bins, the
  oxygen outlet and zone sign, and the module's messages; `--check`, `--fragments`
- `gen_healthcare_clinical.py` -- the ICU, ED, nursing station and surgery support (Healthcare):
  ventilator, the ceiling-hung pendant, boom and trauma light on the shared drop column, crash
  cart, defibrillator, nurses' station counters and corner, status board, workstation on wheels,
  portable ultrasound cart (animated image), dispensing cabinet, medication cart and fridge, wheelchair, rack and corral sign, walker,
  mobile and ceiling lifts, triage desk, decon shower, AMBULANCE ONLY marking and sign, blanket
  warmer, autoclave, case cart, scrub dispenser, and the ED tracking board's case and screen (its
  layout numbers shared with `TileEntityEdTrackingBoardRenderer`); `--check`, `--fragments`
- `gen_healthcare_clinics.py` -- clinics, dialysis, infusion, endoscopy, dental and eye exam
  (Healthcare): exam table, exam light, wall diagnostic set, scale, phlebotomy chair, step stool,
  dialysis chair and machine, the RO water unit, infusion recliner, dental chair, refraction chair
  and acuity chart, endoscopy tower; `--check`, `--fragments`
- `gen_healthcare_diagnostics.py` -- imaging, lab, pharmacy and morgue (Healthcare): the CT and
  MRI drawn whole by their master (`BlockHealthcareUnit`, footprints written into the tab
  lines), the ceiling X-ray tube, X-ray table, bucky, C-arm and cart, mammography, lead apron
  rack, reading workstation, IN USE lights, MRI zone and radiation signs; joining lab benches,
  analysers, centrifuge, microscope, fume hood, biosafety cabinet, blood bank fridge, specimen
  pass-through, pneumatic tube station; the research lab (CO2 incubator, -80 freezer, nitrogen
  dewar, thermocycler, glassware rack) and the vivarium cage rack; compounding hood, bin
  shelving, unit-dose cabinet; the morgue cabinet, autopsy table and body cart; `--check`,
  `--fragments`
- `gen_healthcare_patient_areas.py` -- maternity, rehab, waiting room, teaching and playroom
  (Healthcare): infant warmer, bassinet, incubator, parallel bars, treatment plinth, exercise
  stairs, beam seating, check-in kiosk, call console and the now-serving display (layout numbers
  shared with `TileEntityNowServingDisplayRenderer`), the simulation mannequin drawn for each
  rest, torso model, skeleton, dissection table, tiered lecture seating with its riser, kids'
  table, chairs and toy box; `--check`, `--fragments`
- `gen_healthcare_systems.py` -- building systems (Healthcare): code blue button and annunciator,
  gas zone valve box and master alarm, power outlets, infant security panel; status lamps as
  squares, never discs; and the hospital wayfinding panel's name and tab line (its assets are
  `gen_wayfinding.py`'s); `--check`, `--fragments`
- `gen_healthcare_surgery.py` -- the operating room set (Healthcare), a CSM alternative to a
  third-party theatre pack: operating table (two cells, a seat to lie on), anaesthesia machine
  with an animated screen, twin-head surgical light on the shared drop column (lit by Lighting's
  states), scrub sink bays that join, instrument back table, Mayo stand, kick bucket, supply
  cabinet (two tall) and sterile drape trolley; shares the wards' and clinical generators'
  swatches; `--check`, `--fragments`
- `gen_healthcare_sounds.py` -- the Healthcare module's sounds, synthesised (numpy to ffmpeg to
  OGG): the code blue chime, the medical gas alarm and the infant security alarm, and their
  `sounds.json` entries. No `--check` (Vorbis output is not byte-stable): listen, then commit
- `build_healthcare_demo.py` -- builds the Healthcare demo world in a flat creative world named
  "Healthcare Demo" (refuses any other) through the orchestrator by instance id: one hospital
  floor along a corridor (doors, room signs, rails, stripes, wayfinding panels of four sizes), a
  ward with curtains and linked nurse call, ICU, ED with its tracking boards and ambulance
  entrance, imaging, lab, pharmacy, morgue, nurses' station, clinic, dialysis, maternity and
  NICU, a waiting room with linked now-serving displays, chapel, lecture hall, playroom, laundry,
  rehab and a rooftop helipad; it checks both Healthcare tabs' registration lists against what it
  placed and puts the rest on signed plinths. Sets blocks and runs commands only
