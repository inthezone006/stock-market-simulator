using API.Services;
using Shared;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<StockDbSettings>(
    builder.Configuration.GetSection("StockDbSettings"));

builder.Services.AddSingleton<MongoDbService>();
builder.Services.AddSingleton<StockDataService>();

builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.Run();
