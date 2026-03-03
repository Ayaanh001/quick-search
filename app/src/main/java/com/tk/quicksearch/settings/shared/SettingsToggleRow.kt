package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/**
 * Reusable toggle row component for settings cards.
 * Provides consistent styling and layout across all toggle rows.
 */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    titleTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    horizontalPadding: Dp = DesignTokens.SpacingXXLarge,
    leadingIconSize: Dp = DesignTokens.IconSizeSmall,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    extraVerticalPadding: Dp = 0.dp,
    showDivider: Boolean = true,
) {
    val view = LocalView.current
    val topPadding = DesignTokens.cardItemTopPadding(isFirstItem) + extraVerticalPadding
    val bottomPadding = DesignTokens.cardItemBottomPadding(isLastItem) + extraVerticalPadding
    val onToggle: (Boolean) -> Unit = { newValue ->
        hapticToggle(view)()
        onCheckedChange(newValue)
    }

    Column {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .then(
                        if (onRowClick != null) {
                            Modifier.clickable(onClick = onRowClick)
                        } else {
                            Modifier.toggleable(
                                value = checked,
                                role = Role.Switch,
                                onValueChange = onToggle,
                            )
                        },
                    )
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = bottomPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(leadingIconSize),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing),
            ) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.85f),
            )
        }

        if (showDivider && !isLastItem) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
