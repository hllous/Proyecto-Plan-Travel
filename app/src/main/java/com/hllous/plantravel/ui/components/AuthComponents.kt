package com.hllous.plantravel.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hllous.plantravel.ui.theme.FrauncesFamily

@Composable
fun AuthBrandPanel(
    emoji: String,
    title: String,
    tagline: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
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
                .offset(x = (-40).dp, y = 20.dp)
                .background(Color.White.copy(alpha = 0.04f), CircleShape)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 24.dp, start = 28.dp, end = 28.dp, bottom = 52.dp)
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
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
    }
}
