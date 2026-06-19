using WatchdogLambda.Models;

namespace WatchdogLambda.Services;

public interface IBackendClient
{
    Task<List<FaultEvent>> GetStaleFaultsAsync(DateTime staleSince, CancellationToken ct = default);
    Task ResetFaultAsync(string faultId, CancellationToken ct = default);
}
