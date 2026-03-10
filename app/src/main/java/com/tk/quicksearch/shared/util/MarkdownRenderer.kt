package com.tk.quicksearch.shared.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock

    data class Paragraph(val text: String) : MarkdownBlock

    data class UnorderedListItem(val text: String, val indentLevel: Int) : MarkdownBlock

    data class OrderedListItem(val index: Int, val text: String, val indentLevel: Int) : MarkdownBlock
}

@Composable
internal fun RenderMarkdownDocument(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = parseMarkdown(markdown)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style =
                        when (block.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            3 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        }
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                is MarkdownBlock.UnorderedListItem -> {
                    MarkdownListItem(
                        marker = "\u2022",
                        text = block.text,
                        indentLevel = block.indentLevel,
                    )
                }

                is MarkdownBlock.OrderedListItem -> {
                    MarkdownListItem(
                        marker = "${block.index}.",
                        text = block.text,
                        indentLevel = block.indentLevel,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownListItem(
    marker: String,
    text: String,
    indentLevel: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = (indentLevel * 16).dp),
    ) {
        Text(
            text = marker,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = DesignTokens.SpacingMedium),
        )
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuilder = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphBuilder.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraphBuilder.joinToString(" ").trim())
            paragraphBuilder.clear()
        }
    }

    markdown.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.isBlank()) {
            flushParagraph()
            return@forEach
        }

        val trimmed = line.trimStart()
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2].trim()
            blocks += MarkdownBlock.Heading(level = level, text = text)
            return@forEach
        }

        val leadingSpaces = line.length - line.trimStart().length
        val indentLevel = (leadingSpaces / 2).coerceAtLeast(0)

        val unorderedListMatch = Regex("^[-*+]\\s+(.+)$").matchEntire(trimmed)
        if (unorderedListMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.UnorderedListItem(
                text = unorderedListMatch.groupValues[1].trim(),
                indentLevel = indentLevel,
            )
            return@forEach
        }

        val orderedListMatch = Regex("^(\\d+)\\.\\s+(.+)$").matchEntire(trimmed)
        if (orderedListMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.OrderedListItem(
                index = orderedListMatch.groupValues[1].toIntOrNull() ?: 1,
                text = orderedListMatch.groupValues[2].trim(),
                indentLevel = indentLevel,
            )
            return@forEach
        }

        paragraphBuilder += trimmed
    }

    flushParagraph()
    return blocks
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) -> {
                    val end = text.indexOf("**", index + 2)
                    if (end > index + 1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(parseInlineMarkdown(text.substring(index + 2, end)))
                        pop()
                        index = end + 2
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                text.startsWith("*", index) -> {
                    val end = text.indexOf("*", index + 1)
                    if (end > index) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(parseInlineMarkdown(text.substring(index + 1, end)))
                        pop()
                        index = end + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                text.startsWith("`", index) -> {
                    val end = text.indexOf("`", index + 1)
                    if (end > index) {
                        pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                        append(text.substring(index + 1, end))
                        pop()
                        index = end + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                else -> {
                    append(text[index])
                    index += 1
                }
            }
        }
    }
}
