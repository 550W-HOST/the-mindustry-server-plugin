package studio.papercube.mindustryplugins

object MindustryTextUtil {
    @JvmStatic
    fun removeColorMarkups(str: String):String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            if (str[i] == '[') {
                while (str[i] != ']' && !str[i].isWhitespace()) i++
            } else {
                sb.append(str[i])
            }
            i++
        }
        return sb.toString()
    }
}