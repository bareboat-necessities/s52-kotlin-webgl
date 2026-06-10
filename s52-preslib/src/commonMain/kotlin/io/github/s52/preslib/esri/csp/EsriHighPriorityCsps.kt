package io.github.s52.preslib.esri.csp

object EsriSoundingCsp : EsriConditionalProcedure {
    override val names = setOf("sounding")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val depth = feature.double("VALSOU") ?: feature.leastDepth ?: return false
        val color = if (depth <= context.safetyDepth) "SNDG2" else "SNDG1"
        emit.sounding(depth, color)
        return true
    }
}

object EsriLightsCsp : EsriConditionalProcedure {
    override val names = setOf("lights", "light")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val colours = feature.list("COLOUR").mapNotNull { it.toIntOrNull() }.toSet()
        val base = when {
            3 in colours -> "P1_Light_red.svg"
            4 in colours -> "P1_Light_green.svg"
            6 in colours -> "P1_Light_yellow.svg"
            1 in colours -> "P1_Light_white.svg"
            else -> "P1_Light.svg"
        }
        emit.symbol(base)
        val flare = when {
            3 in colours -> "Light_Flare_red.svg"
            4 in colours -> "Light_Flare_green.svg"
            6 in colours -> "Light_Flare_yellow.svg"
            1 in colours -> "Light_Flare.svg"
            else -> null
        }
        if (flare != null) emit.symbol(flare)
        return true
    }
}

object EsriWrecks05Csp : EsriConditionalProcedure {
    override val names = setOf("wrecks05", "wrecks04")

    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val watlev = feature.int("WATLEV")
        val catwrk = feature.int("CATWRK")
        val valsou = feature.double("VALSOU")
        val leastDepth = valsou ?: feature.leastDepth ?: getLeastDepth(feature, watlev, catwrk)
        val isolatedDanger = isIsolatedDanger(feature, context, leastDepth, watlev)

        if (feature.lowAccuracy) {
            if (feature.primitive == 3) emit.complexLine("LOWACC41", paint = "LOWACC", viewingGroup = 31011)
            else emit.symbol("LOWACC01", viewingGroup = 31011)
        }

        return when (feature.primitive) {
            1 -> pointWreck(feature, context, emit, watlev, catwrk, valsou, isolatedDanger)
            3 -> areaWreck(feature, context, emit, watlev, valsou, isolatedDanger)
            else -> false
        }
    }

    private fun getLeastDepth(feature: EsriCspFeature, watlev: Int?, catwrk: Int?): Double = when {
        catwrk == 1 -> maxOf(20.1, (feature.seabedDepth ?: 86.1) - 66.0)
        watlev == 3 || watlev == 5 -> 0.0
        else -> -15.0
    }

    private fun isIsolatedDanger(feature: EsriCspFeature, context: EsriPortrayalContext, leastDepth: Double, watlev: Int?): Boolean {
        if (context.isolatedDangersOff) return false
        val greatest = feature.greatestDepth
        val isolated = leastDepth <= context.safetyContour && greatest != null && greatest >= context.safetyContour
        val shallow = context.showShallowDangers && leastDepth <= context.safetyContour && greatest != null && greatest >= 0.0
        return (isolated || shallow) && watlev != 1 && watlev != 2
    }

    private fun pointWreck(
        feature: EsriCspFeature,
        context: EsriPortrayalContext,
        emit: EsriInstructionEmitter,
        watlev: Int?,
        catwrk: Int?,
        valsou: Double?,
        isolatedDanger: Boolean
    ): Boolean {
        if (isolatedDanger) {
            emit.symbol("ISODGR01")
            return true
        }
        if (valsou == null) {
            val symbol = when {
                catwrk == 1 && watlev == 3 -> "K29_Wreck_notdangerous.svg"
                catwrk == 4 || catwrk == 5 -> "K24_Wreck_showing_hull.svg"
                watlev != null && watlev in 1..4 -> "K24_Wreck_showing_hull.svg"
                else -> "K25_Wreck_danger_no_depth.svg"
            }
            emit.symbol(symbol)
        } else {
            val symbol = when {
                valsou < 0.0 -> "K1_Obstruction4mm_DryWk.svg"
                valsou <= context.safetyDepth -> "K1_Obstruction4mm_shoalWk.svg"
                else -> "K1_Obstruction4mm_InDepthRangeWk.svg"
            }
            emit.symbol(symbol)
            emit.sounding(valsou, if (valsou <= context.safetyDepth) "SNDG2" else "SNDG1")
        }
        return true
    }

    private fun areaWreck(
        feature: EsriCspFeature,
        context: EsriPortrayalContext,
        emit: EsriInstructionEmitter,
        watlev: Int?,
        valsou: Double?,
        isolatedDanger: Boolean
    ): Boolean {
        val lineStyle = when {
            isolatedDanger -> "DOT"
            valsou != null && valsou <= context.safetyDepth -> "DOT"
            valsou != null && valsou > context.safetyDepth -> "DASH"
            watlev == 1 || watlev == 2 -> "SOLID"
            watlev == 4 -> "DASH"
            else -> "DOT"
        }
        emit.simpleLine("CHBLK", lineStyle, 0.32, paint = "HIGHACC")
        if (valsou == null) {
            val fill = when (watlev) {
                1, 2 -> "#f4e8c1"
                4 -> "#d6dbc9"
                else -> "#d1deef"
            }
            emit.areaFill(fill)
            if (isolatedDanger) emit.symbol("ISODGR01")
        } else if (isolatedDanger) {
            emit.symbol("ISODGR01")
        } else {
            emit.sounding(valsou, if (valsou <= context.safetyDepth) "SNDG2" else "SNDG1")
        }
        return true
    }
}

object EsriDepthAreaCsp : EsriConditionalProcedure {
    override val names = setOf("depare03", "drgare02")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val min = feature.double("DRVAL1") ?: feature.leastDepth ?: 0.0
        val max = feature.double("DRVAL2") ?: feature.greatestDepth ?: min
        val fill = when {
            max <= 0.0 -> "DEPIT"
            max <= context.safetyDepth -> "DEPVS"
            min <= context.safetyDepth -> "DEPMS"
            else -> "DEPDW"
        }
        emit.areaFill(fill)
        return true
    }
}

object EsriRestrictedAreaCsp : EsriConditionalProcedure {
    override val names = setOf("resare04", "rectrc01", "fairwy01", "swpare01")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val categories = feature.list("CATREA").mapNotNull { it.toIntOrNull() }.toSet()
        val symbol = when {
            1 in categories -> "N2_1_RestrictedArea.svg"
            7 in categories -> "N21_1_Fishing_Prohibited.svg"
            12 in categories -> "N20_1_Anchoring_Prohibited.svg"
            else -> "N2_1_RestrictedArea.svg"
        }
        emit.complexLine(symbol.removeSuffix(".svg"))
        if (feature.primitive == 3) emit.areaPattern(symbol.removeSuffix(".svg"), backgroundColor = null)
        emit.symbol(symbol)
        return true
    }
}

object EsriBridgeCsp : EsriConditionalProcedure {
    override val names = setOf("bridge02", "vertical_clearance")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        emit.simpleLine("CHBRN", "SOLID", 0.64)
        val clearance = feature.double("VERCLR") ?: feature.double("VERCOP") ?: feature.double("VERCSA")
        if (clearance != null) emit.text(clearance.formatDepth(), color = "CHBLK")
        return true
    }
}

object EsriCableCsp : EsriConditionalProcedure {
    override val names = setOf("cblohd02", "cblsub02", "pipsol01")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        when (feature.acronym) {
            "CBLOHD" -> emit.complexLine("L31_1_PowerCable")
            "CBLSUB" -> emit.complexLine("L30_Cable")
            else -> emit.simpleLine("CHBLK", "DASH", 0.32)
        }
        return true
    }
}

object EsriSeabedAreaCsp : EsriConditionalProcedure {
    override val names = setOf("sbdare01", "sbdare02")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        val nat = feature.list("NATSUR").firstOrNull()?.toIntOrNull()
        val symbol = when (nat) {
            1 -> "J1_Sand.svg"
            4 -> "J2_Mud.svg"
            5 -> "J3_Clay.svg"
            6 -> "J4_Silt.svg"
            7 -> "J5_Stones.svg"
            else -> "J1_Sand.svg"
        }
        if (feature.primitive == 3) emit.areaPattern(symbol.removeSuffix(".svg"), backgroundColor = "CHBRN")
        else emit.symbol(symbol)
        return true
    }
}

object EsriLandAreaCsp : EsriConditionalProcedure {
    override val names = setOf("lndare01", "lndelv01", "lndelv02", "buaare01")
    override fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean {
        emit.areaFill(if (feature.acronym == "BUAARE") "CHBRN" else "LANDA")
        val elevation = feature.double("ELEVAT")
        if (elevation != null) emit.text(elevation.formatDepth(), color = "CHBLK")
        return true
    }
}

private fun Double.formatDepth(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
