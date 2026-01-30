# 📈 Stock Market Simulator 🚀

Welcome to the **Stock Market Simulator**, a high-performance, modern Android application built with **Jetpack Compose** and **Firebase**. Whether you're a seasoned trader or just starting, this app provides a risk-free environment to master the stock market with real-time data! 💸

## ✨ Features

- **🔐 Secure Authentication**: Sign up with Email/Password or use **Google One Tap Sign-in** for a seamless experience.
- **🖼️ Customizable Profiles**: Upload your profile picture using Firebase Storage and Coil.
- **🎮 Difficulty Levels**: Choose your starting capital from Level 1 ($100,000) to Level 7 ($100) to test your skills!
- **🔍 Live NASDAQ Search**: Find any stock on the NASDAQ exchange with a powerful, integrated Search Bar.
- **📊 Real-time Data**: Powered by the **Finnhub API** for live quotes and market movements.
- **❤️ Personal Watchlist**: Save your favorite stocks to a personalized watchlist synced across devices via Firestore.
- **💼 Portfolio Management**: Track your "Total Account Value," see your "Buying Power," and manage active positions with a sleek fintech UI.
- **🔄 Smart Refresh**: Stay up-to-date with **Pull-to-Refresh** functionality on all major screens.
- **⚡ Atomic Trading**: High-integrity "Buy" and "Sell" transactions using Firestore's "Read-Before-Write" pattern.

## 🛠️ Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 🎨)
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [Gson](https://github.com/google/gson) 🌐
- **Backend**: [Firebase](https://firebase.google.com/) 🔥
  - **Auth**: Secure login & Google Sign-in.
  - **Firestore**: Real-time DB for portfolios & watchlists.
  - **Storage**: Image hosting for profile pictures.
- **Images**: [Coil](https://coil-kt.github.io/coil/) 🖼️
- **API**: [Finnhub.io](https://finnhub.io/) 💹

## 🚀 Getting Started

### Prerequisites

- Android Studio Jellyfish or newer.
- A Firebase project.
- A Finnhub API Key (it's free!).

### Setup

1. **Clone the repo**:
   ```bash
   git clone https://github.com/yourusername/stock-market-simulator.git
   ```

2. **Firebase Configuration**:
   - Add your `google-services.json` to the `app/` directory.
   - Enable **Auth**, **Firestore**, and **Storage** in the Firebase Console.
   - Set Firestore rules to allow authenticated users to read/write their own `/users/{userId}` documents.

3. **API Keys**:
   - Open `MarketRepository.kt` and replace the placeholder with your **Finnhub API Key**.
   - Open `Navigation.kt` and update the `WEB_CLIENT_ID` with your Firebase Web Client ID for Google Sign-in.

4. **Build & Run**:
   Hit the **Run** button in Android Studio and start trading! 🏁

## 📸 Screenshots

| Login | Difficulty Selection | Market | Portfolio |
| :---: | :---: | :---: | :---: |
| ![Login](https://via.placeholder.com/150) | ![Difficulty](https://via.placeholder.com/150) | ![Market](https://via.placeholder.com/150) | ![Portfolio](https://via.placeholder.com/150) |

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/yourusername/stock-market-simulator/issues).

## 📝 License

This project is licensed under the MIT License. 📄

---
Developed with ❤️ by [Rahul](https://github.com/yourusername) 👨‍💻
