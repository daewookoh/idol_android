package net.ib.mn.local.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ib.mn.local.di.QualifierNames.APP_PREFS
import net.ib.mn.local.di.QualifierNames.FREE_BOARD_PREFS
import net.ib.mn.local.di.QualifierNames.IDOL_PREFS
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    private const val LEGACY_PREFS_NAME = "com.exodus.myloveidol"

    @Provides
    @Singleton
    @Named(FREE_BOARD_PREFS)
    fun provideFreeBoardPrefs(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            // TODO 차후 마이그레이션 참고용
//            migrations = listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME)),
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = LEGACY_PREFS_NAME,
                    // TODO 마이그레이션 완료되면 위 코드로 변경
                    keysToMigrate = setOf("selectedFreeBoardLanguage", "selectedFreeBoardLanguageId")
                )
            ),
            produceFile = { context.preferencesDataStoreFile(FREE_BOARD_PREFS) }
        )
    }

    @Provides
    @Singleton
    @Named(IDOL_PREFS)
    fun provideIdolPrefs(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(IDOL_PREFS) }
        )
    }

    @Provides
    @Singleton
    @Named(APP_PREFS)
    fun provideBannerPrefs(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(APP_PREFS) }
        )
    }
}