using Shared;
using API.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class PortfoliosController : ControllerBase
{
    private readonly MongoDbService _mongoDbService;

    public PortfoliosController(MongoDbService mongoDbService)
    {
        _mongoDbService = mongoDbService;
    }

    [HttpGet("my-portfolio")]
    [Authorize]
    public async Task<IActionResult> GetMyPortfolio()
    {
        var userId = User.FindFirstValue("id");
        if (userId == null)
        {
            return Unauthorized();
        }

        var portfolio = await _mongoDbService.GetPortfolioByUserIdAsync(userId);
        if (portfolio == null)
        {
            return NotFound("Portfolio not found.");
        }

        return Ok(portfolio);
    }

    // new: process a purchase, update holdings and cash balance
    [HttpPost("purchase")]
    [Authorize]
    public async Task<IActionResult> Purchase([FromBody] PurchaseRequest request)
    {
        var userId = User.FindFirstValue("id");
        if (userId == null) return Unauthorized();

        if (request == null || string.IsNullOrWhiteSpace(request.Symbol) || request.Shares <= 0 || request.Price <= 0)
            return BadRequest("Invalid purchase request.");

        var portfolio = await _mongoDbService.GetPortfolioByUserIdAsync(userId);
        if (portfolio == null)
        {
            // create a new portfolio with default cash if none exists
            portfolio = new Portfolio { UserId = userId, CashBalance = 10000m, Holdings = new List<Holding>() };
            await _mongoDbService.CreatePortfolioAsync(portfolio);
            // reload to get Id
            portfolio = await _mongoDbService.GetPortfolioByUserIdAsync(userId) ?? portfolio;
        }

        var totalCost = request.Price * request.Shares;
        if (portfolio.CashBalance < totalCost)
            return BadRequest("Insufficient funds.");

        // deduct cash
        portfolio.CashBalance -= totalCost;

        // update holding
        var existing = portfolio.Holdings.FirstOrDefault(h => h.Symbol.Equals(request.Symbol, StringComparison.OrdinalIgnoreCase));
        if (existing == null)
        {
            portfolio.Holdings.Add(new Holding
            {
                Symbol = request.Symbol.ToUpper(),
                Shares = request.Shares,
                AverageCost = request.Price
            });
        }
        else
        {
            var oldTotalCost = existing.AverageCost * existing.Shares;
            var newTotalShares = existing.Shares + request.Shares;
            var newAvg = (oldTotalCost + (request.Price * request.Shares)) / newTotalShares;
            existing.Shares = newTotalShares;
            existing.AverageCost = newAvg;
        }

        await _mongoDbService.UpdatePortfolioAsync(portfolio);

        return Ok(portfolio);
    }
}
