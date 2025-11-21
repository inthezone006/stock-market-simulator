using Microsoft.UI.Dispatching;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using System;
using Windows.System;

namespace Stock_Market_Simulator;

public sealed partial class SearchPage : Page
{
    private bool _contentLoaded;
    private AutoSuggestBox? _searchBox;

    public SearchPage()
    {
        InitializeComponent();
        // wire Loaded so events are attached after XAML is fully applied
        this.Loaded += SearchPage_Loaded;
    }

    // Minimal InitializeComponent fallback so the page compiles even if XAML generated partial is not present.
    // If the XAML build action is correct then the generated partial will provide InitializeComponent and
    // you should remove this implementation to avoid duplication.
    public void InitializeComponent()
    {
        if (_contentLoaded)
            return;

        _contentLoaded = true;

        var resourceLocator = new Uri("ms-appx:///SearchPage.xaml");
        Microsoft.UI.Xaml.Application.LoadComponent(this, resourceLocator, Microsoft.UI.Xaml.Controls.Primitives.ComponentResourceLocation.Application);
    }

    private void SearchPage_Loaded(object? sender, RoutedEventArgs e)
    {
        // Locate the named control provided by the XAML and wire handlers (attach only once).
        _searchBox = this.FindName("SearchBox") as AutoSuggestBox;
        if (_searchBox != null)
        {
            _searchBox.QuerySubmitted -= SearchBox_QuerySubmitted;
            _searchBox.QuerySubmitted += SearchBox_QuerySubmitted;

            _searchBox.KeyDown -= SearchBox_KeyDown;
            _searchBox.KeyDown += SearchBox_KeyDown;
        }

        var searchButton = this.FindName("SearchButton") as Button;
        if (searchButton != null)
        {
            searchButton.Click -= SearchButton_Click;
            searchButton.Click += SearchButton_Click;
        }
    }

    private void SearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
    {
        var query = (args.QueryText ?? string.Empty).Trim();
        if (!string.IsNullOrEmpty(query))
        {
            NavigateToStockPage(query);
        }
    }

    private void SearchButton_Click(object? sender, RoutedEventArgs e)
    {
        var query = (_searchBox?.Text ?? string.Empty).Trim();
        if (!string.IsNullOrEmpty(query))
        {
            NavigateToStockPage(query);
        }
    }

    private void SearchBox_KeyDown(object? sender, KeyRoutedEventArgs e)
    {
        if (e.Key == VirtualKey.Enter)
        {
            var query = (_searchBox?.Text ?? string.Empty).Trim();
            if (!string.IsNullOrEmpty(query))
            {
                NavigateToStockPage(query);
                e.Handled = true;
            }
        }
    }

    private void NavigateToStockPage(string query)
    {
        // Perform navigation on UI thread; Frame is available when page is loaded.
        try
        {
            this.Frame?.Navigate(typeof(StockSearchPage), query);
        }
        catch
        {
            // swallow here to avoid propagating native exceptions; log or show UI feedback instead if needed
        }
    }
}