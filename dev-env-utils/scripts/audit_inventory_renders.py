#!/usr/bin/env python3
"""
Measures, in a running dev client, how every OBJ-backed CSM item actually sits in its inventory
slot -- and optionally corrects the ones that hang outside it.

An item's inventory render comes from the ``transform`` block of its blockstate's ``inventory``
variant. Nothing about that block is checkable by reading it: whether a model lands inside its
16x16 slot depends on the model's own bounds, where its geometry sits relative to the pivot, the
rotation, and the scale, all interacting. The only honest way to know is to look at the rendered
pixels, which is what this does.

It found fifteen items outside their slot that nobody had noticed, and -- more usefully -- it
proved a purely offline estimate of the same thing wrong on most of the tree. Do not trust a
prediction here; measure.

HOW A MEASUREMENT IS TAKEN
    The probe item is placed alone in a hotbar slot and the frame is differenced against a
    reference frame of that slot empty, so the pixels that changed ARE the item's render. The slot
    rectangle is not assumed: it is calibrated from ``minecraft:stone``, whose GUI render is
    exactly 14.14 x 15.73 gui px centred in its slot, so its diff box gives the slot centre
    directly. Every number reported is in gui pixels, positive OUTSIDE the slot edge and negative
    for margin inside it.

    Four things had to be true before the numbers meant anything, and each one produced confident
    nonsense first:

      * The player is parked in the void below the world. The hotbar is translucent, so an
        animated world behind it -- a cycling signal, a drifting cloud -- shows up as a difference
        and swamps the item. The void renders as flat darkness and never changes, which is the one
        backdrop that costs nothing: no blocks are placed and no world is edited.
      * Items are swapped through the SERVER endpoint. Sending chat from the client closes any
        open screen, and command feedback draws a chat line straight over the hotbar.
      * The selected slot is parked away from the probe slot. Changing the SELECTED stack pops the
        item-name label up over the hotbar and re-renders the first-person hand.
      * Frames are settled, not waited for. A server-side item swap reaches the client's render
        after a delay that varies with load; a fixed wait either wastes time or, worse, measures
        the PREVIOUS item.

TRANSLATION UNITS
    A Forge blockstate transform takes ``translation`` in BLOCK units. A vanilla ``models/item``
    display block spells the same thing in 1/16 of a block, and mixing the two up moves a model
    sixteen times too far -- clean off the screen, which reads in here as "no render". The slot is
    16 gui px across and one block spans it, so one gui pixel is 1/16 of a translation unit, which
    is the conversion ``--fix`` applies.

WRITING
    ``--fix`` edits the translation and scale where they sit in the file rather than re-serialising
    it. Most of these blockstates -- 81 of the 283 at the time of writing, nearly all of the
    furnishings module -- are written in a compact style that a ``json.dump`` round trip would
    reformat wholesale, burying a one-line change in fifty lines of whitespace. An inserted array
    is written inline or expanded to match the arrays already in that object. Every edit is checked
    by parsing the result and comparing it against the intended structure, so a text edit cannot
    quietly change anything but the values it meant to.

ITERATING
    A resource reload does NOT rebake these item models, so a correction needs a rebuild and a
    client restart before it can be re-measured. Stopping the Gradle task does not stop the game:
    it forks its own JVM, which keeps the MCMCP port, and the next run measures the STALE client
    while reporting nothing changed. Kill the game process itself between rounds.

Usage:
    python audit_inventory_renders.py                  # measure everything, report what is out
    python audit_inventory_renders.py --check          # exit 1 if anything is outside its slot
    python audit_inventory_renders.py --only hottub,winerack
    python audit_inventory_renders.py --fix            # write corrections, then rebuild + restart
    python audit_inventory_renders.py --json out.json  # full measurements for every item

Needs a dev client running with MCMCP's client AND server endpoints enabled (see
run/config/mcmcp.cfg); the world does not matter, and the player is left where it found them.
"""

import argparse
import glob
import json
import os
import re
import sys
import time
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout  # noqa: E402

try:
    from PIL import Image, ImageChops
except ImportError:                                            # pragma: no cover
    sys.exit("This tool needs Pillow: pip install Pillow")

REPO_ROOT = layout.REPO_ROOT
MCMCP_CFG = os.path.join(REPO_ROOT, "run", "config", "mcmcp.cfg")

PROBE_SLOT = 4          # hotbar slot the probe item goes in; neighbours stay empty
PARK_SLOT = 0           # selected slot, kept away from the probe
PAD = 20                # gui px outside the slot to look for stray pixels
DIFF = 12               # per-channel difference that counts as "the item drew here"
INSIDE = 0.5            # gui px of overflow tolerated before an item is called out
TARGET = 14.6           # what --fix scales an oversized render down to, leaving a visible margin

# A vanilla cube's GUI render, used to calibrate the slot rectangle.
CALIBRATION_ITEM = "minecraft:stone"
CALIBRATION_SIZE = (14.14, 15.73)

# Where the player is parked while measuring: well below the world, where the void renders as
# unchanging darkness behind the translucent hotbar. Creative flight holds them there, nothing is
# built, and they are put back where they were on the way out.
PARK_POS = (3000, -25, 3000)

# Vanilla's block item display, which is what "forge:default-block" stands for, in BLOCK units.
# Spelling the whole set out matters when replacing that string: in the map form any transform
# type left unlisted falls back to identity rather than to the block default, so overriding only
# "gui" would wreck the held and dropped renders of a block that had been using the default.
FORGE_DEFAULT_BLOCK = {
    "gui": {"rotation": [30, 225, 0], "scale": 0.625},
    "ground": {"translation": [0, 0.1875, 0], "scale": 0.25},
    "fixed": {"scale": 0.5},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0.15625, 0],
                              "scale": 0.375},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 0.15625, 0],
                             "scale": 0.375},
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": 0.4},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": 0.4},
}


# ---- MCMCP ---------------------------------------------------------------------------------

class Endpoint:
    """One MCMCP JSON-RPC endpoint (the game client, or the server behind it)."""

    def __init__(self, port, token):
        self.url = "http://127.0.0.1:%d/mcp" % port
        self.token = token
        self.session = None
        self.seq = 0

    def _post(self, payload):
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json, text/event-stream",
                   "Authorization": "Bearer " + self.token}
        if self.session:
            headers["Mcp-Session-Id"] = self.session
        req = urllib.request.Request(self.url, data=json.dumps(payload).encode(), headers=headers)
        with urllib.request.urlopen(req, timeout=60) as r:
            sid = r.headers.get("Mcp-Session-Id")
            if sid:
                self.session = sid
            body = r.read().decode()
        if body.lstrip().startswith("{"):
            return json.loads(body)
        out = None                                   # the endpoint may answer as SSE
        for line in body.splitlines():
            if line.startswith("data:"):
                out = json.loads(line[5:].strip())
        return out

    def connect(self):
        self.seq += 1
        self._post({"jsonrpc": "2.0", "id": self.seq, "method": "initialize",
                    "params": {"protocolVersion": "2024-11-05", "capabilities": {},
                               "clientInfo": {"name": "csm-inventory-audit", "version": "1"}}})
        self._post({"jsonrpc": "2.0", "method": "notifications/initialized"})
        return self

    def call(self, tool, **args):
        self.seq += 1
        r = self._post({"jsonrpc": "2.0", "id": self.seq, "method": "tools/call",
                        "params": {"name": tool, "arguments": args}})
        if r is None:
            raise RuntimeError("no response from %s for %s" % (self.url, tool))
        if "error" in r:
            raise RuntimeError("%s: %s" % (tool, r["error"]))
        for c in r.get("result", {}).get("content", []):
            if c.get("type") == "text":
                try:
                    return json.loads(c["text"])
                except ValueError:
                    return c["text"]
        return r.get("result")


def read_mcmcp_config():
    """Token and ports out of the dev instance's own config, so nothing is hard-coded here."""
    if not os.path.exists(MCMCP_CFG):
        sys.exit("No %s -- start the dev client once so MCMCP writes its config." % MCMCP_CFG)
    text = open(MCMCP_CFG, encoding="utf-8").read()

    def field(pattern, default=None, cast=str):
        m = re.search(pattern, text)
        if not m:
            if default is None:
                sys.exit("Could not read %s from %s" % (pattern, MCMCP_CFG))
            return default
        return cast(m.group(1).strip())

    token = field(r"S:authToken=(.*)")
    if not token:
        sys.exit("MCMCP authToken is empty in %s" % MCMCP_CFG)
    return (token,
            field(r"I:clientPort=(\d+)", 25585, int),
            field(r"I:serverPort=(\d+)", 25586, int))


# ---- what to measure -----------------------------------------------------------------------

def lang_names():
    names = {}
    for d in layout.asset_dirs("lang"):
        p = os.path.join(d, "en_us.lang")
        if not os.path.exists(p):
            continue
        for line in open(p, encoding="utf-8"):
            line = line.strip()
            if line.startswith("tile.") and ".name=" in line:
                key, val = line.split("=", 1)
                names[key[len("tile."):-len(".name")]] = val
    return names


def candidates():
    """Every blockstate whose inventory variant renders an OBJ, across all source trees."""
    names = lang_names()
    out = []
    for d in layout.asset_dirs("blockstates"):
        for path in sorted(glob.glob(os.path.join(d, "*.json"))):
            reg = os.path.basename(path)[:-5]
            try:
                bs = json.load(open(path, encoding="utf-8"))
            except ValueError:
                continue
            inv = bs.get("variants", {}).get("inventory")
            if not isinstance(inv, list) or not inv or not isinstance(inv[0], dict):
                continue
            model = inv[0].get("model") or bs.get("defaults", {}).get("model")
            if not model or not model.endswith(".obj"):
                continue
            out.append({"reg": reg, "name": names.get(reg, reg), "model": model,
                        "blockstate": path})
    return out


# ---- measuring -----------------------------------------------------------------------------

class Probe:
    """Drives the running client and turns one item at a time into a slot measurement."""

    def __init__(self, client, server, shots_dir):
        self.client = client
        self.server = server
        self.shots = shots_dir
        self.scale = 1
        self.slot = (0, 0)         # slot interior origin, in screen pixels
        self.window = (0, 0, 0, 0)
        self.reference = None
        self._restore = None

    # -- plumbing

    def shot(self, name):
        path = os.path.join(self.shots, name + ".png")
        try:
            os.remove(path)
        except OSError:
            pass
        self.client.call("client_screenshot", name=name)
        for _ in range(80):
            if os.path.exists(path) and os.path.getsize(path):
                try:
                    im = Image.open(path)
                    im.load()
                    return im.convert("RGB")
                except (OSError, ValueError):
                    pass
            time.sleep(0.05)
        raise RuntimeError("client never wrote screenshot %s" % name)

    def settle(self, tag="_inv_settle"):
        """Frames until two in a row agree: the only reliable signal that a swap has landed."""
        prev = self.shot(tag + "a")
        for _ in range(25):
            cur = self.shot(tag + "b")
            if self.diff(prev, cur) is None:
                return cur
            prev = cur
        return prev

    def diff(self, a, b):
        w = self.window
        d = ImageChops.difference(a.crop(w), b.crop(w))
        mask = d.convert("L").point(lambda v: 255 if v >= DIFF else 0)
        box = mask.getbbox()
        return None if box is None else (w[0] + box[0], w[1] + box[1],
                                         w[0] + box[2], w[1] + box[3])

    def park(self):
        """Put the player back on the exact spot the reference frame was taken from.

        Nothing holds a creative player up in the void, so they fall -- slowly changing what is
        behind the translucent hotbar until the reference frame no longer describes the backdrop
        and every later measurement is of the backdrop rather than the item. Re-parking before
        each probe costs one command and removes the drift entirely.
        """
        self.server.call("server_run_command", command="tp @p %d %d %d 0 0" % PARK_POS)

    def backdrop_drifted(self):
        """True if the empty slot no longer looks like it did when the reference was taken."""
        self.give("minecraft:air")
        return self.diff(self.reference, self.settle("_inv_drift")) is not None

    def give(self, item):
        """Put an item in the probe slot and wait until the CLIENT agrees it is there.

        Settling on a stable frame is not enough on its own: until the swap arrives, the previous
        item's render is perfectly stable too, and settle() would happily return it. Confirming
        the stack against the client's own inventory first is what stops a measurement being
        attributed to the item before it.
        """
        expect = item.split()[0]
        self.server.call("server_run_command",
                         command="replaceitem entity @p slot.hotbar.%d %s" % (PROBE_SLOT, item))
        for _ in range(100):
            slots = self.client.call("client_inventory")["inventory"]["items"]
            here = next((s["item"] for s in slots if s["slot"] == PROBE_SLOT), "minecraft:air")
            if here == expect:
                return
            time.sleep(0.05)
        raise RuntimeError("client never saw %s in the probe slot" % expect)

    # -- setup / teardown

    def begin(self):
        info = self.client.call("client_gui_state")
        if not info.get("worldLoaded"):
            sys.exit("Load a world in the dev client first.")
        w, h = info["displayWidth"], info["displayHeight"]
        for f in (4, 3, 2, 1):
            if w // f >= 320 and h // f >= 240:
                self.scale = f
                break
        sw, sh = w // self.scale, h // self.scale

        where = self.client.call("client_player_state")["position"]
        self._restore = (where["x"], where["y"], where["z"])

        self.client.call("client_gui_close")
        self.server.call("server_run_command", command="gamemode 1 @p")
        self.server.call("server_run_command", command="tp @p %d %d %d 0 0" % PARK_POS)
        self.give("minecraft:air")
        self.client.call("client_select_slot", slot=PARK_SLOT)
        self.client.call("client_wait", ticks=80)

        # Rough window from the vanilla hotbar geometry, then calibrate the slot inside it.
        gx = (sw // 2 - 91 + 3 + PROBE_SLOT * 20) * self.scale
        gy = (sh - 22 + 3) * self.scale
        self.window = (gx - PAD * self.scale, gy - PAD * self.scale,
                       min(w, gx + (16 + PAD) * self.scale), min(h, gy + (16 + PAD) * self.scale))
        self.reference = self.settle("_inv_ref")
        if self.diff(self.reference, self.shot("_inv_ref2")) is not None:
            print("WARNING: the frame is not stable with an empty slot; measurements may be noisy")

        self.give(CALIBRATION_ITEM + " 1")
        cal = self.diff(self.reference, self.settle("_inv_cal"))
        if cal is None:
            sys.exit("Calibration item did not render -- is the probe slot visible?")
        cx, cy = (cal[0] + cal[2]) / 2.0, (cal[1] + cal[3]) / 2.0
        self.slot = (cx - 8 * self.scale, cy - 8 * self.scale)
        self.window = (int(max(0, self.slot[0] - PAD * self.scale)),
                       int(max(0, self.slot[1] - PAD * self.scale)),
                       int(min(w, self.slot[0] + (16 + PAD) * self.scale)),
                       int(min(h, self.slot[1] + (16 + PAD) * self.scale)))
        got = ((cal[2] - cal[0]) / self.scale, (cal[3] - cal[1]) / self.scale)
        print("calibrated on %s: %.2f x %.2f gui px (expected %.2f x %.2f), gui scale %d" % (
            CALIBRATION_ITEM, got[0], got[1], CALIBRATION_SIZE[0], CALIBRATION_SIZE[1], self.scale))
        if abs(got[0] - CALIBRATION_SIZE[0]) > 1.5 or abs(got[1] - CALIBRATION_SIZE[1]) > 1.5:
            print("WARNING: calibration is off expectation; the slot may be misplaced")

    def end(self):
        self.give("minecraft:air")
        if self._restore:
            self.server.call("server_run_command",
                             command="tp @p %.2f %.2f %.2f" % self._restore)

    # -- the measurement

    def measure(self, reg):
        self.park()
        self.give("csm:%s 1" % reg)
        box = self.diff(self.reference, self.settle())
        if box is None:
            return None
        sx, sy = self.slot
        x0, y0 = (box[0] - sx) / self.scale, (box[1] - sy) / self.scale
        x1, y1 = (box[2] - sx) / self.scale, (box[3] - sy) / self.scale
        return {"left": round(-x0, 2), "top": round(-y0, 2),
                "right": round(x1 - 16, 2), "bottom": round(y1 - 16, 2),
                "w": round(x1 - x0, 2), "h": round(y1 - y0, 2),
                "cx": round((x0 + x1) / 2 - 8, 2), "cy": round((y0 + y1) / 2 - 8, 2),
                "clipped": (box[0] <= self.window[0] or box[1] <= self.window[1]
                            or box[2] >= self.window[2] or box[3] >= self.window[3])}


def overflow(m):
    return max(m["left"], m["right"], m["top"], m["bottom"])


# ---- correcting ----------------------------------------------------------------------------

def _num(v):
    """A JSON number written the short way: 0 rather than 0.0, 0.4978 rather than 0.49780000."""
    v = round(float(v), 4)
    return str(int(v)) if v == int(v) else ("%.4f" % v).rstrip("0").rstrip(".")


def _match(text, i):
    """Index just past the bracket or brace opened at ``i``, skipping over strings."""
    close = {"{": "}", "[": "]"}[text[i]]
    depth = 0
    while i < len(text):
        c = text[i]
        if c == '"':
            i += 1
            while text[i] != '"':
                i += 2 if text[i] == "\\" else 1
        elif c in "{[":
            depth += 1
        elif c in "}]":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise ValueError("unbalanced %s" % close)


def _members(text, obj):
    """Every ``key -> (key_start, value_start, value_end)`` directly inside the object at ``obj``."""
    out, i = {}, obj + 1
    end = _match(text, obj) - 1
    while i < end:
        c = text[i]
        if c == '"':
            key_start = i
            i += 1
            while text[i] != '"':
                i += 2 if text[i] == "\\" else 1
            key = text[key_start + 1:i]
            i = text.index(":", i) + 1
            while text[i] in " \t\r\n":
                i += 1
            value_start = i
            if text[i] in "{[":
                i = _match(text, i)
            else:
                while i < end and text[i] not in ",}\r\n":
                    i += 1
            out[key] = (key_start, value_start, i)
        elif c in "{[":
            i = _match(text, i)
        else:
            i += 1
    return out


def _set_member(text, obj, key, literal):
    """Set ``key`` inside the object at ``obj`` to ``literal``, leaving all other bytes alone.

    Replaces the value where the key exists. Where it does not, the member is inserted ahead of
    the first existing one, indented and punctuated like it -- so a file written in a compact
    style keeps that style instead of being re-serialised wholesale.
    """
    members = _members(text, obj)
    if key in members:
        _, value_start, value_end = members[key]
        return text[:value_start] + literal + text[value_end:]
    if not members:
        raise ValueError("cannot place %s in an empty object" % key)
    first = min(m[0] for m in members.values())
    line_start = text.rfind("\n", 0, first) + 1
    indent = text[line_start:first]
    return text[:first] + '"%s": %s,\n%s' % (key, literal, indent) + text[first:]


def _array_literal(text, obj, values):
    """An array written the way arrays are already written in this object -- inline or expanded."""
    sibling = next((v for k, v in _members(text, obj).items() if text[v[1]] == "["), None)
    raw = text[sibling[1]:sibling[2]] if sibling else ""
    if sibling and "\n" in raw:
        line_start = text.rfind("\n", 0, sibling[0]) + 1
        indent = text[line_start:sibling[0]]
        inner = indent + "  "
        return "[\n" + ",\n".join(inner + _num(v) for v in values) + "\n" + indent + "]"
    if sibling and raw.startswith("[ "):
        return "[ " + ", ".join(_num(v) for v in values) + " ]"
    return "[" + ", ".join(_num(v) for v in values) + "]"


def _gui_object_start(text):
    """Index of the ``{`` opening the inventory variant's gui transform, for in-place editing."""
    variants = _members(text, text.index("{"))["variants"]
    inv = _members(text, variants[1])["inventory"]
    entry = text.index("{", inv[1])
    transform = _members(text, entry)["transform"]
    return _members(text, transform[1])["gui"][1]


def gui_transform_of(bs):
    """The inventory variant's gui transform, materialised if it was inherited or named."""
    inv = bs["variants"]["inventory"][0]
    tf = inv.get("transform")
    if tf is None:
        inv["transform"] = tf = {}
    if isinstance(tf, str):
        inv["transform"] = tf = json.loads(json.dumps(FORGE_DEFAULT_BLOCK))
    gui = tf.get("gui")
    if gui is None or isinstance(gui, str):
        gui = dict(FORGE_DEFAULT_BLOCK["gui"])
        tf["gui"] = gui
    return gui


def correct(record):
    """Centre one item's gui render in its slot, and bring it down in scale if it is too big."""
    m = record["measured"]
    path = record["blockstate"]
    original = open(path, encoding="utf-8").read()
    bs = json.loads(original)
    gui = gui_transform_of(bs)

    scale = gui.get("scale", 0.625)
    shrink = 1.0
    biggest = max(m["w"], m["h"])
    if not isinstance(scale, list) and biggest > TARGET:
        shrink = TARGET / biggest
        gui["scale"] = round(scale * shrink, 4)

    # One gui pixel is 1/16 of a translation unit -- see TRANSLATION UNITS above.
    t = gui.get("translation", [0, 0, 0])
    t = [round(float(t[0]) - m["cx"] * shrink / 16.0, 4),
         round(float(t[1]) + m["cy"] * shrink / 16.0, 4),
         round(float(t[2]), 4)]
    if t == [0.0, 0.0, 0.0]:
        gui.pop("translation", None)
    else:
        gui["translation"] = t

    # Edit the values where they sit rather than re-serialising: most of these blockstates are
    # written in a compact style that a json.dump round trip would reformat wholesale, burying a
    # one-line change in fifty lines of whitespace.
    try:
        rewritten = original
        if "transform" not in json.loads(original)["variants"]["inventory"][0] \
                or isinstance(json.loads(original)["variants"]["inventory"][0]["transform"], str) \
                or "gui" not in json.loads(original)["variants"]["inventory"][0]["transform"] \
                or isinstance(json.loads(original)["variants"]["inventory"][0]["transform"].get(
                    "gui"), str):
            # Nothing to edit in place: the transform was inherited or named rather than spelled
            # out, so the materialised one has to be written out in full.
            rewritten = json.dumps(bs, indent=2) + "\n"
        else:
            obj = _gui_object_start(rewritten)
            if "scale" in gui:
                rewritten = _set_member(rewritten, obj, "scale", _num(gui["scale"]))
            obj = _gui_object_start(rewritten)
            if "translation" in gui:
                rewritten = _set_member(rewritten, obj, "translation",
                                        _array_literal(rewritten, obj, gui["translation"]))
        # An in-place text edit must never change anything but the values it meant to. Comparing
        # the parsed result against the structure built above is what makes that checkable.
        if json.loads(rewritten) != bs:
            raise ValueError("in-place edit changed something it should not have")
    except (ValueError, KeyError, IndexError) as exc:
        print("  %-32s NOT written (%s). Apply by hand: scale %s, translation %s"
              % (record["reg"], exc, gui.get("scale"), gui.get("translation")))
        return False

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(rewritten)
    print("  %-32s scale %-10s translation %s" % (
        record["reg"], gui.get("scale"), gui.get("translation", "(none)")))
    return True


# ---- main ----------------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[1],
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--only", help="comma-separated registry names to measure instead of all")
    ap.add_argument("--check", action="store_true",
                    help="exit 1 if any item renders outside its slot")
    ap.add_argument("--fix", action="store_true",
                    help="write corrections for items that are out; needs a rebuild and a client "
                         "restart before re-measuring")
    ap.add_argument("--json", metavar="PATH", help="write every measurement to this file")
    args = ap.parse_args()

    token, client_port, server_port = read_mcmcp_config()
    try:
        client = Endpoint(client_port, token).connect()
    except Exception as exc:                                   # noqa: BLE001
        sys.exit("Cannot reach the dev client on port %d (%s).\nStart it with ./gradlew runClient."
                 % (client_port, exc))
    try:
        server = Endpoint(server_port, token).connect()
    except Exception as exc:                                   # noqa: BLE001
        sys.exit("Cannot reach the server endpoint on port %d (%s).\n"
                 "Set B:enableServerEndpoint=true in %s and restart the client."
                 % (server_port, exc, MCMCP_CFG))

    items = candidates()
    if args.only:
        wanted = {s.strip() for s in args.only.split(",") if s.strip()}
        missing = wanted - {i["reg"] for i in items}
        if missing:
            sys.exit("Not OBJ-backed inventory items: %s" % ", ".join(sorted(missing)))
        items = [i for i in items if i["reg"] in wanted]
    print("measuring %d item(s)" % len(items))

    shots = client.call("client_runtime_info")["screenshotDirectory"]
    probe = Probe(client, server, shots)
    probe.begin()
    try:
        for n, rec in enumerate(items, 1):
            rec["measured"] = probe.measure(rec["reg"])
            if n % 50 == 0:
                print("  %d/%d" % (n, len(items)))
        # The reference frame is the yardstick for every measurement above, so a run is only
        # worth reading if the empty slot still looks the way it did when that frame was taken.
        # Checking is what turns a silently wrong run into a loud one.
        if probe.backdrop_drifted():
            sys.exit("\nABORT: the backdrop changed during the run, so the measurements above "
                     "describe\nthe background rather than the items. Re-run; if it keeps "
                     "happening, something in\nview is animating or the player is not staying put.")
    finally:
        probe.end()

    if args.json:
        json.dump(items, open(args.json, "w"), indent=1)
        print("wrote %s" % args.json)

    blank = [r for r in items if r["measured"] is None]
    out = sorted((r for r in items if r["measured"] and overflow(r["measured"]) > INSIDE),
                 key=lambda r: -overflow(r["measured"]))
    clipped = [r for r in items if r["measured"] and r["measured"]["clipped"]]

    for rec in blank:
        print("NO RENDER  %-30s %s" % (rec["reg"], rec["name"]))
        print("           nothing drew in the slot -- a broken model, or a translation so large "
              "it left the screen")
    for rec in clipped:
        print("CLIPPED    %-30s measurement limited by the screen edge; its real overflow may be "
              "larger" % rec["reg"])

    if not out:
        print("\nall %d item(s) render inside their slot" % (len(items) - len(blank)))
    else:
        print("\n%d of %d item(s) render outside their slot:\n" % (len(out), len(items)))
        print("%-32s %6s %6s %6s %6s   %s" % ("registry", "left", "right", "top", "bottom",
                                              "display name"))
        for rec in out:
            m = rec["measured"]
            cell = lambda v: ("%+.1f" % v) if v > INSIDE else "   .  "   # noqa: E731
            print("%-32s %6s %6s %6s %6s   %s" % (
                rec["reg"], cell(m["left"]), cell(m["right"]), cell(m["top"]), cell(m["bottom"]),
                rec["name"]))
        print("\n(gui pixels outside the 16px slot edge; '.' means that edge is inside)")

    if args.fix and out:
        print("\nwriting corrections:")
        written = sum(correct(rec) for rec in out)
        print("\n%d blockstate(s) written. Rebuild, then restart the client -- a resource reload\n"
              "does not rebake these models, and stopping the Gradle task does not stop the game\n"
              "JVM, so kill it or the next run measures the stale client." % written)

    if args.check and (out or blank):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
