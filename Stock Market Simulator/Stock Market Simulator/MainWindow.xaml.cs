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
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Microsoft.UI.Windowing;

namespace Stock_Market_Simulator;

public sealed partial class MainWindow : Window
{
    private readonly HttpClient _httpClient = new();
    private string _jwtToken;

    public MainWindow()
    {
        this.InitializeComponent();
        this.SystemBackdrop = new MicaBackdrop();
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(AppTitleBar);
        //this.AppWindow.TitleBar.LayoutMetricsChanged += TitleBar_LayoutMetricsChanged;
        AuthService.OnLoginStateChanged += UpdateNavView;
        AuthService.OnPortfolioChanged += UpdateBalanceDisplay;

        UpdateNavView();
    }

    //private void TitleBar_LayoutMetricsChanged(AppWindowTitleBar sender, object args)
    //{
    //    TitleBarRightContent.Margin = new Thickness(0, 0, sender.RightInset, 0);
    //}

    private void NavView_ItemInvoked(NavigationView sender, NavigationViewItemInvokedEventArgs args)
    {
        if (args.InvokedItemContainer?.Tag is string pageTag)
        {
            if (pageTag == "Logout")
            {
                AuthService.Logout();
                return;
            }

            var pageType = Type.GetType($"StockSimulator.UI.{pageTag}");
            if (pageType != null)
            {
                ContentFrame.Navigate(pageType);
            }
        }
    }

    private void RootGrid_Loaded(object sender, RoutedEventArgs e)
    {
        double captionButtonsWidth = this.AppWindow.TitleBar.RightInset;

        TitleBarRightContent.Margin = new Thickness(0, 0, captionButtonsWidth, 0); 
    }

    private void StockSearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
    {
        if (!string.IsNullOrEmpty(args.QueryText))
        {
            ContentFrame.Navigate(typeof(StockSearchPage), args.QueryText);
        }
    }

    private void UpdateBalanceDisplay()
    {
        if (AuthService.IsLoggedIn && AuthService.CurrentPortfolio != null)
        {
            BalancePanel.Visibility = Visibility.Visible;
            BalanceTextBlock.Text = AuthService.CurrentPortfolio.CashBalance.ToString("c");
        }
        else
        {
            BalancePanel.Visibility = Visibility.Collapsed;
        }
    }

    private void UpdateNavView()
    {
        UpdateBalanceDisplay();
        if (AuthService.IsLoggedIn)
        {
            StockSearchBox.Visibility = Visibility.Visible;
            DashboardNavItem.Visibility = Visibility.Visible;
            PortfolioNavItem.Visibility = Visibility.Visible;
            SeparatorNavItem.Visibility = Visibility.Visible;
            LogoutNavItem.Visibility = Visibility.Visible;
            LoginNavItem.Visibility = Visibility.Collapsed;

            NavView.SelectedItem = DashboardNavItem;
            ContentFrame.Navigate(typeof(DashboardPage));
        }
        else
        {
            StockSearchBox.Visibility = Visibility.Collapsed;
            DashboardNavItem.Visibility = Visibility.Collapsed;
            PortfolioNavItem.Visibility = Visibility.Collapsed;
            SeparatorNavItem.Visibility = Visibility.Collapsed;
            LogoutNavItem.Visibility = Visibility.Collapsed;
            LoginNavItem.Visibility = Visibility.Visible;


            NavView.SelectedItem = LoginNavItem;
            ContentFrame.Navigate(typeof(LoginPage));
        }
    }
}
