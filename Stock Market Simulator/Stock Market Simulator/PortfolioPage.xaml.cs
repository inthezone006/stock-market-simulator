using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Stock_Market_Simulator.Services;
using Shared;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace Stock_Market_Simulator
{
    public sealed partial class PortfolioPage : Page
    {
        private readonly HttpClient _httpClient = new();

        public PortfolioPage()
        {
            this.InitializeComponent();
        }

        protected override async void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            if (AuthService.IsLoggedIn)
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", AuthService.JwtToken);

                var apiUrl = "https://localhost:7179/api/Portfolios/my-portfolio";
                var portfolio = await _httpClient.GetFromJsonAsync<Portfolio>(apiUrl);

                if (portfolio != null)
                {
                    HoldingsGrid.ItemsSource = portfolio.Holdings;
                }
            }
        }
    }
}
