package net.ib.mn.utils

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.ui.PlayerView
import com.airbnb.lottie.LottieAnimationView
import net.ib.mn.databinding.FavoriteRankingItemBinding
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.SRankingItemBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.view.ExodusImageView
import net.ib.mn.view.ProgressBarLayout

class RankingBindingProxy {
    val root: View
    val badgeBirth: AppCompatImageView
    val badgeDebut: AppCompatImageView
    val badgeComeback: AppCompatImageView
    val badgeMemorialDay: AppCompatTextView
    val badgeAllInDay: AppCompatImageView
    val nameView: AppCompatTextView
    val rankView: AppCompatTextView
    val voteCountView: AppCompatTextView
    val containerRanking : LinearLayoutCompat
    val clPhoto : ConstraintLayout
    val imageView: AppCompatImageView
    val voteBtn: AppCompatImageView
    val progressBar: ProgressBarLayout
    val photoBorder: AppCompatImageView
    val groupView: AppCompatTextView
    val progressBarFrame: FrameLayout

    // 이붙
    val containerPhotos: ConstraintLayout
    val photo1: ExodusImageView
    val photo2: ExodusImageView
    val photo3: ExodusImageView
    val playerview1: PlayerView
    val playerview2: PlayerView
    val playerview3: PlayerView
//    val loProgressGradient : LottieAnimationView // 스크롤 최상단으로 튀는 현상때문에 제거

    // 기부천사/기부요정
    val iconAngel: AppCompatTextView
    val iconFairy: AppCompatTextView
    val iconMiracle: AppCompatTextView
    val iconRookie: AppCompatTextView
    val iconSuperRookie: AppCompatTextView

    // 즐겨찾기
    var section: ViewGroup? = null
    var tvSection: AppCompatTextView? = null
    // 바 표시 영역
    var llRankingHeart: ViewGroup? = null

    var rankLottie: LottieAnimationView? = null
    var nameLottie: LottieAnimationView? = null
    var voteLottie: LottieAnimationView? = null

    constructor(binding: SRankingItemBinding) {
        root = binding.root
        badgeBirth = binding.badgeBirth
        badgeDebut = binding.badgeDebut
        badgeComeback = binding.badgeComeback
        badgeMemorialDay = binding.badgeMemorialDay
        badgeAllInDay = binding.badgeAllInDay
        nameView = binding.name
        rankView = binding.rankIndex
        voteCountView = binding.count
        containerRanking = binding.containerRanking
        clPhoto = binding.clPhoto
        imageView = binding.photo
        voteBtn = binding.btnHeart
        progressBar = binding.progress
        photoBorder = binding.photoBorder
        groupView = binding.group
        containerPhotos = binding.containerPhotos
        photo1 = binding.photo1
        photo2 = binding.photo2
        photo3 = binding.photo3
        playerview1 = binding.playerview1
        playerview2 = binding.playerview2
        playerview3 = binding.playerview3
//        loProgressGradient = binding.loProgressGradient
        iconAngel = binding.imageAngel
        iconFairy = binding.imageFairy
        iconMiracle = binding.imageMiracle
        iconRookie = binding.imageRookie
        iconSuperRookie = binding.imageSuperRookie
        progressBarFrame = binding.progressBar
        llRankingHeart = binding.llRankingHeart
        rankLottie = binding.lottieTutorialRankingProfile
        nameLottie = binding.lottieTutorialRankingName
        voteLottie = binding.lottieTutorialVote
    }

    constructor(binding: RankingItemBinding) {
        root = binding.root
        badgeBirth = binding.badgeBirth
        badgeDebut = binding.badgeDebut
        badgeComeback = binding.badgeComeback
        badgeMemorialDay = binding.badgeMemorialDay
        badgeAllInDay = binding.badgeAllInDay
        nameView = binding.name
        rankView = binding.rankIndex
        voteCountView = binding.count
        containerRanking = binding.containerRanking
        clPhoto = binding.clPhoto
        imageView = binding.photo
        voteBtn = binding.btnHeart
        progressBar = binding.progress
        photoBorder = binding.photoBorder
        groupView = binding.group
        containerPhotos = binding.containerPhotos
        photo1 = binding.photo1
        photo2 = binding.photo2
        photo3 = binding.photo3
        playerview1 = binding.playerview1
        playerview2 = binding.playerview2
        playerview3 = binding.playerview3
//        loProgressGradient = binding.loProgressGradient
        iconAngel = binding.imageAngel
        iconFairy = binding.imageFairy
        iconMiracle = binding.imageMiracle
        iconRookie = binding.imageRookie
        iconSuperRookie = binding.imageSuperRookie
        progressBarFrame = binding.progressBar
        llRankingHeart = binding.llRankingHeart
        rankLottie = binding.lottieTutorialRankingProfile
        nameLottie = binding.lottieTutorialRankingName
    }

    constructor(binding: TextureRankingItemBinding) {
        root = binding.root
        badgeBirth = binding.badgeBirth
        badgeDebut = binding.badgeDebut
        badgeComeback = binding.badgeComeback
        badgeMemorialDay = binding.badgeMemorialDay
        badgeAllInDay = binding.badgeAllInDay
        nameView = binding.name
        rankView = binding.rankIndex
        voteCountView = binding.count
        containerRanking = binding.containerRanking
        clPhoto = binding.clPhoto
        imageView = binding.photo
        voteBtn = binding.btnHeart
        progressBar = binding.progress
        photoBorder = binding.photoBorder
        groupView = binding.group
        containerPhotos = binding.containerPhotos
        photo1 = binding.photo1
        photo2 = binding.photo2
        photo3 = binding.photo3
        playerview1 = binding.playerview1
        playerview2 = binding.playerview2
        playerview3 = binding.playerview3
//        loProgressGradient = binding.loProgressGradient
        iconAngel = binding.imageAngel
        iconFairy = binding.imageFairy
        iconMiracle = binding.imageMiracle
        iconRookie = binding.imageRookie
        iconSuperRookie = binding.imageSuperRookie
        progressBarFrame = binding.progressBar
        llRankingHeart = binding.llRankingHeart
        rankLottie = binding.lottieTutorialRankingProfile
        nameLottie = binding.lottieTutorialRankingName
    }

    constructor(binding: FavoriteRankingItemBinding) {
        root = binding.root
        badgeBirth = binding.incFavoriteRankingItem.badgeBirth
        badgeDebut = binding.incFavoriteRankingItem.badgeDebut
        badgeComeback = binding.incFavoriteRankingItem.badgeComeback
        badgeMemorialDay = binding.incFavoriteRankingItem.badgeMemorialDay
        badgeAllInDay = binding.incFavoriteRankingItem.badgeAllInDay
        nameView = binding.incFavoriteRankingItem.name
        rankView = binding.incFavoriteRankingItem.rankIndex
        voteCountView = binding.incFavoriteRankingItem.count
        containerRanking = binding.incFavoriteRankingItem.containerRanking
        clPhoto = binding.incFavoriteRankingItem.clPhoto
        imageView = binding.incFavoriteRankingItem.photo
        voteBtn = binding.incFavoriteRankingItem.btnHeart
        progressBar = binding.incFavoriteRankingItem.progress
        photoBorder = binding.incFavoriteRankingItem.photoBorder
        groupView = binding.incFavoriteRankingItem.group
        containerPhotos = binding.incFavoriteRankingItem.containerPhotos
        photo1 = binding.incFavoriteRankingItem.photo1
        photo2 = binding.incFavoriteRankingItem.photo2
        photo3 = binding.incFavoriteRankingItem.photo3
        playerview1 = binding.incFavoriteRankingItem.playerview1
        playerview2 = binding.incFavoriteRankingItem.playerview2
        playerview3 = binding.incFavoriteRankingItem.playerview3
//        loProgressGradient = binding.loProgressGradient
        iconAngel = binding.incFavoriteRankingItem.imageAngel
        iconFairy = binding.incFavoriteRankingItem.imageFairy
        iconMiracle = binding.incFavoriteRankingItem.imageMiracle
        iconRookie = binding.incFavoriteRankingItem.imageRookie
        iconSuperRookie = binding.incFavoriteRankingItem.imageSuperRookie
        progressBarFrame = binding.incFavoriteRankingItem.progressBar
        llRankingHeart = binding.incFavoriteRankingItem.llRankingHeart

        section = binding.section
        tvSection = binding.textviewSection
    }
}