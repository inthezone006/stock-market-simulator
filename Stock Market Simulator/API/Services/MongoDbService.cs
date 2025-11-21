namespace API.Services;

using Shared;
using Microsoft.Extensions.Options;
using MongoDB.Driver;

public class MongoDbService
{
    private readonly IMongoCollection<User> _usersCollection;
    private readonly IMongoCollection<Portfolio> _portfoliosCollection;

    public MongoDbService(IOptions<StockDbSettings> stockDbSettings)
    {
        var mongoClient = new MongoClient(stockDbSettings.Value.ConnectionString);
        var mongoDatabase = mongoClient.GetDatabase(stockDbSettings.Value.DatabaseName);

        _usersCollection = mongoDatabase.GetCollection<User>("user-data");
        _portfoliosCollection = mongoDatabase.GetCollection<Portfolio>("portfolio-data");
    }

    public async Task CreateUserAsync(User newUser) =>
        await _usersCollection.InsertOneAsync(newUser);

    public async Task<User> GetUserByUsernameAsync(string username) =>
        await _usersCollection.Find(u => u.Username == username).FirstOrDefaultAsync();

    public async Task<Portfolio?> GetPortfolioByUserIdAsync(string userId)
    {
        return await _portfoliosCollection.Find(p => p.UserId == userId).FirstOrDefaultAsync();
    }
    public async Task CreatePortfolioAsync(Portfolio newPortfolio) =>
        await _portfoliosCollection.InsertOneAsync(newPortfolio);

    // new: replace the whole portfolio document after changes
    public async Task UpdatePortfolioAsync(Portfolio updatedPortfolio) =>
        await _portfoliosCollection.ReplaceOneAsync(p => p.Id == updatedPortfolio.Id, updatedPortfolio);
}