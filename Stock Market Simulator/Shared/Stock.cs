using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Shared
{
    public class StockDataPoint
    {
        public DateTime Date { get; set; }
        public decimal Price { get; set; }
    }

    public class Stock
    {
        public string Symbol { get; set; } = string.Empty;
        public string CompanyName { get; set; } = string.Empty;
        public decimal CurrentPrice { get; set; }
        public List<StockDataPoint> HistoricalData { get; set; } = new();
    }
}
