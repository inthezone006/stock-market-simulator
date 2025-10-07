using Microsoft.AspNetCore.DataProtection.KeyManagement;
using Shared;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json.Serialization;

namespace API.Services;
public class StockDataService
{
    private readonly HttpClient _httpClient = new();

    private const string ApiKey = "RVSMVOOU1E8IEZVE";

    public async Task<Stock> GetStockDataAsync(string symbol)
    { 

        string url = $"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol={symbol}&apikey={ApiKey}";
        try
        {
            var timeSeriesResponse = await _httpClient.GetFromJsonAsync<AlphaVantageTimeSeries>(url);
            if (timeSeriesResponse?.TimeSeries != null && timeSeriesResponse.TimeSeries.Any())
            {
                var latestEntry = timeSeriesResponse.TimeSeries.First();
                var historicalData = timeSeriesResponse.TimeSeries
                                        .Select(kvp => new StockDataPoint { Date = DateTime.Parse(kvp.Key), Price = kvp.Value.Close })
                                        .ToList();

                return new Stock
                {
                    Symbol = timeSeriesResponse.MetaData.Symbol,
                    CompanyName = timeSeriesResponse.MetaData.Symbol,
                    CurrentPrice = latestEntry.Value.Close,
                    HistoricalData = historicalData
                };
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error fetching data for {symbol}: {ex.Message}");
        }

        return new Stock { Symbol = symbol, CompanyName = "Data not available", CurrentPrice = 0 };
    }
    }
    internal class AlphaVantageQuote
    {
    [JsonPropertyName("Global Quote")]
    public GlobalQuote? GlobalQuote { get; set; }
    }

    internal class AlphaVantageTimeSeries
    {
        [JsonPropertyName("Meta Data")]
        public MetaData MetaData { get; set; }

        [JsonPropertyName("Time Series (Daily)")]
        public Dictionary<string, DailyData>? TimeSeries { get; set; }
    }
    internal class MetaData
    {
        [JsonPropertyName("2. Symbol")]
        public string Symbol { get; set; }
    }
    internal class DailyData
    {
        [JsonPropertyName("4. close")]
        [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)]
        public decimal Close { get; set; }
    }

internal class GlobalQuote
    {
    [JsonPropertyName("01. symbol")]
    public string Symbol { get; set; } = string.Empty;

    [JsonPropertyName("05. price")]
    [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)]
    public decimal Price { get; set; }
}
