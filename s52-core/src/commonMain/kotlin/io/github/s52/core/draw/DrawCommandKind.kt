package io.github.s52.core.draw

/**
 * Renderer-independent command family.
 *
 * The enum gives renderers, transcript writers, and golden tests a stable
 * discriminator without relying on Kotlin class names.
 */
enum class DrawCommandKind(val stableName: String, val order: Int) {
    AreaFill("area-fill", 0),
    AreaPattern("area-pattern", 1),
    LineSimple("line-simple", 2),
    LineComplex("line-complex", 3),
    PointSymbol("point-symbol", 4),
    Text("text", 5),
    Sounding("sounding", 6)
}
