#!/usr/bin/env python3
"""BLE NUS Debugger — entry point.

Usage:
    python main.py --api              # HTTP API on :8091
    python main.py --cli              # Interactive CLI
    python main.py --cli --json       # CLI with JSON output (for agents)
    python main.py --scan             # Quick scan and exit
    python main.py --scan --name track-kinesis
"""

import argparse
import asyncio
import json
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    stream=sys.stderr,
)


def main():
    """Parse args and dispatch to the selected sub-mode (scan, api, or cli)."""
    parser = argparse.ArgumentParser(description="BLE NUS Debugger for T470")

    # Mutually exclusive mode group — exactly one is required.
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--api", action="store_true", help="Start HTTP API server on :8091")
    group.add_argument("--cli", action="store_true", help="Start interactive CLI")
    group.add_argument("--scan", action="store_true", help="Scan for BLE devices and exit")

    # Optional flags shared across modes.
    parser.add_argument("--port", type=int, default=8091, help="API port (default: 8091)")
    parser.add_argument("--json", action="store_true", help="JSON output mode (CLI)")
    parser.add_argument("--name", type=str, help="Filter scan by device name")
    parser.add_argument("--timeout", type=float, default=5.0, help="Scan timeout in seconds")

    args = parser.parse_args()

    if args.scan:
        _run_scan(args)
    elif args.api:
        _run_api(args)
    elif args.cli:
        _run_cli(args)


def _run_scan(args):
    """Run a BLE scan, print results as JSON, and exit."""
    from ble_conn import BleManager

    async def scan():
        ble = BleManager()
        results = await ble.scan(timeout=args.timeout, name_filter=args.name)
        print(json.dumps(results, indent=2))
    asyncio.run(scan())


def _run_api(args):
    """Start the aiohttp HTTP API server on the specified port."""
    from api import create_app
    from aiohttp import web

    app = create_app()
    print(f"BLE NUS API running on http://localhost:{args.port}", file=sys.stderr)
    web.run_app(app, host="0.0.0.0", port=args.port, print=None)


def _run_cli(args):
    """Launch the interactive CLI (stdin-driven, async event loop)."""
    from cli import Cli

    async def run():
        cli = Cli(json_mode=args.json)
        await cli.run()
    asyncio.run(run())


if __name__ == "__main__":
    main()
