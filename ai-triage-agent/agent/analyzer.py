from dataclasses import dataclass, field
from agent.parser import ParsedCrashLog


@dataclass
class AnalysisResult:
    root_cause:      str
    confidence:      str            # high | medium | low
    affected_module: str | None
    recommendation:  str
    matched_pattern: str
    raw_analysis:    dict = field(default_factory=dict)


def analyze(fault_event: dict, parsed: ParsedCrashLog) -> AnalysisResult:
    fault_type   = fault_event.get("faultType",   "UNKNOWN")
    process_name = fault_event.get("processName", "unknown")

    result = _match_pattern(fault_type, parsed)

    if not result.affected_module:
        if parsed.top_frame:
            result.affected_module = f"{process_name}/{parsed.top_frame}"
        else:
            result.affected_module = process_name

    result.raw_analysis.update({
        "signalName":     parsed.signal_name,
        "faultAddress":   parsed.fault_address,
        "topFrame":       parsed.top_frame,
        "stackDepth":     len(parsed.stack_frames),
        "indicators":     parsed.indicators,
        "patternVersion": "1.0",
    })

    return result


def _match_pattern(fault_type: str, parsed: ParsedCrashLog) -> AnalysisResult:
    ind = set(parsed.indicators)

    if "oom" in ind:
        return AnalysisResult(
            root_cause="Process killed by OOM killer due to memory exhaustion.",
            confidence="high",
            affected_module=None,
            recommendation="Profile heap allocations. Check for unbounded data structure growth or memory leaks.",
            matched_pattern="oom_kill",
            raw_analysis={"matchedPattern": "oom_kill"},
        )

    if "null_address" in ind and fault_type == "SIGSEGV":
        frame = parsed.top_frame
        loc = f" in {frame}()" if frame else ""
        return AnalysisResult(
            root_cause=f"Null pointer dereference{loc}. A pointer was used before initialisation or after being set to null.",
            confidence="high",
            affected_module=None,
            recommendation=f"Add null check before dereferencing pointer{' in ' + frame if frame else ''}. Verify all initialisation paths.",
            matched_pattern="null_deref",
            raw_analysis={"matchedPattern": "null_deref"},
        )

    if "use_after_free" in ind:
        return AnalysisResult(
            root_cause="Use-after-free memory corruption. Memory was accessed after being deallocated.",
            confidence="high",
            affected_module=None,
            recommendation="Run with AddressSanitizer (ASAN) to identify the allocation and free site. Verify object ownership semantics.",
            matched_pattern="use_after_free",
            raw_analysis={"matchedPattern": "use_after_free"},
        )

    if "heap_corruption" in ind:
        return AnalysisResult(
            root_cause="Heap corruption detected — double free or corrupted allocator metadata.",
            confidence="high",
            affected_module=None,
            recommendation="Check for double-free bugs. Run with ASAN or Valgrind to trace the allocation site.",
            matched_pattern="heap_corruption",
            raw_analysis={"matchedPattern": "heap_corruption"},
        )

    if "assertion_failed" in ind:
        return AnalysisResult(
            root_cause="Assertion failure. A runtime invariant was violated.",
            confidence="high",
            affected_module=None,
            recommendation="Review the assertion condition. Check caller inputs and function preconditions.",
            matched_pattern="assertion_failed",
            raw_analysis={"matchedPattern": "assertion_failed"},
        )

    if "stack_overflow" in ind:
        return AnalysisResult(
            root_cause="Stack overflow, likely from unbounded recursion or excessive stack allocation.",
            confidence="medium",
            affected_module=None,
            recommendation="Add a recursion depth limit or convert to an iterative approach. Verify base cases.",
            matched_pattern="stack_overflow",
            raw_analysis={"matchedPattern": "stack_overflow"},
        )

    if fault_type == "SIGSEGV" and parsed.fault_address:
        return AnalysisResult(
            root_cause=f"Segmentation fault at address {parsed.fault_address}. Invalid memory access — possible out-of-bounds pointer.",
            confidence="medium",
            affected_module=None,
            recommendation="Run under GDB or ASAN to isolate the invalid access. Check pointer arithmetic and array bounds.",
            matched_pattern="segfault_generic",
            raw_analysis={"matchedPattern": "segfault_generic"},
        )

    if fault_type in ("OOM", "TIMEOUT"):
        label = "out-of-memory" if fault_type == "OOM" else "timeout"
        return AnalysisResult(
            root_cause=f"Process terminated due to {label} condition.",
            confidence="medium",
            affected_module=None,
            recommendation="Profile resource usage. Check for leaks or unbounded operations.",
            matched_pattern=f"{fault_type.lower()}_event",
            raw_analysis={"matchedPattern": f"{fault_type.lower()}_event"},
        )

    if fault_type == "PROCESS_EXIT":
        return AnalysisResult(
            root_cause="Process exited unexpectedly. Insufficient crash data for precise diagnosis.",
            confidence="low",
            affected_module=None,
            recommendation="Check application logs for errors before exit. Enable core dumps for deeper analysis.",
            matched_pattern="process_exit",
            raw_analysis={"matchedPattern": "process_exit"},
        )

    return AnalysisResult(
        root_cause=f"Process terminated with fault type '{fault_type}'. Insufficient log data for precise root cause determination.",
        confidence="low",
        affected_module=None,
        recommendation="Collect a full crash dump or enable verbose logging to gather more diagnostic data.",
        matched_pattern="unknown",
        raw_analysis={"matchedPattern": "unknown"},
    )
