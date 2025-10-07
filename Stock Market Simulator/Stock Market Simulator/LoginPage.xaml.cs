using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using Stock_Market_Simulator.Services;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;

namespace Stock_Market_Simulator
{

    public sealed partial class LoginPage : Page
    {
        private readonly HttpClient _httpClient = new();
        private string _jwtToken;

        public LoginPage()
        {
            this.InitializeComponent();
        }
        private void ShowNotification(string title, string message, InfoBarSeverity severity)
        {
            NotificationInfoBar.Title = title;
            NotificationInfoBar.Message = message;
            NotificationInfoBar.Severity = severity;
            NotificationInfoBar.IsOpen = true;
        }

        private async void LoginButton_Click(object sender, RoutedEventArgs e)
        {
            LoginButton.IsEnabled = false;
            LoadingRing.IsActive = true;
            NotificationInfoBar.IsOpen = false;
            bool success = await AuthService.LoginAsync(UsernameTextBox.Text, PasswordBox.Password);

            if (!success)
            {
                ShowNotification("Error", "Invalid username or password.", InfoBarSeverity.Error);
            }

            LoginButton.IsEnabled = true;
            LoadingRing.IsActive = false;
        }

        private void RegisterHyperlink_Click(object sender, RoutedEventArgs e)
        {
             this.Frame.Navigate(typeof(RegisterPage));
        }

        public class LoginResponse { public string token { get; set; } }
    }
}