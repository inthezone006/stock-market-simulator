using LiveChartsCore;
using LiveChartsCore.SkiaSharpView;
using LiveChartsCore.SkiaSharpView.Painting;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using Shared;
using SkiaSharp;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.Foundation.Collections;

namespace Stock_Market_Simulator;
public class Stock
{
    public string Symbol { get; set; }
    public string CompanyName { get; set; }
    public decimal CurrentPrice { get; set; }
    public List<StockDataPoint> HistoricalData { get; set; }
}

public sealed partial class StockSearchPage : Page
{
    private readonly HttpClient _httpClient = new();

    public StockSearchPage()
    {
        this.InitializeComponent();
    }

    protected override async void OnNavigatedTo(NavigationEventArgs e)
    {
        base.OnNavigatedTo(e);
        if (e.Parameter is string symbol && !string.IsNullOrEmpty(symbol))
        {
            await FetchStockData(symbol);
        }
    }

    private async Task FetchStockData(string symbol)
    {
        LoadingRing.IsActive = true;
        ResultsGrid.Visibility = Visibility.Collapsed;
        ErrorTextBlock.Visibility = Visibility.Collapsed;
        HeaderTextBlock.Text = $"Searching for {symbol.ToUpper()}...";

        var apiUrl = $"https://localhost:7179/api/Stocks/{symbol}";

        try
        {
            var stockData = await _httpClient.GetFromJsonAsync<Stock>(apiUrl);

            if (stockData != null && stockData.HistoricalData.Any())
            {
                HeaderTextBlock.Text = "Stock Details";
                SymbolTextBlock.Text = stockData.Symbol.ToUpper();
                CompanyNameTextBlock.Text = stockData.CompanyName;
                CurrentPriceTextBlock.Text = stockData.CurrentPrice.ToString("c");
                PriceChart.XAxes = new[] { new Axis { IsVisible = false } };
                PriceChart.YAxes = new[] { new Axis { IsVisible = false } };
                PriceChart.Series = new ISeries[]
                {
                new LineSeries<StockDataPoint>
                {
                    Values = stockData.HistoricalData.OrderBy(d => d.Date).ToList(),
                    Mapping = (dataPoint, index) => new(index, (double)dataPoint.Price),
                    Fill = new SolidColorPaint(SKColors.CornflowerBlue.WithAlpha(50)),
                    Stroke = new SolidColorPaint(SKColors.CornflowerBlue) { StrokeThickness = 2 },
                    GeometryFill = null,
                    GeometryStroke = null
                }
                };

                PriceChart.Background = null;

                ResultsGrid.Visibility = Visibility.Visible;
            }
            else
            {
                HeaderTextBlock.Text = "Stock Details";
                ErrorTextBlock.Text = "No data found for the given symbol.";
                ErrorTextBlock.Visibility = Visibility.Visible;
            }
        }
        catch (HttpRequestException)
        {
            HeaderTextBlock.Text = "Stock Details";
            ErrorTextBlock.Text = "Could not connect to the API.";
            ErrorTextBlock.Visibility = Visibility.Visible;
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }
}
