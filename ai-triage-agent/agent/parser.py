import re
from dataclasses import dataclass, field


@dataclass
class ParsedCrashLog:
    signal_name:  str | None       = None
    fault_address: str | None      = None
    stack_frames: list[str]        = field(default_factory=list)
    top_frame:    str | None       = None
    indicators:   list[str]        = field(default_factory=list)


def parse_crash_log(raw_log: str) -> ParsedCrashLog:
    if not raw_log:
        return ParsedCrashLog()

    result = ParsedCrashLog()
    lines = raw_log.splitlines()

    _extract_signal(raw_log, result)
    _extract_fault_address(raw_log, result)
    _extract_stack_frames(lines, result)
    _extract_indicators(raw_log, result)

    return result


def _extract_signal(log: str, result: ParsedCrashLog) -> None:
    for sig in ("SIGSEGV", "SIGABRT", "SIGBUS", "SIGFPE", "SIGILL"):
        if sig in log:
            result.signal_name = sig
            return


def _extract_fault_address(log: str, result: ParsedCrashLog) -> None:
    patterns = [
        r"fault address\s+(0x[0-9a-fA-F]+)",
        r"segfault at\s+(0x[0-9a-fA-F]+|\d+)",
        r"address\s+(0x[0-9a-fA-F]+)",
    ]
    for pattern in patterns:
        m = re.search(pattern, log, re.IGNORECASE)
        if m:
            result.fault_address = m.group(1)
            return


def _extract_stack_frames(lines: list[str], result: ParsedCrashLog) -> None:
    for line in lines:
        # GDB-style: "#0  0x... in function_name"
        m = re.match(r"#\d+\s+(?:0x[0-9a-f]+\s+in\s+)?(\S+(?:::\S+)?)", line.strip())
        if m:
            frame = m.group(1).rstrip("()")
            result.stack_frames.append(frame)

    if result.stack_frames:
        result.top_frame = result.stack_frames[0]


def _extract_indicators(log: str, result: ParsedCrashLog) -> None:
    checks = [
        ("null_address",     lambda: bool(result.fault_address and re.match(r"0x0+$", result.fault_address))),
        ("heap_corruption",  lambda: bool(re.search(r"double free|corrupted size|corrupted prev_size|corrupted double-linked list", log, re.IGNORECASE))),
        ("use_after_free",   lambda: bool(re.search(r"use.?after.?free", log, re.IGNORECASE))),
        ("assertion_failed", lambda: bool(re.search(r"assertion.*failed|assert\s*\(|\bAbort trap\b", log, re.IGNORECASE))),
        ("stack_overflow",   lambda: bool(re.search(r"stack.*overflow|infinite recursion|maximum recursion depth", log, re.IGNORECASE))),
        ("oom",              lambda: bool(re.search(r"out of memory|oom[- ]kill|killed process", log, re.IGNORECASE))),
    ]
    for name, check in checks:
        try:
            if check():
                result.indicators.append(name)
        except Exception:
            pass
