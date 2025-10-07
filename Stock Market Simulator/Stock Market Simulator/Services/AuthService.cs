using Shared;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;
using System.Threading.Tasks;

namespace Stock_Market_Simulator.Services;

public static class AuthService
{
    private static readonly HttpClient _httpClient = new();
    public static string? JwtToken { get; private set; }
    public static bool IsLoggedIn => !string.IsNullOrEmpty(JwtToken);
    public static Portfolio? CurrentPortfolio { get; private set; }
    public static event Action? OnPortfolioChanged;

    public static event Action? OnLoginStateChanged;

    public static async Task<bool> LoginAsync(string username, string password)
    {
        var apiUrl = "https://localhost:7179/api/Users/login";
        var loginData = new { Username = username, Password = password };

        try
        {
            var response = await _httpClient.PostAsJsonAsync(apiUrl, loginData);
            if (response.IsSuccessStatusCode)
            {
                var responseData = await response.Content.ReadFromJsonAsync<LoginResponse>();
                JwtToken = responseData?.token;
                OnLoginStateChanged?.Invoke();
                return true;
            }
        }
        catch { /* Handle connection errors */ }

        if (IsLoggedIn)
        {
            await FetchPortfolioAsync();
            OnLoginStateChanged?.Invoke();
            return true;
        }

        return false;
    }

    public static void Logout()
    {
        JwtToken = null;
        OnLoginStateChanged?.Invoke();
    }

    public static async Task FetchPortfolioAsync()
    {
        if (!IsLoggedIn) return;

        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", JwtToken);
        var apiUrl = "https://localhost:7179/api/Portfolios/my-portfolio";
        CurrentPortfolio = await _httpClient.GetFromJsonAsync<Portfolio>(apiUrl);
        OnPortfolioChanged?.Invoke();
    }

    private class LoginResponse { public string token { get; set; } }
}
