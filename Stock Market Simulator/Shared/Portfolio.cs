namespace Shared;


using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

public class Portfolio
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; }

    [BsonRepresentation(BsonType.ObjectId)]
    public string UserId { get; set; }

    public decimal CashBalance { get; set; }
    public List<Holding> Holdings { get; set; } = new();
}

public class Holding
{
    public string Symbol { get; set; }
    public int Shares { get; set; }
    public decimal AverageCost { get; set; }
}