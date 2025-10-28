/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ib.mn.core.data.repository.AuthRepository
import net.ib.mn.core.data.repository.AuthRepositoryImpl
import net.ib.mn.core.data.repository.AwardsRepository
import net.ib.mn.core.data.repository.AwardsRepositoryImpl
import net.ib.mn.core.data.repository.BlocksRepository
import net.ib.mn.core.data.repository.BlocksRepositoryImpl
import net.ib.mn.core.data.repository.EmoticonRepository
import net.ib.mn.core.data.repository.EmoticonRepositoryImpl
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.FavoritesRepositoryImpl
import net.ib.mn.core.data.repository.FilesRepository
import net.ib.mn.core.data.repository.FilesRepositoryImpl
import net.ib.mn.core.data.repository.GameRepository
import net.ib.mn.core.data.repository.GameRepositoryImpl
import net.ib.mn.core.data.repository.HeartpickRepository
import net.ib.mn.core.data.repository.HeartpickRepositoryImpl
import net.ib.mn.core.data.repository.HofsRepository
import net.ib.mn.core.data.repository.HofsRepositoryImpl
import net.ib.mn.core.data.repository.ImagesRepository
import net.ib.mn.core.data.repository.ImagesRepositoryImpl
import net.ib.mn.core.data.repository.MessagesRepository
import net.ib.mn.core.data.repository.MessagesRepositoryImpl
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.MiscRepositoryImpl
import net.ib.mn.core.data.repository.MissionsRepository
import net.ib.mn.core.data.repository.MissionsRepositoryImpl
import net.ib.mn.core.data.repository.NoticeEventRepository
import net.ib.mn.core.data.repository.NoticeEventRepositoryImpl
import net.ib.mn.core.data.repository.OnepickRepository
import net.ib.mn.core.data.repository.OnepickRepositoryImpl
import net.ib.mn.core.data.repository.PlayRepository
import net.ib.mn.core.data.repository.PlayRepositoryImpl
import net.ib.mn.core.data.repository.QuizRepository
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.core.data.repository.RedirectRepository
import net.ib.mn.core.data.repository.RedirectRepositoryImpl
import net.ib.mn.core.data.repository.ScheduleRepository
import net.ib.mn.core.data.repository.ScheduleRepositoryImpl
import net.ib.mn.core.data.repository.SearchRepository
import net.ib.mn.core.data.repository.SearchRepositoryImpl
import net.ib.mn.core.data.repository.StampsRepository
import net.ib.mn.core.data.repository.StampsRepositoryImpl
import net.ib.mn.core.data.repository.SupportRepository
import net.ib.mn.core.data.repository.ThemepickRepository
import net.ib.mn.core.data.repository.ThemepickRepositoryImpl
import net.ib.mn.core.data.repository.TimestampRepository
import net.ib.mn.core.data.repository.TimestampRepositoryImpl
import net.ib.mn.core.data.repository.TrendsRepository
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.UsersRepositoryImpl
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.core.data.repository.certificate.VoteCertificateRepository
import net.ib.mn.core.data.repository.certificate.VoteCertificateRepositoryImpl
import net.ib.mn.core.data.repository.charts.ChartsRepository
import net.ib.mn.core.data.repository.charts.ChartsRepositoryImpl
import net.ib.mn.core.data.repository.comments.CommentsRepository
import net.ib.mn.core.data.repository.comments.CommentsRepositoryImpl
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.data.repository.config.ConfigRepositoryImpl
import net.ib.mn.core.data.repository.friends.FriendsRepository
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.data.repository.idols.IdolsRepositoryImpl
import net.ib.mn.core.data.repository.recommend.RecommendRepository
import net.ib.mn.core.data.repository.recommend.RecommendRepositoryImpl


/**
 * @see
 * */

@InstallIn(SingletonComponent::class)
@Module
internal abstract class DataModule {

    @Binds
    abstract fun bindsIdolsRepository(
        idolsRepositoryImpl: IdolsRepositoryImpl
    ): IdolsRepository

    @Binds
    abstract fun bindsConfigRepository(
        configRepositoryImpl: ConfigRepositoryImpl
    ): ConfigRepository

    @Binds
    abstract fun bindsChartsRepository(
        chartsRepositoryImpl: ChartsRepositoryImpl
    ): ChartsRepository

    @Binds
    abstract fun bindsArticlesRepository(
        articlesRepositoryImpl: ArticlesRepositoryImpl
    ): ArticlesRepository

    @Binds
    abstract fun bindsQuizRepository(
        quizRepositoryImpl: QuizRepositoryImpl
    ): QuizRepository

    @Binds
    abstract fun bindScheduleRepository(
        scheduleRepositoryImpl: ScheduleRepositoryImpl
    ): ScheduleRepository

    @Binds
    abstract fun bindHeartpickRepository(
        heartpickRepositoryImpl: HeartpickRepositoryImpl
    ): HeartpickRepository

    @Binds
    abstract fun bindOnepickRepository(
        onepickRepositoryImpl: OnepickRepositoryImpl
    ): OnepickRepository

    @Binds
    abstract fun bindThemepickRepository(
        themepickRepositoryImpl: ThemepickRepositoryImpl
    ): ThemepickRepository

    @Binds
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    abstract fun bindPlayRepository(
        playRepositoryImpl: PlayRepositoryImpl
    ): PlayRepository

    @Binds
    abstract fun bindTrendsRepository(
        trendsRepositoryImpl: TrendsRepositoryImpl
    ): TrendsRepository

    @Binds
    abstract fun bindAwardsRepository(
        awardsRepository: AwardsRepositoryImpl
    ): AwardsRepository

    @Binds
    abstract fun bindMessagesRepository(
        messagesRepositoryImpl: MessagesRepositoryImpl
    ): MessagesRepository

    @Binds
    abstract fun bindUsersRepository(
        usersRepositoryImpl: UsersRepositoryImpl
    ): UsersRepository

    @Binds
    abstract fun bindTimestampRepository(
        timestampRepositoryImpl: TimestampRepositoryImpl
    ): TimestampRepository

    @Binds
    abstract fun bindsVoteCertificateRepository(
        voteCertificateRepositoryImpl: VoteCertificateRepositoryImpl
    ): VoteCertificateRepository

    @Binds
    abstract fun bindsFavoritesRepository(
        favoritesRepositoryImpl: FavoritesRepositoryImpl
    ): FavoritesRepository

    @Binds
    abstract fun bindsNoticeEventRepository(
        noticeEventRepositoryImpl: NoticeEventRepositoryImpl
    ): NoticeEventRepository

    @Binds
    abstract fun bindsHofsRepository(
        hofsRepositoryImpl: HofsRepositoryImpl
    ): HofsRepository

    @Binds
    abstract fun bindsCommentsRepository(
        commentsRepositoryImpl: CommentsRepositoryImpl
    ): CommentsRepository

    @Binds
    abstract fun bindsBlocksRepository(
        blocksRepositoryImpl: BlocksRepositoryImpl
    ): BlocksRepository

    @Binds
    abstract fun bindsMissionsRepository(
        missionsRepositoryImpl: MissionsRepositoryImpl
    ): MissionsRepository

    @Binds
    abstract fun bindsRedirectRepository(
        redirectRepositoryImpl: RedirectRepositoryImpl
    ): RedirectRepository

    @Binds
    abstract fun bindsAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindsStampRepository(
        stampsRepositoryImpl: StampsRepositoryImpl
    ): StampsRepository

    @Binds
    abstract fun bindsEmoticonRepository(
        emoticonRepositoryImpl: EmoticonRepositoryImpl
    ): EmoticonRepository

    @Binds
    abstract fun bindsImagesRepository(
        imagesRepositoryImpl: ImagesRepositoryImpl
    ): ImagesRepository

    @Binds
    abstract fun bindsFilesRepository(
        filesRepositoryImpl: FilesRepositoryImpl
    ): FilesRepository

    @Binds
    abstract fun bindsMiscRepository(
        miscRepositoryImpl: MiscRepositoryImpl
    ): MiscRepository

    @Binds
    abstract fun bindsFriendsRepository(
        friendsRepositoryImpl: FriendsRepositoryImpl
    ): FriendsRepository

    @Binds
    abstract fun bindsRecommendRepository(
        recommendRepositoryImpl: RecommendRepositoryImpl
    ): RecommendRepository

    @Binds
    abstract fun bindsGameRepository(
        gameRepositoryImpl: GameRepositoryImpl
    ): GameRepository
}