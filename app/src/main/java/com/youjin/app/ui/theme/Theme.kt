package com.youjin.app.ui.theme

import android.app.Activity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.youjin.app.data.ThemeMode

/**
 * 目标强调色：只用于图标底色、进度条与数据点等小面积元素。
 * 界面骨架保持「黑白灰 + Apple 蓝」，避免多色拼贴。
 */
val GoalPalette = listOf(
    Color(0xFF0071E3), // 蓝
    Color(0xFF34C759), // 绿
    Color(0xFFFF9500), // 橙
    Color(0xFFAF52DE), // 紫
    Color(0xFFFF375F), // 粉
    Color(0xFF30B0C7), // 青
    Color(0xFF5856D6), // 靛
    Color(0xFFA2845E), // 棕
)

/**
 * Apple 风格的设计令牌。Material 3 的 ColorScheme 无法表达 Apple 的
 * 三级文字、hairline 分隔线、输入框底色等语义，这里单独提供。
 */
@Immutable
data class ApplePalette(
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val field: Color,
    val fill: Color,
    val fillStrong: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val border: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val shadow: Color,
    val isDark: Boolean,
)

private val LightPalette = ApplePalette(
    background = Color(0xFFF5F5F7),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFFFFFF),
    field = Color(0xFFFFFFFF),
    fill = Color(0xFFE9E9EB),
    fillStrong = Color(0xFFDCDCE0),
    label = Color(0xFF1D1D1F),
    secondaryLabel = Color(0xFF6E6E73),
    tertiaryLabel = Color(0xFF8E8E93),
    separator = Color(0xFFE5E5EA),
    border = Color(0xFFDCDCE0),
    blue = Color(0xFF0071E3),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    shadow = Color(0x12000000),
    isDark = false,
)

private val DarkPalette = ApplePalette(
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardElevated = Color(0xFF2C2C2E),
    field = Color(0xFF1C1C1E),
    fill = Color(0xFF2C2C2E),
    fillStrong = Color(0xFF3A3A3C),
    label = Color(0xFFF5F5F7),
    secondaryLabel = Color(0xFFA1A1A6),
    tertiaryLabel = Color(0xFF8E8E93),
    separator = Color(0xFF38383A),
    border = Color(0xFF3A3A3C),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    shadow = Color(0x40000000),
    isDark = true,
)

private val LocalApplePalette = staticCompositionLocalOf { LightPalette }

/** 设计令牌入口：`AppleTheme.colors.blue` / `AppleTheme.Easing` */
object AppleTheme {

    val colors: ApplePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalApplePalette.current

    /** 统一的丝滑减速曲线（对应提示词中的 cubic-bezier(0.25, 0.1, 0.25, 1)） */
    val Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** 按压缩放：带阻尼的弹簧，杜绝线性过渡 */
    val PressSpring: SpringSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 900f)

    /** 数值/进度变化用的柔和弹簧 */
    val SoftSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 240f)
}

/**
 * 字体：Android 无 SF Pro 授权字体，使用系统无衬线（Roboto）配合 SF 风格的
 * 字重与负字距，标题 font-semibold + tracking-tight，正文 font-normal。
 */
private val AppleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 66.sp,
        letterSpacing = (-2.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.6).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

/** 精致圆角：卡片 20、控件 14、弹窗 22、胶囊按钮/标签 rounded-full */
private val AppleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

private val LightColors = lightColorScheme(
    primary = LightPalette.blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F1FD),
    onPrimaryContainer = Color(0xFF003A73),
    secondary = LightPalette.secondaryLabel,
    onSecondary = Color.White,
    secondaryContainer = LightPalette.fill,
    onSecondaryContainer = LightPalette.label,
    tertiary = LightPalette.secondaryLabel,
    onTertiary = Color.White,
    tertiaryContainer = LightPalette.fill,
    onTertiaryContainer = LightPalette.label,
    background = LightPalette.background,
    onBackground = LightPalette.label,
    surface = LightPalette.card,
    onSurface = LightPalette.label,
    surfaceVariant = LightPalette.fill,
    onSurfaceVariant = LightPalette.secondaryLabel,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = LightPalette.border,
    outlineVariant = LightPalette.separator,
    error = LightPalette.red,
    onError = Color.White,
    errorContainer = Color(0xFFFFE9E7),
    onErrorContainer = Color(0xFFB3261E),
    inverseSurface = LightPalette.label,
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF64B5FF),
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = DarkPalette.blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003A73),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = DarkPalette.secondaryLabel,
    onSecondary = Color.Black,
    secondaryContainer = DarkPalette.fill,
    onSecondaryContainer = DarkPalette.label,
    tertiary = DarkPalette.secondaryLabel,
    onTertiary = Color.Black,
    tertiaryContainer = DarkPalette.fill,
    onTertiaryContainer = DarkPalette.label,
    background = DarkPalette.background,
    onBackground = DarkPalette.label,
    surface = DarkPalette.card,
    onSurface = DarkPalette.label,
    surfaceVariant = DarkPalette.fill,
    onSurfaceVariant = DarkPalette.secondaryLabel,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = DarkPalette.card,
    surfaceContainer = DarkPalette.card,
    surfaceContainerHigh = DarkPalette.cardElevated,
    surfaceContainerHighest = DarkPalette.cardElevated,
    outline = DarkPalette.border,
    outlineVariant = DarkPalette.separator,
    error = DarkPalette.red,
    onError = Color.White,
    errorContainer = Color(0xFF5C0B08),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = DarkPalette.label,
    inverseOnSurface = Color.Black,
    inversePrimary = Color(0xFF0071E3),
    scrim = Color.Black,
)

@Composable
fun YoujinTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (darkTheme) DarkPalette else LightPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalApplePalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppleTypography,
            shapes = AppleShapes,
            content = content,
        )
    }
}
