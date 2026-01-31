# 📈 Stock Market Simulator

Welcome to the **Stock Market Simulator**, a high-performance and modern Android application built with **Jetpack Compose** and **Firebase**. Master the art of trading in a risk-free, real-time environment designed with a sleek fintech aesthetic! 💸

## ✨ Features

### 🔐 Advanced Security & Auth
- **Multi-Method Login**: Sign up with and Email/Password or use **Google One Tap Sign-in**.
- **Secure Onboarding**: Mandatory password setup for all users (including Google) and a strict 5-point password validation system.
- **Identity Protection**: Re-authentication required for sensitive actions like account deletion.

### 🎮 Gamified Experience
- **Difficulty Levels**: Choose your starting capital from **Level 1 ($100,000)** all the way to **Level 7 ($100)**.
- **Global Leaderboard**: Compete for the top spot! Both Global and Level specific rankings show the **Top 5** traders globally.
- **Personal Recognition**: Your name gets a **Royal Crown (👑)** and primary-colored highlight when you make the leaderboard.

### 💼 Portfolio & Trading
- **Live Market Data**: Powered by the **Finnhub API** for real-time NASDAQ quotes and statistics.
- **Fintech Dashboard**: Track your **Total Account Value**, **Cash Balance**, and **Live Equity** in a professional UI.
- **Comprehensive Trade History**: View your **Active Positions** and **Old Positions** (full trading history) in one place.
- **Atomic Transactions**: High-integrity Buy and Sell operations using Firestore's "Read-Before-Write" patterns.

### 🔍 Market Intelligence
- **Unified Search Bar**: A powerful Material 3 search interface that takes over the full screen for an immersive experience.
- **Personal Watchlist**: Save your favorite stocks to a persistent list (❤️), synced across all your devices.
- **Deep Insights**: View Open, Prev Close, Day High/Low, and Percent Change for any stock.

### ⚙️ Account Management
- **Customizable Profiles**: Upload and change your profile picture via Firebase Storage.
- **Smart Notifications 🔔**: Granular Push Notification controls for Large Stock Drops, Low Balances, and New Sign-ins.
- **Immersive Settings**: A clean, dark-themed, and refreshable account management hub to edit your display name, password, or email verification status.

## 🛠️ Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: Modern MVVM Pattern with Repository logic.
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [Gson](https://github.com/google/gson) for real-time API calls.
- **Backend**: [Firebase](https://firebase.google.com/) 🔥
  - **Auth**: Real-time authentication and account linking.
  - **Firestore**: Scalable NoSQL database for portfolios, watchlists, and leaderboards.
  - **Storage**: Secure image hosting for user profiles.
- **Images**: [Coil](https://coil-kt.github.io/coil/) for fast, asynchronous image loading.
- **Refresh**: Material 3 **Pull-to-Refresh** integrated on all data-heavy screens.

## 🚀 Getting Started

### Prerequisites

- Android Studio Jellyfish or newer.
- A Firebase project (Free **Spark** Tier compatible).
- A [Finnhub.io](https://finnhub.io/) API Key.

### Setup

1. **Clone the repo**:
   ```bash
   git clone https://github.com/yourusername/stock-market-simulator.git
   ```

2. **Firebase Configuration**:
   - Download your `google-services.json` from the console and place it in the `app/` folder.
   - Enable **Authentcation** (Email & Google), **Firestore**, and **Storage**.
   - Set up a Firestore Composite Index for the `users` collection on fields `level` (Ascending) and `balance` (Descending) to enable leaderboard filtering.

3. **API Integration**:
   - Replace the API key in `MarketRepository.kt` with your own Finnhub key.
   - Update `WEB_CLIENT_ID` in `Navigation.kt` with your Firebase Web Client ID.

4. **Build & Run**: 🏁 Launch the app and start building your empire!

## 🤝 Contributing

This project is a labor of love for fintech and mobile development. Contributions and suggestions are always welcome!

## 📝 License

This project is licensed under the MIT License. 📄

---
Developed with ❤️ by [Rahul](https://github.com/yourusername) 👨‍💻
