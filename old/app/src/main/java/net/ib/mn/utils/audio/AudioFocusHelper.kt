/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 미디어 오디오 포커스 관련 클래스.
 *
 * */

package net.ib.mn.utils.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import net.ib.mn.utils.Logger
import net.ib.mn.utils.version.DeviceVersion

/**
 * @see
 * */

class AudioFocusHelper(private val context: Context) {

    // 오디오 포커스 잃었을때 callback 용
    var audioFocusLossCallback: () -> Unit = { }
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // 오디오 포커스 변경 리스너
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
        // 외부에 의해 오디오 포커스 loss되는 상황이면 callback 줘서   volume 무음으로 바꿔줌.
        if (it == AudioManager.AUDIOFOCUS_LOSS) {
            audioFocusLossCallback.invoke()
        }
    }

    init {
        audioFocusRequest = if(DeviceVersion.isAndroid8Later()) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        } else {
            null
        }
    }

    // 오디오 포커스를 가지고온다.
    fun requestAudioFocus() {
        // 백그라운드 music이미 unactive 중이면 return
        if (!audioManager.isMusicActive) {
            return
        }
        if (DeviceVersion.isAndroid8Later()) {
            audioManager.requestAudioFocus(audioFocusRequest ?: return)
        } else {
            // audio focus true를 줘서  현재 라이브 스트리밍 audio에 포커스가 가도록 처리
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
    }

    // 오디오포커스를 다시 돌려준다.
    fun removeAudioFocus() {
        // 백그라운드 music이미 active 중이면 return
        if (audioManager.isMusicActive) {
            return
        }

        if (DeviceVersion.isAndroid8Later()) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest ?: return)
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    // 조건 없이 무조건 audio 없앰처리
    fun removeAudioFocusWithNoCondition() {
        if (DeviceVersion.isAndroid8Later()) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest ?: return)
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AudioFocusHelper? = null

        fun getInstance(
            context: Context,
        ): AudioFocusHelper = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AudioFocusHelper(context).also {
                INSTANCE = it
            }
        }
    }
}