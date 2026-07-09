"""BLE connection manager using bleak. Async throughout."""

import asyncio
import logging
from typing import Callable

from bleak import BleakClient, BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData

from nus import NusUUIDs, NusMessage, parse_frame

log = logging.getLogger(__name__)


class BleManager:
    """Async BLE manager for Nordic UART Service (NUS).

    Handles scanning, connecting, subscribing to notifications,
    and sending data to a NUS peripheral.
    """

    def __init__(self, on_data: Callable[[NusMessage], None] | None = None):
        self.client: BleakClient | None = None
        self.device: BLEDevice | None = None
        # Callback invoked when a parsed NUS message arrives via TX notify.
        self._on_data = on_data
        self._connected = False
        # Default MTU; updated after negotiate if the peripheral supports larger.
        self._mtu = 23

    @property
    def connected(self) -> bool:
        return self._connected

    @property
    def mtu(self) -> int:
        return self._mtu

    async def scan(self, timeout: float = 5.0, name_filter: str | None = None) -> list[dict]:
        """Scan for BLE devices.

        Uses a detection callback so results accumulate without polling.
        bleak's BleakScanner can also work as an async iterator, but a
        callback avoids per-advertisement wake-up overhead.

        Args:
            timeout: Seconds to scan.
            name_filter: Optional device name exact-match filter.

        Returns:
            List of dicts with keys name, addr, rssi.
        """
        found: list[dict] = []

        def on_detect(device: BLEDevice, adv: AdvertisementData):
            if name_filter and device.name != name_filter:
                return
            found.append({
                "name": device.name or "Unknown",
                "addr": device.address,
                "rssi": adv.rssi,
            })

        scanner = BleakScanner(detection_callback=on_detect)
        await scanner.start()
        await asyncio.sleep(timeout)
        await scanner.stop()
        return found

    async def connect(self, addr: str, name: str | None = None) -> bool:
        """Connect to a BLE device and discover NUS services.

        Steps:
        1. Resolve address to a BLEDevice via scanning.
        2. Instantiate BleakClient with a disconnect callback.
        3. Connect, negotiate MTU, discover services, subscribe to TX.

        Args:
            addr: BLE MAC address.
            name: Optional device name (used only for logging).

        Returns:
            True on success, False on failure.
        """
        try:
            # BleakScanner.find_device_by_address reuses cached scan results
            # which avoids a full re-scan if recently scanned.
            self.device = await BleakScanner.find_device_by_address(addr, timeout=5.0)
            if not self.device:
                log.error("Device not found: %s", addr)
                return False

            self.client = BleakClient(
                self.device,
                disconnected_callback=self._on_disconnect,
            )
            await self.client.connect()
            self._connected = True

            # Negotiate MTU — larger MTU reduces overhead of multi-frame sends.
            # bleak calls request_mtu which wraps the GATT Exchange MTU proc.
            try:
                self._mtu = await self.client.request_mtu(256)
            except Exception:
                self._mtu = 23

            # Force service discovery so we can find NUS characteristics.
            await self.client.get_services()

            # Subscribe to TX characteristic notifications.
            await self._subscribe_tx()
            log.info("Connected to %s (MTU=%d)", name or addr, self._mtu)
            return True

        except Exception as e:
            log.error("Connect failed: %s", e)
            self._connected = False
            return False

    async def _subscribe_tx(self):
        """Subscribe to NUS TX characteristic notifications.

        The TX characteristic uses "notify" (CCCD), not "indicate".
        bleak's start_notify writes the CCCD descriptor automatically.
        """
        if not self.client:
            return

        # Callback receives raw bytearray; parse into NusMessage before
        # forwarding to the user-supplied handler.
        def on_tx(data: bytearray):
            msg = parse_frame(bytes(data))
            if msg and self._on_data:
                self._on_data(msg)

        tx_uuid = NusUUIDs.TX_CHAR
        await self.client.start_notify(tx_uuid, on_tx)
        log.info("Subscribed to NUS TX notifications")

    async def subscribe(self) -> bool:
        """Subscribe to NUS TX notifications.

        Public wrapper around _subscribe_tx(). Must be called after
        connect() to begin receiving data from the peripheral.

        Returns:
            True if subscription was initiated, False if not connected.
        """
        if not self.client or not self._connected:
            return False
        await self._subscribe_tx()
        return True

    async def send(self, data: str | bytes) -> bool:
        """Send data to NUS RX characteristic.

        Strings are wrapped in a length-prefixed JSON frame before
        writing.  Bytes are assumed to be pre-framed and sent as-is.

        Args:
            data: String (framed automatically) or pre-framed bytes.

        Returns:
            True on success, False on failure.
        """
        if not self.client or not self._connected:
            log.error("Not connected")
            return False

        try:
            if isinstance(data, str):
                # Frame raw text: encode → NusMessage → serialize_frame.
                msg = NusMessage(payload={"data": data})
                from nus import serialize_frame
                data = serialize_frame(msg)
            # bytes are sent as-is (caller assumes framing responsibility)

            await self.client.write_gatt_char(NusUUIDs.RX_CHAR, data)
            return True
        except Exception as e:
            log.error("Send failed: %s", e)
            return False

    async def send_raw(self, text: str) -> bool:
        """Send raw text (no frame wrapping) to NUS RX.

        Useful for ad-hoc terminal-style output where the remote side
        does not expect framed messages.

        Args:
            text: Raw text to send.

        Returns:
            True on success, False on failure.
        """
        if not self.client or not self._connected:
            return False
        try:
            await self.client.write_gatt_char(NusUUIDs.RX_CHAR, text.encode("utf-8"))
            return True
        except Exception as e:
            log.error("Send raw failed: %s", e)
            return False

    async def disconnect(self):
        """Disconnect from current device."""
        if self.client and self._connected:
            try:
                await self.client.disconnect()
            finally:
                self._connected = False
                self.client = None
                self.device = None
        else:
            self._connected = False
            self.client = None
            self.device = None

    def _on_disconnect(self, client: BleakClient):
        """Internal disconnect callback passed to BleakClient.

        This may be called by bleak even if we initiated disconnect,
        so we just update state and log a warning.
        """
        self._connected = False
        log.warning("BLE disconnected")
