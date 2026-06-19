import pytest
from agent.parser import parse_crash_log


SIGSEGV_NULL = """
Program received signal SIGSEGV, Segmentation fault.
fault address 0x0000000000000000
#0  0x000055a1b2c3d4e5 in parse_frame ()
#1  0x000055a1b2c3f000 in main ()
"""

SIGSEGV_HEAP = """
Program received signal SIGSEGV
fault address 0x00007fa1b2000040
#0  0x00007fa1 in process_buffer ()
"""

SIGABRT_DOUBLE_FREE = """
free(): double free detected in tcache 2
SIGABRT
Aborted (core dumped)
"""

OOM_LOG = """
Out of memory: Killed process 4512 (worker) total-vm:204800kB
"""

ASSERTION_LOG = """
assertion "ptr != NULL" failed: file "parser.c", line 84
SIGABRT
"""


def test_null_deref_signal_and_address():
    result = parse_crash_log(SIGSEGV_NULL)
    assert result.signal_name == "SIGSEGV"
    assert result.fault_address == "0x0000000000000000"
    assert "null_address" in result.indicators


def test_null_deref_stack_frames():
    result = parse_crash_log(SIGSEGV_NULL)
    assert len(result.stack_frames) >= 1
    assert result.top_frame is not None


def test_heap_sigsegv_no_null_indicator():
    result = parse_crash_log(SIGSEGV_HEAP)
    assert result.signal_name == "SIGSEGV"
    assert "null_address" not in result.indicators


def test_double_free_detected():
    result = parse_crash_log(SIGABRT_DOUBLE_FREE)
    assert "heap_corruption" in result.indicators


def test_oom_detected():
    result = parse_crash_log(OOM_LOG)
    assert "oom" in result.indicators


def test_assertion_failed_detected():
    result = parse_crash_log(ASSERTION_LOG)
    assert "assertion_failed" in result.indicators


def test_empty_log_returns_empty_result():
    result = parse_crash_log("")
    assert result.signal_name is None
    assert result.stack_frames == []
    assert result.indicators == []


def test_none_log_handled():
    result = parse_crash_log(None)
    assert result.signal_name is None
