import logging
import requests
from agent.analyzer import AnalysisResult

log = logging.getLogger(__name__)


class BackendClient:
    def __init__(self, backend_url: str, api_key: str, timeout: int = 10):
        self.backend_url = backend_url.rstrip("/")
        self._session = requests.Session()
        self._session.headers.update({
            "Content-Type": "application/json",
            "X-Api-Key": api_key,
        })
        self._timeout = timeout

    def fetch_pending_faults(self, limit: int = 10) -> list[dict]:
        resp = self._session.get(
            f"{self.backend_url}/api/faults",
            params={"status": "pending", "limit": limit},
            timeout=self._timeout,
        )
        resp.raise_for_status()
        return resp.json()

    def fetch_fault_detail(self, fault_id: str) -> dict:
        resp = self._session.get(
            f"{self.backend_url}/api/faults/{fault_id}",
            timeout=self._timeout,
        )
        resp.raise_for_status()
        return resp.json()

    def update_status(self, fault_id: str, status: str) -> None:
        resp = self._session.patch(
            f"{self.backend_url}/api/faults/{fault_id}/status",
            json={"status": status},
            timeout=self._timeout,
        )
        resp.raise_for_status()

    def post_report(self, payload: dict) -> dict:
        resp = self._session.post(
            f"{self.backend_url}/api/reports",
            json=payload,
            timeout=self._timeout,
        )
        resp.raise_for_status()
        return resp.json()


class Reporter:
    def __init__(self, client: BackendClient):
        self._client = client

    def submit(self, fault_id: str, result: AnalysisResult) -> None:
        payload = {
            "faultEventId":  fault_id,
            "rootCause":     result.root_cause,
            "confidence":    result.confidence,
            "affectedModule": result.affected_module,
            "recommendation": result.recommendation,
            "rawAnalysis":   result.raw_analysis,
        }
        self._client.post_report(payload)
        log.debug("report submitted for fault %s", fault_id)
