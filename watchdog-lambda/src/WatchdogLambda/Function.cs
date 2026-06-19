using Amazon.Lambda.Core;
using Amazon.Lambda.Serialization.SystemTextJson;
using WatchdogLambda.Models;
using WatchdogLambda.Services;

[assembly: LambdaSerializer(typeof(DefaultLambdaJsonSerializer))]

namespace WatchdogLambda;

public sealed class Function
{
    private readonly WatchdogConfig  _config;
    private readonly IBackendClient  _backend;
    private readonly IAlertPublisher _alerts;

    public Function() : this(
        WatchdogConfig.FromEnvironment(),
        backend: null,
        alerts:  null) { }

    internal Function(WatchdogConfig config, IBackendClient? backend, IAlertPublisher? alerts)
    {
        _config  = config;
        _backend = backend ?? new BackendClient(config);
        _alerts  = alerts  ?? new AlertPublisher(config);
    }

    public async Task FunctionHandler(ILambdaContext context)
    {
        var log = context.Logger;
        log.LogInformation("Watchdog scan starting");

        var staleSince  = DateTime.UtcNow.AddMinutes(-_config.StaleThresholdMinutes);
        var staleFaults = await _backend.GetStaleFaultsAsync(staleSince);

        log.LogInformation($"Found {staleFaults.Count} stale fault event(s)");

        int resetCount = 0;
        foreach (var fault in staleFaults)
        {
            try
            {
                await _backend.ResetFaultAsync(fault.Id);
                resetCount++;
                log.LogInformation($"Reset {fault.Id} ({fault.FaultType} / {fault.ProcessName ?? "?"})");
            }
            catch (Exception ex)
            {
                log.LogError($"Failed to reset {fault.Id}: {ex.Message}");
            }
        }

        await _alerts.PublishMetricAsync(staleFaults.Count);

        if (staleFaults.Count >= _config.AlertThreshold)
        {
            await _alerts.PublishAlertAsync(staleFaults.Count, resetCount);
        }

        log.LogInformation($"Watchdog complete — {resetCount}/{staleFaults.Count} events reset");
    }
}
