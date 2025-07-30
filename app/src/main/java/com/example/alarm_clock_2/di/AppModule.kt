package com.example.alarm_clock_2.di

import android.content.Context
import androidx.room.Room
import com.example.alarm_clock_2.alarm.AlarmScheduler
import com.example.alarm_clock_2.data.AppDatabase
import com.example.alarm_clock_2.data.AlarmDao
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.data.HolidayDao
import com.example.alarm_clock_2.data.HolidayRepository
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.calendar.CalendarRepository
import com.example.alarm_clock_2.domain.usecase.AlarmUseCase
import com.example.alarm_clock_2.domain.usecase.AlarmScheduleUseCase
import com.example.alarm_clock_2.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==================== 数据库相关 ====================

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideHolidayDao(db: AppDatabase): HolidayDao = db.holidayDao()

    // ==================== Repository层 ====================

    @Provides
    @Singleton
    fun provideAlarmRepository(dao: AlarmDao): AlarmRepository = AlarmRepository(dao)

    @Provides
    @Singleton
    fun provideHolidayRepository(dao: HolidayDao): HolidayRepository = HolidayRepository(dao)

    @Provides
    @Singleton
    fun provideCalendarRepository(
        settingsDataStore: SettingsDataStore,
        holidayRepository: HolidayRepository
    ): CalendarRepository = CalendarRepository(settingsDataStore, holidayRepository)

    // ==================== UseCase层 ====================

    @Provides
    @Singleton
    fun provideAlarmUseCase(
        alarmRepository: AlarmRepository,
        settingsDataStore: SettingsDataStore
    ): AlarmUseCase = AlarmUseCase(alarmRepository, settingsDataStore)

    @Provides
    @Singleton
    fun provideAlarmScheduleUseCase(
        alarmScheduler: AlarmScheduler
    ): AlarmScheduleUseCase = AlarmScheduleUseCase(alarmScheduler)
}