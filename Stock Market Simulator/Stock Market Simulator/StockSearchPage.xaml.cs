using LiveChartsCore;
using LiveChartsCore.SkiaSharpView;
using LiveChartsCore.SkiaSharpView.Painting;
using LiveChartsCore.SkiaSharpView.WinUI;
using Microsoft.UI;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Microsoft.UI.Xaml.Media;
using Shared;
using SkiaSharp;
using System;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;
using System.Net.Http.Headers;
using Stock_Market_Simulator.Services;

namespace Stock_Market_Simulator;

public sealed partial class StockSearchPage : Page
{
    private readonly HttpClient _httpClient = new();
    private Stock? _currentStock;

    public StockSearchPage()
    {
        this.InitializeComponent();
    }

    protected override async void OnNavigatedTo(NavigationEventArgs e)
    {
        base.OnNavigatedTo(e);
        if (e.Parameter is string symbol && !string.IsNullOrWhiteSpace(symbol))
        {
            await FetchStockData(symbol.Trim());
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

            if (stockData != null && stockData.HistoricalData != null && stockData.HistoricalData.Any())
            {
                _currentStock = stockData;

                // show company name as header
                HeaderTextBlock.Text = $"{stockData.CompanyName} ({stockData.Symbol.ToUpper()})";
                SymbolTextBlock.Text = stockData.Symbol.ToUpper();
                CompanyNameTextBlock.Text = stockData.CompanyName;
                CurrentPriceTextBlock.Text = stockData.CurrentPrice.ToString("C");

                // Prepare chart data (ensure oldest -> newest ordering)
                var ordered = stockData.HistoricalData.OrderBy(d => d.Date).ToArray();
                var values = ordered.Select(d => (double)d.Price).ToArray();
                var labels = ordered.Select(d => d.Date.ToString("MM-dd")).ToArray();

                PriceChart.Series = new ISeries[]
                {
                    new LineSeries<double>
                    {
                        Values = values,
                        Fill = null,
                        Stroke = new SolidColorPaint(SKColors.DeepSkyBlue, 2),
                        GeometrySize = 6
                    }
                };

                PriceChart.XAxes = new Axis[]
                {
                    new Axis { Labels = labels, LabelsRotation = 15 }
                };

                PriceChart.YAxes = new Axis[]
                {
                    new Axis { Labeler = value => value.ToString("C") }
                };

                ResultsGrid.Visibility = Visibility.Visible;
            }
            else
            {
                HeaderTextBlock.Text = "Stock Details";
                ErrorTextBlock.Text = "No data found for the given symbol.";
                ErrorTextBlock.Visibility = Visibility.Visible;
            }
        }
        catch (Exception ex)
        {
            HeaderTextBlock.Text = "Stock Details";
            ErrorTextBlock.Text = $"Could not fetch data: {ex.Message}";
            ErrorTextBlock.Visibility = Visibility.Visible;
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private async void PurchaseButton_Click(object sender, RoutedEventArgs e)
    {
        PurchaseStatusTextBlock.Visibility = Visibility.Collapsed;

        if (_currentStock == null)
        {
            PurchaseStatusTextBlock.Text = "No stock loaded.";
            PurchaseStatusTextBlock.Foreground = new SolidColorBrush(Colors.Red);
            PurchaseStatusTextBlock.Visibility = Visibility.Visible;
            return;
        }

        if (!AuthService.IsLoggedIn)
        {
            // not logged in - navigate to login
            this.Frame?.Navigate(typeof(LoginPage));
            return;
        }

        var sharesValue = (int)SharesNumberBox.Value;
        if (sharesValue <= 0)
        {
            PurchaseStatusTextBlock.Text = "Enter a valid number of shares.";
            PurchaseStatusTextBlock.Foreground = new SolidColorBrush(Colors.Red);
            PurchaseStatusTextBlock.Visibility = Visibility.Visible;
            return;
        }

        LoadingRing.IsActive = true;
        try
        {
            _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", AuthService.JwtToken);
            var purchaseRequest = new { Symbol = _currentStock.Symbol, Shares = sharesValue, Price = _currentStock.CurrentPrice };
            var response = await _httpClient.PostAsJsonAsync("https://localhost:7179/api/Portfolios/purchase", purchaseRequest);

            if (response.IsSuccessStatusCode)
            {
                // refresh client-side portfolio
                await AuthService.FetchPortfolioAsync();

                PurchaseStatusTextBlock.Text = $"Purchased {sharesValue} shares of {_currentStock.Symbol} at {_currentStock.CurrentPrice:C}";
                PurchaseStatusTextBlock.Foreground = new SolidColorBrush(Colors.Green);
                PurchaseStatusTextBlock.Visibility = Visibility.Visible;
            }
            else
            {
                var err = await response.Content.ReadAsStringAsync();
                PurchaseStatusTextBlock.Text = $"Purchase failed: {err}";
                PurchaseStatusTextBlock.Foreground = new SolidColorBrush(Colors.Red);
                PurchaseStatusTextBlock.Visibility = Visibility.Visible;
            }
        }
        catch (Exception ex)
        {
            PurchaseStatusTextBlock.Text = $"Error: {ex.Message}";
            PurchaseStatusTextBlock.Foreground = new SolidColorBrush(Colors.Red);
            PurchaseStatusTextBlock.Visibility = Visibility.Visible;
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private void BackButton_Click(object sender, RoutedEventArgs e)
    {
        if (this.Frame?.CanGoBack == true)
        {
            this.Frame.GoBack();
        }
        else
        {
            this.Frame?.Navigate(typeof(DashboardPage));
        }
    }
}