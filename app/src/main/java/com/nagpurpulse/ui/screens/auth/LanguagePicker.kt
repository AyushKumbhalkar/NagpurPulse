package com.nagpurpulse.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nagpurpulse.ui.locale.AppLocale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Small "globe + language" chip with a dropdown to switch between English, Hindi and Marathi. */
@Composable
fun LanguagePickerChip(
    contentColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val currentTag = AppLocale.currentTag(context)
    val currentName = AppLocale.options.first { it.tag == currentTag }.nativeName
    val description = stringResource(R.string.language_picker_cd)

    Box(
        modifier = if (compact) modifier.heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = description) { expanded = true } else modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(backgroundColor)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(50))
                .heightIn(min = if (compact) 36.dp else 48.dp)
                .then(if (compact) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = description) { expanded = true })
                .padding(
                    start = if (compact) 14.dp else 10.dp, end = if (compact) 10.dp else 6.dp,
                    top = if (compact) 0.dp else 6.dp, bottom = if (compact) 0.dp else 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (compact) {
                Text("A", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics { contentDescription = description })
                Spacer(Modifier.width(10.dp))
                Text("अ", color = contentColor.copy(alpha = 0.55f), fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(currentTag.take(2).uppercase(), color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.Language, contentDescription = description, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(currentName, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLocale.options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.nativeName,
                            fontWeight = if (option.tag == currentTag) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        if (option.tag != currentTag) {
                            AuthAnalytics.log(context, "language_changed", "language" to option.tag)
                            context.findActivity()?.let { AppLocale.apply(it, option.tag) }
                        }
                    }
                )
            }
        }
    }
}
