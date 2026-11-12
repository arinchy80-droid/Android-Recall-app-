package com.example.ui.components

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RecallCyanSecondary
import com.example.ui.theme.RecallVioletPrimary
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sin

@Composable
fun AudioPlayerCard(
    audioPath: String,
    title: String = "Recorded Voice Memory",
    modifier: Modifier = Modifier
) {
    val fileExists = remember(audioPath) {
        try {
            audioPath.isNotBlank() && File(audioPath).exists()
        } catch (_: Exception) {
            false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playerError by remember { mutableStateOf(false) }

    val mediaPlayer = remember(audioPath) {
        if (!fileExists) null
        else {
            try {
                MediaPlayer().apply {
                    setDataSource(audioPath)
                    prepare()
                    totalDurationMs = duration.coerceAtLeast(1000)
                }
            } catch (e: Exception) {
                playerError = true
                null
            }
        }
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            currentPositionMs = 0
            try {
                it.seekTo(0)
            } catch (_: Exception) {
            }
        }
        onDispose {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer.stop()
                }
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isPlaying && mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPositionMs = mediaPlayer.currentPosition
                } else {
                    isPlaying = false
                }
            } catch (_: Exception) {
                isPlaying = false
            }
            delay(150L)
        }
    }

    fun applyPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
                val wasPlaying = mediaPlayer.isPlaying
                val params = PlaybackParams().setSpeed(speed)
                mediaPlayer.playbackParams = params
                if (!wasPlaying) {
                    mediaPlayer.pause()
                }
                isPlaying = wasPlaying
            }
        } catch (_: Exception) {
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        mp.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                    } catch (_: Exception) {
                    }
                }
                mp.start()
                isPlaying = true
            }
        } catch (_: Exception) {
            isPlaying = false
        }
    }

    val progressFraction = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_card"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0B132B).copy(alpha = 0.90f),
        border = BorderStroke(1.dp, RecallCyanSecondary.copy(alpha = 0.50f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause circular button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) RecallCyanSecondary else RecallVioletPrimary)
                        .clickable(enabled = mediaPlayer != null && !playerError) { togglePlayPause() }
                        .testTag("audio_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                        tint = if (isPlaying) Color(0xFF040711) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = RecallCyanSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Waveform bars visualization
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    ) {
                        val barCount = 32
                        val spacing = 3.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

                        for (i in 0 until barCount) {
                            val barProgress = i.toFloat() / barCount.toFloat()
                            val waveHeightRatio = (0.3f + 0.7f * kotlin.math.abs(sin(i * 0.65f))).coerceIn(0.25f, 1f)
                            val barHeight = size.height * waveHeightRatio
                            val topOffset = (size.height - barHeight) / 2f
                            val isPlayed = barProgress <= progressFraction

                            drawRoundRect(
                                color = if (isPlayed) RecallCyanSecondary else Color(0xFF475569),
                                topLeft = Offset(i * (barWidth + spacing), topOffset),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Speed selector pill (1x -> 1.25x -> 1.5x -> 2x)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RecallVioletPrimary.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, RecallVioletPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val nextSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            applyPlaybackSpeed(nextSpeed)
                        }
                        .testTag("audio_speed_button")
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Interactive Slider Scrubber & Time Counter
            if (mediaPlayer != null && !playerError) {
                Slider(
                    value = progressFraction,
                    onValueChange = { newFrac ->
                        val targetMs = (newFrac * totalDurationMs).toInt()
                        currentPositionMs = targetMs
                        try {
                            mediaPlayer.seekTo(targetMs)
                        } catch (_: Exception) {
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = RecallCyanSecondary,
                        activeTrackColor = RecallCyanSecondary,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DateUtils.formatSecondsToTime((currentPositionMs / 1000).coerceAtLeast(0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                    Text(
                        text = DateUtils.formatSecondsToTime((totalDurationMs / 1000).coerceAtLeast(1)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Audio file saved (${File(audioPath).name})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }
        }
    }
}
