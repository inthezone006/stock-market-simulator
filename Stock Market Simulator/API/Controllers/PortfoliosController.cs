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
}
