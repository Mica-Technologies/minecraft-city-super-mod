#!/usr/bin/env python3
"""Performance benchmark harness for CSM, driven through MCMCP.

Everything in the performance plan so far has been argued from mechanism -- "this allocates per
frame", "this walks every chunk twice" -- with no measurement behind it. This harness exists to
replace that with numbers, and to make a before/after comparison across two commits something you
run rather than something you reason about.

It does three things:

    build    place a deterministic, signal-dense scene in a flat world
    measure  sample client FPS and server tick time from fixed camera poses, write JSON
    compare  diff two result files

The scene is generated from a fixed layout with no randomness, so the same command produces the
same world every time and two runs differ only by the code under test.

Usage
-----
    # once, with a dev client running and a flat creative world loaded
    python csm_bench.py build

    # then, per commit under test
    python csm_bench.py measure --label baseline-abc1234
    python csm_bench.py compare baseline-abc1234 after-def5678

Requires a running dev client with MCMCP's client endpoint up, and the world already loaded.
Reads the auth token from run/config/mcmcp.cfg.

Measurement notes that matter for trusting the output
-----------------------------------------------------
* FPS is meaningless while the frame rate is capped. The harness reads back maxFps/vsync and
  refuses to record a run that looks capped, rather than quietly reporting the cap.
* The first samples after a teleport are dominated by chunk loading and tile entity init, so each
  pose warms up before it is sampled.
* FPS is noisy. Each pose is sampled repeatedly and reported as a median, with the spread kept so
  a suspiciously wide distribution is visible rather than averaged away.
* **Establish the noise floor before believing any delta.** Measure the same build twice and
  compare those two runs. Whatever difference that shows is the floor; a real change has to beat
  it. Measured on this machine at 854x480 / 12 chunks:

      FPS               +-1.3%   -- deltas above a few percent are trustworthy
      meanTickMillis    +-64%    -- run-to-run spread swamps any plausible change

  That second figure is why `compare` refuses to draw conclusions from tick time by default. It
  is a rolling average that never fully settles between runs; treat it as indicative only, and
  only when a change is large, repeated, and consistent in direction across every pose.

* **Comparing two builds means restarting the client, and that restart is itself a noise source
  bigger than most changes worth measuring.** Two runs of one build inside a single session agree
  to within about 2-3%. Two runs of the same build in *different* sessions have been seen to
  differ by 5-7% -- enough to invent an improvement or hide one. A single cross-restart A/B
  therefore cannot resolve anything smaller than roughly ten percent, and reading one as if it
  could is how this harness first "measured" a change that mechanically could not have been
  slower than its baseline.

  For a change below that threshold, do not A/B across restarts. Put the old and new paths behind
  a runtime toggle so both can be measured inside one session, and compare there. Failing that,
  alternate several runs per build and compare medians of medians -- never one run each.

* Attribution (`csm_bench_attribute.py`) does not have this problem: it removes content and
  re-measures inside a single session, so its numbers are directly comparable.
"""

import argparse
import json
import os
import re
import statistics
import subprocess
import sys
import time
import urllib.error
import urllib.request

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CONFIG_PATH = os.path.join(REPO_ROOT, "run", "config", "mcmcp.cfg")
RESULTS_DIR = os.path.join(REPO_ROOT, "assets", "docs", "benchmarks")

CLIENT_URL = "http://127.0.0.1:25585/mcp"
SERVER_URL = "http://127.0.0.1:25586/mcp"

# --- Scene layout -------------------------------------------------------------------------------
# A grid of intersections. Deterministic: no randomness anywhere, so the same build command
# produces the same world and two measurement runs differ only by the code under test.

# Scaled for the server side, not the client. At 25 intersections the whole server tick ran at
# 0.3 ms and meanTickMillis could not resolve any change from noise -- percentage deltas on a
# number that small are meaningless. 100 powered controllers puts real work in the tick.
GRID = 10                # GRID x GRID intersections
SPACING = 16             # blocks between intersection centres
ORIGIN = (0, 5, 0)       # south-west corner of the grid
ROAD_Y = 4               # road surface
MAST_Y = 9               # signal head height

# Signal heads are the densest custom-rendered block in a real build, so they dominate the scene.
SIGNAL = "csm:controllablehawksignal"
CROSSWALK = "csm:controllablecrosswalksingle"
CONTROLLER = "csm:signalcontroller"
GUIDE_SIGN = "csm:dynamic_guide_sign"
STREET_SIGN = "csm:dynamic_street_sign"
# 1.12 has one concrete block with a colour metadata; the per-colour ids are 1.13+.
ROAD_BLOCK = "minecraft:concrete"
ROAD_META = 7            # gray

FACINGS = [0, 1, 2, 3]

# Camera poses sampled at each measurement. Chosen to cover the cases the plan's findings are
# about: dense signals close up, a long view down a corridor of them, and the dynamic signs.
POSES = [
    # Yaw is degrees clockwise from south: 0 = +Z, 90 = -X, 180 = -Z, 270 = +X. The grid grows in
    # +X and +Z from the origin, so the useful viewing directions are between 270 and 360.
    {"name": "intersection_close", "x": -6.5, "y": 11.0, "z": -6.5, "yaw": 315.0, "pitch": 5.0},
    {"name": "corridor_long",      "x": -14.5, "y": 11.0, "z": 0.5, "yaw": 270.0, "pitch": 2.0},
    {"name": "grid_overview",      "x": -24.0, "y": 42.0, "z": -24.0, "yaw": 315.0, "pitch": 23.0},
    {"name": "dynamic_signs",      "x": 0.5, "y": 11.0, "z": -18.0, "yaw": 0.0, "pitch": 0.0},
]

# Chunk loading and tile entity init dominate the first seconds after a teleport, and
# meanTickMillis is a rolling average, so a short warm-up leaves loading spikes inside the
# window being measured. This was originally 60 and the tick numbers were unusable because of it.
WARMUP_TICKS = 220
SAMPLES_PER_POSE = 12
SAMPLE_GAP_SECONDS = 0.4


class McpError(RuntimeError):
    pass


class Mcp:
    """Minimal MCP-over-HTTP client: initialize, then tools/call."""

    def __init__(self, url, token):
        self.url = url
        self.token = token
        self.session = None
        self._next_id = 1

    def _post(self, payload, want_headers=False):
        body = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(self.url, data=body, method="POST")
        req.add_header("Authorization", "Bearer " + self.token)
        req.add_header("Content-Type", "application/json")
        req.add_header("Accept", "application/json, text/event-stream")
        if self.session:
            req.add_header("Mcp-Session-Id", self.session)
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                raw = resp.read().decode("utf-8", "replace")
                headers = dict(resp.headers)
        except urllib.error.URLError as exc:
            raise McpError("cannot reach %s: %s" % (self.url, exc))
        return (raw, headers) if want_headers else raw

    def connect(self):
        payload = {
            "jsonrpc": "2.0", "id": 0, "method": "initialize",
            "params": {
                "protocolVersion": "2025-06-18", "capabilities": {},
                "clientInfo": {"name": "csm_bench", "version": "1"},
            },
        }
        _, headers = self._post(payload, want_headers=True)
        for key, value in headers.items():
            if key.lower() == "mcp-session-id":
                self.session = value.strip()
        if not self.session:
            raise McpError("no Mcp-Session-Id returned by %s" % self.url)
        self._post({"jsonrpc": "2.0", "method": "notifications/initialized"})
        return self

    def call(self, name, arguments=None):
        self._next_id += 1
        payload = {
            "jsonrpc": "2.0", "id": self._next_id, "method": "tools/call",
            "params": {"name": name, "arguments": arguments or {}},
        }
        raw = self._post(payload)
        # The endpoint may answer as SSE; take the last data: line if so. Reading only the first
        # chunk is how an earlier attempt at this convinced itself half the tools were missing.
        if "data:" in raw and not raw.lstrip().startswith("{"):
            chunks = [ln[5:].strip() for ln in raw.splitlines() if ln.startswith("data:")]
            raw = chunks[-1] if chunks else raw
        try:
            doc = json.loads(raw)
        except ValueError:
            raise McpError("unparseable reply to %s: %s" % (name, raw[:200]))
        if "error" in doc:
            raise McpError("%s failed: %s" % (name, doc["error"].get("message")))
        result = doc.get("result", {})
        if result.get("isError"):
            texts = [c.get("text", "") for c in result.get("content", [])]
            raise McpError("%s returned an error: %s" % (name, " ".join(texts)[:300]))
        structured = result.get("structuredContent")
        if structured is not None:
            return structured
        for content in result.get("content", []):
            if content.get("type") == "text":
                try:
                    return json.loads(content["text"])
                except ValueError:
                    return {"text": content["text"]}
        return {}


def read_token():
    if not os.path.exists(CONFIG_PATH):
        raise SystemExit("no MCMCP config at %s -- is the dev client set up?" % CONFIG_PATH)
    with open(CONFIG_PATH, "r", encoding="utf-8", errors="replace") as handle:
        match = re.search(r"S:authToken=(\S+)", handle.read())
    if not match:
        raise SystemExit("no authToken in %s -- launch the client once to generate one" % CONFIG_PATH)
    return match.group(1)


def git_commit():
    try:
        out = subprocess.check_output(
            ["git", "-C", REPO_ROOT, "rev-parse", "--short", "HEAD"],
            stderr=subprocess.DEVNULL)
        commit = out.decode().strip()
        dirty = subprocess.call(
            ["git", "-C", REPO_ROOT, "diff", "--quiet"], stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL) != 0
        return commit + ("-dirty" if dirty else "")
    except Exception:
        return "unknown"


# --- build --------------------------------------------------------------------------------------

def intersection_blocks(cx, cz):
    """One intersection's worth of placements, as server_set_blocks 'list' entries.

    Sized to be representative rather than minimal: a real signalised intersection carries a
    stack of heads per approach, not one. Since custom-rendered signal heads are the block the
    performance work is mostly about, under-populating here would flatter every result.
    """
    out = []
    for i, (dx, dz) in enumerate([(-2, -2), (2, -2), (2, 2), (-2, 2)]):
        # Three heads per approach, stacked on the mast.
        for level in range(3):
            out.append({"x": cx + dx, "y": MAST_Y - level, "z": cz + dz,
                        "block": SIGNAL, "metadata": FACINGS[i]})
        # Crosswalk signal lower on the same mast.
        out.append({"x": cx + dx, "y": MAST_Y - 4, "z": cz + dz,
                    "block": CROSSWALK, "metadata": FACINGS[i]})
    # One controller cabinet per intersection -- this is the ticking half of the benchmark.
    # It must be POWERED: pauseTicking() short-circuits an unpowered controller, so a scene full
    # of dark cabinets measures nothing at all on the server side. A redstone block beside each
    # one keeps them all running.
    out.append({"x": cx + 4, "y": ROAD_Y + 1, "z": cz + 4, "block": CONTROLLER, "metadata": 0})
    out.append({"x": cx + 5, "y": ROAD_Y + 1, "z": cz + 4,
                "block": "minecraft:redstone_block", "metadata": 0})
    # Street name blade and a guide sign: the two renderers with no display list caching.
    out.append({"x": cx - 4, "y": MAST_Y, "z": cz - 4, "block": STREET_SIGN, "metadata": 0})
    out.append({"x": cx + 6, "y": MAST_Y + 1, "z": cz - 6, "block": GUIDE_SIGN, "metadata": 0})
    return out


def do_build(client, server):
    ox, _, oz = ORIGIN
    span = (GRID - 1) * SPACING
    # One Y-layer at a time: MCMCP caps a single fill at limits.maxBlockVolume (32768 by
    # default) and the whole build volume is several times that. A layer here is ~9.4k blocks.
    print("==> clearing the build volume")
    for y in range(ROAD_Y + 1, MAST_Y + 7):
        server.call("server_set_blocks", {
            "mode": "fill", "block": "minecraft:air",
            "x": ox - 12, "y": y, "z": oz - 12,
            "toX": ox + span + 12, "toY": y, "toZ": oz + span + 12,
        })
    server.call("server_set_blocks", {
        "mode": "fill", "block": ROAD_BLOCK, "metadata": ROAD_META,
        "x": ox - 12, "y": ROAD_Y, "z": oz - 12,
        "toX": ox + span + 12, "toY": ROAD_Y, "toZ": oz + span + 12,
    })

    total = 0
    for gx in range(GRID):
        for gz in range(GRID):
            cx = ox + gx * SPACING
            cz = oz + gz * SPACING
            blocks = intersection_blocks(cx, cz)
            server.call("server_set_blocks", {"mode": "list", "blocks": blocks})
            total += len(blocks)
            print("    intersection (%d,%d) at %d,%d -- %d blocks" % (gx, gz, cx, cz, len(blocks)))
    print("==> placed %d blocks across %d intersections" % (total, GRID * GRID))
    print("    now run: python csm_bench.py measure --label <name>")


# --- measure ------------------------------------------------------------------------------------

def read_video_settings():
    """maxFps / vsync as the game actually has them, from options.txt."""
    path = os.path.join(REPO_ROOT, "run", "options.txt")
    settings = {"maxFps": None, "enableVsync": None}
    if not os.path.exists(path):
        return settings
    with open(path, "r", encoding="utf-8", errors="replace") as handle:
        for line in handle:
            if line.startswith("maxFps:"):
                try:
                    settings["maxFps"] = int(line.split(":", 1)[1].strip())
                except ValueError:
                    pass
            elif line.startswith("enableVsync:"):
                settings["enableVsync"] = line.split(":", 1)[1].strip() == "true"
    return settings


def check_not_capped(runtime):
    """Refuse to record a run whose frame rate is limited by vsync or maxFps.

    Read the settings rather than inferring from the number. An earlier version of this guessed --
    "an FPS near 60/120/144 must be a cap" -- and promptly rejected a genuine 121 fps reading in a
    dense scene because 121 is next to 120. The measurement was real; the guard was wrong.
    """
    fps = runtime.get("fps")
    if fps is None:
        raise SystemExit("client_runtime_info returned no fps field")

    video = read_video_settings()
    if video["enableVsync"]:
        raise SystemExit(
            "VSync is on (run/options.txt enableVsync:true), so FPS measures the display "
            "refresh rate rather than the game. Turn it off and re-run.")

    limit = video["maxFps"]
    # 260 is Minecraft's "Unlimited" sentinel.
    if limit is not None and limit < 260 and fps >= limit - 2:
        raise SystemExit(
            "FPS (%d) is at the configured maxFps of %d, so this measures the cap rather than "
            "the game. Set 'Max Framerate: Unlimited' in video settings and re-run."
            % (fps, limit))
    return video


def sample_pose(client, server, pose):
    client.call("client_send_chat", {"message": "/tp %.2f %.2f %.2f" % (pose["x"], pose["y"], pose["z"])})
    client.call("client_look", {"yaw": pose["yaw"], "pitch": pose["pitch"]})
    client.call("client_wait", {"ticks": WARMUP_TICKS})

    fps_samples = []
    tick_samples = []
    tps_samples = []
    for _ in range(SAMPLES_PER_POSE):
        runtime = client.call("client_runtime_info", {})
        fps_samples.append(runtime.get("fps", 0))
        if server is not None:
            info = server.call("server_world_info", {})
            tick_samples.append(info.get("meanTickMillis", 0.0))
            tps_samples.append(info.get("ticksPerSecond", 0.0))
        time.sleep(SAMPLE_GAP_SECONDS)

    entry = {
        "pose": pose["name"],
        "fps_median": statistics.median(fps_samples),
        "fps_min": min(fps_samples),
        "fps_max": max(fps_samples),
        "fps_samples": fps_samples,
    }
    if tick_samples:
        entry["mean_tick_millis_median"] = statistics.median(tick_samples)
        entry["tps_median"] = statistics.median(tps_samples)
    return entry


def pin_world_conditions(client):
    """Freeze everything that changes what a frame costs but is not the code under test.

    Time of day is the big one: bulbs emit light, the lit-visor overlay scales with daylight, and
    the sky itself is drawn differently. A run at dusk is not comparable with a run at noon, and
    nothing in the numbers would tell you which you got.
    """
    for command in (
        "/gamerule doDaylightCycle false",
        "/time set 6000",              # midday, fixed
        "/weather clear 1000000",
        "/gamerule doWeatherCycle false",
        "/gamerule doMobSpawning false",   # wandering mobs are uncontrolled render + tick load
        "/kill @e[type=!player]",
    ):
        client.call("client_send_chat", {"message": command})
    client.call("client_wait", {"ticks": 40})


def do_measure(client, server, label):
    pin_world_conditions(client)
    runtime = client.call("client_runtime_info", {})
    video = check_not_capped(runtime)

    print("==> client: %dx%d, render distance %s chunks, fancy=%s" % (
        runtime.get("displayWidth", 0), runtime.get("displayHeight", 0),
        runtime.get("renderDistanceChunks"), runtime.get("fancyGraphics")))

    results = []
    for pose in POSES:
        print("==> sampling %s" % pose["name"])
        entry = sample_pose(client, server, pose)
        print("    fps median %.0f (min %.0f, max %.0f)%s" % (
            entry["fps_median"], entry["fps_min"], entry["fps_max"],
            "  tick %.2f ms" % entry["mean_tick_millis_median"]
            if "mean_tick_millis_median" in entry else ""))
        results.append(entry)

    after = client.call("client_runtime_info", {})
    doc = {
        "label": label,
        "commit": git_commit(),
        "recorded_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "scene": {"grid": GRID, "spacing": SPACING, "intersections": GRID * GRID},
        "client": {
            "displayWidth": runtime.get("displayWidth"),
            "displayHeight": runtime.get("displayHeight"),
            "renderDistanceChunks": runtime.get("renderDistanceChunks"),
            "fancyGraphics": runtime.get("fancyGraphics"),
            "maxFps": video.get("maxFps"),
            "enableVsync": video.get("enableVsync"),
            "usedMegabytesStart": runtime.get("usedMegabytes"),
            "usedMegabytesEnd": after.get("usedMegabytes"),
        },
        "poses": results,
    }

    os.makedirs(RESULTS_DIR, exist_ok=True)
    path = os.path.join(RESULTS_DIR, "%s.json" % label)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(doc, handle, indent=2)
        handle.write("\n")
    print("==> wrote %s" % path)
    return doc


# --- compare ------------------------------------------------------------------------------------

def load_result(label):
    path = os.path.join(RESULTS_DIR, "%s.json" % label)
    if not os.path.exists(path):
        raise SystemExit("no result file at %s" % path)
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def do_compare(before_label, after_label):
    before = load_result(before_label)
    after = load_result(after_label)

    if before["client"].get("renderDistanceChunks") != after["client"].get("renderDistanceChunks") \
            or before["client"].get("displayWidth") != after["client"].get("displayWidth"):
        print("!!  WARNING: client settings differ between runs; the comparison is not valid.")
        print("    before: %s" % before["client"])
        print("    after:  %s" % after["client"])

    print("%-22s %10s %10s %10s" % ("pose (fps)", "before", "after", "delta"))
    before_by_pose = {p["pose"]: p for p in before["poses"]}
    for entry in after["poses"]:
        prior = before_by_pose.get(entry["pose"])
        if not prior:
            continue
        b = prior["fps_median"]
        a = entry["fps_median"]
        pct = ((a - b) / b * 100.0) if b else 0.0
        print("%-22s %10.0f %10.0f %+9.1f%%" % (entry["pose"], b, a, pct))

    if any("mean_tick_millis_median" in p for p in after["poses"]):
        print()
        print("%-22s %10s %10s %10s" % ("pose (server ms)", "before", "after", "delta"))
        for entry in after["poses"]:
            prior = before_by_pose.get(entry["pose"])
            if not prior or "mean_tick_millis_median" not in entry:
                continue
            b = prior.get("mean_tick_millis_median", 0.0)
            a = entry["mean_tick_millis_median"]
            pct = ((a - b) / b * 100.0) if b else 0.0
            print("%-22s %10.2f %10.2f %+9.1f%%" % (entry["pose"], b, a, pct))
        print()
        print("    NOTE: meanTickMillis measured +-64%% run-to-run on identical code here, so")
        print("    these numbers are indicative only. Do not report a tick-time change unless it")
        print("    is large, reproduced across runs, and consistent in direction at every pose.")


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("build", help="place the benchmark scene")
    measure = sub.add_parser("measure", help="sample FPS and tick time, write a result file")
    measure.add_argument("--label", required=True, help="name for this run, e.g. baseline-abc1234")
    compare = sub.add_parser("compare", help="diff two result files")
    compare.add_argument("before")
    compare.add_argument("after")
    args = parser.parse_args()

    if args.command == "compare":
        do_compare(args.before, args.after)
        return

    token = read_token()
    client = Mcp(CLIENT_URL, token).connect()
    try:
        server = Mcp(SERVER_URL, token).connect()
    except McpError:
        server = None
        print("!!  no server endpoint; recording client FPS only")

    if args.command == "build":
        if server is None:
            raise SystemExit("build needs the server endpoint -- is a world loaded?")
        do_build(client, server)
    elif args.command == "measure":
        do_measure(client, server, args.label)


if __name__ == "__main__":
    main()
