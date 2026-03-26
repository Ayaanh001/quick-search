package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AppColors.DialogBackground,
        tonalElevation = 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}
