package com.csg.ecard.messagecenter.module.template.blockly;

import java.util.regex.Pattern;

/** 线性模板与条件模板共用的字符串模式匹配规则。 */
public final class TemplateStringMatcher {

    private TemplateStringMatcher() {
    }

    /** 按 Blockly string_like 语义匹配，百分号表示任意长度字符。 */
    public static boolean matchesLike(String text, String likePattern) {
        return Pattern.compile(toLikeRegex(likePattern), Pattern.DOTALL).matcher(text).matches();
    }

    private static String toLikeRegex(String likePattern) {
        StringBuilder regex = new StringBuilder("^");
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < likePattern.length(); i++) {
            char current = likePattern.charAt(i);
            if (current == '%') {
                appendLiteral(regex, literal);
                regex.append(".*");
            } else {
                literal.append(current);
            }
        }
        appendLiteral(regex, literal);
        return regex.append('$').toString();
    }

    private static void appendLiteral(StringBuilder regex, StringBuilder literal) {
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }
}
