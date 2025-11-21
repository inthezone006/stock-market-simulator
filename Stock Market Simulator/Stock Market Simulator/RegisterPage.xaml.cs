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
using System.Net.Http;
using System.Net.Http.Json;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.System;

namespace Stock_Market_Simulator
{
    public sealed partial class RegisterPage : Page
    {
        private readonly HttpClient _httpClient = new();
        public RegisterPage()
        {
            InitializeComponent();

            // wire Enter key on input controls to trigger registration
            UsernameTextBox.KeyDown += Input_KeyDown;
            PasswordBox.KeyDown += Input_KeyDown;
            InitialDepositBox.KeyDown += Input_KeyDown;
        }

        private void Input_KeyDown(object? sender, KeyRoutedEventArgs e)
        {
            if (e.Key == VirtualKey.Enter)
            {
                RegisterButton_Click(RegisterButton, new RoutedEventArgs());
                e.Handled = true;
            }
        }

        private async void RegisterButton_Click(object sender, RoutedEventArgs e)
        {
            StatusTextBlock.Text = "Registering...";

            var apiUrl = "https://localhost:7179/api/Users/register";

            var registrationData = new
            {
                Username = UsernameTextBox.Text,
                Password = PasswordBox.Password,
                InitialDeposit = (decimal)InitialDepositBox.Value
            };

            try
            {
                var response = await _httpClient.PostAsJsonAsync(apiUrl, registrationData);

                if (response.IsSuccessStatusCode)
                {
                    StatusTextBlock.Text = "Registration successful! You can now log in.";
                }
                else
                {
                    var errorMessage = await response.Content.ReadAsStringAsync();
                    StatusTextBlock.Text = $"Error: {errorMessage}";
                }
            }
            catch (HttpRequestException)
            {
                StatusTextBlock.Text = "Connection error. Is the API running?";
            }
        }

        private void LoginHyperlink_Click(object sender, RoutedEventArgs e)
        {
            this.Frame.Navigate(typeof(LoginPage));
        }
    }
}