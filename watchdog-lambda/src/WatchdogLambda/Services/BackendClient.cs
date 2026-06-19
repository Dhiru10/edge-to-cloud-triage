using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using WatchdogLambda.Models;

namespace WatchdogLambda.Services;

public sealed class BackendClient : IBackendClient, IDisposable
{
    private readonly HttpClient _http;
    private readonly string     _baseUrl;

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    public BackendClient(WatchdogConfig config)
    {
        _baseUrl = config.BackendUrl.TrimEnd('/');
        _http    = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };
        _http.DefaultRequestHeaders.Add("X-Api-Key", config.ApiKey);
        _http.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
    }

    public async Task<List<FaultEvent>> GetStaleFaultsAsync(DateTime staleSince, CancellationToken ct = default)
    {
        var staleSinceIso = Uri.EscapeDataString(staleSince.ToString("O"));
        var url = $"{_baseUrl}/api/faults?status=processing&staleSince={staleSinceIso}&limit=50";

        var resp = await _http.GetAsync(url, ct);
        resp.EnsureSuccessStatusCode();

        var json = await resp.Content.ReadAsStringAsync(ct);
        return JsonSerializer.Deserialize<List<FaultEvent>>(json, JsonOpts) ?? [];
    }

    public async Task ResetFaultAsync(string faultId, CancellationToken ct = default)
    {
        var body    = JsonSerializer.Serialize(new { status = "pending" });
        var content = new StringContent(body, Encoding.UTF8, "application/json");
        var resp    = await _http.PatchAsync($"{_baseUrl}/api/faults/{faultId}/status", content, ct);
        resp.EnsureSuccessStatusCode();
    }

    public void Dispose() => _http.Dispose();
}
