package net.ib.mn.core.data.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ib.mn.core.data.repository.account.AccountPreferencesRepository
import net.ib.mn.core.data.repository.account.AccountPreferencesRepositoryImpl
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepository
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepositoryImpl
import net.ib.mn.core.utils.Const
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object LocalPreferenceModule {

    @Provides
    fun provideAccountLocalDataSource(
        @Named("account_pref") pref: SharedPreferences,
        @ApplicationContext context: Context,
    ): AccountPreferencesRepository = AccountPreferencesRepositoryImpl(pref, context)

    @Provides
    fun provideIdolLocalDataSource(
        @Named("idol_pref") pref: SharedPreferences,
        @ApplicationContext context: Context,
    ): LanguagePreferenceRepository = LanguagePreferenceRepositoryImpl(pref, context)

    @Singleton
    @Provides
    @Named("account_pref")
    fun provideAccountSharedPreference(
        @ApplicationContext context: Context
    ): SharedPreferences = context.getSharedPreferences(
        Const.PREFS_ACCOUNT,
        Context.MODE_PRIVATE
    )

    @Singleton
    @Provides
    @Named("idol_pref")
    fun provideIdolSharedPreference(
        @ApplicationContext context: Context
    ): SharedPreferences = context.getSharedPreferences(
        Const.PREF_NAME,
        Context.MODE_PRIVATE
    )
}