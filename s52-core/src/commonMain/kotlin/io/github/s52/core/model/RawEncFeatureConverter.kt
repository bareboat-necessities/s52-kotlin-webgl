package io.github.s52.core.model

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57AttributeValueKind
import io.github.s52.catalog.S57ObjectClass

/** Converts raw parser-facing features into the typed model with diagnostics. */
object RawEncFeatureConverter {
    fun convert(raw: RawEncFeature): FeatureConversionResult {
        val diagnostics = mutableListOf<FeatureConversionDiagnostic>()
        val objectClass = resolveObjectClass(raw, diagnostics)
            ?: return FeatureConversionResult.Failure(diagnostics)

        if (!objectClass.supports(raw.primitive)) {
            diagnostics += raw.error(
                "${objectClass.acronym} does not support ${raw.primitive} primitive"
            )
            return FeatureConversionResult.Failure(diagnostics)
        }

        val typedAttributes = linkedMapOf<S57Attribute, S57Value>()
        raw.rawAttributes.forEach { (name, value) ->
            val attribute = S57Attribute.fromAcronym(name)
            if (attribute == null) {
                diagnostics += raw.error("Unknown S-57 attribute '$name' on ${objectClass.acronym}")
            } else {
                validateAttributeValue(raw, attribute, value, diagnostics)
                if (attribute in typedAttributes) {
                    diagnostics += raw.error("Duplicate S-57 attribute '${attribute.acronym}' on feature ${raw.id}")
                } else {
                    typedAttributes[attribute] = value
                }
            }
        }

        if (diagnostics.any { it.severity == FeatureConversionDiagnostic.Severity.Error }) {
            return FeatureConversionResult.Failure(diagnostics)
        }

        return FeatureConversionResult.Success(
            feature = EncFeature(
                id = raw.id,
                objectClass = objectClass,
                primitive = raw.primitive,
                attributes = S57Attributes(typedAttributes),
                geometry = raw.geometry,
                scaleMin = raw.scaleMin,
                scaleMax = raw.scaleMax
            ),
            diagnostics = diagnostics
        )
    }

    private fun resolveObjectClass(
        raw: RawEncFeature,
        diagnostics: MutableList<FeatureConversionDiagnostic>
    ): S57ObjectClass? {
        val byCode = raw.objectClassCode?.let(S57ObjectClass::fromCode)
        val byAcronym = S57ObjectClass.fromAcronym(raw.objectClassAcronym)

        if (raw.objectClassCode != null && byCode == null) {
            diagnostics += raw.error("Unknown S-57 object class code ${raw.objectClassCode}")
        }
        if (byAcronym == null) {
            diagnostics += raw.error("Unknown S-57 object class acronym '${raw.objectClassAcronym}'")
        }
        if (byCode != null && byAcronym != null && byCode != byAcronym) {
            diagnostics += raw.error(
                "Object class code ${raw.objectClassCode} resolves to ${byCode.acronym}, " +
                    "but acronym is ${byAcronym.acronym}"
            )
        }

        return byCode ?: byAcronym
    }

    private fun validateAttributeValue(
        raw: RawEncFeature,
        attribute: S57Attribute,
        value: S57Value,
        diagnostics: MutableList<FeatureConversionDiagnostic>
    ) {
        val ok = when (attribute.valueKind) {
            S57AttributeValueKind.Integer -> value.canBeInt()
            S57AttributeValueKind.Decimal -> value.canBeDouble()
            S57AttributeValueKind.Text -> value.canBeText()
            S57AttributeValueKind.Enumeration -> value.canBeInt()
            S57AttributeValueKind.EnumerationList -> value.canBeIntList()
            S57AttributeValueKind.Unknown -> true
        }
        if (!ok) {
            diagnostics += raw.error(
                "Attribute ${attribute.acronym} expects ${attribute.valueKind} value, got ${value.kindName()}"
            )
        }
    }

    private fun RawEncFeature.error(message: String): FeatureConversionDiagnostic =
        FeatureConversionDiagnostic(FeatureConversionDiagnostic.Severity.Error, id, message)

    private fun S57Value.kindName(): String = when (this) {
        is S57Value.Integer -> "Integer"
        is S57Value.Decimal -> "Decimal"
        is S57Value.Text -> "Text"
        is S57Value.ListValue -> "ListValue"
        S57Value.Empty -> "Empty"
    }

    private fun S57Value.canBeInt(): Boolean = when (this) {
        is S57Value.Integer -> true
        is S57Value.Decimal -> value % 1.0 == 0.0
        is S57Value.Text -> value.toIntOrNull() != null
        else -> false
    }

    private fun S57Value.canBeDouble(): Boolean = when (this) {
        is S57Value.Integer -> true
        is S57Value.Decimal -> true
        is S57Value.Text -> value.toDoubleOrNull() != null
        else -> false
    }

    private fun S57Value.canBeText(): Boolean = when (this) {
        is S57Value.Text -> true
        is S57Value.Integer -> true
        is S57Value.Decimal -> true
        S57Value.Empty -> true
        is S57Value.ListValue -> false
    }

    private fun S57Value.canBeIntList(): Boolean = when (this) {
        is S57Value.ListValue -> values.all { it.canBeInt() }
        else -> canBeInt()
    }
}
