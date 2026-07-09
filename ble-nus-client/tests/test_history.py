import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from history import MessageHistory
from nus import NusMessage


def test_add_and_get():
    h = MessageHistory(max_size=10)
    h.add(NusMessage(payload={"type": "imu", "v": 1}))
    recent = h.get_recent(1)
    assert len(recent) == 1
    assert recent[0].payload["v"] == 1


def test_max_size_eviction():
    h = MessageHistory(max_size=3)
    for i in range(5):
        h.add(NusMessage(payload={"i": i}))
    assert len(h.get_recent(10)) == 3
    assert h.get_recent(10)[0].payload["i"] == 2  # oldest kept


def test_get_recent_empty():
    h = MessageHistory(max_size=10)
    assert h.get_recent(5) == []


def test_get_all():
    h = MessageHistory(max_size=100)
    for i in range(5):
        h.add(NusMessage(payload={"i": i}))
    assert len(h.get_all()) == 5


def test_direction_filter():
    h = MessageHistory(max_size=10)
    msg1 = NusMessage(payload={"type": "imu"}, raw=b"\x00\x03{}")
    msg2 = NusMessage(payload={"type": "cmd"}, raw=b"\x00\x03{}")
    msg2.direction = "rx"
    h.add(msg1)
    h.add(msg2)
    rx = h.get_recent(10, direction="rx")
    assert len(rx) == 1
