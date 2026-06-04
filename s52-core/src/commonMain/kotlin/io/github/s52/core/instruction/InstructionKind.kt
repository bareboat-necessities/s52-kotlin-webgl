package io.github.s52.core.instruction

enum class InstructionKind(val token: String) {
    SY("SY"),
    LS("LS"),
    LC("LC"),
    AC("AC"),
    AP("AP"),
    TX("TX"),
    TE("TE"),
    CS("CS");

    companion object {
        private val byToken = entries.associateBy { it.token }

        fun fromToken(token: String): InstructionKind? = byToken[token.uppercase()]
    }
}
