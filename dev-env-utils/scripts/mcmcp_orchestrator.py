#!/usr/bin/env python3
"""
mcmcp_orchestrator.py -- a minimal MCP client for the MCMCP orchestrator, for build scripts.

The orchestrator is the one endpoint that knows which game is which: every call names its game by
instance id ('run-f2eb58.server'), so a script can never land in another running game by guessing
a port (a port such as 25586 belongs to whichever game bound it first, and that has been a live
server). It is reached the way Claude Code reaches it, by running its shim over stdio.

    from mcmcp_orchestrator import Orchestrator
    o = Orchestrator()                       # finds the shim, initializes
    o.call('server_set_blocks', instance='run-f2eb58.server', mode='list', blocks=[...])

Results come back parsed from JSON when the tool answered JSON, else as text. A tool error raises
OrchestratorError. Override the shim path with the MCMCP_ORCHESTRATOR environment variable.
"""
import json
import os
import subprocess
import threading

DEFAULT_SHIM = os.path.join(os.environ.get('LOCALAPPDATA', ''), 'MCMCP Orchestrator',
                            'mcmcp-orchestrator.exe')


class OrchestratorError(RuntimeError):
    pass


class Orchestrator:
    def __init__(self, shim=None, timeout=300):
        shim = shim or os.environ.get('MCMCP_ORCHESTRATOR') or DEFAULT_SHIM
        if not os.path.exists(shim):
            raise OrchestratorError('no orchestrator shim at %s' % shim)
        self.timeout = timeout
        self.proc = subprocess.Popen([shim, 'shim'], stdin=subprocess.PIPE,
                                     stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                     bufsize=0)
        self._id = 0
        self._request('initialize', {
            'protocolVersion': '2025-06-18', 'capabilities': {},
            'clientInfo': {'name': 'csm-build-script', 'version': '1'}})
        self._send({'jsonrpc': '2.0', 'method': 'notifications/initialized'})

    def _send(self, msg):
        self.proc.stdin.write((json.dumps(msg) + '\n').encode('utf-8'))
        self.proc.stdin.flush()

    def _readline(self):
        box = {}

        def read():
            box['line'] = self.proc.stdout.readline()

        t = threading.Thread(target=read, daemon=True)
        t.start()
        t.join(self.timeout)
        if t.is_alive():
            raise OrchestratorError('no reply from the orchestrator in %ds' % self.timeout)
        if not box['line']:
            raise OrchestratorError('the orchestrator shim exited')
        return box['line']

    def _request(self, method, params):
        self._id += 1
        rid = self._id
        self._send({'jsonrpc': '2.0', 'id': rid, 'method': method, 'params': params})
        while True:
            line = self._readline().strip()
            if not line:
                continue
            try:
                msg = json.loads(line)
            except ValueError:
                continue
            if msg.get('id') != rid:
                continue  # a notification, or a reply to something else
            if 'error' in msg:
                raise OrchestratorError('%s: %s' % (method, msg['error'].get('message')))
            return msg.get('result', {})

    def call(self, tool, /, **arguments):
        # positional-only, so a tool argument may itself be called 'tool' or 'name'
        result = self._request('tools/call', {'name': tool, 'arguments': arguments})
        texts = [c.get('text', '') for c in result.get('content', []) if c.get('type') == 'text']
        if result.get('isError'):
            raise OrchestratorError('%s failed: %s' % (tool, ' '.join(texts)[:500]))
        if result.get('structuredContent') is not None:
            return result['structuredContent']
        text = '\n'.join(texts)
        try:
            return json.loads(text)
        except ValueError:
            return text

    def close(self):
        try:
            self.proc.stdin.close()
            self.proc.wait(5)
        except Exception:
            self.proc.kill()
