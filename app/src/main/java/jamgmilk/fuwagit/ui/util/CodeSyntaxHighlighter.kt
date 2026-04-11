package jamgmilk.fuwagit.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object CodeSyntaxHighlighter {

    private data class SyntaxRule(
        val regex: Regex,
        val color: Color,
        val fontWeight: FontWeight? = null,
        val fontStyle: FontStyle? = null
    )


    private val rules = listOf(
        // 注释
        SyntaxRule(Regex("(//.*$|/\\*.*?\\*/|#.*$)"), Color(0xFF6A9955)),
        // 字符串
        SyntaxRule(Regex("(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')"), Color(0xFFCE9178)),
        // 关键字
        SyntaxRule(
            Regex("\\b(fun|val|var|const|class|object|interface|enum|sealed|data|typealias|" +
                    "if|else|when|for|while|do|return|break|continue|throw|try|catch|finally|" +
                    "import|package|override|abstract|open|private|protected|internal|public|" +
                    "suspend|inline|noinline|crossinline|reified|where|by|lazy|" +
                    "void|public|private|protected|static|final|class|interface|extends|implements|" +
                    "def|self|cls|import|from|as|with|async|await|" +
                    "const|let|var|function|return|if|else|switch|case|break|default|" +
                    "try|catch|throw|new|this|super|extends|implements|" +
                    "true|false|null|nil|undefined|NaN|Infinity)\\b"),
            Color(0xFF569CD6),
            FontWeight.Bold
        ),
        // 数字
        SyntaxRule(Regex("\\b(\\d+\\.?\\d*(?:[eE][+-]?\\d+)?|0[xX][0-9a-fA-F]+|0[bB][01]+)\\b"), Color(0xFFB5CEA8)),
        // 注解/装饰器
        SyntaxRule(Regex("@\\w+"), Color(0xFFDCDCAA)),
        // 类型名（大驼峰）
        SyntaxRule(Regex("\\b([A-Z][a-zA-Z0-9]*)\\b"), Color(0xFF4EC9B0)),
    )

    /**
     * 简化的语法高亮（性能更好，适合大文件）
     *
     * @param line 代码行内容
     * @return 带高亮的 AnnotatedString
     */
    fun highlightSimple(line: String): AnnotatedString {
        return buildAnnotatedString {
            // 注释
            if (line.startsWith("//") || line.startsWith("#") || line.startsWith("*")) {
                withStyle(SpanStyle(color = Color(0xFF6A9955), fontFamily = FontFamily.Monospace)) {
                    append(line)
                }
                return@buildAnnotatedString
            }
            
            // 使用正则逐个匹配
            var remaining = line
            var position = 0
            
            while (remaining.isNotEmpty()) {
                var matched = false
                
                for (rule in rules) {
                    val match = rule.regex.find(remaining)
                    if (match != null && match.range.first == 0) {
                        // 在开头匹配到了
                        val matchedText = match.value
                        withStyle(SpanStyle(
                            color = rule.color,
                            fontWeight = rule.fontWeight,
                            fontStyle = rule.fontStyle,
                            fontFamily = FontFamily.Monospace
                        )) {
                            append(matchedText)
                        }
                        remaining = remaining.substring(matchedText.length)
                        position += matchedText.length
                        matched = true
                        break
                    }
                }
                
                if (!matched) {
                    // 没有找到匹配，查找下一个可能的匹配位置
                    var nextMatchPos = -1
                    var nextRule: SyntaxRule? = null
                    
                    for (rule in rules) {
                        val match = rule.regex.find(remaining)
                        if (match != null) {
                            if (nextMatchPos == -1 || match.range.first < nextMatchPos) {
                                nextMatchPos = match.range.first
                                nextRule = rule
                            }
                        }
                    }
                    
                    if (nextMatchPos > 0) {
                        // 在这之前没有匹配的部分
                        val before = remaining.substring(0, nextMatchPos)
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append(before)
                        }
                        remaining = remaining.substring(nextMatchPos)
                        position += nextMatchPos
                    } else if (nextMatchPos == 0 && nextRule != null) {
                        val rule = nextRule
                        val match = rule.regex.find(remaining)!!
                        val matchedText = match.value
                        withStyle(SpanStyle(
                            color = rule.color,
                            fontWeight = rule.fontWeight,
                            fontStyle = rule.fontStyle,
                            fontFamily = FontFamily.Monospace
                        )) {
                            append(matchedText)
                        }
                        remaining = remaining.substring(matchedText.length)
                        position += matchedText.length
                    } else {
                        // 剩余部分没有匹配
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append(remaining)
                        }
                        remaining = ""
                    }
                }
            }
        }
    }
}
