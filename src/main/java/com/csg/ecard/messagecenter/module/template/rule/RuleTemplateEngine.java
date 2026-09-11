package com.csg.ecard.messagecenter.module.template.rule;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyMathRules;
import com.csg.ecard.messagecenter.module.template.blockly.TemplateStringMatcher;
import com.csg.ecard.messagecenter.module.template.blockly.TemplateTimeFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/** 平级条件模板的统一校验与渲染引擎，不解释或执行任何表达式代码。 */
public final class RuleTemplateEngine {
    public static final String EDITOR_TYPE = "RULE_VERSIONS";
    public static final int MAX_BYTES = 1024 * 1024;
    public static final int MAX_BRANCHES = 100;
    public static final int MAX_LISTS = 100;
    public static final int MAX_ITEMS = 1000;
    public static final int MAX_CALCULATIONS = 100;
    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([^{}\\s]+)}}");
    private static final Set<String> FORBIDDEN = Set.of("__proto__", "prototype", "constructor", "class");
    private final ObjectMapper mapper;

    public RuleTemplateEngine(ObjectMapper mapper) { this.mapper = mapper; }

    /** 判断带模式标识的执行文档，历史无标识内容仍走 Blockly。 */
    public static boolean isRule(JsonNode root) {
        return root != null && EDITOR_TYPE.equals(root.path("editorType").asText());
    }

    /** 保存时要求画布携带同一规则快照，后端只执行 ruleTemplate。 */
    public JsonNode envelope(JsonNode rule, JsonNode workspace, String templateId,
                             Long sceneId, Map<Long, MsgSceneParam> params) {
        JsonNode normalizedRule = normalizeRule(rule);
        validateNormalized(normalizedRule, sceneId, params);
        check(templateId.equals(normalizedRule.path("templateId").asText()), "ruleTemplate.templateId", "与当前模板不一致");
        check(workspace != null && workspace.isObject(), "workspace", "必须为完整画布对象");
        limit(workspace);
        JsonNode normalizedWorkspace = workspace.deepCopy();
        JsonNode workspaceRule = normalizeRule(normalizedWorkspace.get("ruleTemplate"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) normalizedWorkspace)
                .set("ruleTemplate", workspaceRule);
        check(normalizedRule.equals(workspaceRule), "workspace.ruleTemplate", "必须与执行规则文档完全一致");
        var root = mapper.createObjectNode();
        root.put("editorType", EDITOR_TYPE);
        root.put("schemaVersion", 1);
        root.set("ruleTemplate", normalizedRule);
        root.set("workspace", normalizedWorkspace);
        limit(root);
        return root;
    }

    /** 校验规则文档、场景归属、引用、格式和唯一标识。 */
    public void validate(JsonNode rule, Long sceneId, Map<Long, MsgSceneParam> params) {
        validateNormalized(normalizeRule(rule), sceneId, params);
    }

    private void validateNormalized(JsonNode rule, Long sceneId, Map<Long, MsgSceneParam> params) {
        limit(rule);
        object(rule, "ruleTemplate");
        check(isRule(rule), "editorType", "仅支持 RULE_VERSIONS");
        check(rule.path("schemaVersion").isIntegralNumber() && rule.path("schemaVersion").intValue() == 1,
                "schemaVersion", "仅支持整数1");
        text(rule, "templateId", "ruleTemplate", false);
        check(String.valueOf(sceneId).equals(text(rule, "sceneId", "ruleTemplate", false)), "sceneId", "与当前场景不一致");
        JsonNode versions = array(rule, "versions", "ruleTemplate", MAX_BRANCHES);
        check(!versions.isEmpty(), "versions", "至少需要一个普通分支");
        JsonNode lists = array(rule, "lists", "ruleTemplate", MAX_LISTS);
        Set<String> ids = new HashSet<>();
        Map<String, JsonNode> listMap = new LinkedHashMap<>();
        for (JsonNode list : lists) {
            String p = "lists[" + text(list, "id", "lists", false) + "]";
            unique(list, p, ids);
            text(list, "name", p, false);
            listMap.put(list.path("id").asText(), list);
            MsgSceneParam param = param(text(list, "paramId", p, false), sceneId, params, p);
            check(param.getParamType().endsWith("_ARRAY"), p + ".paramId", "必须引用数组参数");
            group(list.path("filter"), p + ".filter", param.getParamType(), sceneId, params, ids, true);
            content(list.path("content"), p + ".content", param.getParamType(), sceneId, params, Map.of(), ids);
            for (String field : List.of("separator", "prefix", "suffix")) text(list, field, p, true);
        }
        for (JsonNode version : versions) {
            String p = "versions[" + text(version, "id", "versions", false) + "]";
            unique(version, p, ids);
            text(version, "name", p, false);
            group(version.path("condition"), p + ".condition", null, sceneId, params, ids, false);
            content(version.path("content"), p + ".content", null, sceneId, params, listMap, ids);
        }
        JsonNode fallback = rule.path("fallback");
        unique(fallback, "fallback", ids);
        text(fallback, "name", "fallback", false);
        check(!fallback.has("condition"), "fallback.condition", "默认分支不能有条件");
        content(fallback.path("content"), "fallback.content", null, sceneId, params, listMap, ids);
    }

    private void group(JsonNode group, String p, String itemType, Long sceneId,
                       Map<Long, MsgSceneParam> params, Set<String> ids, boolean allowEmpty) {
        object(group, p);
        check(Set.of("all", "any").contains(text(group, "mode", p, false)), p, "mode必须为all或any");
        JsonNode rules = array(group, "rules", p, 100);
        check(allowEmpty || !rules.isEmpty(), p, "普通分支至少包含一条条件");
        for (JsonNode r : rules) {
            String rp = p + ".rules[" + text(r, "id", p, false) + "]";
            unique(r, rp, ids);
            check(!r.has("rules") && !r.has("mode"), rp, "不支持嵌套条件组");
            JsonNode negate = r.get("negate");
            check(negate == null || negate.isBoolean(), rp, "取反配置必须为布尔值");
            String type = reference(r.path("left"), rp + ".left", itemType, sceneId, params);
            calculations(r, type, sceneId, params, ids, rp);
            String op = text(r, "operator", rp, false);
            Set<String> allowed = switch (type) {
                case "STRING" -> Set.of("eq", "ne", "contains", "like", "matches", "empty", "notEmpty");
                case "NUMBER", "TIME" -> Set.of("eq", "ne", "gt", "gte", "lt", "lte", "empty", "notEmpty");
                case "BOOLEAN" -> Set.of("eq", "ne", "empty", "notEmpty");
                default -> Set.of("empty", "notEmpty");
            };
            check(allowed.contains(op), rp, "运算符与类型不匹配");
            if (Set.of("empty", "notEmpty").contains(op)) continue;
            JsonNode right = r.path("right");
            String source = text(right, "source", rp + ".right", false);
            if ("reference".equals(source)) {
                check(type.equals(reference(right.path("reference"), rp + ".right.reference", itemType, sceneId, params)),
                        rp, "左右引用类型必须相同");
            } else {
                check("literal".equals(source), rp + ".right.source", "必须为literal或reference");
                text(right, "value", rp + ".right", true);
                convert(right.get("value"), type, rp + ".right.value");
            }
        }
    }

    private void calculations(JsonNode owner, String type, Long sceneId,
                              Map<Long, MsgSceneParam> params, Set<String> ids, String p) {
        JsonNode calculations = owner.get("calculations");
        if (calculations == null) {
            return;
        }
        check("NUMBER".equals(type), p, "数值计算只能用于数值类型");
        check(calculations.isArray() && calculations.size() <= MAX_CALCULATIONS,
                p, "计算配置必须为数组且不超过100项");
        int index = 0;
        for (JsonNode calculation : calculations) {
            String cp = p + ".calculations[" + index++ + "]";
            object(calculation, cp);
            JsonNode idNode = calculation.get("id");
            if (idNode != null) {
                check(idNode.isTextual() && !idNode.textValue().isBlank()
                                && idNode.textValue().length() <= 128 && ids.add(idNode.textValue()),
                        cp, "计算ID重复、为空或超过128字符");
                cp = p + ".calculations[" + idNode.textValue() + "]";
            }
            String operator = calculationOperator(text(calculation, "operator", cp, false));
            String currentSide = currentSide(calculation, cp);
            JsonNode right = calculation.path("right");
            object(right, cp + ".right");
            String source = text(right, "source", cp + ".right", false);
            if ("reference".equals(source)) {
                JsonNode reference = right.path("reference");
                check("param".equals(reference.path("source").asText()), cp,
                        "计算右值引用只能使用场景参数");
                check("NUMBER".equals(reference(reference, cp + ".right.reference",
                        null, sceneId, params)), cp, "计算右值参数必须为数值类型");
            } else {
                check("literal".equals(source), cp, "计算右值来源必须为固定值或参数引用");
                JsonNode value = right.get("value");
                check(value != null && (value.isTextual() || value.isNumber()), cp,
                        "计算固定值必须为数字");
                BigDecimal number = number(value, cp);
                if ("left".equals(currentSide) && Set.of("DIVIDE", "MODULO").contains(operator)) {
                    check(number.compareTo(BigDecimal.ZERO) != 0, cp,
                            "DIVIDE".equals(operator) ? "除数不能为0" : "取余除数不能为0");
                }
            }
        }
    }

    private String calculationOperator(String operator) {
        return switch (operator.toUpperCase(Locale.ROOT)) {
            case "ADD" -> "ADD";
            case "MINUS", "SUBTRACT" -> "MINUS";
            case "MULTIPLY" -> "MULTIPLY";
            case "DIVIDE" -> "DIVIDE";
            case "MOD", "MODULO", "REMAINDER" -> "MODULO";
            default -> throw new BizException(ErrorCode.PARAM_ERROR,
                    "不支持的计算操作：" + operator);
        };
    }

    private String currentSide(JsonNode calculation, String p) {
        JsonNode value = calculation.get("currentSide");
        if (value == null || value.isNull()) {
            return "left";
        }
        check(value.isTextual() && Set.of("left", "right").contains(value.textValue()),
                p, "计算位置必须为left或right");
        return value.textValue();
    }

    private BigDecimal number(JsonNode value, String p) {
        try {
            BigDecimal number = new BigDecimal(value.asText());
            check(number.precision() <= 100 && Math.abs((long) number.scale()) <= 100,
                    p, "计算数值精度或指数超限");
            return number;
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "计算值不是有效数字，请检查后重试");
        }
    }

    private String reference(JsonNode ref, String p, String itemType, Long sceneId, Map<Long, MsgSceneParam> params) {
        String source = text(ref, "source", p, false);
        String key = text(ref, "key", p, false);
        String type = text(ref, "type", p, false);
        try { ParamType.fromCode(type); } catch (IllegalArgumentException ex) { throw error(p, "未知类型"); }
        if ("param".equals(source)) {
            check(type.equals(param(key, sceneId, params, p).getParamType()), p, "声明类型与场景参数定义不一致");
        } else {
            check("field".equals(source) && itemType != null, p, "当前项字段只能用于列表筛选或列表项正文");
            if ("OBJECT_ARRAY".equals(itemType)) {
                check(PATH.matcher(key).matches(), p, "字段路径不合法");
                for (String part : key.split("\\.")) check(!FORBIDDEN.contains(part), p, "字段路径不安全");
            } else {
                check("$value".equals(key), p, "基础类型列表只允许$value");
                check(type.equals(itemType.replace("_ARRAY", "")), p, "$value类型与列表元素不一致");
            }
        }
        return type;
    }

    private void content(JsonNode content, String p, String itemType, Long sceneId,
                         Map<Long, MsgSceneParam> params, Map<String, JsonNode> lists, Set<String> ids) {
        String body = text(content, "text", p, true);
        Map<String, JsonNode> bindings = new HashMap<>();
        for (JsonNode b : array(content, "bindings", p, 500)) {
            String token = text(b, "token", p, false);
            String bp = p + ".bindings[" + token + "]";
            check(TOKEN.matcher("{{" + token + "}}").matches(), bp, "token不能包含空白或花括号");
            check(bindings.putIfAbsent(token, b) == null, bp, "重复token");
            String type;
            if ("list".equals(b.path("source").asText())) {
                check(itemType == null, bp, "不支持列表嵌套");
                check(lists.containsKey(text(b, "key", bp, false)), bp, "列表引用失效");
                type = text(b, "type", bp, false);
                check("STRING".equals(type), bp, "列表渲染结果类型必须为STRING");
            } else type = reference(b, bp, itemType, sceneId, params);
            calculations(b, type, sceneId, params, ids, bp);
            String format = text(b, "format", bp, false);
            check(Set.of("plain", "money", "date").contains(format), bp, "未知格式");
            check(!"money".equals(format) || "NUMBER".equals(type), bp, "money仅用于NUMBER");
            check(!"date".equals(format) || "TIME".equals(type), bp, "date仅用于TIME");
            if ("money".equals(format)) check(b.path("decimals").isIntegralNumber()
                    && b.path("decimals").intValue() >= 0 && b.path("decimals").intValue() <= 8, bp, "decimals必须为0—8的整数");
            if ("TIME".equals(type) && "date".equals(format)) {
                validateDatePattern(readBinding(b));
            }
            text(b, "fallback", bp, true);
        }
        var matcher = TOKEN.matcher(body);
        while (matcher.find()) check(bindings.containsKey(matcher.group(1)), p + ".{{" + matcher.group(1) + "}}", "占位符未绑定");
        String rest = TOKEN.matcher(body).replaceAll("");
        check(!rest.contains("{{") && !rest.contains("}}"), p, "双花括号不完整");
    }

    /** 预览和发送共用此入口；失败清空正文并返回定位信息。 */
    public RuleRenderResult render(JsonNode rule, Long sceneId, Map<Long, MsgSceneParam> params, Map<String, JsonNode> values) {
        List<RuleRenderResult.Trace> trace = new ArrayList<>();
        try {
            rule = normalizeRule(rule);
            validateNormalized(rule, sceneId, params);
            Map<String, JsonNode> safeValues = values == null ? Map.of() : values;
            limit(mapper.valueToTree(safeValues));
            Context context = new Context(sceneId, params, safeValues, rule);
            JsonNode selected = null;
            for (JsonNode version : rule.path("versions")) {
                List<String> reasons = new ArrayList<>();
                String state = "skipped";
                if (selected == null) {
                    boolean matched = matches(version.path("condition"), null, context,
                            "versions[" + version.path("id").asText() + "]", reasons);
                    state = matched ? "matched" : "unmatched";
                    if (matched) selected = version;
                } else reasons.add("前序分支已命中，未执行");
                trace.add(trace(version, state, reasons));
            }
            JsonNode fallback = rule.path("fallback");
            trace.add(trace(fallback, selected == null ? "matched" : "skipped",
                    List.of(selected == null ? "所有普通分支未命中" : "前序分支已命中，未执行")));
            if (selected == null) selected = fallback;
            String output = renderContent(selected.path("content"), null, context, "分支[" + selected.path("id").asText() + "]");
            if (!context.listReasons.isEmpty()) {
                for (int i = 0; i < trace.size(); i++) {
                    RuleRenderResult.Trace entry = trace.get(i);
                    if ("matched".equals(entry.state())) {
                        List<String> reasons = new ArrayList<>(entry.reasons());
                        reasons.addAll(context.listReasons);
                        trace.set(i, new RuleRenderResult.Trace(entry.id(), entry.name(), entry.state(), List.copyOf(reasons)));
                    }
                }
            }
            return new RuleRenderResult(selected.path("id").asText(), selected.path("name").asText(), output, trace, List.of());
        } catch (BizException ex) {
            return new RuleRenderResult(null, null, "", trace, List.of(ex.getMessage()));
        }
    }

    private RuleRenderResult.Trace trace(JsonNode v, String state, List<String> reasons) {
        return new RuleRenderResult.Trace(v.path("id").asText(), v.path("name").asText(), state, List.copyOf(reasons));
    }

    private boolean matches(JsonNode group, JsonNode item, Context c, String p, List<String> reasons) {
        boolean all = "all".equals(group.path("mode").asText());
        boolean result = all || group.path("rules").isEmpty();
        // 每条条件都判定以返回完整原因，并确保类型错误不会被短路隐藏。
        for (JsonNode rule : group.path("rules")) {
            check(++c.steps <= 10000, p, "单次执行条件数超过10000");
            String rp = p + ".rules[" + rule.path("id").asText() + "]";
            String type = rule.path("left").path("type").asText();
            JsonNode left = resolve(rule.path("left"), item, c, rp);
            JsonNode calculatedLeft = applyCalculations(left, rule, item, c);
            String op = rule.path("operator").asText();
            boolean hit;
            if ("empty".equals(op) || "notEmpty".equals(op)) {
                if (!empty(calculatedLeft)) convert(calculatedLeft, type, rp + ".left");
                hit = "empty".equals(op) == empty(calculatedLeft);
            } else {
                JsonNode rightSpec = rule.path("right");
                JsonNode right = "reference".equals(rightSpec.path("source").asText())
                        ? resolve(rightSpec.path("reference"), item, c, rp + ".right") : rightSpec.get("value");
                JsonNode a = convert(calculatedLeft, type, rp + ".left");
                JsonNode b = convert(right, type, rp + ".right");
                hit = !empty(calculatedLeft) && !empty(right) && compare(a, b, type, op);
            }
            boolean negated = rule.path("negate").asBoolean(false);
            if (negated) {
                hit = !hit;
            }
            reasons.add(rp + ": " + (hit ? "满足" : "不满足")
                    + (negated ? "（已取反）" : ""));
            result = all ? result && hit : result || hit;
        }
        return result;
    }

    private boolean compare(JsonNode a, JsonNode b, String type, String op) {
        if ("contains".equals(op)) {
            return a.asText().contains(b.asText());
        }
        if ("like".equals(op) || "matches".equals(op)) {
            return TemplateStringMatcher.matchesLike(a.asText(), b.asText());
        }
        int cmp = switch (type) {
            case "NUMBER" -> new BigDecimal(a.asText()).compareTo(new BigDecimal(b.asText()));
            case "TIME" -> Instant.parse(a.asText()).compareTo(Instant.parse(b.asText()));
            default -> a.asText().compareTo(b.asText());
        };
        return switch (op) {
            case "eq" -> cmp == 0;
            case "ne" -> cmp != 0;
            case "gt" -> cmp > 0;
            case "gte" -> cmp >= 0;
            case "lt" -> cmp < 0;
            case "lte" -> cmp <= 0;
            default -> false;
        };
    }

    private JsonNode applyCalculations(JsonNode value, JsonNode owner, JsonNode item, Context context) {
        JsonNode calculations = owner.get("calculations");
        if (empty(value) || calculations == null || calculations.isEmpty()) {
            return value;
        }
        BigDecimal result = number(value, "计算");
        int index = 0;
        for (JsonNode calculation : calculations) {
            String name = calculation.path("id").asText();
            if (name.isBlank()) {
                name = String.valueOf(++index);
            } else {
                index++;
            }
            JsonNode rightSpec = calculation.path("right");
            JsonNode configuredValue = "reference".equals(rightSpec.path("source").asText())
                    ? resolve(rightSpec.path("reference"), item, context, "计算“" + name + "”")
                    : rightSpec.get("value");
            if (empty(configuredValue)) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "计算“" + name + "”的右值不能为空");
            }
            BigDecimal operand = number(configuredValue, "计算“" + name + "”");
            String operator = calculationOperator(calculation.path("operator").asText());
            boolean resultOnLeft = "left".equals(currentSide(calculation, "计算“" + name + "”"));
            BigDecimal left = resultOnLeft ? result : operand;
            BigDecimal right = resultOnLeft ? operand : result;
            result = switch (operator) {
                case "ADD" -> left.add(right);
                case "MINUS" -> left.subtract(right);
                case "MULTIPLY" -> left.multiply(right);
                case "DIVIDE" -> {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        throw new BizException(ErrorCode.PARAM_ERROR,
                                "计算“" + name + "”的除数不能为0");
                    }
                    yield BlocklyMathRules.divide(left, right);
                }
                case "MODULO" -> {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        throw new BizException(ErrorCode.PARAM_ERROR,
                                "计算“" + name + "”的取余除数不能为0");
                    }
                    yield left.remainder(right);
                }
                default -> throw new BizException(ErrorCode.PARAM_ERROR, "不支持的计算操作");
            };
        }
        return TextNode.valueOf(result.toPlainString());
    }

    private String renderContent(JsonNode content, JsonNode item, Context c, String p) {
        Map<String, JsonNode> bindings = new HashMap<>();
        content.path("bindings").forEach(b -> bindings.put(b.path("token").asText(), b));
        String body = content.path("text").asText();
        var matcher = TOKEN.matcher(body);
        StringBuilder out = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            append(out, body.substring(end, matcher.start()), p);
            JsonNode b = bindings.get(matcher.group(1));
            String bp = p + ".{{" + matcher.group(1) + "}}";
            String value;
            if ("list".equals(b.path("source").asText())) {
                value = renderList(b.path("key").asText(), c);
            } else {
                JsonNode raw = resolve(b, item, c, bp);
                if (empty(raw)) value = b.path("fallback").asText();
                else if ("TIME".equals(b.path("type").asText()) && "date".equals(b.path("format").asText())) {
                    value = formatDate(raw, readBinding(b));
                } else {
                    JsonNode converted = convert(raw, b.path("type").asText(), bp);
                    boolean calculated = b.has("calculations");
                    if (calculated) {
                        converted = applyCalculations(converted, b, item, c);
                    }
                    value = switch (b.path("format").asText()) {
                        case "money" -> new BigDecimal(converted.asText()).setScale(b.path("decimals").intValue(), RoundingMode.HALF_UP).toPlainString();
                        default -> calculated ? converted.asText()
                                : raw.isContainerNode() ? serialize(raw, bp) : raw.asText();
                    };
                }
            }
            append(out, value, bp);
            end = matcher.end();
        }
        append(out, body.substring(end), p);
        return out.toString();
    }

    private String renderList(String id, Context c) {
        if (c.cache.containsKey(id)) return c.cache.get(id);
        JsonNode list = c.lists.get(id);
        String p = "lists[" + id + "]";
        MsgSceneParam param = param(list.path("paramId").asText(), c.sceneId, c.params, p);
        JsonNode items = c.values.get(param.getParamName());
        convert(items, param.getParamType(), p);
        StringBuilder out = new StringBuilder();
        int index = 0;
        if (!empty(items)) for (JsonNode item : items) {
            List<String> reasons = new ArrayList<>();
            String ip = p + "[" + index++ + "]";
            boolean kept = matches(list.path("filter"), item, c, ip, reasons);
            c.listReasons.addAll(reasons);
            if (!kept) continue;
            String rendered = renderContent(list.path("content"), item, c, ip + ".content");
            if (rendered.isEmpty()) continue;
            if (!out.isEmpty()) append(out, list.path("separator").asText(), p);
            append(out, rendered, p);
        }
        String result = "";
        if (!out.isEmpty()) {
            StringBuilder wrapped = new StringBuilder();
            append(wrapped, list.path("prefix").asText(), p);
            append(wrapped, out.toString(), p);
            append(wrapped, list.path("suffix").asText(), p);
            result = wrapped.toString();
        }
        c.cache.put(id, result);
        return result;
    }

    private JsonNode resolve(JsonNode ref, JsonNode item, Context c, String p) {
        String key = ref.path("key").asText();
        if ("param".equals(ref.path("source").asText())) {
            MsgSceneParam param = param(key, c.sceneId, c.params, p);
            return c.values.get(param.getParamName());
        }
        if ("$value".equals(key)) return item;
        JsonNode result = item;
        for (String part : key.split("\\.")) {
            if (result == null || result.isNull() || result.isMissingNode()) return MissingNode.getInstance();
            check(result.isObject(), p + "." + key, "字段路径经过非对象值");
            result = result.path(part);
        }
        return result;
    }

    private JsonNode convert(JsonNode value, String type, String p) {
        if (empty(value)) return MissingNode.getInstance();
        try {
            switch (type) {
                case "STRING" -> check(value.isTextual(), p, "必须为字符串");
                case "NUMBER" -> {
                    check(value.isNumber() || value.isTextual(), p, "必须为数值或十进制字符串");
                    BigDecimal n = new BigDecimal(value.asText());
                    check(n.precision() <= 100 && Math.abs((long) n.scale()) <= 100, p, "数值精度或指数超限");
                    return TextNode.valueOf(n.toPlainString());
                }
                case "BOOLEAN" -> {
                    check(value.isBoolean() || (value.isTextual() && Set.of("true", "false").contains(value.asText())), p, "必须为true或false");
                }
                case "TIME" -> {
                    check(value.isTextual() || value.isIntegralNumber(), p, "时间必须为日期时间或时间戳");
                    return TextNode.valueOf(TemplateTimeFormatter.parseInstant(value.asText()).toString());
                }
                default -> {
                    check(value.isArray() && value.size() <= MAX_ITEMS, p, "必须为数组且不超过1000项");
                    int i = 0;
                    for (JsonNode element : value) {
                        String ep = p + "[" + i++ + "]";
                        if ("OBJECT_ARRAY".equals(type)) check(element.isObject(), ep, "必须为对象");
                        else convert(element, type.replace("_ARRAY", ""), ep);
                    }
                }
            }
            return value;
        } catch (NumberFormatException | DateTimeException ex) { throw error(p, "类型转换失败（" + type + "）"); }
    }

    private String formatDate(JsonNode value, RuleBinding binding) {
        try {
            return TemplateTimeFormatter.format(value.asText(), datePattern(binding));
        } catch (RuntimeException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    binding.getToken() + "的值不是有效日期，请检查后重试");
        }
    }

    private void validateDatePattern(RuleBinding binding) {
        String pattern = binding.getDatePattern();
        if (pattern == null) {
            return;
        }
        if (pattern.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "日期格式不能为空");
        }
        if (pattern.length() > TemplateTimeFormatter.MAX_PATTERN_LENGTH) {
            throw invalidDatePattern();
        }
        try {
            TemplateTimeFormatter.outputFormatter(pattern);
        } catch (IllegalArgumentException ex) {
            throw invalidDatePattern();
        }
    }

    private String datePattern(RuleBinding binding) {
        return binding.getDatePattern() == null
                ? TemplateTimeFormatter.RULE_DEFAULT_PATTERN
                : binding.getDatePattern();
    }

    private BizException invalidDatePattern() {
        return new BizException(ErrorCode.PARAM_ERROR,
                "日期格式填写有误，请参考：yyyy-MM-dd HH:mm:ss");
    }

    private RuleBinding readBinding(JsonNode node) {
        return mapper.convertValue(node, RuleBinding.class);
    }

    private JsonNode normalizeRule(JsonNode rule) {
        check(rule != null && rule.isObject(), "ruleTemplate", "必须为对象");
        JsonNode normalized = rule.deepCopy();
        normalizeContent(normalized.path("fallback").path("content"));
        normalized.path("versions").forEach(version -> {
            normalizeGroup(version.path("condition"));
            normalizeContent(version.path("content"));
        });
        normalized.path("lists").forEach(list -> {
            normalizeGroup(list.path("filter"));
            normalizeContent(list.path("content"));
        });
        return normalized;
    }

    private void normalizeGroup(JsonNode group) {
        if (!group.isObject() || !group.path("rules").isArray()) {
            return;
        }
        group.path("rules").forEach(rule -> {
            if (!rule.isObject()) {
                return;
            }
            JsonNode negate = rule.get("negate");
            if (negate == null || negate.isNull()
                    || (negate.isBoolean() && !negate.booleanValue())) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) rule).remove("negate");
            }
            normalizeCalculations(rule);
        });
    }

    private void normalizeContent(JsonNode content) {
        if (!content.isObject() || !content.path("bindings").isArray()) {
            return;
        }
        content.path("bindings").forEach(binding -> {
            normalizeCalculations(binding);
            if (!"TIME".equals(binding.path("type").asText())
                    || !"date".equals(binding.path("format").asText())) {
                return;
            }
            JsonNode pattern = binding.get("datePattern");
            if (pattern == null || pattern.isNull()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) binding).remove("datePattern");
                return;
            }
            if (!pattern.isTextual()) {
                throw invalidDatePattern();
            }
            if (pattern.textValue().isEmpty()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) binding).remove("datePattern");
            } else if (pattern.textValue().isBlank()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "日期格式不能为空");
            }
        });
    }

    private void normalizeCalculations(JsonNode owner) {
        if (!owner.isObject()) {
            return;
        }
        JsonNode calculations = owner.get("calculations");
        if (calculations == null || calculations.isNull()
                || (calculations.isArray() && calculations.isEmpty())) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) owner).remove("calculations");
            return;
        }
        if (calculations.isArray()) {
            calculations.forEach(calculation -> {
                if (!calculation.isObject()) {
                    return;
                }
                JsonNode currentSide = calculation.get("currentSide");
                if (currentSide == null || currentSide.isNull()
                        || (currentSide.isTextual() && "left".equals(currentSide.textValue()))) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) calculation).remove("currentSide");
                }
            });
        }
    }

    private MsgSceneParam param(String key, Long sceneId, Map<Long, MsgSceneParam> params, String p) {
        // 字符串ID做精确匹配，不把外部标识转换为数字或运行时取值键。
        MsgSceneParam result = params.values().stream().filter(v -> String.valueOf(v.getId()).equals(key)).findFirst().orElse(null);
        check(result != null && Objects.equals(sceneId, result.getSceneId())
                && (result.getDeleted() == null || result.getDeleted() == 0), p + ".param[" + key + "]", "参数失效或不属于当前场景");
        return result;
    }

    private static boolean empty(JsonNode v) { return v == null || v.isNull() || v.isMissingNode() || (v.isTextual() && v.textValue().isEmpty()) || (v.isArray() && v.isEmpty()); }
    private void append(StringBuilder out, String value, String p) {
        check((long) out.length() + value.length() <= MAX_BYTES, p, "输出长度超限");
        out.append(value);
        check(out.toString().getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES, p, "输出超过1MiB");
    }
    private String serialize(JsonNode node, String p) {
        try { return mapper.writeValueAsString(node); } catch (JsonProcessingException ex) { throw error(p, "JSON序列化失败"); }
    }
    private void limit(JsonNode node) {
        check(node != null, "输入", "不能为空");
        boundedTree(node, 0, new int[]{0});
        check(serialize(node, "输入").getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES, "输入", "超过1MiB");
    }
    private void boundedTree(JsonNode node, int depth, int[] count) {
        check(depth <= 100 && ++count[0] <= 50000, "输入", "深度超过100或节点数超过50000");
        if (node.isTextual()) check(node.textValue().length() <= MAX_BYTES, "输入", "字符串过长");
        node.elements().forEachRemaining(child -> boundedTree(child, depth + 1, count));
    }
    private static String text(JsonNode node, String field, String p, boolean allowEmpty) {
        JsonNode value = node == null ? null : node.get(field);
        check(value != null && value.isTextual() && (allowEmpty || !value.textValue().isBlank()), p + "." + field, "必须为" + (allowEmpty ? "字符串" : "非空字符串"));
        return value.textValue();
    }
    private static JsonNode array(JsonNode node, String field, String p, int max) {
        JsonNode value = node.path(field);
        check(value.isArray() && value.size() <= max, p + "." + field, "必须为数组且不超过" + max + "项");
        return value;
    }
    private static void object(JsonNode node, String p) { check(node != null && node.isObject(), p, "必须为对象"); }
    private static void unique(JsonNode node, String p, Set<String> ids) {
        String id = text(node, "id", p, false);
        check(id.length() <= 128 && ids.add(id), p + ".id", "ID重复或超过128字符");
    }
    private static void check(boolean ok, String p, String message) { if (!ok) throw error(p, message); }
    private static BizException error(String p, String message) { return new BizException(ErrorCode.PARAM_ERROR, p + ": " + message); }

    private static final class Context {
        final Long sceneId;
        final Map<Long, MsgSceneParam> params;
        final Map<String, JsonNode> values;
        final Map<String, JsonNode> lists = new HashMap<>();
        final Map<String, String> cache = new HashMap<>();
        final List<String> listReasons = new ArrayList<>();
        int steps;
        Context(Long sceneId, Map<Long, MsgSceneParam> params, Map<String, JsonNode> values, JsonNode rule) {
            this.sceneId = sceneId; this.params = params; this.values = values;
            rule.path("lists").forEach(l -> lists.put(l.path("id").asText(), l));
        }
    }
}
