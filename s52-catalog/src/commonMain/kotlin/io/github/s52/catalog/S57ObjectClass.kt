package io.github.s52.catalog

/**
 * Phase 1 typed S-57 object class catalogue used by the S-52 portrayal boundary.
 *
 * This file is intentionally shaped like generated code: each enum entry is data-only
 * and contains no portrayal behavior. Later phases can replace this curated starter
 * catalogue with output from the official S-57 object catalogue importer without
 * changing the public model API.
 */
enum class S57ObjectClass(
    val acronym: String,
    val code: Int?,
    val primitives: Set<PrimitiveType>
) {
    ACHARE("ACHARE", null, area()),
    ACHBRT("ACHBRT", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    ADMARE("ADMARE", null, area()),
    AIRARE("AIRARE", null, area()),
    BCNCAR("BCNCAR", 4, point()),
    BCNISD("BCNISD", null, point()),
    BCNLAT("BCNLAT", 7, point()),
    BCNSAW("BCNSAW", null, point()),
    BCNSPP("BCNSPP", null, point()),
    BERTHS("BERTHS", null, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    BOYCAR("BOYCAR", 14, point()),
    BOYINB("BOYINB", null, point()),
    BOYISD("BOYISD", null, point()),
    BOYLAT("BOYLAT", 17, point()),
    BOYSAW("BOYSAW", 20, point()),
    BOYSPP("BOYSPP", null, point()),
    BOYWTW("BOYWTW", null, point()),
    BUAARE("BUAARE", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    BRIDGE("BRIDGE", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    BUISGL("BUISGL", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    BUILNG("BUILNG", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    CANALS("CANALS", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    CBLARE("CBLARE", null, area()),
    CBLOHD("CBLOHD", null, line()),
    CBLSUB("CBLSUB", null, line()),
    CGUSTA("CGUSTA", null, point()),
    COALNE("COALNE", 30, line()),
    CONVYR("CONVYR", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    COSARE("COSARE", null, area()),
    CRANES("CRANES", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    CTNARE("CTNARE", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    CURENT("CURENT", null, point()),
    DAMCON("DAMCON", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    DAYMAR("DAYMAR", null, point()),
    DEPCNT("DEPCNT", 43, line()),
    DEPARE("DEPARE", 42, area()),
    DISMAR("DISMAR", null, point()),
    DOCARE("DOCARE", null, area()),
    DRGARE("DRGARE", null, area()),
    DRYDOC("DRYDOC", null, area()),
    DYKCON("DYKCON", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    EXEZNE("EXEZNE", null, area()),
    FAIRWY("FAIRWY", null, area()),
    FERYRT("FERYRT", null, line()),
    FSHFAC("FSHFAC", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    FSHGRD("FSHGRD", null, area()),
    FLODOC("FLODOC", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    FOGSIG("FOGSIG", null, point()),
    FORSTC("FORSTC", null, area()),
    GATCON("GATCON", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    GRIDRN("GRIDRN", null, area()),
    HRBARE("HRBARE", null, area()),
    HRBFAC("HRBFAC", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    HULKES("HULKES", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    ISTZNE("ISTZNE", null, area()),
    LAKARE("LAKARE", null, area()),
    LAKSHR("LAKSHR", null, line()),
    LIGHTS("LIGHTS", 75, point()),
    LITFLT("LITFLT", null, point()),
    LITVES("LITVES", null, point()),
    LNDARE("LNDARE", 71, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    LNDELV("LNDELV", null, point()),
    LNDRGN("LNDRGN", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    LNDMRK("LNDMRK", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    LOGPON("LOGPON", null, area()),
    MAGVAR("MAGVAR", null, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    MIPARE("MIPARE", null, area()),
    MORFAC("MORFAC", null, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    NAVLNE("NAVLNE", null, line()),
    OBSTRN("OBSTRN", 86, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    OFSPLF("OFSPLF", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    OILBAR("OILBAR", null, area()),
    PILBOP("PILBOP", null, area()),
    PILPNT("PILPNT", null, point()),
    PIPARE("PIPARE", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    PIPSOL("PIPSOL", null, line()),
    PIPOHD("PIPOHD", null, line()),
    PONTON("PONTON", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    PRCARE("PRCARE", null, area()),
    PRDARE("PRDARE", null, area()),
    PYLONS("PYLONS", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    RADLNE("RADLNE", null, line()),
    RADRFL("RADRFL", null, point()),
    RADSTA("RADSTA", null, point()),
    RDOCAL("RDOCAL", null, line()),
    RCTLPT("RCTLPT", null, point()),
    RECTRC("RECTRC", null, line()),
    RCRTCL("RCRTCL", null, line()),
    RDOSTA("RDOSTA", null, point()),
    RESARE("RESARE", 112, area()),
    RETRFL("RETRFL", null, point()),
    RIVERS("RIVERS", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    ROADWY("ROADWY", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    RTPBCN("RTPBCN", null, point()),
    SBDARE("SBDARE", null, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    SEAARE("SEAARE", null, area()),
    SILTNK("SILTNK", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    SISTAT("SISTAT", null, point()),
    SLCONS("SLCONS", null, setOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)),
    SLOGRD("SLOGRD", null, area()),
    SLOTOP("SLOTOP", null, setOf(PrimitiveType.Point, PrimitiveType.Line)),
    SMCFAC("SMCFAC", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    SOUNDG("SOUNDG", 129, point()),
    SPRING("SPRING", null, point()),
    STSLNE("STSLNE", null, line()),
    SUBTLN("SUBTLN", null, line()),
    TESARE("TESARE", null, area()),
    TOPMAR("TOPMAR", 144, point()),
    TSELNE("TSELNE", null, line()),
    TSSBND("TSSBND", null, line()),
    TSSCRS("TSSCRS", null, area()),
    TSSLPT("TSSLPT", null, area()),
    TSSRON("TSSRON", null, area()),
    TUNNEL("TUNNEL", null, setOf(PrimitiveType.Line, PrimitiveType.Area)),
    UNSARE("UNSARE", null, area()),
    TWRTPT("TWRTPT", null, point()),
    UWTROC("UWTROC", null, setOf(PrimitiveType.Point, PrimitiveType.Area)),
    VEGATN("VEGATN", null, area()),
    WATTUR("WATTUR", null, point()),
    WEDKLP("WEDKLP", null, point()),
    WRECKS("WRECKS", 159, setOf(PrimitiveType.Point, PrimitiveType.Area)),

    M_ACCY("M_ACCY", null, area()),
    M_CSCL("M_CSCL", null, area()),
    M_COVR("M_COVR", 302, area()),
    M_HDAT("M_HDAT", null, area()),
    M_HOPA("M_HOPA", null, area()),
    M_NPUB("M_NPUB", null, area()),
    M_NSYS("M_NSYS", 312, area()),
    M_QUAL("M_QUAL", 308, area()),
    M_SDAT("M_SDAT", null, area()),
    M_SREL("M_SREL", null, area()),
    M_UNIT("M_UNIT", null, area()),
    C_AGGR("C_AGGR", 400, collection()),
    C_ASSO("C_ASSO", 401, collection());

    fun supports(primitive: PrimitiveType): Boolean = primitive in primitives

    companion object {
        private val byAcronym: Map<String, S57ObjectClass> = entries.associateBy { it.acronym }
        private val byCode: Map<Int, S57ObjectClass> = entries.mapNotNull { objectClass ->
            objectClass.code?.let { code -> code to objectClass }
        }.toMap()

        fun fromAcronym(value: String): S57ObjectClass? = byAcronym[value.trim().uppercase()]

        fun fromCode(code: Int): S57ObjectClass? = byCode[code]

        fun requireAcronym(value: String): S57ObjectClass =
            fromAcronym(value) ?: error("Unknown S-57 object class acronym '$value'")
    }
}

private fun point(): Set<PrimitiveType> = setOf(PrimitiveType.Point)
private fun line(): Set<PrimitiveType> = setOf(PrimitiveType.Line)
private fun area(): Set<PrimitiveType> = setOf(PrimitiveType.Area)
private fun collection(): Set<PrimitiveType> = setOf(PrimitiveType.Collection)
