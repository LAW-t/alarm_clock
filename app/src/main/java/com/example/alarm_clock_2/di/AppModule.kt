package com.example.alarm_clock_2.di

import android.content.Context
import androidx.room.Room
import com.example.alarm_clock_2.data.AppDatabase
import com.example.alarm_clock_2.data.AlarmDao
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.data.HolidayDao
import com.example.alarm_clock_2.data.HolidayRepository
import com.example.alarm_clock_2.calendar.CalendarRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "alarm.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides fun provideHolidayDao(db: AppDatabase): HolidayDao = db.holidayDao()

    @Provides @Singleton fun provideHolidayRepository(dao: HolidayDao): HolidayRepository = HolidayRepository(dao)

    @Provides
    @Singleton
    fun provideCalendarRepository(
        settingsDataStore: com.example.alarm_clock_2.data.SettingsDataStore,
        holidayRepository: HolidayRepository
    ): CalendarRepository = CalendarRepository(settingsDataStore, holidayRepository)

    @Provides
    @Singleton
    fun provideAlarmRepository(dao: AlarmDao): AlarmRepository = AlarmRepository(dao)
} 