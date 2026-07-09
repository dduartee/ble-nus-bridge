import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from nus import parse_frame, serialize_frame, NusMessage


def test_parse_valid_frame():
    payload = b'{"type":"imu","app":"track","ax":0.1}'
    frame = bytes([0, len(payload)]) + payload
    msg = parse_frame(frame)
    assert msg is not None
    assert msg.length == len(payload)
    assert msg.payload["type"] == "imu"
    assert msg.payload["ax"] == 0.1


def test_parse_too_short():
    assert parse_frame(b"\x00") is None
    assert parse_frame(b"") is None


def test_parse_length_mismatch():
    frame = bytes([0, 10]) + b'{"short":1}'
    assert parse_frame(frame) is None


def test_parse_invalid_json():
    frame = bytes([0, 5]) + b"hello"
    assert parse_frame(frame) is None


def test_serialize_roundtrip():
    msg = NusMessage(payload={"type": "cmd", "app": "test", "action": "start"})
    raw = serialize_frame(msg)
    parsed = parse_frame(raw)
    assert parsed.payload == msg.payload


def test_serialize_empty_payload():
    msg = NusMessage(payload={})
    raw = serialize_frame(msg)
    assert raw[0] == 0 and raw[1] == 2  # length=2 (just "{}")
