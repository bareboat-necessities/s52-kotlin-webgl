package io.github.s52.core.model

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.catalog.S57ObjectClassKey
import io.github.s52.catalog.toKey
import io.github.s52.core.geometry.EncGeometry

data class EncFeature(
    val id: Long,
    val objectClass: S57ObjectClass,
    val primitive: PrimitiveType,
    val attributes: S57Attributes,
    val geometry: EncGeometry,
    val scaleMin: Int? = null,
    val scaleMax: Int? = null,
    val objectClassKey: S57ObjectClassKey = objectClass.toKey()
) {
    init {
        require(objectClass.supports(primitive)) {
            "Object class ${objectClass.acronym} does not support primitive $primitive"
        }
    }
}
