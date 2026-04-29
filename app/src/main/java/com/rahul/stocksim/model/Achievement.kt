package com.rahul.stocksim.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // Resource name or Emoji
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,
    val target: Float = 1f,
    val unlockedAt: Long? = null
)

val ALL_ACHIEVEMENTS = listOf(
    Achievement(
        id = "first_trade",
        title = "First Steps",
        description = "Complete your first trade",
        icon = "🚀"
    ),
    Achievement(
        id = "profit_maker",
        title = "Profit Maker",
        description = "Sell a stock for a profit",
        icon = "💰"
    ),
    Achievement(
        id = "diversified",
        title = "Diversified",
        description = "Own 5 different stocks simultaneously",
        icon = "📊"
    ),
    Achievement(
        id = "big_spender",
        title = "High Roller",
        description = "Spend over $10,000 on a single trade",
        icon = "💎"
    ),
    Achievement(
        id = "ai_enthusiast",
        title = "AI Enthusiast",
        description = "Use Gemini AI Analysis 5 times",
        icon = "🧠"
    ),
    Achievement(
        id = "diamond_hands",
        title = "Diamond Hands",
        description = "Hold a stock for more than 7 days",
        icon = "💎🙌"
    ),
    Achievement(
        id = "risk_taker",
        title = "Risk Taker",
        description = "Buy a stock that is down more than 10% today",
        icon = "📉"
    ),
    Achievement(
        id = "paper_hands",
        title = "Paper Hands",
        description = "Sell a stock within 24 hours of buying",
        icon = "🧻"
    ),
    Achievement(
        id = "whale",
        title = "Whale",
        description = "Reach a portfolio value of $250,000",
        icon = "🐋"
    ),
    Achievement(
        id = "bull_runner",
        title = "Bull Runner",
        description = "Achieve 10% profit on a single stock",
        icon = "🐂"
    ),
    Achievement(
        id = "diversified_sector",
        title = "Sector Specialist",
        description = "Own stocks in 3 different industries",
        icon = "🏢"
    )
)
