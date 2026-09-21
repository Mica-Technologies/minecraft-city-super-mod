"""Shared helpers for the block performance harness: MCMCP clients and small utilities.

The client endpoint is 25585 and the server endpoint 25586; both take the bearer token from
run/config/mcmcp.cfg. `csm_bench.Mcp` (dev-env-utils/scripts) does the JSON-RPC.
"""
import json, os, sys, time, statistics, threading

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(REPO, "dev-env-utils", "scripts"))
import csm_bench as cb

# Written by mcmcp game_dump_registries {namespace: csm, file: perf_inventory.json}
DUMP = os.path.join(REPO, "run", "mcmcp", "dumps", "perf_inventory.json")

tok = cb.read_token()
C = cb.Mcp(cb.CLIENT_URL, tok).connect()
S = cb.Mcp(cb.SERVER_URL, tok).connect()


def cmd(c):
    return S.call("server_run_command", {"command": c})
