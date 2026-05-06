package com.github.musicyou.service

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class PlayableFormatNotFoundException :
    PlaybackException("Playable format not found", null, ERROR_CODE_REMOTE_ERROR)

@OptIn(UnstableApi::class)
class UnplayableException :
    PlaybackException("Unplayable", null, ERROR_CODE_REMOTE_ERROR)

@OptIn(UnstableApi::class)
class LoginRequiredException :
    PlaybackException("Login required", null, ERROR_CODE_REMOTE_ERROR)

@OptIn(UnstableApi::class)
class VideoIdMismatchException :
    PlaybackException("Video id mismatch", null, ERROR_CODE_REMOTE_ERROR)