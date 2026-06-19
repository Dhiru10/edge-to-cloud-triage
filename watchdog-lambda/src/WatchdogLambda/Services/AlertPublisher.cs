using Amazon.CloudWatch;
using Amazon.CloudWatch.Model;
using Amazon.SimpleNotificationService;
using Amazon.SimpleNotificationService.Model;
using WatchdogLambda.Models;

namespace WatchdogLambda.Services;

public sealed class AlertPublisher : IAlertPublisher
{
    private readonly WatchdogConfig                    _config;
    private readonly IAmazonCloudWatch                 _cw;
    private readonly IAmazonSimpleNotificationService  _sns;

    public AlertPublisher(WatchdogConfig config)
        : this(config, new AmazonCloudWatchClient(), new AmazonSimpleNotificationServiceClient()) { }

    internal AlertPublisher(WatchdogConfig config, IAmazonCloudWatch cw, IAmazonSimpleNotificationService sns)
    {
        _config = config;
        _cw     = cw;
        _sns    = sns;
    }

    public async Task PublishMetricAsync(int staleCount, CancellationToken ct = default)
    {
        await _cw.PutMetricDataAsync(new PutMetricDataRequest
        {
            Namespace  = _config.CloudWatchNamespace,
            MetricData =
            [
                new MetricDatum
                {
                    MetricName = "StaleEvents",
                    Value      = staleCount,
                    Unit       = StandardUnit.Count,
                    Timestamp  = DateTime.UtcNow,
                }
            ]
        }, ct);
    }

    public async Task PublishAlertAsync(int staleCount, int resetCount, CancellationToken ct = default)
    {
        if (string.IsNullOrEmpty(_config.SnsTopicArn)) return;

        await _sns.PublishAsync(new PublishRequest
        {
            TopicArn = _config.SnsTopicArn,
            Subject  = $"[EdgeTriage] {staleCount} stale triage job(s) detected",
            Message  = $"{staleCount} fault event(s) were stuck in 'processing' beyond the " +
                       $"{_config.StaleThresholdMinutes}-minute threshold. " +
                       $"{resetCount} were reset to 'pending' for retry.",
        }, ct);
    }
}
