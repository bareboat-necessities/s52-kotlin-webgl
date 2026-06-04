package io.github.s52.core.model

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.EncGeometry

/** Boundary model for upstream parsers before catalogue validation. */
data class RawEncFeature(
    val id: Long,
    val objectClassAcronym: String,
    val primitive: PrimitiveType,
    val rawAttributes: Map<String, S57Value>,
    val geometry: EncGeometry,
    val objectClassCode: Int? = null,
    val scaleMin: Int? = null,
    val scaleMax: Int? = null
)

fun RawEncFeature.toTypedFeature(): EncFeature {
    val objectClass = objectClassCode?.let(S57ObjectClass::fromCode)
        ?: S57ObjectClass.fromAcronym(objectClassAcronym)
        ?: error("Unsupported S-57 object class: $objectClassAcronym / $objectClassCode")

    val typedAttributes = rawAttributes.mapKeys { (name, _) ->
        S57Attribute.fromAcronym(name)
            ?: error("Unsupported S-57 attribute '$name' on ${objectClass.acronym} feature $id")
    }

    return EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = primitive,
        attributes = S57Attributes(typedAttributes),
        geometry = geometry,
        scaleMin = scaleMin,
        scaleMax = scaleMax
    )
}
