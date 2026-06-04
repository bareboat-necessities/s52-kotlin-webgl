package io.github.s52.core.draw

import io.github.s52.core.settings.DisplayCategory

object DisplayCategoryFilter {
    fun isVisible(commandCategory: DisplayCategory, selectedCategory: DisplayCategory): Boolean {
        return commandCategory in visibleCategories(selectedCategory)
    }

    fun visibleCategories(selectedCategory: DisplayCategory): Set<DisplayCategory> = when (selectedCategory) {
        DisplayCategory.DisplayBase -> setOf(DisplayCategory.DisplayBase)
        DisplayCategory.Standard -> setOf(DisplayCategory.DisplayBase, DisplayCategory.Standard)
        DisplayCategory.Other -> setOf(DisplayCategory.DisplayBase, DisplayCategory.Standard, DisplayCategory.Other)
        DisplayCategory.MarinersStandard -> setOf(
            DisplayCategory.DisplayBase,
            DisplayCategory.Standard,
            DisplayCategory.MarinersStandard
        )
    }
}
