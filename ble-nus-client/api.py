"""HTTP API + WebSocket for BLE NUS debugger.

Provides a REST API and WebSocket endpoint built on aiohttp.
Shared module-level state holds the BleManager, MessageHistory,
and set of connected WebSocket clients.
"""

import asyncio
import json
import logging
from aiohttp import web

from ble_conn import BleManager
from history import MessageHistory
from nus import NusMessage, serialize_frame

log = logging.getLogger(__name__)

# Shared state — BleManager uses bleak for BLE I/O, MessageHistory
# buffers parsed messages, ws_clients tracks live WS connections.
ble = BleManager()
history = MessageHistory(max_size=200)
ws_clients: list[web.WebSocketResponse] = []


def on_ble_data(msg: NusMessage):
    """Called by BleManager when NUS TX data arrives from the peripheral.

    Sets direction to "rx", records the message in history, and schedules
    a broadcast to all WebSocket clients.

    Uses ``call_soon`` + ``ensure_future`` to fire the broadcast as a
    fire-and-forget coroutine from a sync callback context.
    """
    msg.direction = "rx"
    history.add(msg)
    asyncio.get_event_loop().call_soon(
        asyncio.ensure_future,
        broadcast_ws(msg),
    )


async def broadcast_ws(msg: NusMessage):
    """Send a JSON-serialized message to every connected WebSocket client.

    Dead clients are collected and removed in a single pass so that
    one failed ``send_str`` does not interrupt the broadcast loop.
    """
    data = json.dumps({
        "type": "data",
        "direction": "rx",
        "payload": msg.payload,
        "timestamp": msg.timestamp,
    })
    dead = []
    for ws in ws_clients:
        try:
            await ws.send_str(data)
        except Exception:
            dead.append(ws)
    for ws in dead:
        ws_clients.remove(ws)


# ── Routes ─────────────────────────────────────────────────

async def handle_scan(request: web.Request) -> web.Response:
    """GET /scan?timeout=5&name=track-kinesis

    Scans for BLE devices. Returns a JSON list of discovered peripherals.
    """
    timeout = float(request.query.get("timeout", "5"))
    name = request.query.get("name")
    results = await ble.scan(timeout=timeout, name_filter=name)
    return web.json_response({"devices": results})


async def handle_connect(request: web.Request) -> web.Response:
    """POST /connect {"addr": "AA:BB:CC:DD:EE:FF", "name": "optional"}

    Connect to a BLE peripheral by MAC address. Returns connection
    details including negotiated MTU on success.
    """
    body = await request.json()
    addr = body.get("addr")
    name = body.get("name")
    if not addr:
        return web.json_response({"error": "addr required"}, status=400)

    ok = await ble.connect(addr, name)
    if ok:
        return web.json_response({
            "status": "connected",
            "addr": addr,
            "name": name or ble.device.name if ble.device else None,
            "mtu": ble.mtu,
        })
    return web.json_response({"error": "connect failed"}, status=500)


async def handle_disconnect(request: web.Request) -> web.Response:
    """POST /disconnect

    Disconnect from the currently connected BLE peripheral.
    """
    await ble.disconnect()
    return web.json_response({"status": "disconnected"})


async def handle_send(request: web.Request) -> web.Response:
    """POST /send {"data": ">cmd:start"} or {"hex": "000568656c6c6f"}

    Send data to the BLE peripheral. Accepts a plain string under the
    ``data`` key (sent raw, not framed) or hex-encoded bytes under ``hex``.
    """
    body = await request.json()

    if "hex" in body:
        data = bytes.fromhex(body["hex"])
        ok = await ble.send(data)
    elif "data" in body:
        ok = await ble.send_raw(body["data"])
    else:
        return web.json_response({"error": "data or hex required"}, status=400)

    if ok:
        return web.json_response({"status": "sent"})
    return web.json_response({"error": "send failed"}, status=500)


async def handle_status(request: web.Request) -> web.Response:
    """GET /status

    Returns the current BLE connection state, MTU, device info,
    and buffered message count.
    """
    return web.json_response({
        "connected": ble.connected,
        "mtu": ble.mtu,
        "device": ble.device.name if ble.device else None,
        "addr": ble.device.address if ble.device else None,
        "messages_buffered": len(history),
    })


async def handle_history(request: web.Request) -> web.Response:
    """GET /history?limit=50&direction=rx

    Returns recent BLE messages from the in-memory ring buffer.
    Supports optional direction filtering and limit.
    """
    limit = int(request.query.get("limit", "50"))
    direction = request.query.get("direction")
    msgs = history.get_recent(limit, direction=direction)
    return web.json_response({
        "messages": [
            {
                "payload": m.payload,
                "direction": getattr(m, "direction", "unknown"),
                "timestamp": m.timestamp,
            }
            for m in msgs
        ],
        "total": len(history),
    })


async def handle_ws(request: web.Request) -> web.WebSocketResponse:
    """WS /ws — real-time BLE data stream.

    Each connected client receives JSON-encoded BLE data messages
    broadcast by ``broadcast_ws``. The loop simply drains incoming
    WS messages (no client-to-server protocol planned yet).
    """
    ws = web.WebSocketResponse()
    await ws.prepare(request)
    ws_clients.append(ws)
    log.info("WebSocket client connected (%d total)", len(ws_clients))
    try:
        async for msg in ws:
            pass
    finally:
        ws_clients.remove(ws)
        log.info("WebSocket client disconnected (%d total)", len(ws_clients))
    return ws


# ── App factory ────────────────────────────────────────────

def create_app() -> web.Application:
    """Build and return a configured aiohttp Application.

    Injects ``on_ble_data`` as the BLE data callback before
    registering all REST and WebSocket routes.
    """
    ble._on_data = on_ble_data

    app = web.Application()
    app.router.add_get("/scan", handle_scan)
    app.router.add_post("/connect", handle_connect)
    app.router.add_post("/disconnect", handle_disconnect)
    app.router.add_post("/send", handle_send)
    app.router.add_get("/status", handle_status)
    app.router.add_get("/history", handle_history)
    app.router.add_get("/ws", handle_ws)
    return app
