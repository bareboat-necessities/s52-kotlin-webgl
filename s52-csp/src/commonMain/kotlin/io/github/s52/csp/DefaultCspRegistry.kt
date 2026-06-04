package io.github.s52.csp

import io.github.s52.core.csp.MapCspRegistry

object DefaultCspRegistry {
    fun phase0(): MapCspRegistry = MapCspRegistry(
        listOf(
            DepthAreaCsp()
        )
    )
}
