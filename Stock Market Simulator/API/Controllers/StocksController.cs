using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using API.Services;

namespace API.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class StocksController : ControllerBase
    {
        private readonly StockDataService _stockDataService;

        public StocksController(StockDataService stockDataService)
        {
            _stockDataService = stockDataService;
        }

        [HttpGet("{symbol}")] // e.g., GET /api/stocks/MSFT
        public async Task<IActionResult> GetStockData(string symbol)
        {
            // Note: For a real app, you would fetch more data here,
            // especially historical data for the graph.
            var stockData = await _stockDataService.GetStockDataAsync(symbol);
            if (stockData.CurrentPrice == 0)
            {
                return NotFound("Stock symbol not found or API failed.");
            }
            return Ok(stockData);
        }
    }
}
