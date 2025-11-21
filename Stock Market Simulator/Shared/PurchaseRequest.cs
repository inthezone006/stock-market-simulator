namespace Shared;

public class PurchaseRequest
{
    // mark Symbol as required to satisfy the non-nullable property warning (C# 11+)
    public required string Symbol { get; set; }
    public int Shares { get; set; }
    public decimal Price { get; set; }
}