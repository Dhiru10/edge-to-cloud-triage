namespace WatchdogLambda.Models;

public sealed class WatchdogConfig
{
    public string BackendUrl            { get; init; } = "";
    public string ApiKey                { get; init; } = "";
    public int    StaleThresholdMinutes { get; init; } = 10;
    public int    AlertThreshold        { get; init; } = 5;
    public string SnsTopicArn           { get; init; } = "";
    public string CloudWatchNamespace   { get; init; } = "TriageSystem";

    public static WatchdogConfig FromEnvironment() => new()
    {
        BackendUrl            = Env("BACKEND_URL"),
        ApiKey                = Env("API_KEY"),
        StaleThresholdMinutes = int.Parse(Env("STALE_THRESHOLD_MINUTES", "10")),
        AlertThreshold        = int.Parse(Env("ALERT_THRESHOLD", "5")),
        SnsTopicArn           = Env("SNS_TOPIC_ARN"),
    };

    private static string Env(string key, string fallback = "") =>
        Environment.GetEnvironmentVariable(key) ?? fallback;
}
