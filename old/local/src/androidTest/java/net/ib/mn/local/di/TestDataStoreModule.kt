package net.ib.mn.local.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import net.ib.mn.local.di.QualifierNames.APP_PREFS
import net.ib.mn.local.di.QualifierNames.FREE_BOARD_PREFS
import net.ib.mn.local.di.QualifierNames.IDOL_PREFS
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataStoreModule::class]
)
object TestDataStoreModule {

    @Provides
    @Singleton
    @Named(FREE_BOARD_PREFS)
    fun provideFreeBoardPrefs(@ApplicationContext context: Context): DataStore<Preferences> {
        val testFile = File(context.filesDir, "test_prefs_${System.currentTimeMillis()}.preferences_pb")
        return PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )
    }

    @Provides
    @Singleton
    @Named(IDOL_PREFS)
    fun provideIdolPrefs(@ApplicationContext context: Context): DataStore<Preferences> {
        val testFile = File(context.filesDir, "test_idol_prefs_${System.currentTimeMillis()}.preferences_pb")
        return PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )
    }

    @Provides
    @Singleton
    @Named(APP_PREFS)
    fun provideAppPrefs(@ApplicationContext context: Context): DataStore<Preferences> {
        val testFile = File(context.filesDir, "test_app_prefs_${System.currentTimeMillis()}.preferences_pb")
        return PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )
    }
}