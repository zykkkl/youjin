package com.youjin.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.youjin.app.ui.theme.AppleTheme

/* ------------------------------------------------------------------ */
/* 交互：物理按压阻尼 + 丝滑弹簧                                        */
/* ------------------------------------------------------------------ */

/** 统一按压反馈：0.96~0.98 的阻尼缩放，使用弹簧而非线性动画 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = AppleTheme.PressSpring,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/* ------------------------------------------------------------------ */
/* 容器                                                                */
/* ------------------------------------------------------------------ */

/** 白色圆角卡片：大量留白、无重阴影，可交互时带按压缩放；支持长按（主界面管理菜单） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = AppleTheme.colors.card,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val action = if (onClick != null || onLongClick != null) {
        Modifier
            .pressScale(source, 0.98f)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = source,
                        indication = null,
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = source,
                        indication = null,
                        onClick = { onClick?.invoke() },
                    )
                }
            )
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(action),
        shape = shape,
        color = color,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** iOS inset grouped 风格的列表卡：整组一个白底圆角容器 */
@Composable
fun AppleGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppleTheme.colors.card,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

/** 1dp hairline 分隔线，替代重描边 */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(AppleTheme.colors.separator)
    )
}

/* ------------------------------------------------------------------ */
/* 标题与导航                                                           */
/* ------------------------------------------------------------------ */

/** iOS 大标题页头：34sp semibold，大量留白 */
@Composable
fun AppleLargeTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.secondaryLabel,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 详情页小导航栏：返回箭头 + 居中标题 + 右侧操作 */
@Composable
fun AppleNavBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val p = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            AppleIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "返回",
                onClick = onBack,
                tint = p.blue,
                iconSize = 28.dp,
            )
        } else {
            Spacer(Modifier.width(44.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

/** 小节标题：13sp、次级文字色，对应 iOS 分组列表的 section header */
@Composable
fun AppleSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = p.secondaryLabel,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/* ------------------------------------------------------------------ */
/* 按钮                                                                */
/* ------------------------------------------------------------------ */

/** 主按钮：Apple 蓝填充、14dp 圆角、按压回弹 */
@Composable
fun AppleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false,
    fillWidth: Boolean = true,
    height: Dp = 50.dp,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    val background = when {
        !enabled -> p.fill
        destructive -> p.red
        else -> p.blue
    }
    val content = when {
        !enabled -> p.tertiaryLabel
        else -> Color.White
    }
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .pressScale(source, 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = content,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = content,
                maxLines = 1,
            )
        }
    }
}

/** 次级按钮：浅灰底 + 蓝色文字，用于“取消”等并列操作 */
@Composable
fun AppleSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    height: Dp = 50.dp,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .pressScale(source, 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .background(p.fill)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) p.blue else p.tertiaryLabel,
            maxLines = 1,
        )
    }
}

/** 纯文字操作（蓝色链接样式，hover/按压仅做轻微阻尼） */
@Composable
fun AppleTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppleTheme.colors.blue,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(source, 0.96f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) color else p.tertiaryLabel,
            maxLines = 1,
        )
    }
}

/** 图标按钮：44dp 触达区，圆形按压反馈 */
@Composable
fun AppleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppleTheme.colors.label,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(source, 0.92f)
            .clip(CircleShape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else AppleTheme.colors.tertiaryLabel,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 悬浮操作按钮：圆形或胶囊，轻阴影 + 按压回弹 */
@Composable
fun AppleFloatingButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    val shape: Shape = if (text == null) CircleShape else RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(if (text == null) 56.dp else 50.dp)
            .then(if (text == null) Modifier.width(56.dp) else Modifier.widthIn(min = 96.dp))
            .pressScale(source, 0.94f)
            .shadow(6.dp, shape, clip = false, ambientColor = p.shadow, spotColor = p.shadow)
            .clip(shape)
            .background(p.blue)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = if (text == null) 0.dp else 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(24.dp))
        if (text != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 选择控件                                                            */
/* ------------------------------------------------------------------ */

/** 胶囊筛选标签：选中反色（黑底白字），按压 0.95 阻尼 */
@Composable
fun AppleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) p.label else Color.Transparent,
        animationSpec = tween(200, easing = AppleTheme.Easing),
        label = "chipBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> p.tertiaryLabel
            selected -> if (p.isDark) Color.Black else Color.White
            else -> p.label
        },
        animationSpec = tween(200, easing = AppleTheme.Easing),
        label = "chipContent",
    )
    Box(
        modifier = modifier
            .height(34.dp)
            .pressScale(source, 0.95f)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (selected) Color.Transparent else p.border, CircleShape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** 分段控件：灰色轨道 + 白色滑块，对应 iOS UISegmentedControl */
@Composable
fun AppleSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(p.fill)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            Segment(
                label = label,
                selected = index == selectedIndex,
                enabled = enabled,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) p.card else Color.Transparent,
        animationSpec = tween(220, easing = AppleTheme.Easing),
        label = "segmentBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> p.label
            !enabled -> p.tertiaryLabel
            else -> p.secondaryLabel
        },
        animationSpec = tween(220, easing = AppleTheme.Easing),
        label = "segmentContent",
    )
    Box(
        modifier = modifier
            .height(34.dp)
            .pressScale(source, 0.97f)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** 开关：圆角轨道 + 蓝色开启态（克制用色，仅蓝/灰） */
@Composable
fun AppleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = AppleTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = p.blue,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = p.fillStrong,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = Color.Transparent,
        ),
    )
}

/** 圆形勾选：清单步骤专用，勾选后蓝底白勾 */
@Composable
fun AppleCheckCircle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(28.dp)
            .pressScale(source, 0.9f)
            .clip(CircleShape)
            .background(if (checked) p.blue else Color.Transparent)
            .border(1.5.dp, if (checked) Color.Transparent else p.border, CircleShape)
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 输入                                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun appleFieldColors(): TextFieldColors {
    val p = AppleTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.label,
        unfocusedTextColor = p.label,
        disabledTextColor = p.tertiaryLabel,
        errorTextColor = p.label,
        focusedContainerColor = p.field,
        unfocusedContainerColor = p.field,
        disabledContainerColor = p.field,
        errorContainerColor = p.field,
        cursorColor = p.blue,
        errorCursorColor = p.blue,
        focusedBorderColor = p.blue,
        unfocusedBorderColor = p.border,
        disabledBorderColor = p.separator,
        errorBorderColor = p.red,
        focusedPlaceholderColor = p.tertiaryLabel,
        unfocusedPlaceholderColor = p.tertiaryLabel,
        disabledPlaceholderColor = p.tertiaryLabel,
        focusedLeadingIconColor = p.secondaryLabel,
        unfocusedLeadingIconColor = p.secondaryLabel,
        focusedTrailingIconColor = p.secondaryLabel,
        unfocusedTrailingIconColor = p.secondaryLabel,
    )
}

/** 表单输入框：白底、14dp 圆角、聚焦时 1dp 蓝色描边 */
@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = AppleTheme.colors.tertiaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        shape = RoundedCornerShape(14.dp),
        colors = appleFieldColors(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

/** 搜索框：白底胶囊 + 放大镜，未输入时显示占位文字 */
@Composable
fun AppleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(p.field)
            .border(1.dp, p.border, RoundedCornerShape(13.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = p.tertiaryLabel,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.tertiaryLabel,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = p.label),
                cursorBrush = SolidColor(p.blue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            AppleIconButton(
                icon = Icons.Default.Clear,
                contentDescription = "清空搜索",
                onClick = { onValueChange("") },
                tint = p.tertiaryLabel,
                size = 32.dp,
                iconSize = 16.dp,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 列表行                                                              */
/* ------------------------------------------------------------------ */

/** 设置/列表通用行：标题、说明、右侧控件与 chevron */
@Composable
fun AppleRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    titleColor: Color = AppleTheme.colors.label,
    trailing: @Composable () -> Unit = {},
) {
    val p = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.appleRowClick(onClick)
                } else {
                    Modifier
                }
            )
            .heightIn(min = 52.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = p.secondaryLabel,
                )
            }
        }
        trailing()
        if (showChevron) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = p.tertiaryLabel,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 底部标签栏：hairline 分隔 + 10sp 标签，选中态 Apple 蓝 */
@Composable
fun AppleTabBar(
    tabs: List<AppleTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    Column(modifier.fillMaxWidth().background(p.background)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

data class AppleTab(val label: String, val icon: ImageVector)

@Composable
private fun TabItem(
    tab: AppleTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    val color by animateColorAsState(
        targetValue = if (selected) p.blue else p.tertiaryLabel,
        animationSpec = tween(200, easing = AppleTheme.Easing),
        label = "tabColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = AppleTheme.PressSpring,
        label = "tabScale",
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = color)
    }
}

/* ------------------------------------------------------------------ */
/* 反馈                                                                */
/* ------------------------------------------------------------------ */

/** 深色提示条：圆角 16，动作文字用高亮蓝 */
@Composable
fun AppleSnackbarHost(hostState: SnackbarHostState) {
    val p = AppleTheme.colors
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = if (p.isDark) p.cardElevated else Color(0xFF1D1D1F),
            contentColor = Color.White,
            actionColor = Color(0xFF64B5FF),
            shape = RoundedCornerShape(16.dp),
            actionOnNewLine = false,
        )
    }
}

/** Apple 风格弹窗：22dp 圆角、居中标题、并列按钮 */
@Composable
fun AppleDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "确定",
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = "取消",
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val p = AppleTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(22.dp),
            color = p.cardElevated,
            tonalElevation = 0.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.secondaryLabel,
                        textAlign = TextAlign.Center,
                    )
                }
                if (content != null) {
                    Spacer(Modifier.height(20.dp))
                    content()
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (dismissLabel != null) {
                        AppleSecondaryButton(
                            text = dismissLabel,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            fillWidth = false,
                            height = 46.dp,
                        )
                    }
                    AppleButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        fillWidth = false,
                        height = 46.dp,
                        enabled = confirmEnabled,
                        destructive = destructive,
                    )
                }
            }
        }
    }
}

/** 列表行点击反馈：极淡的灰色按压底色，无涟漪 */
@Composable
fun Modifier.appleRowClick(onClick: () -> Unit): Modifier {
    val p = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressedColor by animateColorAsState(
        targetValue = if (pressed) p.fill.copy(alpha = if (p.isDark) 0.55f else 1f) else Color.Transparent,
        animationSpec = tween(120, easing = AppleTheme.Easing),
        label = "rowPress",
    )
    return background(pressedColor).clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    )
}

/** 弹层顶部抓手 */
@Composable
fun AppleSheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .width(36.dp)
            .height(5.dp)
            .clip(CircleShape)
            .background(AppleTheme.colors.fillStrong)
    )
}

/** 目标图标底色块：小面积彩色，按压时会随卡片轻微放大 */
@Composable
fun AppleIconTile(
    emoji: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val p = AppleTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(tint.copy(alpha = if (p.isDark) 0.22f else 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.42f).sp)
    }
}

/** 细线箭头（右），说明可进入下一级 */
@Composable
fun AppleChevron(color: Color = AppleTheme.colors.tertiaryLabel) {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(18.dp),
    )
}

/** 画一条短横线（用于空状态等极简装饰，不使用花哨图形） */
@Composable
fun AppleHairlineDot(color: Color = AppleTheme.colors.border) {
    Canvas(Modifier.size(width = 22.dp, height = 1.dp)) {
        drawRect(color = color)
    }
}
