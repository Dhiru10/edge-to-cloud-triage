import pytest
from agent.parser import ParsedCrashLog
from agent.analyzer import analyze, AnalysisResult


def fault(fault_type="SIGSEGV", process_name="test-proc", exit_code=-1):
    return {
        "id": "test-uuid",
        "faultType": fault_type,
        "processName": process_name,
        "exitCode": exit_code,
    }


def test_null_deref_high_confidence():
    parsed = ParsedCrashLog(
        signal_name="SIGSEGV",
        fault_address="0x0000000000000000",
        stack_frames=["parse_frame", "main"],
        top_frame="parse_frame",
        indicators=["null_address"],
    )
    result = analyze(fault("SIGSEGV"), parsed)
    assert result.confidence == "high"
    assert result.matched_pattern == "null_deref"
    assert "null" in result.root_cause.lower()


def test_null_deref_includes_frame_in_cause():
    parsed = ParsedCrashLog(
        signal_name="SIGSEGV",
        fault_address="0x0000000000000000",
        stack_frames=["my_func"],
        top_frame="my_func",
        indicators=["null_address"],
    )
    result = analyze(fault("SIGSEGV"), parsed)
    assert "my_func" in result.root_cause


def test_oom_high_confidence():
    parsed = ParsedCrashLog(indicators=["oom"])
    result = analyze(fault("OOM"), parsed)
    assert result.confidence == "high"
    assert result.matched_pattern == "oom_kill"


def test_heap_corruption_high_confidence():
    parsed = ParsedCrashLog(indicators=["heap_corruption"])
    result = analyze(fault("SIGABRT"), parsed)
    assert result.confidence == "high"
    assert result.matched_pattern == "heap_corruption"


def test_use_after_free_high_confidence():
    parsed = ParsedCrashLog(indicators=["use_after_free"])
    result = analyze(fault("SIGSEGV"), parsed)
    assert result.confidence == "high"
    assert result.matched_pattern == "use_after_free"


def test_generic_segfault_medium_confidence():
    parsed = ParsedCrashLog(
        signal_name="SIGSEGV",
        fault_address="0x00007fa100000040",
        indicators=[],
    )
    result = analyze(fault("SIGSEGV"), parsed)
    assert result.confidence == "medium"
    assert result.matched_pattern == "segfault_generic"


def test_unknown_low_confidence():
    parsed = ParsedCrashLog()
    result = analyze(fault("UNKNOWN"), parsed)
    assert result.confidence == "low"
    assert result.matched_pattern == "unknown"


def test_raw_analysis_always_populated():
    parsed = ParsedCrashLog(indicators=["null_address"], signal_name="SIGSEGV",
                             fault_address="0x0", stack_frames=["fn"])
    result = analyze(fault("SIGSEGV"), parsed)
    assert "matchedPattern" in result.raw_analysis
    assert "patternVersion" in result.raw_analysis
    assert result.raw_analysis["stackDepth"] == 1


def test_affected_module_falls_back_to_process_name():
    parsed = ParsedCrashLog()
    result = analyze(fault("PROCESS_EXIT", process_name="my-daemon"), parsed)
    assert result.affected_module == "my-daemon"
