package com.thermoguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thermoguard.app.ui.theme.Panel
import com.thermoguard.app.ui.theme.TextDim

@Composable
fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier, hint: String? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label.uppercase(), color = TextDim, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (hint != null) Text(hint, color = TextDim, fontSize = 11.sp)
    }
}
