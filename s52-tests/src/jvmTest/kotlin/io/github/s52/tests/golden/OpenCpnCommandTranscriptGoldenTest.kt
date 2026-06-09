package io.github.s52.tests.golden

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.settings.DisplayCategory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenCpnCommandTranscriptGoldenTest {
    @Test
    fun representativeOpenCpnCommandTranscriptMatchesGoldenExactly() {
        val actual = S52DrawCommandTranscript.serialize(representativeCommands())
        val goldenPath = "golden/opencpn-command-transcripts/representative.commands.jsonl"
        val expected = readGolden(goldenPath)
        if (shouldUpdateGoldens()) {
            writeGolden(goldenPath, actual)
        } else {
            assertEquals(expected, actual)
        }
    }

    private fun representativeCommands(): List<S52DrawCommand> {
        val holedArea = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.0, 40.0),
                Coordinate(-73.99, 40.0),
                Coordinate(-73.99, 40.01),
                Coordinate(-74.0, 40.01)
            ),
            holes = listOf(
                listOf(
                    Coordinate(-73.997, 40.003),
                    Coordinate(-73.993, 40.003),
                    Coordinate(-73.993, 40.007),
                    Coordinate(-73.997, 40.007)
                )
            )
        )

        return listOf(
            S52DrawCommand.AreaFill(
                featureId = 1001,
                geometry = holedArea,
                colorToken = "DEPMD",
                priority = 2,
                viewingGroup = 21010,
                category = DisplayCategory.Standard,
                overRadar = false
            ),
            S52DrawCommand.AreaPattern(
                featureId = 1002,
                geometry = holedArea,
                patternName = "ACHARE02",
                parameters = listOf("anchor"),
                backgroundColorToken = "DEPDW",
                priority = 5,
                viewingGroup = 24010,
                category = DisplayCategory.Standard,
                overRadar = false
            ),
            S52DrawCommand.LineComplex(
                featureId = 1003,
                geometry = EncGeometry.LineString(
                    listOf(
                        Coordinate(-74.01, 40.02),
                        Coordinate(-74.0, 40.025),
                        Coordinate(-73.99, 40.02)
                    )
                ),
                lineStyleName = "COALNE01",
                parameters = listOf("shoreline"),
                priority = 4,
                viewingGroup = 11060,
                category = DisplayCategory.DisplayBase,
                overRadar = false
            ),
            S52DrawCommand.PointSymbol(
                featureId = 1004,
                geometry = EncGeometry.Point(Coordinate(-73.995, 40.005)),
                symbolName = "BOYLAT01",
                parameters = listOf("CATLAM=1"),
                rotationDegrees = 315.0,
                priority = 8,
                viewingGroup = 27010,
                category = DisplayCategory.Standard,
                overRadar = true
            ),
            S52DrawCommand.Text(
                featureId = 1005,
                geometry = EncGeometry.Point(Coordinate(-73.992, 40.006)),
                textExpression = "OBJNAM",
                rawArgs = listOf("OBJNAM", "2", "1", "2", "15110", "21", "CHBLK"),
                textKind = InstructionKind.TX,
                colorToken = "CHBLK",
                priority = 9,
                viewingGroup = 28010,
                category = DisplayCategory.Standard,
                overRadar = true
            ),
            S52DrawCommand.Sounding(
                featureId = 1006,
                geometry = EncGeometry.Point(Coordinate(-73.991, 40.004, 12.7)),
                depthLabel = "12.7",
                colorToken = "SNDG2",
                priority = 9,
                viewingGroup = 33010,
                category = DisplayCategory.Standard,
                overRadar = true
            )
        )
    }

    private fun readGolden(path: String): String =
        requireNotNull(javaClass.classLoader.getResource(path)) { "Missing golden resource: $path" }
            .readText()

    private fun writeGolden(path: String, text: String) {
        val root = File(System.getProperty("user.dir")).resolve("src/jvmTest/resources")
        val file = root.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    private fun shouldUpdateGoldens(): Boolean =
        System.getProperty("s52.updateGoldens") == "true" ||
            System.getenv("S52_UPDATE_GOLDENS") == "true"
}
