package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.SolidColor

enum class WallpaperTheme(val id: String, val displayName: String, val subtitle: String) {
    DAYLIGHT_ETHEREAL("daylight_ethereal", "Memorae Daylight", "Luminous daytime sky with floating world clouds"),
    TWILIGHT_SUNSET("twilight_sunset", "Cosmic Neural Brain", "Deep space neural network cosmos"),
    NEBULA_VIOLET("nebula_violet", "Nebula Violet", "Deep purple & indigo neural glow"),
    CYBER_OCEAN("cyber_ocean", "Cyber Ocean", "Bioluminescent teal & deep slate"),
    OLED_MIDNIGHT("oled_midnight", "OLED Midnight", "Darkened cosmic neural contrast"),
    CUSTOM_PHOTO("custom_photo", "Custom Photo", "Your personal gallery image with scrim")
}

object AppVisualPreferences {
    private const val PREFS_NAME = "recall_app_prefs"
    private const val KEY_THEME = "pref_wallpaper_theme_v3"
    private const val KEY_CUSTOM_PATH = "pref_custom_wallpaper_path"
    private const val KEY_COMPACT_STREAM = "pref_compact_stream"
    private const val KEY_HAPTICS = "pref_haptics_enabled"
    private const val KEY_REMINDER_HOUR = "pref_debrief_reminder_hour"

    private val _selectedTheme = MutableStateFlow(WallpaperTheme.DAYLIGHT_ETHEREAL)
    val selectedTheme: StateFlow<WallpaperTheme> = _selectedTheme.asStateFlow()

    private val _customWallpaperPath = MutableStateFlow<String?>(null)
    val customWallpaperPath: StateFlow<String?> = _customWallpaperPath.asStateFlow()

    private val _isCompactStream = MutableStateFlow(false)
    val isCompactStream: StateFlow<Boolean> = _isCompactStream.asStateFlow()

    private val _isHapticsEnabled = MutableStateFlow(true)
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _debriefReminderHour = MutableStateFlow(20) // 8:00 PM default, -1 = Off
    val debriefReminderHour: StateFlow<Int> = _debriefReminderHour.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val themeId = prefs.getString(KEY_THEME, WallpaperTheme.DAYLIGHT_ETHEREAL.id)
                _selectedTheme.value = WallpaperTheme.entries.find { it.id == themeId } ?: WallpaperTheme.DAYLIGHT_ETHEREAL
                _customWallpaperPath.value = prefs.getString(KEY_CUSTOM_PATH, null)
                _isCompactStream.value = prefs.getBoolean(KEY_COMPACT_STREAM, false)
                _isHapticsEnabled.value = prefs.getBoolean(KEY_HAPTICS, true)
                _debriefReminderHour.value = prefs.getInt(KEY_REMINDER_HOUR, 20)
            } catch (_: Exception) {
            }
        }
    }

    fun setTheme(context: Context, theme: WallpaperTheme) {
        _selectedTheme.value = theme
        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, theme.id)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun saveCustomWallpaperUri(context: Context, uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val rawBytes = input.use { it.readBytes() }
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > 720 || boundsOpts.outHeight / sampleSize > 1440) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return false
            val outFile = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            cachedCustomBitmap = bmp.asImageBitmap()
            _customWallpaperPath.value = outFile.absolutePath
            _selectedTheme.value = WallpaperTheme.CUSTOM_PHOTO
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_PATH, outFile.absolutePath)
                .putString(KEY_THEME, WallpaperTheme.CUSTOM_PHOTO.id)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setCompactStream(context: Context, compact: Boolean) {
        _isCompactStream.value = compact
        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPACT_STREAM, compact)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAPTICS, enabled)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun setDebriefReminderHour(context: Context, hour: Int) {
        _debriefReminderHour.value = hour
        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_REMINDER_HOUR, hour)
                .apply()
            com.example.util.NotificationHelper.scheduleDailyDebriefAlarm(
                context = context.applicationContext,
                hourOfDay = hour,
                minute = 0
            )
        } catch (_: Exception) {
        }
    }
}

@Volatile
private var cachedCustomBitmap: ImageBitmap? = null

private fun getSafeCustomBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    cachedCustomBitmap?.let { return it }
    return try {
        val file = File(path)
        if (!file.exists()) return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()?.also {
            cachedCustomBitmap = it
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Cosmic Neural Brain Artwork Wallpaper across all screens of the app,
 * with a balanced readability scrim so the background artwork shines clearly
 * while all typography, action items, chips, and cards remain crisp and legible.
 */
@Composable
fun NeuralAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(context) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AppVisualPreferences.ensureInitialized(context)
        }
    }

    val selectedTheme by AppVisualPreferences.selectedTheme.collectAsState()
    val customPath by AppVisualPreferences.customWallpaperPath.collectAsState()

    val customBitmapState = remember(customPath) { androidx.compose.runtime.mutableStateOf(cachedCustomBitmap) }
    val customBitmap = customBitmapState.value

    androidx.compose.runtime.LaunchedEffect(customPath) {
        if (!customPath.isNullOrBlank()) {
            val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                getSafeCustomBitmap(customPath)
            }
            customBitmapState.value = loaded
        } else {
            customBitmapState.value = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (selectedTheme == WallpaperTheme.DAYLIGHT_ETHEREAL) MemoraeDaylightSkyTop else Color(0xFF040711))
    ) {
        if (selectedTheme == WallpaperTheme.CUSTOM_PHOTO && customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (selectedTheme == WallpaperTheme.DAYLIGHT_ETHEREAL) {
            // Memorae Ethereal Daylight Sky Atmosphere
            // 1. Sky Gradient from Top Blue to Horizon Peach/White
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MemoraeDaylightSkyTop,
                                MemoraeDaylightSkyMid,
                                MemoraeDaylightSkyHorizon,
                                Color(0xFFF0F6FF)
                            )
                        )
                    )
            )

            // 2. Soft Ambient Sun Glow at Top
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color(0xFFFFFBEB).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(500f, -100f),
                            radius = 900f
                        )
                    )
            )

            // 3. Volumetric Floating Ambient Clouds & Floating World Horizon Depth
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top-left soft cloud puff
                drawOval(
                    color = Color.White.copy(alpha = 0.50f),
                    topLeft = Offset(-w * 0.15f, h * 0.03f),
                    size = Size(w * 0.65f, h * 0.12f)
                )

                // Top-right floating cloud
                drawOval(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.60f, h * 0.01f),
                    size = Size(w * 0.60f, h * 0.11f)
                )

                // Mid-right ambient cloud (behind cards)
                drawOval(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(w * 0.70f, h * 0.38f),
                    size = Size(w * 0.50f, h * 0.10f)
                )

                // Lower horizon atmospheric mist layer
                drawOval(
                    color = Color.White.copy(alpha = 0.40f),
                    topLeft = Offset(-w * 0.10f, h * 0.72f),
                    size = Size(w * 1.20f, h * 0.22f)
                )
            }
        } else {
            // Cosmic Neural Brain wallpaper across all screens
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.bg_recall_wallpaper),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Optional subtle thematic color glaze when alternate preset is chosen
            when (selectedTheme) {
                WallpaperTheme.NEBULA_VIOLET -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color(0xFF1E1040).copy(alpha = 0.22f),
                                    0.5f to Color(0xFF2D124D).copy(alpha = 0.25f),
                                    1.0f to Color(0xFF060410).copy(alpha = 0.30f)
                                )
                            )
                    )
                }
                WallpaperTheme.CYBER_OCEAN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color(0xFF030D14).copy(alpha = 0.22f),
                                    0.5f to Color(0xFF082838).copy(alpha = 0.26f),
                                    1.0f to Color(0xFF030910).copy(alpha = 0.30f)
                                )
                            )
                    )
                }
                WallpaperTheme.OLED_MIDNIGHT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF020409).copy(alpha = 0.45f))
                    )
                }
                else -> Unit
            }

            // Balanced Readability Vignette Scrim for Cosmic Themes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xFF040711).copy(alpha = 0.24f),
                            0.25f to Color(0xFF060A17).copy(alpha = 0.14f),
                            0.50f to Color(0xFF070B19).copy(alpha = 0.10f),
                            0.75f to Color(0xFF060A17).copy(alpha = 0.18f),
                            1.0f to Color(0xFF040711).copy(alpha = 0.36f)
                        )
                    )
            )
        }

        // Page UI Content Layer
        content()
    }
}

/**
 * Bespoke Glowing Dual-Hemisphere Neon Brain Emblem matching the reference header icon.
 */
@Composable
fun NeonBrainIcon(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val cornerRadius = size * 0.24f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        RecallCyanSecondary.copy(alpha = 0.85f),
                        Color(0xFF166093).copy(alpha = 0.75f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_recall_logo),
            contentDescription = "Recall App Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Bespoke Gradient Calendar-Check Icon matching the reference calendar card header.
 */
@Composable
fun NeonCalendarCheckIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RecallCyanSecondary.copy(alpha = 0.25f),
                        RecallVioletPrimary.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(RecallCyanSecondary, RecallVioletPrimary)
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.58f)) {
            val w = this.size.width
            val h = this.size.height
            val brush = Brush.linearGradient(listOf(RecallCyanSecondary, Color(0xFFC084FC)))

            // Calendar outer rounded box
            drawRoundRect(
                brush = brush,
                topLeft = Offset(w * 0.08f, h * 0.18f),
                size = Size(w * 0.84f, h * 0.74f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            // Top binding rings
            drawLine(
                color = RecallCyanSecondary,
                start = Offset(w * 0.30f, h * 0.06f),
                end = Offset(w * 0.30f, h * 0.26f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFC084FC),
                start = Offset(w * 0.70f, h * 0.06f),
                end = Offset(w * 0.70f, h * 0.26f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Checkmark inside calendar
            val checkPath = Path().apply {
                moveTo(w * 0.28f, h * 0.56f)
                lineTo(w * 0.45f, h * 0.72f)
                lineTo(w * 0.74f, h * 0.40f)
            }
            drawPath(
                path = checkPath,
                color = RecallCyanSecondary,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Bespoke Glowing Lightbulb Icon for Natural Language Search Suggestions.
 */
@Composable
fun NeonLightbulbIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Outer ambient glow
        drawCircle(
            color = RecallCyanSecondary.copy(alpha = 0.25f),
            radius = w * 0.52f,
            center = Offset(w * 0.5f, h * 0.42f)
        )
        // Bulb dome
        drawCircle(
            brush = Brush.verticalGradient(listOf(RecallCyanSecondary, Color(0xFFFBBF24))),
            radius = w * 0.32f,
            center = Offset(w * 0.5f, h * 0.40f),
            style = Stroke(width = 1.8.dp.toPx())
        )
        // Base lines
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(w * 0.36f, h * 0.78f),
            end = Offset(w * 0.64f, h * 0.78f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = RecallCyanSecondary,
            start = Offset(w * 0.42f, h * 0.90f),
            end = Offset(w * 0.58f, h * 0.90f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}


/**
 * High-Definition 3D Glossy Dark-Blue Orb Icon Types modeled after the reference image.
 */
enum class OrbIconType {
    PLUS,
    TEXT_EDIT,
    GALLERY,
    ASK_AI,
    STREAM,
    PEOPLE,
    DEBRIEF_MIC,
    CAMERA,
    CONFIRM_CHECK,
    SEND,
    CLOSE,
    STOP,
    BELL,
    CUSTOM_VECTOR
}

/**
 * Renders a high-definition 3D Glossy Dark-Blue Orb Icon using the reference orb assets
 * (`ic_orb_plus`, `ic_orb_text_edit`, `ic_orb_gallery`, `ic_orb_ask_ai`, and `ic_orb_blank`
 * with 3D sculpted silver-white metallic emblems and drop shadows).
 */
@Composable
fun Glossy3DOrbIcon(
    orbType: OrbIconType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    customVector: ImageVector? = null,
    contentDescription: String? = null,
    isHighlighted: Boolean = false
) {
    val drawableRes = when (orbType) {
        OrbIconType.PLUS -> R.drawable.ic_orb_plus
        OrbIconType.TEXT_EDIT -> R.drawable.ic_orb_text_edit
        OrbIconType.GALLERY -> R.drawable.ic_orb_gallery
        OrbIconType.ASK_AI -> R.drawable.ic_orb_ask_ai
        else -> R.drawable.ic_orb_blank
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                RecallCyanSecondary,
                                Color(0xFF93C5FD),
                                RecallVioletPrimary
                            )
                        ),
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // For synthesized 3D emblems on top of the clean 3D glossy dark-blue orb base (ic_orb_blank)
        when (orbType) {
            OrbIconType.PLUS,
            OrbIconType.TEXT_EDIT,
            OrbIconType.GALLERY,
            OrbIconType.ASK_AI -> Unit

            OrbIconType.STREAM -> {
                Canvas(modifier = Modifier.size(size * 0.54f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                    val shadowColor = Color(0xFF040B16).copy(alpha = 0.65f)

                    // 3 stacked 3D silver cards representing the Memory Stream
                    val barHeights = listOf(0.14f, 0.43f, 0.72f)
                    val barWidths = listOf(0.90f, 0.78f, 0.88f)
                    barHeights.forEachIndexed { idx, topRatio ->
                        val bw = w * barWidths[idx]
                        val bh = h * 0.19f
                        val left = (w - bw) / 2f
                        val top = h * topRatio
                        // Drop shadow
                        drawRoundRect(
                            color = shadowColor,
                            topLeft = Offset(left + 1.5.dp.toPx(), top + 2.dp.toPx()),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bh * 0.45f, bh * 0.45f)
                        )
                        // 3D silver bar
                        drawRoundRect(
                            brush = silverBrush,
                            topLeft = Offset(left, top),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bh * 0.45f, bh * 0.45f)
                        )
                    }
                }
            }

            OrbIconType.PEOPLE -> {
                Canvas(modifier = Modifier.size(size * 0.56f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                    val secondarySilverBrush = Brush.verticalGradient(
                        listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF64748B))
                    )
                    val shadowColor = Color(0xFF040B16).copy(alpha = 0.65f)

                    // Secondary person (right rear)
                    drawCircle(
                        color = shadowColor,
                        radius = w * 0.14f,
                        center = Offset(w * 0.69f, h * 0.34f)
                    )
                    drawCircle(
                        brush = secondarySilverBrush,
                        radius = w * 0.14f,
                        center = Offset(w * 0.67f, h * 0.31f)
                    )
                    val rightShoulders = Path().apply {
                        moveTo(w * 0.46f, h * 0.80f)
                        quadraticTo(w * 0.67f, h * 0.50f, w * 0.90f, h * 0.80f)
                        close()
                    }
                    drawPath(rightShoulders, brush = secondarySilverBrush)

                    // Primary person (front left-center)
                    drawCircle(
                        color = shadowColor,
                        radius = w * 0.18f,
                        center = Offset(w * 0.41f, h * 0.31f)
                    )
                    drawCircle(
                        brush = silverBrush,
                        radius = w * 0.18f,
                        center = Offset(w * 0.38f, h * 0.28f)
                    )
                    val mainShoulders = Path().apply {
                        moveTo(w * 0.10f, h * 0.86f)
                        quadraticTo(w * 0.38f, h * 0.48f, w * 0.68f, h * 0.86f)
                        close()
                    }
                    drawPath(mainShoulders, brush = silverBrush)
                }
            }

            OrbIconType.DEBRIEF_MIC -> {
                Canvas(modifier = Modifier.size(size * 0.56f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                    val shadowColor = Color(0xFF040B16).copy(alpha = 0.65f)

                    // Capsule shadow
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = Offset(w * 0.36f, h * 0.11f),
                        size = Size(w * 0.32f, h * 0.48f),
                        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                    )
                    // 3D Silver Mic Capsule
                    drawRoundRect(
                        brush = silverBrush,
                        topLeft = Offset(w * 0.34f, h * 0.08f),
                        size = Size(w * 0.32f, h * 0.48f),
                        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                    )
                    // U-cradle arc
                    drawArc(
                        brush = silverBrush,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.22f, h * 0.26f),
                        size = Size(w * 0.56f, h * 0.46f),
                        style = Stroke(width = (w * 0.09f).coerceAtLeast(2f), cap = StrokeCap.Round)
                    )
                    // Vertical stem & base
                    drawLine(
                        brush = silverBrush,
                        start = Offset(w * 0.50f, h * 0.72f),
                        end = Offset(w * 0.50f, h * 0.90f),
                        strokeWidth = (w * 0.09f).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        brush = silverBrush,
                        start = Offset(w * 0.32f, h * 0.90f),
                        end = Offset(w * 0.68f, h * 0.90f),
                        strokeWidth = (w * 0.09f).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                }
            }

            OrbIconType.CAMERA -> {
                Canvas(modifier = Modifier.size(size * 0.56f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                    val shadowColor = Color(0xFF040B16).copy(alpha = 0.65f)

                    // Top prism hump
                    val prismPath = Path().apply {
                        moveTo(w * 0.32f, h * 0.26f)
                        lineTo(w * 0.40f, h * 0.14f)
                        lineTo(w * 0.60f, h * 0.14f)
                        lineTo(w * 0.68f, h * 0.26f)
                        close()
                    }
                    drawPath(prismPath, brush = silverBrush)

                    // Camera body shadow & 3D body
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = Offset(w * 0.12f, h * 0.28f),
                        size = Size(w * 0.80f, h * 0.58f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
                    )
                    drawRoundRect(
                        brush = silverBrush,
                        topLeft = Offset(w * 0.10f, h * 0.25f),
                        size = Size(w * 0.80f, h * 0.58f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
                    )
                    // Dark-blue recessed lens barrel & bright inner reflection
                    drawCircle(
                        color = Color(0xFF0E2238),
                        radius = w * 0.21f,
                        center = Offset(w * 0.50f, h * 0.54f)
                    )
                    drawCircle(
                        brush = silverBrush,
                        radius = w * 0.14f,
                        center = Offset(w * 0.50f, h * 0.54f),
                        style = Stroke(width = (w * 0.05f).coerceAtLeast(1.5f))
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.045f,
                        center = Offset(w * 0.45f, h * 0.49f)
                    )
                }
            }

            OrbIconType.CONFIRM_CHECK -> {
                Canvas(modifier = Modifier.size(size * 0.54f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                    )
                    val shadowPath = Path().apply {
                        moveTo(w * 0.18f, h * 0.55f)
                        lineTo(w * 0.42f, h * 0.78f)
                        lineTo(w * 0.86f, h * 0.28f)
                    }
                    drawPath(
                        path = shadowPath,
                        color = Color(0xFF040B16).copy(alpha = 0.70f),
                        style = Stroke(width = (w * 0.16f).coerceAtLeast(2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val checkPath = Path().apply {
                        moveTo(w * 0.16f, h * 0.52f)
                        lineTo(w * 0.40f, h * 0.75f)
                        lineTo(w * 0.84f, h * 0.25f)
                    }
                    drawPath(
                        path = checkPath,
                        brush = silverBrush,
                        style = Stroke(width = (w * 0.16f).coerceAtLeast(2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            OrbIconType.SEND -> {
                Canvas(modifier = Modifier.size(size * 0.52f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                    val planePath = Path().apply {
                        moveTo(w * 0.12f, h * 0.20f)
                        lineTo(w * 0.88f, h * 0.50f)
                        lineTo(w * 0.12f, h * 0.80f)
                        lineTo(w * 0.28f, h * 0.50f)
                        close()
                    }
                    drawPath(planePath, brush = silverBrush)
                }
            }

            OrbIconType.CLOSE -> {
                Canvas(modifier = Modifier.size(size * 0.46f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = (w * 0.16f).coerceAtLeast(2.5f)
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFCBD5E1))
                    )
                    drawLine(
                        brush = silverBrush,
                        start = Offset(w * 0.18f, h * 0.18f),
                        end = Offset(w * 0.82f, h * 0.82f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        brush = silverBrush,
                        start = Offset(w * 0.82f, h * 0.18f),
                        end = Offset(w * 0.18f, h * 0.82f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            OrbIconType.STOP -> {
                Canvas(modifier = Modifier.size(size * 0.44f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFCA5A5), Color(0xFFEF4444))),
                        topLeft = Offset(w * 0.15f, h * 0.15f),
                        size = Size(w * 0.70f, h * 0.70f),
                        cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
                    )
                }
            }

            OrbIconType.BELL -> {
                Canvas(modifier = Modifier.size(size * 0.52f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val silverBrush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                    val shadowColor = Color(0xFF040B16).copy(alpha = 0.65f)

                    // Bell body
                    val bellPath = Path().apply {
                        moveTo(w * 0.50f, h * 0.14f)
                        cubicTo(w * 0.35f, h * 0.22f, w * 0.28f, h * 0.45f, w * 0.24f, h * 0.68f)
                        lineTo(w * 0.18f, h * 0.74f)
                        lineTo(w * 0.82f, h * 0.74f)
                        lineTo(w * 0.76f, h * 0.68f)
                        cubicTo(w * 0.72f, h * 0.45f, w * 0.65f, h * 0.22f, w * 0.50f, h * 0.14f)
                        close()
                    }
                    drawPath(bellPath, color = shadowColor)
                    drawPath(bellPath, brush = silverBrush)
                    // Clapper
                    drawCircle(
                        brush = silverBrush,
                        radius = w * 0.10f,
                        center = Offset(w * 0.50f, h * 0.82f)
                    )
                }
            }

            OrbIconType.CUSTOM_VECTOR -> {
                if (customVector != null) {
                    // Subtle drop shadow + bright silver-white foreground icon
                    Icon(
                        imageVector = customVector,
                        contentDescription = null,
                        tint = Color(0xFF040B16).copy(alpha = 0.70f),
                        modifier = Modifier
                            .size(size * 0.50f)
                            .padding(top = 1.5.dp, start = 1.dp)
                    )
                    Icon(
                        imageVector = customVector,
                        contentDescription = contentDescription,
                        tint = Color(0xFFF8FAFC),
                        modifier = Modifier.size(size * 0.50f)
                    )
                }
            }
        }
    }
}

/**
 * High-Definition 3D Glossy Dark-Blue Confirmation & Function Button
 * styled after the user's attached reference orb image.
 */
@Composable
fun Glossy3DActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbType: OrbIconType = OrbIconType.CONFIRM_CHECK,
    customIcon: ImageVector? = null,
    enabled: Boolean = true,
    isAccentCyan: Boolean = false,
    height: Dp = 44.dp
) {
    val bgBrush = if (!enabled) {
        Brush.verticalGradient(
            listOf(Color(0xFF1E293B).copy(alpha = 0.75f), Color(0xFF0F172A).copy(alpha = 0.75f))
        )
    } else if (isAccentCyan) {
        Brush.verticalGradient(
            listOf(Color(0xFF234569), Color(0xFF132A46), Color(0xFF0A192F))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF284666), Color(0xFF152942), Color(0xFF0B1829))
        )
    }

    val borderBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF93C5FD).copy(alpha = if (enabled) 0.85f else 0.35f),
            Color(0xFF3B5978).copy(alpha = if (enabled) 0.90f else 0.35f),
            Color(0xFF1E344D).copy(alpha = if (enabled) 0.95f else 0.35f)
        )
    )

    Surface(
        shape = RoundedCornerShape(height / 2),
        color = Color.Transparent,
        shadowElevation = if (enabled) 6.dp else 0.dp,
        modifier = modifier
            .heightIn(min = height)
            .clip(RoundedCornerShape(height / 2))
            .background(bgBrush)
            .border(1.5.dp, borderBrush, RoundedCornerShape(height / 2))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Glossy3DOrbIcon(
                    orbType = if (customIcon != null && orbType == OrbIconType.CUSTOM_VECTOR) OrbIconType.CUSTOM_VECTOR else orbType,
                    customVector = customIcon,
                    size = (height - 12.dp).coerceIn(20.dp, 30.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.sp,
                    color = if (enabled) Color.White else Color(0xFF94A3B8),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Circular 3D Glossy Dark-Blue Orb Icon Button matching the reference icon style.
 */
@Composable
fun GlassCircleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconTint: Color = Color(0xFFE2E8F0),
    orbType: OrbIconType = OrbIconType.CUSTOM_VECTOR,
    borderColors: List<Color> = listOf(
        RecallCyanSecondary.copy(alpha = 0.7f),
        RecallVioletPrimary.copy(alpha = 0.7f)
    )
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Glossy3DOrbIcon(
            orbType = orbType,
            customVector = icon,
            contentDescription = contentDescription,
            size = size
        )
    }
}


/**
 * Ultra-high-definition Cybernetic Navigation Bar with frosted glassmorphism,
 * glowing top laser beam, and bespoke 3D glossy dark-blue orb tab items matching the reference image.
 */
@Composable
fun HighDefinitionNavBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF070F1C).copy(alpha = 0.94f),
        tonalElevation = 12.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Glowing top border line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF60A5FA).copy(alpha = 0.5f),
                            RecallCyanSecondary.copy(alpha = 0.9f),
                            Color(0xFF93C5FD).copy(alpha = 0.95f),
                            RecallVioletPrimary.copy(alpha = 0.5f)
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

/**
 * High-definition tab item featuring the 3D Glossy Dark-Blue Orb Icon from the reference image,
 * active glowing glass capsule, neon underline indicator, and badge counter.
 */
@Composable
fun HighDefinitionTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    orbType: OrbIconType? = null,
    testTag: String = "",
    badgeCount: Int = 0,
    activeColor: Color = RecallVioletPrimary,
    glowSecondary: Color = RecallCyanSecondary
) {
    val resolvedOrbType = orbType ?: when (label.lowercase()) {
        "stream" -> OrbIconType.STREAM
        "reminders" -> OrbIconType.BELL
        "people" -> OrbIconType.PEOPLE
        "gallery" -> OrbIconType.GALLERY
        "ask ai" -> OrbIconType.ASK_AI
        "debrief" -> OrbIconType.DEBRIEF_MIC
        else -> OrbIconType.CUSTOM_VECTOR
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF284666).copy(alpha = 0.65f),
                            Color(0xFF0F2138).copy(alpha = 0.75f)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.3.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF93C5FD).copy(alpha = 0.90f),
                                RecallCyanSecondary.copy(alpha = 0.75f),
                                Color(0xFF1E3A5F).copy(alpha = 0.85f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 3.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Glossy3DOrbIcon(
                orbType = resolvedOrbType,
                customVector = icon,
                contentDescription = label,
                size = if (selected) 32.dp else 27.dp,
                isHighlighted = selected
            )

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(RecallCyanSecondary)
                        .border(1.dp, Color(0xFF0F172A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else "$badgeCount",
                        color = Color(0xFF0F172A),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFFCBD5E1),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Glowing neon underline bar on selected tab
        if (selected) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(RecallCyanSecondary, Color(0xFF93C5FD))
                        )
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(2.5.dp))
        }
    }
}

/**
 * Translucent Dark TextField colors for high-contrast white text readability across all screens.
 */
@Composable
fun translucentWhiteTextFieldColors(
    focusedBorder: Color = RecallCyanSecondary,
    unfocusedBorder: Color = Color(0xFF334155),
    textColor: Color = Color.White
) = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF64748B),
    focusedLabelColor = RecallCyanSecondary,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedLeadingIconColor = RecallCyanSecondary,
    unfocusedLeadingIconColor = Color(0xFF94A3B8),
    focusedTrailingIconColor = RecallCyanSecondary,
    unfocusedTrailingIconColor = Color(0xFF94A3B8),
    focusedBorderColor = focusedBorder,
    unfocusedBorderColor = unfocusedBorder,
    focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.90f),
    unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.75f),
    cursorColor = RecallCyanSecondary
)

/**
 * Returns a distinct, vibrant color for any tag or category name so tags never look washed out or uniform.
 */
fun getVibrantTagColor(tagOrCategory: String): Color {
    return when (tagOrCategory.trim().removePrefix("#").lowercase()) {
        "all" -> Color(0xFF0284C7)
        "favorites", "favorite", "priority" -> Color(0xFFD97706)
        "meeting", "meetings", "events" -> Color(0xFF4F46E5)
        "work", "quarterly" -> Color(0xFF2563EB)
        "projects", "project", "proposal" -> Color(0xFF7C3AED)
        "ideas", "idea" -> Color(0xFFD97706)
        "personal" -> Color(0xFF059669)
        "finance" -> Color(0xFF0D9488)
        "health" -> Color(0xFFDB2777)
        "travel" -> Color(0xFF0891B2)
        "family" -> Color(0xFFEA580C)
        "learning" -> Color(0xFF6D28D9)
        "journal" -> Color(0xFF0F766E)
        "visuals", "photo memory" -> Color(0xFF0284C7)
        "clients", "client" -> Color(0xFF0369A1)
        "code & tech", "tech" -> Color(0xFF334155)
        "shopping" -> Color(0xFFE11D48)
        "habits" -> Color(0xFF16A34A)
        "urgent", "blocker" -> Color(0xFFDC2626)
        "follow-up", "needs catch-up" -> Color(0xFFC026D3)
        "decision" -> Color(0xFF4338CA)
        "friends", "social & people" -> Color(0xFF0D9488)
        "mindful & gratitude" -> Color(0xFF059669)
        "work & standup" -> Color(0xFF2563EB)
        else -> {
            val palette = listOf(
                Color(0xFF4F46E5),
                Color(0xFF0891B2),
                Color(0xFF059669),
                Color(0xFFD97706),
                Color(0xFFDB2777),
                Color(0xFF7C3AED),
                Color(0xFFEA580C)
            )
            val hash = kotlin.math.abs(tagOrCategory.lowercase().hashCode())
            palette[hash % palette.size]
        }
    }
}

/**
 * Crisp, high-contrast color-coded tag badge.
 */
@Composable
fun VibrantTagBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = getVibrantTagColor(text),
    onRemove: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, color),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
            if (onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private val personAvatarBitmapCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

fun savePersonAvatarFromBitmap(context: Context, bitmap: Bitmap): String? {
    return try {
        val outFile = File(context.filesDir, "person_avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        val path = outFile.absolutePath
        personAvatarBitmapCache[path] = bitmap.asImageBitmap()
        path
    } catch (_: Throwable) {
        null
    }
}

fun savePersonAvatarFromUri(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val rawBytes = input.use { it.readBytes() }
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > 512 || boundsOpts.outHeight / sampleSize > 512) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null
        val outFile = File(context.filesDir, "person_avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        val path = outFile.absolutePath
        personAvatarBitmapCache[path] = bmp.asImageBitmap()
        path
    } catch (_: Throwable) {
        null
    }
}

fun loadPersonAvatarBitmap(pathOrUri: String?): ImageBitmap? {
    if (pathOrUri.isNullOrBlank()) return null
    personAvatarBitmapCache[pathOrUri]?.let { return it }
    return try {
        val file = File(pathOrUri)
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        val img = bmp.asImageBitmap()
        personAvatarBitmapCache[pathOrUri] = img
        img
    } catch (_: Throwable) {
        null
    }
}

@Composable
fun PersonAvatarImage(
    name: String,
    avatarUri: String?,
    size: Dp = 48.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    modifier: Modifier = Modifier
) {
    val avatarBitmap = remember(avatarUri) { loadPersonAvatarBitmap(avatarUri) }
    Surface(
        shape = CircleShape,
        color = RecallVioletPrimary.copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(listOf(RecallCyanSecondary, RecallVioletPrimary))
        ),
        modifier = modifier.size(size)
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = "$name profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RecallVioletPrimary.copy(alpha = 0.85f),
                                Color(0xFF1E1B4B)
                            )
                        )
                    )
            ) {
                Text(
                    text = name.trim().take(1).uppercase().ifBlank { "?" },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Seamless borderless colors for OutlinedTextFields placed inside a UnifiedTaskbarInputBox or dark card.
 */
@Composable
fun seamlessInnerTextFieldColors(
    textColor: Color = Color.White
) = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF64748B),
    focusedLabelColor = RecallCyanSecondary,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedLeadingIconColor = RecallCyanSecondary,
    unfocusedLeadingIconColor = Color(0xFF94A3B8),
    focusedTrailingIconColor = RecallCyanSecondary,
    unfocusedTrailingIconColor = Color(0xFF94A3B8),
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = RecallCyanSecondary
)

/**
 * Compact drop-down menu button designed to sit inside the integrated top taskbar of an input box.
 */
@Composable
fun TaskbarDropdownButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = RecallVioletPrimary,
    dotColor: Color? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Unified dark input box container featuring an integrated horizontal taskbar
 * across the top of the inside of the box for drop-down menu controls.
 */
@Composable
fun UnifiedTaskbarInputBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    borderColor: Color = RecallVioletPrimary.copy(alpha = 0.6f),
    taskbarContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    bodyContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val taskbarScrollState = rememberScrollState()
    Surface(
        shape = shape,
        color = Color(0xFF0F172A).copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Integrated Top Taskbar inside the input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                    .horizontalScroll(taskbarScrollState)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = taskbarContent
            )
            // Crisp hairline divider separating the internal taskbar from the writing canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF334155))
            )
            // Input field(s) & optional footer inside the unified box
            bodyContent()
        }
    }
}

/**
 * Signature Memorae pill button with a vibrant horizontal magenta-to-violet gradient,
 * high-contrast white text, subtle shadow, and ripple interaction.
 */
@Composable
fun MemoraeGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    gradient: Brush = Brush.horizontalGradient(
        listOf(MemoraeGradientMagenta, MemoraeGradientViolet)
    ),
    testTag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp,
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(25.dp))
            .background(
                if (enabled) gradient
                else Brush.horizontalGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Memorae frosted search pill matching Screenshot 3:
 * Translucent frosted glass container with rounded pill shape,
 * placeholder text, clear action, and trailing search icon.
 */
@Composable
fun MemoraeSearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String = "Search...",
    modifier: Modifier = Modifier,
    testTag: String = "memorae_search_pill"
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(RecallCyanSecondary),
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    innerTextField()
                }
            )

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Status Count Pill from Memorae Reminders (e.g. Active: 7, Recurrent: 12, Completed: 64).
 */
@Composable
fun MemoraeStatusPill(
    label: String,
    count: Int,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) RecallVioletPrimary.copy(alpha = 0.40f) else Color(0xFF1E293B).copy(alpha = 0.65f),
        border = BorderStroke(
            1.dp,
            if (isSelected) RecallVioletPrimary else Color(0xFFFFFFFF).copy(alpha = 0.22f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/**
 * Universal Floating Quick-Capture Pill Dock matching Screenshots 1 & 4.
 * Features translucent frosted glass container, "Type or record a new thought" inline field,
 * quick '+' template/action button, and circular white button with blue microphone icon.
 */
@Composable
fun MemoraeFloatingInputDock(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onPlusClick: () -> Unit = {},
    placeholderText: String = "Type or record a new thought",
    isRecording: Boolean = false,
    modifier: Modifier = Modifier,
    testTag: String = "memorae_floating_dock"
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = 0.40f),
                    RecallCyanSecondary.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.40f)
                )
            )
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(RecallCyanSecondary),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp)
                    .testTag(testTag),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    innerTextField()
                }
            )

            // Plus template button
            IconButton(
                onClick = onPlusClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Options",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Circular white action button with blue microphone / send icon (matching Memorae reference)
            Surface(
                shape = CircleShape,
                color = if (isRecording) Color(0xFFEF4444)
                else if (value.isNotBlank()) MemoraeGradientViolet
                else Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (value.isNotBlank()) onSend() else onMicClick()
                    }
                    .testTag("floating_dock_action_btn")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop
                        else if (value.isNotBlank()) Icons.AutoMirrored.Filled.Send
                        else Icons.Default.Mic,
                        contentDescription = "Mic or Send",
                        tint = if (isRecording || value.isNotBlank()) Color.White else Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3D Pastel Gradient Companion Sphere Avatar matching Screenshots 1 & 2.
 * Renders a glowing spherical character with gentle blush and cheerful facial features.
 */
@Composable
fun MemoraeCompanionAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val radius = w / 2f

            // 1. Soft spherical radial gradient with highlights
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD1DC), // Soft peach highlight
                        Color(0xFFE879F9), // Pastel magenta
                        Color(0xFF818CF8), // Pastel violet
                        Color(0xFF38BDF8)  // Pastel cyan bottom glow
                    ),
                    center = Offset(radius * 0.75f, radius * 0.65f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = Offset(radius, radius)
            )

            // 2. Specular glass gloss curve at top-left
            drawOval(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(radius * 0.35f, radius * 0.15f),
                size = Size(radius * 0.85f, radius * 0.40f)
            )

            // 3. Cute eyes
            val eyeRadius = radius * 0.10f
            val eyeY = radius * 0.95f
            val leftEyeX = radius * 0.72f
            val rightEyeX = radius * 1.28f

            // Left eye
            drawCircle(
                color = Color(0xFF0F172A),
                radius = eyeRadius,
                center = Offset(leftEyeX, eyeY)
            )
            // Left eye shine
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.35f,
                center = Offset(leftEyeX - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
            )

            // Right eye
            drawCircle(
                color = Color(0xFF0F172A),
                radius = eyeRadius,
                center = Offset(rightEyeX, eyeY)
            )
            // Right eye shine
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.35f,
                center = Offset(rightEyeX - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
            )

            // 4. Cheerful smile
            val mouthPath = Path().apply {
                moveTo(radius * 0.88f, radius * 1.12f)
                quadraticTo(radius, radius * 1.24f, radius * 1.12f, radius * 1.12f)
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF0F172A),
                style = Stroke(
                    width = radius * 0.05f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 5. Soft blush cheeks
            drawOval(
                color = Color(0xFFF43F5E).copy(alpha = 0.28f),
                topLeft = Offset(radius * 0.42f, radius * 1.05f),
                size = Size(radius * 0.26f, radius * 0.14f)
            )
            drawOval(
                color = Color(0xFFF43F5E).copy(alpha = 0.28f),
                topLeft = Offset(radius * 1.32f, radius * 1.05f),
                size = Size(radius * 0.26f, radius * 0.14f)
            )
        }
    }
}

/**
 * Frosted circular action icon button matching Memorae top corner controls
 * (settings gear, meditation/concierge icon, speaker audio, back arrow).
 */
@Composable
fun MemoraeCircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0xFFFFFFFF).copy(alpha = 0.25f),
    borderColor: Color = Color(0xFFFFFFFF).copy(alpha = 0.35f)
) {
    Surface(
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.50f)
            )
        }
    }
}

/**
 * Memorae frosted glass card container for cards, items, and panels.
 */
@Composable
fun MemoraeGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0xFF0F172A).copy(alpha = 0.65f),
    borderColor: Color = Color.White.copy(alpha = 0.25f),
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = shadowElevation,
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}


