package io.github.s52.catalog

/**
 * Generated-style starter for common S-57 enumerated attribute values.
 *
 * This is not portrayal behavior. CSPs and lookup filters should compare typed
 * attributes and integer values; this table supplies names for validation,
 * diagnostics, and future generated-code replacement.
 */
enum class S57EnumeratedValue(
    val attribute: S57Attribute,
    val code: Int,
    val label: String
) {
    COLOUR_WHITE(S57Attribute.COLOUR, 1, "white"),
    COLOUR_BLACK(S57Attribute.COLOUR, 2, "black"),
    COLOUR_RED(S57Attribute.COLOUR, 3, "red"),
    COLOUR_GREEN(S57Attribute.COLOUR, 4, "green"),
    COLOUR_BLUE(S57Attribute.COLOUR, 5, "blue"),
    COLOUR_YELLOW(S57Attribute.COLOUR, 6, "yellow"),
    COLOUR_GREY(S57Attribute.COLOUR, 7, "grey"),
    COLOUR_BROWN(S57Attribute.COLOUR, 8, "brown"),
    COLOUR_AMBER(S57Attribute.COLOUR, 9, "amber"),
    COLOUR_VIOLET(S57Attribute.COLOUR, 10, "violet"),
    COLOUR_ORANGE(S57Attribute.COLOUR, 11, "orange"),
    COLOUR_MAGENTA(S57Attribute.COLOUR, 12, "magenta"),
    COLOUR_PINK(S57Attribute.COLOUR, 13, "pink"),

    WATLEV_PARTLY_SUBMERGED(S57Attribute.WATLEV, 1, "partly submerged at high water"),
    WATLEV_ALWAYS_DRY(S57Attribute.WATLEV, 2, "always dry"),
    WATLEV_ALWAYS_UNDERWATER(S57Attribute.WATLEV, 3, "always underwater/submerged"),
    WATLEV_COVERS_AND_UNCOVERS(S57Attribute.WATLEV, 4, "covers and uncovers"),
    WATLEV_AWASH(S57Attribute.WATLEV, 5, "awash"),
    WATLEV_FLOATING(S57Attribute.WATLEV, 7, "floating"),

    EXPSOU_WITHIN_RANGE(S57Attribute.EXPSOU, 1, "within range of depth of surrounding depth area"),
    EXPSOU_SHOALER(S57Attribute.EXPSOU, 2, "shoaler than range of depth of surrounding depth area"),
    EXPSOU_DEEPER(S57Attribute.EXPSOU, 3, "deeper than range of depth of surrounding depth area"),

    CATWRK_NON_DANGEROUS(S57Attribute.CATWRK, 1, "non-dangerous wreck"),
    CATWRK_DANGEROUS(S57Attribute.CATWRK, 2, "dangerous wreck"),
    CATWRK_DISTRIBUTED_REMAINS(S57Attribute.CATWRK, 3, "distributed remains of wreck"),
    CATWRK_MAST_SHOWING(S57Attribute.CATWRK, 4, "wreck showing mast/masts"),
    CATWRK_HULL_SHOWING(S57Attribute.CATWRK, 5, "wreck showing any portion of hull or superstructure"),

    CATOBS_SNAG_STUMP(S57Attribute.CATOBS, 1, "snag/stump"),
    CATOBS_WELLHEAD(S57Attribute.CATOBS, 2, "wellhead"),
    CATOBS_DIFFUSER(S57Attribute.CATOBS, 3, "diffuser"),
    CATOBS_CRIB(S57Attribute.CATOBS, 4, "crib"),
    CATOBS_FISH_HAVEN(S57Attribute.CATOBS, 5, "fish haven"),
    CATOBS_FOUL_AREA(S57Attribute.CATOBS, 6, "foul area"),

    CATLAM_PORT_HAND(S57Attribute.CATLAM, 1, "port-hand lateral mark"),
    CATLAM_STARBOARD_HAND(S57Attribute.CATLAM, 2, "starboard-hand lateral mark"),
    CATLAM_PREFERRED_CHANNEL_STARBOARD(S57Attribute.CATLAM, 3, "preferred channel to starboard"),
    CATLAM_PREFERRED_CHANNEL_PORT(S57Attribute.CATLAM, 4, "preferred channel to port"),

    CATLIT_DIRECTIONAL(S57Attribute.CATLIT, 1, "directional function"),
    CATLIT_REAR(S57Attribute.CATLIT, 4, "rear/upper light"),
    CATLIT_FRONT(S57Attribute.CATLIT, 5, "front/lower light"),
    CATLIT_AERO(S57Attribute.CATLIT, 9, "aero light"),
    CATLIT_FOG_DETECTOR(S57Attribute.CATLIT, 10, "fog detector light"),
    CATLIT_FLOODLIGHT(S57Attribute.CATLIT, 11, "flood light"),
    CATLIT_STRIP_LIGHT(S57Attribute.CATLIT, 12, "strip light"),
    CATLIT_SUBSIDIARY(S57Attribute.CATLIT, 13, "subsidiary light"),
    CATLIT_SPOTLIGHT(S57Attribute.CATLIT, 14, "spotlight"),
    CATLIT_FRONT_LEADING(S57Attribute.CATLIT, 15, "front leading light"),
    CATLIT_REAR_LEADING(S57Attribute.CATLIT, 16, "rear leading light"),
    CATLIT_LOWER_MOIRE(S57Attribute.CATLIT, 17, "lower moire effect light"),
    CATLIT_UPPER_MOIRE(S57Attribute.CATLIT, 18, "upper moire effect light"),
    CATLIT_EMERGENCY(S57Attribute.CATLIT, 19, "emergency light"),
    CATLIT_BEARING(S57Attribute.CATLIT, 20, "bearing light"),
    CATLIT_HORIZONTAL(S57Attribute.CATLIT, 21, "horizontally disposed light"),
    CATLIT_VERTICAL(S57Attribute.CATLIT, 22, "vertically disposed light"),

    TOPSHP_CONE_POINT_UP(S57Attribute.TOPSHP, 1, "cone point up"),
    TOPSHP_CONE_POINT_DOWN(S57Attribute.TOPSHP, 2, "cone point down"),
    TOPSHP_SPHERE(S57Attribute.TOPSHP, 3, "sphere"),
    TOPSHP_TWO_SPHERES(S57Attribute.TOPSHP, 4, "two spheres"),
    TOPSHP_CYLINDER(S57Attribute.TOPSHP, 5, "cylinder"),
    TOPSHP_X_SHAPE(S57Attribute.TOPSHP, 6, "x-shape"),
    TOPSHP_UPRIGHT_CROSS(S57Attribute.TOPSHP, 7, "upright cross"),

    CATZOC_A1(S57Attribute.CATZOC, 1, "zone of confidence A1"),
    CATZOC_A2(S57Attribute.CATZOC, 2, "zone of confidence A2"),
    CATZOC_B(S57Attribute.CATZOC, 3, "zone of confidence B"),
    CATZOC_C(S57Attribute.CATZOC, 4, "zone of confidence C"),
    CATZOC_D(S57Attribute.CATZOC, 5, "zone of confidence D"),
    CATZOC_U(S57Attribute.CATZOC, 6, "zone of confidence U");

    companion object {
        private val byAttributeAndCode: Map<Pair<S57Attribute, Int>, S57EnumeratedValue> =
            entries.associateBy { it.attribute to it.code }

        fun fromCode(attribute: S57Attribute, code: Int): S57EnumeratedValue? =
            byAttributeAndCode[attribute to code]

        fun valuesFor(attribute: S57Attribute): List<S57EnumeratedValue> =
            entries.filter { it.attribute == attribute }
    }
}
