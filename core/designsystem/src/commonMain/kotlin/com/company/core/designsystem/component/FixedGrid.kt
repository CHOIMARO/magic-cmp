package com.company.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Grid with a fixed number of equal columns. It does not scroll.
 *
 * Use it for short lists inside a scrolling screen, like CSS `grid-template-columns: repeat(n, 1fr)`.
 *
 * @param items Items to show.
 * @param columns Number of columns.
 * @param modifier Modifier for the grid.
 * @param horizontalSpacing Space between the columns.
 * @param verticalSpacing Space between the rows.
 * @param verticalAlignment Vertical alignment of the cells in a row.
 * @param content Content of one cell.
 */
@Composable
fun <T> FixedGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable (T) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalAlignment = verticalAlignment,
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { content(item) }
                }
                repeat(columns - rowItems.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
