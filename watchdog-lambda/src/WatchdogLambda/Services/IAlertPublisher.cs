namespace WatchdogLambda.Services;

public interface IAlertPublisher
{
    Task PublishMetricAsync(int staleCount, CancellationToken ct = default);
    Task PublishAlertAsync(int staleCount, int resetCount, CancellationToken ct = default);
}
