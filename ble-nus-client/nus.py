"""NUS (Nordic UART Service) protocol: UUIDs, frame parser, serializer."""

from dataclasses import dataclass, field
from typing import Any
import json


# ── NUS UUIDs (configurable defaults) ─────────────────────
class NusUUIDs:
    SERVICE  = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    TX_CHAR  = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"  # device → client (notify)
    RX_CHAR  = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"  # client → device (write)
    CCCD     = "00002902-0000-1000-8000-00805F9B34FB"


# ── Frame format: 2-byte BE length + JSON payload ─────────
@dataclass
class NusMessage:
    payload: dict[str, Any] = field(default_factory=dict)
    raw: bytes = b""
    timestamp: float = 0.0
    length: int = 0


def parse_frame(data: bytes) -> NusMessage | None:
    """Parse a 2-byte length-prefixed JSON frame.

    Returns NusMessage on success, None on parse error.
    """
    if len(data) < 2:
        return None

    payload_len = (data[0] << 8) | data[1]
    if payload_len == 0 or payload_len > len(data) - 2:
        return None

    try:
        payload_str = data[2 : 2 + payload_len].decode("utf-8")
        payload = json.loads(payload_str)
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None

    return NusMessage(payload=payload, raw=data[: 2 + payload_len], length=payload_len)


def serialize_frame(msg: NusMessage) -> bytes:
    """Serialize a NusMessage to 2-byte length-prefixed JSON."""
    payload_bytes = json.dumps(msg.payload, separators=(",", ":")).encode("utf-8")
    length = len(payload_bytes)
    return bytes([(length >> 8) & 0xFF, length & 0xFF]) + payload_bytes
