"""Ring buffer for BLE NUS messages."""

import time
from collections import deque
from nus import NusMessage


class MessageHistory:
    """Thread-safe ring buffer of NusMessage objects with optional direction filtering.

    Maintains an in-memory fixed-size history. New messages push out
    the oldest ones when the buffer is full.
    """

    def __init__(self, max_size: int = 100):
        """Initialize with a maximum capacity.

        Args:
            max_size: Maximum number of messages to retain.
        """
        self._buffer: deque[NusMessage] = deque(maxlen=max_size)

    def add(self, msg: NusMessage) -> None:
        """Append a message. Auto-stamps with current time if timestamp is 0.

        Args:
            msg: The NUS message to store.
        """
        if msg.timestamp == 0.0:
            msg.timestamp = time.time()
        self._buffer.append(msg)

    def get_recent(self, n: int = 50, direction: str | None = None) -> list[NusMessage]:
        """Return the *n* most recent messages, newest last.

        Args:
            n: Number of messages to return.
            direction: If set, only return messages whose ``.direction``
                       attribute matches this value (e.g. ``"rx"``, ``"tx"``).

        Returns:
            List of up to *n* messages meeting the filter criteria.
        """
        items = list(self._buffer)
        if direction:
            items = [m for m in items if getattr(m, "direction", None) == direction]
        return items[-n:]

    def get_all(self) -> list[NusMessage]:
        """Return every message in the buffer (oldest first)."""
        return list(self._buffer)

    def clear(self) -> None:
        """Remove all messages from the buffer."""
        self._buffer.clear()

    def __len__(self) -> int:
        return len(self._buffer)
