package io.github.s52.catalog

/**
 * Phase 0 hand-written subset of S-57 object classes.
 *
 * Phase 1 replaces this file with generated code from the S-57 object catalogue.
 */
enum class S57ObjectClass(
    val acronym: String,
    val code: Int,
    val primitives: Set<PrimitiveType>
) {
    BCNCAR("BCNCAR", 4, setOf(PrimitiveType.Point)),
    BCNLAT("BCNLAT", 7, setOf(PrimitiveType.Point)),
    BOYCAR("BOYCAR", 14, setOf(PrimitiveType.Point)),
    BOYLAT("BOYLAT", 17, setOf(PrimitiveType.Point)),
    BOYSAW("BOYSAW", 20, setOf(PrimitiveType.Point)),
    COALNE("COALNE", 30, setOf(PrimitiveType.Line)),
    DEPARE("DEPARE", 42, setOf(PrimitiveType.Area)),
    DEPCNT("DEPCNT", 43, setOf(PrimitiveType.Line)),
    LIGHTS("LIGHTS", 75, setOf(PrimitiveType.Point)),
    LNDARE("LNDARE", 71, setOf(PrimitiveType.Area)),
    M_COVR("M_COVR", 302, setOf(PrimitiveType.Area)),
    M_QUAL("M_QUAL", 308, setOf(PrimitiveType.Area)),
    OBSTRN("OBSTRN", 86, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    RESARE("RESARE", 112, setOf(PrimitiveType.Area)),
    SOUNDG("SOUNDG", 129, setOf(PrimitiveType.Point)),
    TOPMAR("TOPMAR", 144, setOf(PrimitiveType.Point)),
    WRECKS("WRECKS", 159, setOf(PrimitiveType.Point, PrimitiveType.Area));

    fun supports(primitive: PrimitiveType): Boolean = primitive in primitives

    companion object {
        private val byAcronym: Map<String, S57ObjectClass> = entries.associateBy { it.acronym }
        private val byCode: Map<Int, S57ObjectClass> = entries.associateBy { it.code }

        fun fromAcronym(value: String): S57ObjectClass? = byAcronym[value.uppercase()]

        fun fromCode(code: Int): S57ObjectClass? = byCode[code]
    }
}
