using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Shared;
using Stock_Market_Simulator.Services;
using System.Net.Http.Headers;
using System.Net.Http;
using System.Linq;
using System;
using System.Net.Http.Json;

namespace Stock_Market_Simulator
{
    public sealed partial class DashboardPage : Page
    {
        private string _jwtToken;
        private static readonly HttpClient _httpClient = new HttpClient();

        public DashboardPage()
        {
            this.InitializeComponent();
        }

        protected override async void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            if (AuthService.IsLoggedIn && AuthService.JwtToken != null)
            {
                try
                {
                    _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", AuthService.JwtToken);

                    var apiUrl = "https://localhost:7179/api/Portfolios/my-portfolio";
                    var portfolio = await _httpClient.GetFromJsonAsync<Portfolio>(apiUrl);
                    if (portfolio != null)
                    {
                        CashBalanceTextBlock.Text = portfolio.CashBalance.ToString("c");
                        decimal stockValue = portfolio.Holdings.Sum(h => h.Shares * h.AverageCost);
                        PortfolioValueTextBlock.Text = stockValue.ToString("c");
                    }
                }
                catch (Exception ex)
                {
                    CashBalanceTextBlock.Text = "Error";
                    PortfolioValueTextBlock.Text = $"Could not load data: {ex.Message}";
                }
            }
            else
            {
                CashBalanceTextBlock.Text = "Not logged in";
                PortfolioValueTextBlock.Text = "Not logged in";
            }
        }
    }
}