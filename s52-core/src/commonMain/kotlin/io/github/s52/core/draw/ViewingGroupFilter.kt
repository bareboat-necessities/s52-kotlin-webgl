package io.github.s52.core.draw

import io.github.s52.core.settings.MarinerSettings

object ViewingGroupFilter {
    fun isVisible(viewingGroup: Int, settings: MarinerSettings): Boolean {
        if (viewingGroup in settings.disabledViewingGroups) return false
        val enabled = settings.enabledViewingGroups ?: return true
        return viewingGroup in enabled
    }
}
