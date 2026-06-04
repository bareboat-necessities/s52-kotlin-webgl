package io.github.s52.catalog

/**
 * Phase 0 hand-written subset of S-57 attributes.
 *
 * Phase 1 replaces this file with generated code from the S-57 attribute catalogue.
 */
enum class S57Attribute(
    val acronym: String,
    val code: Int
) {
    CATOBS("CATOBS", 34),
    CATREA("CATREA", 56),
    CATWRK("CATWRK", 71),
    COLOUR("COLOUR", 75),
    COLPAT("COLPAT", 76),
    DRVAL1("DRVAL1", 87),
    DRVAL2("DRVAL2", 88),
    HEIGHT("HEIGHT", 98),
    INFORM("INFORM", 102),
    LITCHR("LITCHR", 107),
    OBJNAM("OBJNAM", 116),
    SIGGRP("SIGGRP", 141),
    SIGPER("SIGPER", 142),
    TXTDSC("TXTDSC", 156),
    VALDCO("VALDCO", 174),
    VALSOU("VALSOU", 179),
    WATLEV("WATLEV", 187);

    companion object {
        private val byAcronym: Map<String, S57Attribute> = entries.associateBy { it.acronym }
        private val byCode: Map<Int, S57Attribute> = entries.associateBy { it.code }

        fun fromAcronym(value: String): S57Attribute? = byAcronym[value.uppercase()]

        fun fromCode(code: Int): S57Attribute? = byCode[code]
    }
}
