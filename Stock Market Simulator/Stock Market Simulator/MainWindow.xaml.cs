using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using Stock_Market_Simulator.Services;
using System;
using System.Net.Http;

namespace Stock_Market_Simulator;

public sealed partial class MainWindow : Window
{
    private readonly HttpClient _httpClient = new();

    public MainWindow()
    {
        this.InitializeComponent();
        this.SystemBackdrop = new MicaBackdrop();
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(AppTitleBar);

        AuthService.OnLoginStateChanged += UpdateNavView;
        AuthService.OnPortfolioChanged += UpdateBalanceDisplay;

        UpdateNavView();
        UpdateBalanceDisplay();
    }

    private void NavView_ItemInvoked(NavigationView sender, NavigationViewItemInvokedEventArgs args)
    {
        if (args.InvokedItemContainer?.Tag is string navItemTag)
        {
            NavigateToPage(navItemTag);
        }
    }

    private void NavigateToPage(string pageTag)
    {
        if (pageTag == "Logout")
        {
            AuthService.Logout();
            ContentFrame.Navigate(typeof(LoginPage), null, new Microsoft.UI.Xaml.Media.Animation.DrillInNavigationTransitionInfo());
            return;
        }

        Type? pageType = pageTag switch
        {
            "Login" => typeof(LoginPage),
            "Register" => typeof(RegisterPage),
            "Dashboard" => typeof(DashboardPage),
            "Portfolio" => typeof(PortfolioPage),
            // open a Search entry page (contains the search bar) which then navigates to StockSearchPage
            "Search" => typeof(SearchPage),
            _ => null
        };

        if (pageType != null)
        {
            ContentFrame.Navigate(pageType, null, new Microsoft.UI.Xaml.Media.Animation.DrillInNavigationTransitionInfo());
        }
    }

    private void RootGrid_Loaded(object sender, RoutedEventArgs e)
    {
        double captionButtonsWidth = this.AppWindow.TitleBar.RightInset;
        TitleBarRightContent.Margin = new Thickness(0, 0, captionButtonsWidth, 0);
    }

    // Ensure UI updates run on the UI dispatcher — AuthService may raise events from background threads.
    private void UpdateBalanceDisplay()
    {
        // Use DispatcherQueue to marshal to UI thread. Safe to call from UI thread as well.
        this.DispatcherQueue.TryEnqueue(() =>
        {
            if (AuthService.IsLoggedIn && AuthService.CurrentPortfolio != null)
            {
                BalancePanel.Visibility = Visibility.Visible;
                BalanceTextBlock.Text = AuthService.CurrentPortfolio.CashBalance.ToString("C");
            }
            else
            {
                BalancePanel.Visibility = Visibility.Collapsed;
            }
        });
    }

    private void UpdateNavView()
    {
        UpdateBalanceDisplay();
        if (AuthService.IsLoggedIn)
        {
            SearchNavItem.Visibility = Visibility.Visible;
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
            SearchNavItem.Visibility = Visibility.Collapsed;
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