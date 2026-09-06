package app.utils

/**
 * The host a URL really names.
 *
 * Read the same way `urlparse().hostname` reads it, which is the only reading that matters when
 * the string came from someone else: user-info before an `@` is not the host, an IPv6 literal
 * loses its brackets, the port goes, and the rest is lowercase. Null when there is no usable
 * host, which a caller deciding whether to trust something must treat as "do not".
 *
 * `https://klipy.com:x@evil.example/a.gif` names `evil.example`. Cutting at the first colon,
 * which is what this replaced, called it `klipy.com` and auto-loaded it.
 */
fun urlHost(url: String): String? {
    val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
    if (afterScheme.isEmpty()) return null
    val authority = afterScheme.takeWhile { it != '/' && it != '?' && it != '#' }
    val hostPort = authority.substringAfterLast('@')
    val host = if (hostPort.startsWith("[")) {
        hostPort.substringAfter('[').substringBefore(']')
    } else {
        hostPort.substringBefore(':')
    }
    return host.lowercase().takeIf { it.isNotEmpty() && it.none(Char::isWhitespace) }
}

/**
 * The path of a URL, without query or fragment. "" when there is none.
 *
 * A trusted-domain entry may carry a path prefix, and the prefix is matched against this. The
 * query is dropped because the reference client compares against `urlparse().path`, which has
 * none either.
 */
fun urlPath(url: String): String {
    val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
    val slash = afterScheme.indexOf('/')
    val stop = afterScheme.indexOfAny(charArrayOf('?', '#')).let { if (it < 0) afterScheme.length else it }
    return if (slash < 0 || slash > stop) "" else afterScheme.substring(slash, stop)
}
