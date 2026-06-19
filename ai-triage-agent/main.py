import logging
import os
import time

from agent.parser import parse_crash_log
from agent.analyzer import analyze
from agent.reporter import BackendClient, Reporter

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
log = logging.getLogger(__name__)


def main() -> None:
    client = BackendClient(
        backend_url=os.environ.get("BACKEND_URL", "http://localhost:8080"),
        api_key=os.environ.get("API_KEY", "dev-api-key-change-in-prod"),
    )
    reporter = Reporter(client)
    poll_interval = int(os.environ.get("POLL_INTERVAL_SECONDS", "10"))

    log.info("triage agent starting — backend=%s  poll=%ds", client.backend_url, poll_interval)

    while True:
        try:
            _process_pending(client, reporter)
        except Exception as ex:
            log.error("unexpected error in poll cycle: %s", ex)
        time.sleep(poll_interval)


def _process_pending(client: BackendClient, reporter: Reporter) -> None:
    summaries = client.fetch_pending_faults()
    if not summaries:
        return

    log.info("found %d pending fault event(s)", len(summaries))

    for summary in summaries:
        fault_id = summary["id"]
        try:
            client.update_status(fault_id, "processing")

            fault = client.fetch_fault_detail(fault_id)
            parsed = parse_crash_log(fault.get("rawLog") or "")
            result = analyze(fault, parsed)

            reporter.submit(fault_id, result)
            log.info(
                "triaged %s [%s/%s] pattern=%s",
                fault_id[:8],
                fault.get("faultType", "?"),
                result.confidence,
                result.matched_pattern,
            )

        except Exception as ex:
            log.error("failed to triage fault %s: %s", fault_id, ex)
            try:
                client.update_status(fault_id, "failed")
            except Exception as patch_ex:
                log.warning("could not mark fault %s as failed: %s", fault_id, patch_ex)


if __name__ == "__main__":
    main()
