package net.ib.mn.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ib.mn.common.exception.ErrorHandlerImpl
import net.ib.mn.lib.component.ErrorHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindErrorHandler(errorHandler: ErrorHandlerImpl): ErrorHandler

}