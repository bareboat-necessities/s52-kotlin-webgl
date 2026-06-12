package io.github.s52.preslib.esri.csp

import kotlin.math.abs

object EsriDepthContourCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_depth_contour")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val depth = feature.double("VALDCO") ?: feature.double("DRVAL1") ?: feature.leastDepth
        val isSafety = depth != null && abs(depth - context.safetyContour) <= 0.05
        emit.simpleLine("DEPSC", "SOLID", if (isSafety) 0.72 else 0.28)
        if (depth != null) emit.text(depth.fmt(), color = "CHBLK")
        return true
    }
}

object EsriUnsurveyedAreaCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_unsurveyed_area")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        emit.areaFill("DEPIT")
        emit.complexLine("LOWACC41", paint = "LOWACC", viewingGroup = 31011)
        emit.text("Unsurveyed", color = "CHBLK")
        return true
    }
}

object EsriCoastlineCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_coastline")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val lowAccuracy = feature.lowAccuracy || ((feature.int("QUAPOS") ?: -1) in setOf(4, 5, 9))
        emit.simpleLine("CSTLN", if (lowAccuracy) "DASH" else "SOLID", if (lowAccuracy) 0.32 else 0.56)
        return true
    }
}

object EsriShoreConstructionCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_shore_construction")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.acronym.uppercase()) {
            "SLCONS" -> {
                val cat = feature.int("CATSLC")
                val width = if (cat in setOf(1, 6, 15, 16)) 0.72 else 0.48
                emit.simpleLine("CHBLK", "SOLID", width)
                if (feature.primitive == 3) emit.areaFill("LANDA")
            }
            "DYKCON" -> {
                emit.simpleLine("CHBRN", "SOLID", 0.56)
                if (feature.primitive == 3) emit.areaFill("LANDA")
            }
            "DAMCON" -> {
                emit.simpleLine("CHBLK", "SOLID", 0.72)
                if (feature.primitive == 3) emit.areaFill("LANDA")
            }
            "GATCON" -> emit.simpleLine("CHBLK", "DASH", 0.48)
            else -> emit.simpleLine("CHBLK", "SOLID", 0.48)
        }
        return true
    }
}

object EsriWaterFeaturesCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_water_features")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.primitive) {
            2 -> emit.simpleLine("DEPSC", "SOLID", 0.36)
            3 -> {
                emit.areaFill("DEPIT")
                emit.simpleLine("DEPSC", "SOLID", 0.28)
            }
            else -> emit.symbol("C20_River.svg")
        }
        val name = feature.string("OBJNAM")
        if (!name.isNullOrBlank()) emit.text(name, color = "CHBLK")
        return true
    }
}

object EsriRoadCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_road")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        if (feature.primitive == 3) emit.areaFill("LANDA")
        emit.simpleLine("CHBRN", if (feature.int("CATROD") == 4) "DASH" else "SOLID", 0.36)
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = "CHBRN") }
        return true
    }
}

object EsriTunnelCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_tunnel")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        emit.simpleLine("CHBRN", "DASH", 0.36)
        return true
    }
}

object EsriVegetationCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_vegetation")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val category = feature.int("CATVEG")
        val pattern = when (category) {
            1, 2, 3, 4, 5, 6, 7, 8 -> "C30_WoodedArea"
            9 -> "C32_Mangrove"
            11, 12 -> "C33_Marsh"
            15 -> "C_t_Eelgrass"
            else -> if (feature.acronym.equals("FORSTC", true)) "C30_WoodedArea" else "C33_Marsh"
        }
        emit.areaFill("LANDA")
        emit.areaPattern(pattern, backgroundColor = "LANDA")
        emit.simpleLine("CHGRD", "DASH", 0.24)
        return true
    }
}

object EsriUnderwaterHazardCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_underwater_hazard")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val valsou = feature.double("VALSOU") ?: feature.leastDepth
        val isolated = valsou != null && valsou <= context.safetyContour && (feature.greatestDepth ?: context.safetyContour) >= context.safetyContour
        if (isolated && !context.isolatedDangersOff) {
            emit.symbol("ISODGR01")
        } else {
            val symbol = when {
                feature.acronym.equals("UWTROC", true) && valsou == null -> "K13_Underwater_Rock_uncertain.svg"
                feature.acronym.equals("UWTROC", true) -> "K14_Underwater_Rock_known.svg"
                valsou != null && valsou <= context.safetyDepth -> "K1_Obstruction4mm_shoalWk.svg"
                valsou != null -> "K1_Obstruction4mm_InDepthRangeWk.svg"
                else -> "K40_Obstruction_depth_unknown.svg"
            }
            emit.symbol(symbol)
        }
        if (feature.primitive == 3) emit.simpleLine("CHBLK", "DOT", 0.32)
        if (valsou != null) emit.sounding(valsou, if (valsou <= context.safetyDepth) "SNDG2" else "SNDG1")
        return true
    }
}

object EsriSpoilGroundCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_spoil_ground")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        emit.complexLine("N62_SpoilGround")
        if (feature.primitive == 3) emit.areaPattern("N62_SpoilGround", backgroundColor = null)
        return true
    }
}

object EsriCablesPipelinesCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_cables_pipelines")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.acronym.uppercase()) {
            "CBLOHD" -> emit.complexLine("L31_1_PowerCable")
            "CBLSUB", "CBLARE" -> emit.complexLine("L30_Cable")
            "PIPOHD" -> emit.complexLine("D28_OverheadPipeline")
            "PIPSOL", "PIPARE" -> emit.complexLine("L40_1_Pipeline")
            else -> emit.simpleLine("CHBLK", "DASH", 0.32)
        }
        if (feature.primitive == 3) emit.areaPattern("N2_1_RestrictedArea", backgroundColor = null)
        return true
    }
}

object EsriOffshoreInstallationCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_offshore_installation")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.acronym.uppercase()) {
            "OFSPLF" -> emit.symbol("L10_Platform.svg")
            "OILBAR" -> {
                emit.complexLine("F29_1_FloatingBarrier")
                if (feature.primitive == 3) emit.areaPattern("F29_1_FloatingBarrier")
            }
            "WATTUR" -> emit.symbol("L24_UnderwaterTurbine.svg")
            else -> emit.symbol("L10_Platform.svg")
        }
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = "CHBLK") }
        return true
    }
}

object EsriAnchoragesBerthsCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_anchorages_berths")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.acronym.uppercase()) {
            "BERTHS" -> emit.symbol("F19_1_BerthNumber.svg")
            else -> emit.symbol("N12_Anchorage.svg")
        }
        if (feature.primitive == 2) emit.simpleLine("CHMGD", "DASH", 0.32)
        if (feature.primitive == 3) emit.areaPattern("N12_Anchorage", backgroundColor = null)
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = "CHMGD") }
        return true
    }
}

object EsriPortsCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_ports")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val symbol = when (feature.acronym.uppercase()) {
            "MORFAC" -> "Q40_MooringBuoy.svg"
            "HRBFAC" -> "F11_1_Marina.svg"
            "DOCARE" -> "F27_WetDock.svg"
            "DRYDOC" -> "F25_DryDock.svg"
            "FLODOC" -> "F26_FloatingDock.svg"
            "PONTON" -> "F16_Pontoon.svg"
            "GRIDRN" -> "F24_Gridiron.svg"
            else -> "F13_Wharf.svg"
        }
        if (feature.primitive == 3) emit.areaFill("LANDA")
        if (feature.primitive != 1) emit.simpleLine("CHBLK", "SOLID", 0.48)
        emit.symbol(symbol)
        return true
    }
}

object EsriFishingAquacultureCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_fishing_aquaculture")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val symbol = when (feature.acronym.uppercase()) {
            "FSHGRD" -> "K45_FishTrapArea.svg"
            "FSHFAC" -> "K44_2_FishTrap.svg"
            else -> "K48_1_MarineFarm.svg"
        }
        if (feature.primitive == 3) emit.areaPattern(symbol.removeSuffix(".svg"), backgroundColor = null)
        emit.symbol(symbol)
        return true
    }
}

object EsriRoutesCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_routes")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.acronym.uppercase()) {
            "TSSBND" -> emit.complexLine("M15_TSSBoundary")
            "TSELNE" -> emit.complexLine("M12_TrafficSeparationLine")
            "RECTRC", "RCRTCL", "NAVLNE" -> emit.complexLine("M1_NavigationLine")
            "RADLNE", "RDOCAL" -> emit.complexLine("M32_RadarLine")
            "FERYRT" -> emit.complexLine("M50_FerryRoute")
            "SUBTLN" -> emit.complexLine("N33_SubmarineTransitLane")
            else -> emit.simpleLine("CHMGD", "DASH", 0.32)
        }
        if (feature.primitive == 3) emit.areaPattern("N2_1_RestrictedArea", backgroundColor = null)
        val orientation = feature.double("ORIENT")
        if (orientation != null) emit.text("${orientation.fmt()} deg", color = "CHMGD")
        return true
    }
}

object EsriBuoyCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_buoy")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val symbol = when (feature.acronym.uppercase()) {
            "BOYLAT" -> lateralBuoy(feature)
            "BOYCAR" -> cardinalBuoy(feature)
            "BOYISD" -> "Q130_4_IsolatedDangerBuoy.svg"
            "BOYSAW" -> "Q130_5_SafeWaterBuoy.svg"
            "BOYSPP" -> "Q130_6_SpecialPurposeBuoy.svg"
            "BOYWTW" -> "Q130_7_NewDangerMark.svg"
            "BOYINB" -> "Q40_MooringBuoy.svg"
            "LITFLT" -> "Q30_LightFloat.svg"
            "LITVES" -> "Q32_LightVessel.svg"
            else -> buoyShape(feature)
        }
        emit.symbol(symbol)
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = "CHBLK") }
        return true
    }

    private fun lateralBuoy(feature: EsriCspFeature): String {
        val catlam = feature.int("CATLAM")
        return when (catlam) {
            1 -> "Q21_Can_buoy.svg"
            2 -> "Q20b_Conical_buoy.svg"
            3, 4 -> "Q130_1_PreferredChannelBuoy.svg"
            else -> buoyShape(feature)
        }
    }

    private fun cardinalBuoy(feature: EsriCspFeature): String = when (feature.int("CATCAM")) {
        1 -> "Q130_3_CardinalBuoy_North.svg"
        2 -> "Q130_3_CardinalBuoy_East.svg"
        3 -> "Q130_3_CardinalBuoy_South.svg"
        4 -> "Q130_3_CardinalBuoy_West.svg"
        else -> "Q130_3_CardinalBuoy_North.svg"
    }

    private fun buoyShape(feature: EsriCspFeature): String = when (feature.int("BOYSHP")) {
        1 -> "Q20b_Conical_buoy.svg"
        2 -> "Q21_Can_buoy.svg"
        3 -> "Q22a_Spherical_buoy.svg"
        4 -> "Q23_Pillar_buoy.svg"
        5 -> "Q24_Spar_buoy.svg"
        6 -> "Q25_Barrel_buoy.svg"
        7 -> "Q26_Superbuoy.svg"
        else -> "Q1_DefaultBuoy.svg"
    }
}

object EsriBeaconCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_beacon")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val symbol = when (feature.acronym.uppercase()) {
            "BCNLAT" -> when (feature.int("CATLAM")) {
                1 -> "Q92_MinorGreenLateralBeacon.svg"
                2 -> "Q91_MinorRedLateralBeacon.svg"
                else -> "Q80_DefaultBeacon.svg"
            }
            "BCNCAR" -> when (feature.int("CATCAM")) {
                1 -> "Q130_3_CardinalBeacon_North.svg"
                2 -> "Q130_3_CardinalBeacon_East.svg"
                3 -> "Q130_3_CardinalBeacon_South.svg"
                4 -> "Q130_3_CardinalBeacon_West.svg"
                else -> "Q80_DefaultBeacon.svg"
            }
            "BCNISD" -> "Q130_4_IsolatedDangerBeacon.svg"
            "BCNSAW" -> "Q130_5_SafeWaterBeacon.svg"
            "BCNSPP" -> "Q130_6_SpecialPurposeBeacon.svg"
            "DAYMAR" -> daymark(feature)
            "TOPMAR" -> topmark(feature)
            else -> "Q80_DefaultBeacon.svg"
        }
        emit.symbol(symbol)
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = "CHBLK") }
        return true
    }

    private fun daymark(feature: EsriCspFeature): String = when (feature.int("TOPSHP") ?: feature.int("BCNSHP")) {
        1 -> "Q101_SquareDaymark.svg"
        2 -> "Q101_TriangularDaymarkPointUp.svg"
        3 -> "Q101_TriangularDaymarkPointDown.svg"
        else -> "Q101_SquareDaymark.svg"
    }

    private fun topmark(feature: EsriCspFeature): String = when (feature.int("TOPSHP")) {
        1 -> "Q9_TopmarkConePointUp.svg"
        2 -> "Q9_TopmarkConePointDown.svg"
        3 -> "Q9_TopmarkTwoConesPointUp.svg"
        4 -> "Q9_TopmarkTwoConesPointDown.svg"
        5 -> "Q9_TopmarkTwoConesBaseToBase.svg"
        6 -> "Q9_TopmarkTwoConesPointToPoint.svg"
        7 -> "Q9_TopmarkSphere.svg"
        8 -> "Q9_TopmarkTwoSpheres.svg"
        9 -> "Q9_TopmarkXShape.svg"
        10 -> "Q9_TopmarkCross.svg"
        else -> "Q9_Topmark.svg"
    }
}

object EsriLandmarkCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_landmark")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val symbol = when (feature.acronym.uppercase()) {
            "BUILNG", "BUISGL" -> "D5_Building.svg"
            "SILTNK" -> if (feature.int("CATSIW") == 3) "E33_Silo.svg" else "E32_Tank.svg"
            "PYLONS" -> "D26_Pylon.svg"
            "CRANES" -> "F53_Crane.svg"
            "LNDMRK" -> landmarkSymbol(feature)
            else -> "E1_PointFeature.svg"
        }
        if (feature.primitive == 3) {
            emit.areaFill("LANDA")
            emit.simpleLine("CHBLK", "SOLID", 0.28)
        }
        emit.symbol(symbol)
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = conspicuousColor(feature)) }
        return true
    }

    private fun landmarkSymbol(feature: EsriCspFeature): String = when (feature.int("CATLMK")) {
        1 -> "Q100_Cairn.svg"
        2 -> "E24_Monument.svg"
        3 -> "E20_Tower.svg"
        4 -> "E21_WaterTower.svg"
        5 -> "E22_Chimney.svg"
        6 -> "E23_FlareStack.svg"
        7 -> "E25_Windmill.svg"
        8 -> "E28_Mast.svg"
        9 -> "E29_RadioTelevisionTower.svg"
        10 -> "E30_3_RadarScanner.svg"
        11 -> "E31_DishAerial.svg"
        12 -> "E17_MosqueMinaret.svg"
        13 -> "E10_1_ReligiousBuildingChristian.svg"
        14 -> "E13_ReligiousBuildingNonChristian.svg"
        else -> "E1_PointFeature.svg"
    }

    private fun conspicuousColor(feature: EsriCspFeature): String = if (feature.int("CONVIS") == 1 || feature.int("CONRAD") == 1) "CHBLK" else "CHBRN"
}

object EsriControlMagneticCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_control_magnetic")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        if (feature.acronym.equals("MAGVAR", true)) {
            if (feature.primitive == 2) emit.simpleLine("CHMGD", "DASH", 0.28) else emit.symbol("B68_MagneticVariation.svg")
            val value = feature.double("VALMAG")
            if (value != null) emit.text("Var ${value.fmt()}", color = "CHMGD")
        } else {
            emit.symbol("B22_FixedPoint.svg")
        }
        return true
    }
}

object EsriTidesCurrentsCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_tides_currents")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        if (feature.primitive == 2) emit.simpleLine("CHMGD", "DASH", 0.28) else emit.symbol("H40_Current.svg")
        val orient = feature.double("ORIENT")
        val rate = feature.double("CURVEL") ?: feature.double("VALACM")
        if (orient != null || rate != null) emit.text(listOfNotNull(orient?.let { "${it.fmt()} deg" }, rate?.let { "${it.fmt()} kn" }).joinToString(" "), color = "CHMGD")
        return true
    }
}

object EsriServicesPointsCsp : EsriConditionalProcedure {
    override val names = setOf("chart1_services_points")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val symbol = when (feature.acronym.uppercase()) {
            "FOGSIG" -> "R1_FogSignal.svg"
            "RADSTA" -> "S1_RadarStation.svg"
            "RADRFL" -> "S4_RadarReflector.svg"
            "RDOSTA" -> "S14_RadioDirectionFindingStation.svg"
            "SISTAT" -> "T20_SignalStation.svg"
            "CGUSTA" -> "T10_CoastGuardStation.svg"
            "PILPNT" -> "T1_1_PilotBoardingPlace.svg"
            "SMCFAC" -> "U_a_MarinaFacilities.svg"
            "FSHFAC" -> "F10_FishingHarbor.svg"
            "WEDKLP" -> "J13_1_KelpWeed.svg"
            "SPRING" -> "J15_Spring.svg"
            else -> "E1_PointFeature.svg"
        }
        emit.symbol(symbol)
        feature.string("OBJNAM")?.takeIf { it.isNotBlank() }?.let { emit.text(it, color = "CHBLK") }
        return true
    }
}

private fun Double.fmt(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
