package kr.ac.duksung.dobongzip.ui.like

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val horizontalSpacingPx: Int,
    private val verticalSpacingPx: Int,
    private val includeEdge: Boolean = true
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val pos = parent.getChildAdapterPosition(view)
        val column = pos % spanCount

        if (includeEdge) {
            outRect.left = horizontalSpacingPx - column * horizontalSpacingPx / spanCount
            outRect.right = (column + 1) * horizontalSpacingPx / spanCount
            if (pos < spanCount) outRect.top = verticalSpacingPx
            outRect.bottom = verticalSpacingPx
        } else {
            outRect.left = column * horizontalSpacingPx / spanCount
            outRect.right = horizontalSpacingPx - (column + 1) * horizontalSpacingPx / spanCount
            if (pos >= spanCount) outRect.top = verticalSpacingPx
        }
    }
}
