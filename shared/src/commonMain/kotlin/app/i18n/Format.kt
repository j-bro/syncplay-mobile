package app.i18n

/**
 * Fills the placeholders in a generated string.
 *
 * The generator strips the `%1$` position markers from the XML, so what reaches here is a plain
 * run of `%s` and `%d` filled left to right. `%%` writes one literal percent, the Android
 * convention translators already know. Anything else, a lone `%` included, is copied as is.
 *
 * This lives in the generated code's own package on purpose: the generated files import nothing,
 * so they resolve `format` from here rather than from the JVM's `kotlin.text` version, which
 * would not exist on iOS at all.
 */
internal fun String.format(vararg args: Any?): String {
    val out = StringBuilder(length + args.size * 8)
    var arg = 0
    var i = 0
    while (i < length) {
        val c = this[i]
        val next = if (i + 1 < length) this[i + 1] else ' '
        when {
            c == '%' && next == '%' -> { out.append('%'); i += 2 }
            c == '%' && (next == 's' || next == 'd') && arg < args.size -> {
                out.append(args[arg++].toString())
                i += 2
            }
            else -> { out.append(c); i++ }
        }
    }
    return out.toString()
}
