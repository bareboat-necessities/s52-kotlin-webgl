package io.github.s52.preslib.esri.rules

/**
 * Hand-written U.S. Chart No. 1 / ECDIS fallback bridge.
 *
 * The generated ESRI CustomSymbolMap rules are still preferred when present.
 * These rules fill broad Chart No. 1 coverage gaps for the S-57 object classes
 * that commonly appear in NOAA ENCs but are not always represented by a direct
 * ESRI XML rule in the checked-in generated registry.
 */
object EsriChart1FallbackRules {
    val rules: List<EsriPortrayalRule> = buildList {
        var order = 100_000

        fun function(objects: List<String>, primitive: Int? = null, name: String) {
            add(
                EsriPortrayalRule(
                    objects = objects,
                    primitive = primitive,
                    action = EsriRuleAction.Function(listOf(name)),
                    sourceOrder = order++
                )
            )
        }

        fun function(`object`: String, primitive: Int? = null, name: String) = function(listOf(`object`), primitive, name)

        function("SOUNDG", 1, "sounding")
        function("DEPARE", 3, "depare03")
        function("DRGARE", 3, "drgare02")
        function("DEPCNT", 2, "chart1_depth_contour")
        function("UNSARE", 3, "chart1_unsurveyed_area")

        function(listOf("COALNE", "LAKSHR"), 2, "chart1_coastline")
        function(listOf("SLCONS", "DYKCON", "DAMCON", "GATCON"), null, "chart1_shore_construction")
        function(listOf("LNDARE", "LNDRGN", "BUAARE"), 3, "lndare01")
        function(listOf("RIVERS", "CANALS"), null, "chart1_water_features")
        function("LAKARE", 3, "chart1_water_features")
        function("ROADWY", null, "chart1_road")
        function("TUNNEL", null, "chart1_tunnel")

        function(listOf("FORSTC", "VEGATN"), 3, "chart1_vegetation")
        function("SBDARE", null, "sbdare01")
        function("WEDKLP", null, "chart1_services_points")
        function("SPRING", 1, "chart1_services_points")

        function("WRECKS", null, "wrecks05")
        function(listOf("UWTROC", "OBSTRN"), null, "chart1_underwater_hazard")
        function("SLOGRD", 3, "chart1_spoil_ground")

        function(listOf("CBLSUB", "CBLOHD", "PIPSOL", "PIPOHD", "PIPARE", "CBLARE"), null, "chart1_cables_pipelines")
        function(listOf("OFSPLF", "OILBAR", "WATTUR"), null, "chart1_offshore_installation")

        function(listOf("ACHARE", "ACHBRT", "BERTHS"), null, "chart1_anchorages_berths")
        function(listOf("MORFAC", "HRBFAC", "DOCARE", "DRYDOC", "FLODOC", "PONTON", "GRIDRN"), null, "chart1_ports")
        function(listOf("FSHFAC", "FSHGRD"), null, "chart1_fishing_aquaculture")

        function(listOf("RESARE", "CTNARE", "PRCARE", "MIPARE", "ADMARE", "EXEZNE", "ISTZNE", "SEAARE", "COSARE", "PILBOP", "FAIRWY"), null, "resare04")
        function(listOf("NAVLNE", "RECTRC", "RCRTCL", "RDOCAL", "RADLNE", "STSLNE", "SUBTLN", "FERYRT"), 2, "chart1_routes")
        function(listOf("TSSBND", "TSELNE"), 2, "chart1_routes")
        function(listOf("TSSCRS", "TSSLPT", "TSSRON"), 3, "chart1_routes")

        function("LIGHTS", 1, "lights")
        function(listOf("BOYLAT", "BOYCAR", "BOYISD", "BOYSAW", "BOYSPP", "BOYINB", "BOYWTW", "LITFLT", "LITVES"), 1, "chart1_buoy")
        function(listOf("BCNLAT", "BCNCAR", "BCNISD", "BCNSAW", "BCNSPP", "DAYMAR", "TOPMAR", "RTPBCN"), 1, "chart1_beacon")
        function("RETRFL", 1, "chart1_beacon")

        function(listOf("LNDMRK", "BUILNG", "BUISGL", "SILTNK", "PYLONS", "CRANES"), null, "chart1_landmark")
        function(listOf("MAGVAR", "RCTLPT"), null, "chart1_control_magnetic")
        function(listOf("CURENT", "TWRTPT"), null, "chart1_tides_currents")
        function(listOf("FOGSIG", "RADSTA", "RADRFL", "RDOSTA", "SISTAT", "CGUSTA", "PILPNT", "SMCFAC"), 1, "chart1_services_points")
    }
}
