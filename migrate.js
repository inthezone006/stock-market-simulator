const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();
const db = admin.firestore();
const FINNHUB_KEY = 'd38davhr01qlbdj4vutgd38davhr01qlbdj4vuu0';

async function migrateAllUsers() {
  const usersSnapshot = await db.collection('users').get();
  const priceCache = new Map();

  console.log(`Starting migration for ${usersSnapshot.size} users...`);

  for (const userDoc of usersSnapshot.docs) {
    const userData = userDoc.data();
    const balance = userData.balance || 0;

    // Get the portfolio sub-collection for this user
    const portfolioSnapshot = await userDoc.ref.collection('portfolio').get();
    let totalStockValue = 0;

    for (const stockDoc of portfolioSnapshot.docs) {
      const { symbol, quantity } = stockDoc.data();

      // Cache prices to avoid hitting API rate limits
      if (!priceCache.has(symbol)) {
        try {
          const res = await axios.get(`https://finnhub.io/api/v1/quote?symbol=${symbol}&token=${FINNHUB_KEY}`);
          priceCache.set(symbol, res.data.c || 0);
          // Small delay to respect rate limits (60 calls/min)
          await new Promise(r => setTimeout(r, 200));
        } catch (e) {
          console.error(`Error fetching price for ${symbol}: ${e.message}`);
          priceCache.set(symbol, 0);
        }
      }

      totalStockValue += (priceCache.get(symbol) * quantity);
    }

    const totalAccountValue = balance + totalStockValue;

    await userDoc.ref.update({
      totalAccountValue: totalAccountValue,
      lastSync: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log(`Updated User ${userDoc.id}: Balance $${balance.toFixed(2)} + Stocks $${totalStockValue.toFixed(2)} = Total $${totalAccountValue.toFixed(2)}`);
  }
  console.log('Migration complete!');
}

migrateAllUsers().catch(console.error);