package com.example.curio.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Utility to handle swipe-to-delete logic and UI (red background + trash icon).
 * @param onSwiped Callback triggered when the swipe is completed.
 */
class SwipeToDeleteCallback(
    private val onSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val context = recyclerView.context
        val deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
        val background = ColorDrawable(Color.RED)

        // Draw red background
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // Calculate position for trash icon
        val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2
        val iconTop = itemView.top + iconMargin
        val iconBottom = iconTop + (deleteIcon?.intrinsicHeight ?: 0)
        val iconLeft = itemView.right - iconMargin - (deleteIcon?.intrinsicWidth ?: 0)
        val iconRight = itemView.right - iconMargin

        deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        deleteIcon?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}