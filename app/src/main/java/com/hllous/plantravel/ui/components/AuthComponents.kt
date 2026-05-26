package com.hllous.plantravel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.theme.PlusJakartaSansFamily

private val ARCH_HEIGHT = 80.dp
private val ARCH_EXTRA = 120.dp  // ellipse extends past screen edges so the arc exits the sides cleanly

@Composable
fun AuthBrandPanel(
    emoji: String,
    title: String,
    tagline: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onThemeChange: ((Boolean, Offset?) -> Unit)? = null,
) {
    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    var themeToggleCenter by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(primaryColor)
    ) {
        // Decorative circles
        Box(
            Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
        )
        Box(
            Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = (-20).dp)
                .background(Color.White.copy(alpha = 0.04f), CircleShape)
        )

        if (onThemeChange != null) {
            IconButton(
                onClick = { onThemeChange(!isDarkTheme, themeToggleCenter) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 6.dp, end = 12.dp)
                    .onGloballyPositioned { coords ->
                        themeToggleCenter = coords.boundsInRoot().center
                    }
            ) {
                Icon(
                    if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Modo claro" else "Modo oscuro",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Content — bottom padding reserves space above the arch
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp, start = 28.dp, end = 28.dp, bottom = ARCH_HEIGHT + 16.dp)
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = (-0.8).sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FrauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }

        // Arch separator: half-ellipse in background color drawn over the bottom of the primary band.
        // Replicates the ::after pseudo-element from the HTML prototype.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ARCH_HEIGHT)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val extra = ARCH_EXTRA.toPx()

            // The ellipse is wider than the canvas by `extra` on each side.
            // The canvas clips at x=0 and x=w, so the arc exits the screen
            // mid-trajectory instead of falling to the bottom like an oval.
            val archPath = Path()
            archPath.moveTo(-extra, h)
            archPath.arcTo(
                rect = Rect(-extra, 0f, w + extra, h * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            archPath.lineTo(w + extra, h)
            archPath.lineTo(-extra, h)
            archPath.close()

            drawPath(archPath, bgColor)
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        !enabled -> colors.outline.copy(alpha = 0.4f)
        isFocused -> colors.primary
        else -> colors.outline
    }

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(7.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.5f),
            ),
            decorationBox = { innerTextField ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                color = colors.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}
