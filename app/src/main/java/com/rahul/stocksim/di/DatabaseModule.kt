package com.rahul.stocksim.di

import android.content.Context
import androidx.room.Room
import com.rahul.stocksim.data.local.StockDao
import com.rahul.stocksim.data.local.StockDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStockDatabase(@ApplicationContext context: Context): StockDatabase {
        return Room.databaseBuilder(
            context,
            StockDatabase::class.java,
            "stock_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideStockDao(database: StockDatabase): StockDao {
        return database.stockDao()
    }
}
