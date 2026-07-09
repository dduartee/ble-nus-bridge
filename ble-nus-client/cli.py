"""Interactive CLI for BLE NUS debugger. Works with AI agents via stdin/stdout."""

import asyncio
import json
import logging
import sys
import threading
from nus import NusMessage, serialize_frame
from ble_conn import BleManager
from history import MessageHistory

log = logging.getLogger(__name__)

HELP_TEXT = """Commands:
  scan [name] [timeout]     Scan for BLE devices
  connect <addr> [name]     Connect to device
  disconnect                Disconnect
  send <text>               Send raw text to NUS RX
  send-json <json>          Send JSON frame to NUS RX
  status                    Show connection status
  history [n]               Show last N messages
  clear                     Clear message history
  help                      Show this help
  quit                      Exit
"""


class Cli:
    def __init__(self, json_mode: bool = False):
        self.json_mode = json_mode
        self.ble = BleManager(on_data=self._on_data)
        self.history = MessageHistory(max_size=200)
        self._running = True

    def _on_data(self, msg: NusMessage):
        """Callback fired by BleManager when a NUS RX notification arrives."""
        msg.direction = "rx"
        self.history.add(msg)
        # JSON mode: emit machine-parseable lines. Pretty mode: human-friendly preview.
        if self.json_mode:
            print(json.dumps({
                "event": "data",
                "direction": "rx",
                "payload": msg.payload,
                "ts": msg.timestamp,
            }))
        else:
            preview = json.dumps(msg.payload, ensure_ascii=False)[:120]
            print(f"\n  \u2190 RX: {preview}")

    async def run(self):
        """Main loop — reads stdin in a thread, processes commands."""
        loop = asyncio.get_event_loop()
        reader = await self._start_stdin_reader()

        while self._running:
            try:
                line = await reader
                if line is None:
                    break
                await self._dispatch(line.strip())
            except asyncio.CancelledError:
                break
            except Exception as e:
                self._print({"error": str(e)})

    async def _start_stdin_reader(self):
        """Bridge blocking stdin to async via a thread + Queue.

        stdin is blocking, so we read it in a daemon thread.
        Each line is pushed onto an asyncio.Queue via call_soon_threadsafe.
        When EOF or Ctrl+D is received, None is enqueued to signal shutdown.
        The coroutine returns the first line immediately, and subsequent
        calls to `await reader` yield subsequent lines.
        """
        loop = asyncio.get_event_loop()
        q: asyncio.Queue[str | None] = asyncio.Queue()

        def read_stdin():
            try:
                for line in sys.stdin:
                    loop.call_soon_threadsafe(q.put_nowait, line)
            except (EOFError, KeyboardInterrupt):
                pass
            loop.call_soon_threadsafe(q.put_nowait, None)

        threading.Thread(target=read_stdin, daemon=True).start()
        # The first get() blocks until the thread delivers a line.
        return await q.get()

    async def _dispatch(self, line: str):
        """Parse and execute a single command line."""
        if not line:
            return
        parts = line.split(None, 2)
        cmd = parts[0].lower()

        if cmd == "quit" or cmd == "exit":
            await self.ble.disconnect()
            self._running = False

        elif cmd == "help":
            self._print({"help": HELP_TEXT})

        elif cmd == "scan":
            name = parts[1] if len(parts) > 1 else None
            timeout = float(parts[2]) if len(parts) > 2 else 5.0
            self._print({"scanning": True, "name": name, "timeout": timeout})
            devices = await self.ble.scan(timeout=timeout, name_filter=name)
            self._print({"devices": devices})

        elif cmd == "connect":
            if len(parts) < 2:
                self._print({"error": "usage: connect <addr> [name]"})
                return
            addr = parts[1]
            name = parts[2] if len(parts) > 2 else None
            ok = await self.ble.connect(addr, name)
            self._print({
                "status": "connected" if ok else "failed",
                "addr": addr,
                "mtu": self.ble.mtu if ok else None,
            })

        elif cmd == "disconnect":
            await self.ble.disconnect()
            self._print({"status": "disconnected"})

        elif cmd == "send":
            if len(parts) < 2:
                self._print({"error": "usage: send <text>"})
                return
            text = line[len(parts[0]):].strip()
            ok = await self.ble.send_raw(text)
            self._print({"status": "sent" if ok else "failed", "data": text})

        elif cmd == "send-json":
            if len(parts) < 2:
                self._print({"error": "usage: send-json <json>"})
                return
            raw_json = line[len(parts[0]):].strip()
            try:
                payload = json.loads(raw_json)
                msg = NusMessage(payload=payload)
                data = serialize_frame(msg)
                ok = await self.ble.send(data)
                self._print({"status": "sent" if ok else "failed", "payload": payload})
            except json.JSONDecodeError as e:
                self._print({"error": f"invalid JSON: {e}"})

        elif cmd == "status":
            self._print({
                "connected": self.ble.connected,
                "mtu": self.ble.mtu,
                "device": self.ble.device.name if self.ble.device else None,
                "addr": self.ble.device.address if self.ble.device else None,
                "buffered": len(self.history),
            })

        elif cmd == "history":
            n = int(parts[1]) if len(parts) > 1 else 20
            msgs = self.history.get_recent(n)
            self._print({
                "messages": [
                    {"payload": m.payload, "ts": m.timestamp}
                    for m in msgs
                ]
            })

        elif cmd == "clear":
            self.history.clear()
            self._print({"status": "cleared"})

        else:
            self._print({"error": f"unknown command: {cmd}. Type 'help'."})

    def _print(self, data: dict):
        """Output a dict — JSON line in json_mode, pretty-printed otherwise."""
        if self.json_mode:
            print(json.dumps(data, ensure_ascii=False))
        else:
            print(json.dumps(data, indent=2, ensure_ascii=False))
