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
