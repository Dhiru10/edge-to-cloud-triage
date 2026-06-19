namespace WatchdogLambda.Models;

public sealed class FaultEvent
{
    public string   Id                   { get; set; } = "";
    public string   DeviceId             { get; set; } = "";
    public string   FaultType            { get; set; } = "";
    public string?  ProcessName          { get; set; }
    public string   Status               { get; set; } = "";
    public DateTime? ProcessingStartedAt { get; set; }
    public DateTime  OccurredAt          { get; set; }
}
