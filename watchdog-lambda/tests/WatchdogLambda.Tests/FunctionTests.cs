using Amazon.Lambda.Core;
using WatchdogLambda;
using WatchdogLambda.Models;
using WatchdogLambda.Services;

namespace WatchdogLambda.Tests;

public class FunctionTests
{
    [Fact]
    public async Task WhenNoStaleFaults_MetricPublishedAsZero()
    {
        var backend = new FakeBackendClient([]);
        var alerts  = new FakeAlertPublisher();
        var sut     = MakeFunction(backend, alerts);

        await sut.FunctionHandler(new TestLambdaContext());

        Assert.Equal(0, alerts.LastStaleCount);
        Assert.Empty(backend.ResetIds);
        Assert.True(alerts.MetricPublished);
    }

    [Fact]
    public async Task WhenStaleFaultsExist_AllAreReset()
    {
        var faults = new List<FaultEvent>
        {
            new() { Id = "fault-1", FaultType = "SIGSEGV", Status = "processing" },
            new() { Id = "fault-2", FaultType = "OOM",     Status = "processing" },
        };
        var backend = new FakeBackendClient(faults);
        var alerts  = new FakeAlertPublisher();
        var sut     = MakeFunction(backend, alerts);

        await sut.FunctionHandler(new TestLambdaContext());

        Assert.Equal(2, alerts.LastStaleCount);
        Assert.Contains("fault-1", backend.ResetIds);
        Assert.Contains("fault-2", backend.ResetIds);
    }

    [Fact]
    public async Task WhenCountExceedsThreshold_AlertIsSent()
    {
        var faults = Enumerable.Range(0, 6)
            .Select(i => new FaultEvent { Id = $"f{i}", FaultType = "UNKNOWN", Status = "processing" })
            .ToList();
        var backend = new FakeBackendClient(faults);
        var alerts  = new FakeAlertPublisher();
        var sut     = MakeFunction(backend, alerts, alertThreshold: 5);

        await sut.FunctionHandler(new TestLambdaContext());

        Assert.True(alerts.AlertSent);
    }

    [Fact]
    public async Task WhenResetFails_OtherFaultsStillProcessed()
    {
        var faults = new List<FaultEvent>
        {
            new() { Id = "fault-ok",   FaultType = "SIGSEGV", Status = "processing" },
            new() { Id = "fault-fail", FaultType = "OOM",     Status = "processing" },
        };
        var backend = new FakeBackendClient(faults, failIds: ["fault-fail"]);
        var alerts  = new FakeAlertPublisher();
        var sut     = MakeFunction(backend, alerts);

        await sut.FunctionHandler(new TestLambdaContext());

        Assert.Contains("fault-ok", backend.ResetIds);
        Assert.DoesNotContain("fault-fail", backend.ResetIds);
    }

    private static Function MakeFunction(
        IBackendClient backend,
        IAlertPublisher alerts,
        int alertThreshold = 10)
    {
        var config = new WatchdogConfig
        {
            BackendUrl            = "http://localhost",
            StaleThresholdMinutes = 10,
            AlertThreshold        = alertThreshold,
        };
        return new Function(config, backend, alerts);
    }
}

// ── Test doubles ──────────────────────────────────────────────────────────────

internal sealed class FakeBackendClient : IBackendClient
{
    private readonly List<FaultEvent> _faults;
    private readonly HashSet<string>  _failIds;

    public List<string> ResetIds { get; } = [];

    public FakeBackendClient(List<FaultEvent> faults, IEnumerable<string>? failIds = null)
    {
        _faults  = faults;
        _failIds = failIds is null ? [] : new HashSet<string>(failIds);
    }

    public Task<List<FaultEvent>> GetStaleFaultsAsync(DateTime staleSince, CancellationToken ct = default)
        => Task.FromResult(_faults);

    public Task ResetFaultAsync(string faultId, CancellationToken ct = default)
    {
        if (_failIds.Contains(faultId))
            throw new HttpRequestException($"Simulated failure for {faultId}");
        ResetIds.Add(faultId);
        return Task.CompletedTask;
    }
}

internal sealed class FakeAlertPublisher : IAlertPublisher
{
    public int  LastStaleCount  { get; private set; }
    public bool MetricPublished { get; private set; }
    public bool AlertSent       { get; private set; }

    public Task PublishMetricAsync(int staleCount, CancellationToken ct = default)
    {
        LastStaleCount  = staleCount;
        MetricPublished = true;
        return Task.CompletedTask;
    }

    public Task PublishAlertAsync(int staleCount, int resetCount, CancellationToken ct = default)
    {
        AlertSent = true;
        return Task.CompletedTask;
    }
}

internal sealed class TestLambdaContext : ILambdaContext
{
    public string          AwsRequestId       => "test-request-id";
    public IClientContext? ClientContext       => null;
    public string          FunctionName       => "triage-watchdog-test";
    public string          FunctionVersion    => "1";
    public ICognitoIdentity? Identity         => null;
    public string          InvokedFunctionArn => "arn:aws:lambda:us-east-1:000000000000:function:test";
    public ILambdaLogger   Logger             => new TestLogger();
    public string          LogGroupName       => "/aws/lambda/test";
    public string          LogStreamName      => "test-stream";
    public int             MemoryLimitInMB    => 256;
    public TimeSpan        RemainingTime      => TimeSpan.FromSeconds(25);
}

internal sealed class TestLogger : ILambdaLogger
{
    public void Log(string message)     { }
    public void LogLine(string message) { }
    public void LogInformation(string message) { }
    public void LogError(string message)       { }
}
