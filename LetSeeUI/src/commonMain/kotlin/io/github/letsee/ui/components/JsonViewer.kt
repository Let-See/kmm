package io.github.letsee.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json

private val prettyPrintJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

@Composable
fun JsonViewer(
    json: String,
    onCopy: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val prettyPrinted = remember(json) { prettyPrint(json) }
    val highlighted = remember(prettyPrinted, isDark) {
        highlightJson(prettyPrinted, isDark)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = highlighted,
                modifier = Modifier.padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        }

        IconButton(
            onClick = { onCopy(prettyPrinted) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .semantics { contentDescription = "Copy JSON to clipboard" }
                .testTag("letsee_copy_json"),
        ) {
            Text(
                text = "Copy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun prettyPrint(json: String): String {
    return try {
        prettyPrintJson.encodeToString(Json.parseToJsonElement(json))
    } catch (_: Exception) {
        json
    }
}

private fun highlightJson(text: String, isDark: Boolean): AnnotatedString {
    val keyColor = if (isDark) Color(0xFF82AAFF) else Color(0xFF6699CC)
    val stringColor = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
    val numberColor = if (isDark) Color(0xFFFFCC80) else Color(0xFFFF9800)
    val booleanColor = if (isDark) Color(0xFFCE93D8) else Color(0xFF9C27B0)
    val nullColor = if (isDark) Color(0xFFEF5350) else Color(0xFFE53935)

    data class StyledRange(val start: Int, val end: Int, val style: SpanStyle)

    val ranges = mutableListOf<StyledRange>()
    val quotedPattern = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
    val quotedMatches = quotedPattern.findAll(text).toList()
    val quotedIntervals = mutableListOf<IntRange>()

    for (match in quotedMatches) {
        val afterEnd = match.range.last + 1
        val isKey = if (afterEnd < text.length) {
            val remaining = text.substring(afterEnd)
            val idx = remaining.indexOfFirst { !it.isWhitespace() }
            idx >= 0 && remaining[idx] == ':'
        } else {
            false
        }
        val color = if (isKey) keyColor else stringColor
        ranges.add(StyledRange(match.range.first, afterEnd, SpanStyle(color = color)))
        quotedIntervals.add(match.range)
    }

    fun isInsideQuoted(pos: Int): Boolean = quotedIntervals.any { pos in it }

    Regex("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?").findAll(text).forEach { match ->
        if (!isInsideQuoted(match.range.first)) {
            ranges.add(StyledRange(match.range.first, match.range.last + 1, SpanStyle(color = numberColor)))
        }
    }

    Regex("\\b(?:true|false)\\b").findAll(text).forEach { match ->
        if (!isInsideQuoted(match.range.first)) {
            ranges.add(StyledRange(match.range.first, match.range.last + 1, SpanStyle(color = booleanColor)))
        }
    }

    Regex("\\bnull\\b").findAll(text).forEach { match ->
        if (!isInsideQuoted(match.range.first)) {
            ranges.add(StyledRange(match.range.first, match.range.last + 1, SpanStyle(color = nullColor)))
        }
    }

    return buildAnnotatedString {
        append(text)
        for ((start, end, style) in ranges) {
            addStyle(style, start, end)
        }
    }
}
