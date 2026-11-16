"""Turns a Forge-format (``forge_marker``) blockstate whose properties only swap textures into a
vanilla multipart one that draws every state exactly as before, from one model location.

A Forge variant blockstate bakes one variant, with its own model location, for every
combination of its properties, even when a property only lays a different texture over the same
model: a TV's channel, a gate sign's letter and number. A multipart blockstate is one model
location for the whole block (``CsmMultipartStateMapper``), whatever its rules; but a multipart
rule cannot set textures, so each (model, textures) pair a state is drawn with becomes a child
model file: the same model as its parent, with the variant's textures. Each state then matches
exactly one rule, which names that child and the variant's rotation. The bake is the same: the
same parent's elements, the same resolved textures, the same 90-degree rotation, so the part bake
cache and the quad sharing find the same parts and quads as before.

Only what a vanilla multipart rule can say converts: a model, ``x``, ``y`` and ``uvlock``. A
blockstate that uses Forge's ``transform``, ``submodel``, ``custom`` or the like raises, as does a
model that is not a JSON file (a JSON child cannot have an OBJ parent).

``convert`` takes the Forge blockstate dict and returns the multipart blockstate and the child
models to write; the generator that wrote the Forge form calls it and writes both.
``convert_if_texture_only`` does that only for a blockstate that can convert and has a property
that swaps textures and nothing else, and writes the children, so a generator can pass every
blockstate it writes through it. Either registers the Forge form with ``model_depth``
(``SEPARATE_AS``), which separates the models as that form drew them, the item's included, so
the conversion moves no face. ``verify_multipart_conversion.py`` checks the result.

``convert_obj`` is the second entry point, for blockstates of OBJ models whose properties pick
parts (a cell's model, its mount stubs, an end's cap): see the section at the end.
"""
import copy
import itertools

import model_depth

_RULE_KEYS = {"model", "textures", "x", "y", "uvlock"}


def _merge(base, part, where):
    out = dict(base)
    for k, v in part.items():
        if k not in _RULE_KEYS:
            raise ValueError("%s: %r cannot be said by a multipart rule" % (where, k))
        if k == "textures":
            out["textures"] = dict(out.get("textures", {}), **v)
        else:
            out[k] = v
    return out


def _one(v):
    if isinstance(v, list):
        if len(v) != 1:
            raise ValueError("weighted variants do not convert: %r" % (v,))
        return v[0]
    return v


def resolve(state):
    """Every state's Forge resolution: ({property: [values]}, {combo tuple: variant dict}) with
    the combo in sorted property order, the variant holding model, textures, x, y, uvlock."""
    defaults = state.get("defaults", {})
    _merge({}, defaults, "defaults")
    variants = state["variants"]
    props = {}
    full = {}
    for key, val in variants.items():
        if key in ("inventory", "normal"):
            continue  # the item's, and the one Forge uses for a block with no properties
        if "=" in key:
            pairs = [tuple(kv.split("=", 1)) for kv in key.split(",")]
            for p, v in pairs:
                props.setdefault(p, [])
                if v not in props[p]:
                    props[p].append(v)
            full[tuple(sorted(pairs))] = _one(val)
        else:
            props[key] = list(val.keys())
    if full and any("=" not in k and k not in ("inventory", "normal") for k in variants):
        raise ValueError("named variants mixed with property sections")
    names = sorted(props)
    out = {}
    for combo in itertools.product(*(props[n] for n in names)):
        key = tuple((n, v) for n, v in zip(names, combo))
        if full:
            if key not in full:
                raise ValueError("no variant for %s" % (key,))
            v = _merge(defaults, full[key], key)
        else:
            v = dict(defaults)
            set_by = {}
            for n, val in zip(names, combo):
                part = variants[n][val]
                for k in part:
                    if k != "textures" and k in set_by:
                        raise ValueError("%s and %s both set %s" % (set_by[k], n, k))
                    set_by[k] = n
                v = _merge(v, part, key)
        out[key] = v
    return {n: props[n] for n in names}, out


def inventory(state):
    """The item's Forge resolution, or None when the blockstate has no inventory variant."""
    inv = state["variants"].get("inventory")
    if inv is None:
        return None
    return _merge(state.get("defaults", {}), _one(inv), "inventory")


def texture_only(props, table):
    """The properties that change some state's textures and nothing else about any state."""
    names = list(props)
    out = []
    for i, n in enumerate(names):
        groups = {}
        for key in table:
            groups.setdefault(key[:i] + key[i + 1:], []).append(key)
        geometry = False
        textures = False
        for keys in groups.values():
            vs = [table[k] for k in keys]
            if len({repr(sorted((k, v) for k, v in x.items() if k != "textures"))
                    for x in vs}) > 1:
                geometry = True
            if len({repr(sorted(x.get("textures", {}).items())) for x in vs}) > 1:
                textures = True
        if textures and not geometry:
            out.append(n)
    return out


def _relevant(props, table):
    """The properties some state's drawing depends on."""
    names = list(props)
    keep = []
    for i, n in enumerate(names):
        others = {}
        for key, v in table.items():
            rest = key[:i] + key[i + 1:]
            sig = repr(sorted(v.items(), key=lambda kv: kv[0]))
            if others.setdefault(rest, sig) != sig:
                keep.append(n)
                break
    return keep


def block_path(model):
    """``csm:a/b`` (a blockstate's model name) as the parent a model file names."""
    ns, path = model.split(":", 1)
    return "%s:block/%s" % (ns, path)


def named_by_textures(folder, block, table, inv=None):
    """Child names ``<folder>/states/<block>/<model>_<texture>[_<texture>...]``: the model's
    file name, then the file name of each texture that differs between states, by key."""
    varying = set()
    seen = {}
    for v in list(table.values()) + ([inv] if inv else []):
        for k, t in v.get("textures", {}).items():
            if seen.setdefault(k, t) != t:
                varying.add(k)

    def name(model, tex):
        tags = [tex[k].rsplit("/", 1)[1] for k in sorted(varying) if k in tex]
        return "%s/states/%s/%s" % (folder, block, "_".join([model.rsplit("/", 1)[1]] + tags))
    return name


def convert(state, child_name, block):
    """Converts a texture-swapping Forge blockstate.

    :param state:      the Forge-format blockstate dict
    :param child_name: ``child_name(model, textures) -> model name`` (blockstate form,
                       ``csm:a/b``) for the child model drawing ``model`` with ``textures``,
                       unique per pair; or a folder (``csm:a``) for ``named_by_textures``
    :param block:      the blockstate's registry name; ``model_depth`` separates its models as
                       the Forge form draws them (``SEPARATE_AS``), so they stay as they were
    :return: (multipart blockstate dict, {child model name: model file dict})
    """
    forge_form = copy.deepcopy(state)
    props, table = resolve(state)
    if not callable(child_name):
        child_name = named_by_textures(child_name, block, table, inventory(state))
    keep = _relevant(props, table)
    names = list(props)
    idx = [names.index(n) for n in keep]
    children = {}
    owner = {}

    def model_for(v):
        model = v["model"]
        if model.endswith(".obj") or model.startswith("builtin/"):
            raise ValueError("not a JSON model: %s" % model)
        tex = v.get("textures")
        if not tex:
            return model
        child = child_name(model, tex)
        sig = (model, tuple(sorted(tex.items())))
        if owner.setdefault(child, sig) != sig:
            raise ValueError("child name %s used for two models" % child)
        children[child] = {"parent": block_path(model), "textures": dict(tex)}
        return child

    rules = []
    seen = set()
    for key, v in table.items():
        when = tuple(key[i] for i in idx)
        if when in seen:
            continue
        seen.add(when)
        apply = {"model": model_for(v)}
        for k in ("x", "y"):
            if v.get(k):
                apply[k] = v[k]
        if v.get("uvlock"):
            apply["uvlock"] = True
        rule = {"when": dict(when), "apply": apply} if when else {"apply": apply}
        rules.append(rule)
    out = {}
    inv = inventory(state)
    if inv is not None:
        apply = {"model": model_for(inv)}
        for k in ("x", "y"):
            if inv.get(k):
                apply[k] = inv[k]
        if inv.get("uvlock"):
            apply["uvlock"] = True
        out["variants"] = {"inventory": apply}
    out["multipart"] = rules
    model_depth.SEPARATE_AS[block + ".json"] = forge_form
    return out, children


def convert_if_texture_only(reg, state, folder, dump):
    """``state`` converted (``convert``, children named by ``named_by_textures`` under
    ``folder``) when it is a Forge blockstate a multipart one can say and some property of it
    only swaps textures; otherwise ``state`` as it is. Child models are written with
    ``dump("models/block/<path>.json", data)``."""
    if not isinstance(state, dict) or not state.get("forge_marker"):
        return state
    try:
        props, table = resolve(state)
        if not texture_only(props, table):
            return state
        new, kids = convert(state, folder, reg)
    except (ValueError, KeyError, AttributeError):
        return state
    for name, data in sorted(kids.items()):
        dump("models/block/%s.json" % name.split(":", 1)[1], data)
    return new


# =============================================================================== OBJ multipart
# A Forge blockstate of OBJ models whose variants differ in their parts (a shape property that
# picks a cell's model and its mount stubs, end and flank properties that add sub-models) has a
# model location and a Forge MultiModel for every combination of its properties: the light pole
# families of #99 held 66 thousand of them. As multipart it is one location. A multipart rule
# names a model, x, y and uvlock only, so the OBJ options a Forge variant carries (its custom
# data, its retextures, an item's display transform) go into the model's location, which
# CsmObjModelLoader reads: ``csm:a/b.obj?flip-v&body=csm:blocks/x``. The file is parsed once
# however many option sets name it.

OBJ_CUSTOM_KEYS = ("flip-v", "ambient", "gui3d")


def obj_location(model, custom=None, textures=None, transform=None):
    """An OBJ model's location with its options (CsmObjModelLoader): custom data, textures by
    material (a texture keyed ``particle`` is left out: an OBJ model has no such material, and
    Forge's retexture ignores it), and a Forge transform preset."""
    if not model.endswith(".obj"):
        raise ValueError("not an OBJ model: %s" % model)
    opts = []
    for k, v in sorted((custom or {}).items()):
        if k not in OBJ_CUSTOM_KEYS:
            raise ValueError("custom %r cannot be said in an OBJ location" % k)
        v = str(v).lower()
        if v == "true":
            opts.append(k)
        else:
            opts.append("%s=%s" % (k, v))
    for k, v in sorted((textures or {}).items()):
        if k == "particle":
            continue
        name = k[1:] if k.startswith("#") else k
        if name in OBJ_CUSTOM_KEYS or name == "transform" or not name or "&" in v or "?" in v:
            raise ValueError("texture %r -> %r cannot be said in an OBJ location" % (k, v))
        opts.append("%s=%s" % (name, v))
    if transform:
        opts.append("transform=%s" % transform)
    return model + ("?" + "&".join(opts) if opts else "")


_PART_KEYS = {"model", "custom", "textures", "x", "y", "uvlock", "submodel", "transform"}


def _merge_part(base, part, where):
    out = dict(base)
    for k, v in part.items():
        if k not in _PART_KEYS:
            raise ValueError("%s: %r is not handled" % (where, k))
        if k in ("textures", "custom", "submodel"):
            merged = dict(out.get(k, {}))
            for kk, vv in v.items():
                if k == "submodel" and kk in merged:
                    raise ValueError("%s: two sub-models named %s" % (where, kk))
                merged[kk] = vv
            out[k] = merged
        else:
            out[k] = v
    return out


def _part_key(v, x, y, where):
    if v.get("transform"):
        raise ValueError("%s: a transform cannot be said by a multipart rule" % where)
    if v.get("uvlock"):
        raise ValueError("%s: uvlock is not handled" % where)
    return (obj_location(v["model"], v.get("custom"), v.get("textures")), x % 360, y % 360)


def forge_obj_parts(state):
    """Every state's parts under Forge: ({property: [values]}, {combo: [(location, x, y)]}),
    the combo in sorted property order. A part is the variant's model, or one of its sub-models,
    each under the variant's rotation (Forge bakes a sub-model under the variant's state
    composed with the sub-model's own, which must be absent here). A sub-model takes only its
    own custom data and textures, as Forge's ``SubModel`` does."""
    if not state.get("forge_marker"):
        raise ValueError("not a Forge blockstate")
    defaults = _merge_part({}, state.get("defaults", {}), "defaults")
    variants = state["variants"]
    props = {}
    for key, val in variants.items():
        if key in ("inventory", "normal"):
            continue
        if "=" in key or not isinstance(val, dict):
            raise ValueError("only property sections convert: %s" % key)
        props[key] = list(val.keys())
    names = sorted(props)
    out = {}
    for combo in itertools.product(*(props[n] for n in names)):
        key = tuple(zip(names, combo))
        v = dict(defaults)
        set_by = {}
        for n, val in key:
            part = _one(variants[n][val])
            for k in part:
                if k in ("model", "x", "y", "uvlock", "transform") and k in set_by:
                    raise ValueError("%s and %s both set %s" % (set_by[k], n, k))
                set_by[k] = n
            v = _merge_part(v, part, key)
        x, y = v.get("x", 0), v.get("y", 0)
        parts = [_part_key(v, x, y, key)]
        for sub_name, sub in sorted(v.get("submodel", {}).items()):
            if "submodel" in sub:
                raise ValueError("%s: nested sub-models" % (key,))
            parts.append(_part_key(sub, x, y, "%s/%s" % (key, sub_name)))
        out[key] = parts
    return {n: props[n] for n in names}, out


def forge_obj_inventory(state):
    """The item's location under Forge (the inventory variant over the defaults), or None."""
    inv = state["variants"].get("inventory")
    if inv is None:
        return None
    v = _merge_part(_merge_part({}, state.get("defaults", {}), "defaults"), _one(inv), "inventory")
    if v.get("submodel") or v.get("x") or v.get("y"):
        raise ValueError("inventory: only a model, custom data, textures and a transform convert")
    transform = v.get("transform")
    if transform is not None and not isinstance(transform, str):
        raise ValueError("inventory: only a named transform converts")
    return obj_location(v["model"], v.get("custom"), v.get("textures"), transform)


def _apply(part):
    loc, x, y = part
    a = {"model": loc}
    if x:
        a["x"] = x
    if y:
        a["y"] = y
    return a


def rules_from_parts(props, table):
    """Multipart rules drawing each state with exactly its parts. For each part, the properties
    its presence depends on are found, and the states holding it are grouped by every such
    property but one (the one with the most values), which the rule lists: ``"shape": "3|7|11"``.
    Checked state by state before returning."""
    names = list(props)
    holders = {}
    for key, parts in table.items():
        for p in parts:
            holders.setdefault(p, set()).add(key)
    rules = []
    # A multipart model takes its particle (and ambient occlusion) from its first rule's part, as
    # a Forge variant took them from its own model rather than a sub-model: the variants' own
    # models' rules come first.
    bases = {parts[0] for parts in table.values() if parts}
    for part in sorted(holders, key=lambda p: (p not in bases, p[0], p[1], p[2])):
        held = holders[part]
        relevant = []
        for i, n in enumerate(names):
            groups = {}
            for key in table:
                groups.setdefault(key[:i] + key[i + 1:], set()).add(key in held)
            if any(len(g) > 1 for g in groups.values()):
                relevant.append(i)
        if not relevant:
            rules.append({"apply": _apply(part)})
            continue
        listed = max(relevant, key=lambda i: (len(props[names[i]]), -i))
        others = [i for i in relevant if i != listed]
        by_rest = {}
        for key in held:
            rest = tuple(key[i] for i in others)
            by_rest.setdefault(rest, set()).add(key[listed][1])
        for rest in sorted(by_rest):
            values = [v for v in props[names[listed]] if v in by_rest[rest]]
            when = {}
            for n, v in rest:
                when[n] = v
            if len(values) < len(props[names[listed]]):
                when[names[listed]] = "|".join(values)
            rules.append({"when": when, "apply": _apply(part)})
    # every state draws exactly the parts it drew under Forge
    for key, parts in table.items():
        got = []
        for r in rules:
            w = r.get("when", {})
            if all(dict(key)[n] in v.split("|") for n, v in w.items()):
                a = r["apply"]
                got.append((a["model"], a.get("x", 0), a.get("y", 0)))
        if sorted(got) != sorted(parts):
            raise AssertionError("%s draws %s, not %s" % (key, sorted(got), sorted(parts)))
    return rules


def obj_multipart(props, table, inventory_location=None):
    """The multipart blockstate for a parts table (``forge_obj_parts``, possibly edited)."""
    out = {}
    if inventory_location is not None:
        out["variants"] = {"inventory": {"model": inventory_location}}
    out["multipart"] = rules_from_parts(props, table)
    return out


def convert_obj(state):
    """A Forge blockstate of OBJ parts as one multipart blockstate drawing every state and the
    item exactly as Forge did (see ``forge_obj_parts``)."""
    props, table = forge_obj_parts(state)
    return obj_multipart(props, table, forge_obj_inventory(state))
