package com.csg.ecard.messagecenter.module.template.rule;

import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Map;
import static com.csg.ecard.messagecenter.module.template.rule.RuleFixtures.*;
import static org.assertj.core.api.Assertions.*;

class RuleTemplateEngineTest {
    private final RuleTemplateEngine engine = new RuleTemplateEngine(MAPPER);
    private RuleRenderResult run(ObjectNode draft, String values) throws Exception {
        Map<String, JsonNode> map = new java.util.LinkedHashMap<>();
        MAPPER.readTree(values).fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue()));
        return engine.render(draft, 1L, params(), map);
    }
    private ObjectNode first(ObjectNode d) { return (ObjectNode) d.path("versions").get(0); }

    @Test void firstMatchStopsBranchesAndIncludesDefault() throws Exception {
        ObjectNode d = draft();
        d.withArray("versions").add(branch("v2", group("all", condition("c2", "param", "1", "NUMBER", "gt", "-1")), content("second")));
        var result = run(d, "{\"p1\":0}");
        assertThat(result.content()).isEqualTo("selected");
        assertThat(result.trace()).extracting(RuleRenderResult.Trace::state).containsExactly("matched", "skipped", "skipped");
        assertThat(result.trace().get(0).reasons()).hasSize(1);
        result = run(d, "{\"p1\":-2}");
        assertThat(result.matchedId()).isEqualTo("default");
        assertThat(result.trace()).extracting(RuleRenderResult.Trace::state).containsExactly("unmatched", "unmatched", "matched");
    }
    @Test void skipFallbackAllowsEmptyContentAndHistoricalFallbackStillSends() throws Exception {
        ObjectNode d = draft();
        first(d).set("condition", group("all",
                condition("skip-condition", "param", "1", "NUMBER", "eq", "1")));
        ObjectNode fallback = (ObjectNode) d.path("fallback");
        fallback.put("action", "SKIP");
        fallback.remove("content");

        RuleRenderResult skipped = run(d, "{\"p1\":0}");
        assertThat(skipped.skipSend()).isTrue();
        assertThat(skipped.content()).isEmpty();
        assertThat(skipped.matchedId()).isEqualTo("default");
        assertThat(skipped.trace().get(skipped.trace().size() - 1).reasons())
                .containsExactly(RuleTemplateEngine.SKIP_SEND_MESSAGE);

        RuleRenderResult matched = run(d, "{\"p1\":1}");
        assertThat(matched.skipSend()).isFalse();
        assertThat(matched.content()).isEqualTo("selected");

        fallback.remove("action");
        fallback.set("content", content("historical fallback"));
        RuleRenderResult historical = run(d, "{\"p1\":0}");
        assertThat(historical.skipSend()).isFalse();
        assertThat(historical.content()).isEqualTo("historical fallback");
    }
    @Test void fallbackActionIsValidatedAndSendDefaultIsNormalized() {
        ObjectNode rule = draft();
        ObjectNode workspaceRule = rule.deepCopy();
        ((ObjectNode) workspaceRule.path("fallback")).put("action", "SEND");
        ObjectNode workspace = MAPPER.createObjectNode();
        workspace.set("ruleTemplate", workspaceRule);
        JsonNode envelope = engine.envelope(rule, workspace, "10", 1L, params());
        assertThat(envelope.path("ruleTemplate").path("fallback").path("action").asText())
                .isEqualTo("SEND");

        ((ObjectNode) rule.path("fallback")).put("action", "UNKNOWN");
        assertThatThrownBy(() -> engine.validate(rule, 1L, params()))
                .hasMessageContaining("fallback.action").hasMessageContaining("SEND或SKIP");
    }
    @Test void allAnyReferenceAndFalseAreHandled() throws Exception {
        ObjectNode d = draft();
        ObjectNode c = condition("c1", "param", "1", "NUMBER", "eq", "");
        ObjectNode right = MAPPER.createObjectNode().put("source", "reference"); right.set("reference", ref("param", "2", "NUMBER")); c.set("right", right);
        ObjectNode g = group("all", c, condition("c2", "param", "3", "BOOLEAN", "eq", "false"));
        first(d).set("condition", g);
        assertThat(run(d, "{\"p1\":0,\"p2\":0,\"p3\":false}").matchedId()).isEqualTo("v1");
        assertThat(run(d, "{\"p1\":0,\"p2\":1,\"p3\":false}").matchedId()).isEqualTo("default");
        g.put("mode", "any");
        assertThat(run(d, "{\"p1\":0,\"p2\":1,\"p3\":false}").matchedId()).isEqualTo("v1");
        assertThat(run(d, "{\"p1\":\"bad\",\"p2\":1,\"p3\":false}").errors()).singleElement().asString().contains("c1", "类型转换失败");
    }
    @Test void negateContainsAndLikeFollowBlocklySemantics() throws Exception {
        ObjectNode d = draft();
        ObjectNode rule = condition("c1", "param", "5", "STRING", "contains", "abc");
        rule.put("negate", true);
        first(d).set("condition", group("all", rule));
        assertThat(run(d, "{\"p5\":\"xabcx\"}").matchedId()).isEqualTo("default");
        assertThat(run(d, "{\"p5\":\"xyz\"}").matchedId()).isEqualTo("v1");

        rule.put("negate", false);
        rule.put("operator", "matches");
        ((ObjectNode) rule.path("right")).put("value", "A%Z");
        assertThat(run(d, "{\"p5\":\"A\\nZ\"}").matchedId()).isEqualTo("v1");
        ((ObjectNode) rule.path("right")).put("value", "A.Z");
        assertThat(run(d, "{\"p5\":\"A0Z\"}").matchedId()).isEqualTo("default");
        assertThat(run(d, "{\"p5\":\"A.Z\"}").matchedId()).isEqualTo("v1");
    }
    @Test void conditionCalculationsRunInArrayOrderAndSupportParameterOperands() throws Exception {
        ObjectNode d = draft();
        ObjectNode rule = condition("c1", "param", "1", "NUMBER", "eq", "1");
        rule.putArray("calculations")
                .add(calculation("add", "ADD", "5"))
                .add(calculation("multiply", "MULTIPLY", "2"))
                .add(calculationReference("subtract", "subtract", "2"))
                .add(calculation("divide", "DIVIDE", "4"))
                .add(calculation("modulo", "MODULO", "6"));
        first(d).set("condition", group("all", rule));

        assertThat(run(d, "{\"p1\":10,\"p2\":2}").matchedId()).isEqualTo("v1");
        assertThat(run(d, "{\"p1\":10,\"p2\":3}").matchedId()).isEqualTo("default");
    }
    @Test void bindingCalculationsRunBeforePlainAndMoneyFormatting() throws Exception {
        ObjectNode d = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "money", 2);
        amount.putArray("calculations")
                .add(calculationReference("add", "ADD", "2"))
                .add(calculation("multiply", "MULTIPLY", "3"))
                .add(calculation("divide", "DIVIDE", "4"))
                .add(calculation("modulo", "MODULO", "5"));
        first(d).set("content", content("{{amount}}", amount));
        assertThat(run(d, "{\"p1\":10,\"p2\":2}").content()).isEqualTo("4.00");

        amount.put("format", "plain");
        assertThat(run(d, "{\"p1\":10,\"p2\":2}").content()).isEqualTo("4");
    }
    @Test void currentSideControlsConditionAndBindingOperandOrder() throws Exception {
        ObjectNode d = draft();
        ObjectNode rule = condition("c1", "param", "1", "NUMBER", "ne", "4");
        rule.put("negate", true);
        rule.putArray("calculations")
                .add(calculationReference("c-minus", "MINUS", "2").put("currentSide", "right"))
                .add(calculation("c-divide", "DIVIDE", "80").put("currentSide", "right"))
                .add(calculation("c-modulo", "MODULO", "6"));
        first(d).set("condition", group("all", rule));

        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "money", 2);
        amount.putArray("calculations")
                .add(calculation("b-minus", "MINUS", "10").put("currentSide", "right"))
                .add(calculation("b-divide", "DIVIDE", "80").put("currentSide", "right"))
                .add(calculation("b-modulo", "MODULO", "23").put("currentSide", "right"));
        first(d).set("content", content("{{amount}}", amount));

        assertThat(run(d, "{\"p1\":2,\"p2\":10}").content()).isEqualTo("3.00");
        assertThat(run(d, "{\"p1\":3,\"p2\":10}").matchedId()).isEqualTo("default");
    }
    @Test void calculationErrorsAreFriendlyAndStopRendering() throws Exception {
        ObjectNode d = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "plain", 0);
        amount.putArray("calculations").add(calculationReference("divide", "DIVIDE", "2"));
        first(d).set("content", content("prefix{{amount}}", amount));
        assertThat(run(d, "{\"p1\":10,\"p2\":0}").errors())
                .containsExactly("计算“divide”的除数不能为0");
        assertThat(run(d, "{\"p1\":10}").errors())
                .containsExactly("计算“divide”的右值不能为空");

        ((ObjectNode) amount.path("calculations").get(0)).put("operator", "POWER");
        assertThat(run(d, "{\"p1\":10,\"p2\":2}").errors())
                .containsExactly("不支持的计算操作：POWER");
    }
    @Test void currentSideChecksTheActualDivisionAndModuloDivisor() throws Exception {
        ObjectNode d = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "plain", 0);
        ObjectNode calculation = calculation("divide", "DIVIDE", "10").put("currentSide", "right");
        amount.putArray("calculations").add(calculation);
        first(d).set("content", content("{{amount}}", amount));

        assertThat(run(d, "{\"p1\":0}").errors())
                .containsExactly("计算“divide”的除数不能为0");
        calculation.put("operator", "MODULO");
        assertThat(run(d, "{\"p1\":0}").errors())
                .containsExactly("计算“divide”的取余除数不能为0");

        calculation.put("operator", "DIVIDE");
        ((ObjectNode) calculation.path("right")).put("value", "0");
        assertThat(run(d, "{\"p1\":2}").content()).isEqualTo("0");
    }
    @Test void missingDoesNotMatchNeOrBecomeZeroAndEmptyPreservesZeroFalse() throws Exception {
        ObjectNode d = draft();
        first(d).set("condition", group("all", condition("c1", "param", "1", "NUMBER", "ne", "1")));
        for (String input : new String[]{"{}", "{\"p1\":null}", "{\"p1\":\"\"}"}) assertThat(run(d, input).matchedId()).isEqualTo("default");
        first(d).set("condition", group("all", condition("c1", "param", "1", "NUMBER", "notEmpty", ""), condition("c2", "param", "3", "BOOLEAN", "notEmpty", "")));
        first(d).set("content", content("{{n}}/{{b}}/{{n}}", binding("n", "param", "1", "NUMBER", "plain", 0), binding("b", "param", "3", "BOOLEAN", "plain", 0)));
        assertThat(run(d, "{\"p1\":0,\"p3\":false}").content()).isEqualTo("0/false/0");
        first(d).set("condition", group("all", condition("c1", "param", "6", "NUMBER_ARRAY", "empty", "")));
        assertThat(run(d, "{\"p6\":[]}").matchedId()).isEqualTo("v1");
    }
    @ParameterizedTest @CsvSource({"1.005,2,1.01", "2.675,2,2.68", "-1.005,2,-1.01", "2.5,0,3", "-2.5,0,-3", "-0.004,2,0.00", "0.000000005,8,0.00000001"})
    void decimalRounding(String amount, int decimals, String expected) throws Exception {
        ObjectNode d = draft(); first(d).set("condition", group("all", condition("c1", "param", "1", "NUMBER", "notEmpty", "")));
        first(d).set("content", content("{{n}}", binding("n", "param", "1", "NUMBER", "money", decimals)));
        assertThat(run(d, MAPPER.writeValueAsString(Map.of("p1", amount))).content()).isEqualTo(expected);
    }
    @ParameterizedTest @CsvSource({"2026-01-01T16:00:00Z,2026-01-02", "2026-01-01T15:59:59Z,2026-01-01", "2026-01-01T23:30:00-05:00,2026-01-02", "2026-01-01,2026-01-01", "2026-01-01 00:00:00,2026-01-01"})
    void dateZoneBoundary(String value, String expected) throws Exception {
        ObjectNode d = draft(); first(d).set("content", content("{{t}}", binding("t", "param", "4", "TIME", "date", 0)));
        assertThat(run(d, MAPPER.writeValueAsString(Map.of("p1", 0, "p4", value))).content()).isEqualTo(expected);
    }
    @Test void datePatternFormatsMillisecondsInChineseAndKeepsMilliseconds() throws Exception {
        ObjectNode d = draft();
        ObjectNode chinese = binding("就餐日期", "param", "4", "TIME", "date", 0)
                .put("datePattern", "yyyy年MM月dd日");
        first(d).set("content", content("{{就餐日期}}", chinese));
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12 11:12:13.123\"}").content())
                .isEqualTo("2026年11月12日");

        chinese.put("datePattern", "yyyy-MM-dd HH:mm:ss.SSS");
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12 11:12:13.123\"}").content())
                .isEqualTo("2026-11-12 11:12:13.123");
    }
    @Test void missingOrEmptyDatePatternUsesHistoricalDefault() throws Exception {
        ObjectNode d = draft();
        ObjectNode date = binding("就餐日期", "param", "4", "TIME", "date", 0);
        first(d).set("content", content("{{就餐日期}}", date));
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12 11:12:13.123\"}").content())
                .isEqualTo("2026-11-12");
        date.put("datePattern", "");
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12 11:12:13.123\"}").content())
                .isEqualTo("2026-11-12");
    }
    @Test void dateErrorsAreFriendlyAndDoNotExposeInternalPaths() throws Exception {
        ObjectNode d = draft();
        ObjectNode date = binding("就餐日期", "param", "4", "TIME", "date", 0)
                .put("datePattern", "yyyy-MM-dd '");
        first(d).set("content", content("{{就餐日期}}", date));
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12\"}").errors())
                .containsExactly("日期格式填写有误，请参考：yyyy-MM-dd HH:mm:ss");
        date.put("datePattern", "   ");
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12\"}").errors())
                .containsExactly("日期格式不能为空");
        date.put("datePattern", "y".repeat(51));
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12\"}").errors())
                .containsExactly("日期格式填写有误，请参考：yyyy-MM-dd HH:mm:ss");
        date.put("datePattern", "yyyy年MM月dd日");
        String error = run(d, "{\"p1\":0,\"p4\":\"不是日期\"}").errors().get(0);
        assertThat(error).isEqualTo("就餐日期的值不是有效日期，请检查后重试")
                .doesNotContain("ruleTemplate", "bindings[", "Exception", "java.");
    }
    @Test void timestampAndOffsetDateInputsUseSharedParser() throws Exception {
        ObjectNode d = draft();
        ObjectNode date = binding("t", "param", "4", "TIME", "date", 0)
                .put("datePattern", "yyyy-MM-dd HH:mm:ss");
        first(d).set("content", content("{{t}}", date));
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-11-12T03:12:13Z\"}").content())
                .isEqualTo("2026-11-12 11:12:13");
        assertThat(run(d, "{\"p1\":0,\"p4\":1794453133000}").content())
                .isEqualTo("2026-11-12 11:12:13");
        assertThat(run(d, "{\"p1\":0,\"p4\":1794453133}").content())
                .isEqualTo("2026-11-12 11:12:13");
    }
    @Test void invalidDatesAndMoneyFormatsAreLocated() throws Exception {
        ObjectNode d = draft(); first(d).set("content", content("{{t}}", binding("t", "param", "4", "TIME", "date", 0)));
        assertThat(run(d, "{\"p1\":0,\"p4\":\"2026-02-30\"}").errors())
                .containsExactly("t的值不是有效日期，请检查后重试");
        for (int decimals : new int[]{-1,9}) {
            first(d).set("content", content("{{n}}", binding("n", "param", "1", "NUMBER", "money", decimals)));
            assertThat(run(d, "{}").errors()).singleElement().asString().contains("decimals");
        }
        first(d).set("content", content("{{t}}", binding("t", "param", "4", "TIME", "money", 2)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("money");
    }
    @Test void primitiveListFiltersAndSuppressesEmptyDecorations() throws Exception {
        ObjectNode d = draft();
        d.withArray("lists").add(list("6", group("all", condition("lc", "field", "$value", "NUMBER", "gt", "1")), content("{{x}}", binding("x", "field", "$value", "NUMBER", "plain", 0))));
        first(d).set("content", content("{{l}}", binding("l", "list", "list1", "STRING", "plain", 0)));
        assertThat(run(d, "{\"p1\":0,\"p6\":[0,1,2,3]}").content()).isEqualTo("[2,3]");
        for (String input : new String[]{"{\"p1\":0}", "{\"p1\":0,\"p6\":[]}", "{\"p1\":0,\"p6\":[0,1]}"}) assertThat(run(d, input).content()).isEmpty();
        ((ObjectNode)d.path("lists").get(0)).set("content", content(""));
        assertThat(run(d, "{\"p1\":0,\"p6\":[2,3]}").content()).isEmpty();
    }
    @Test void objectPathAndParameterComparisonAndEmptyFilter() throws Exception {
        ObjectNode d = draft();
        ObjectNode rule = condition("lc", "field", "account.balance", "NUMBER", "gte", "");
        ObjectNode right = MAPPER.createObjectNode().put("source", "reference"); right.set("reference", ref("param", "2", "NUMBER")); rule.set("right", right);
        ObjectNode list = list("7", group("all", rule), content("{{x}}", binding("x", "field", "label", "STRING", "plain", 0)));
        d.withArray("lists").add(list);
        first(d).set("content", content("{{l}}/{{l}}", binding("l", "list", "list1", "STRING", "plain", 0)));
        String input = "{\"p1\":0,\"p2\":2,\"p7\":[{\"account\":{\"balance\":1},\"label\":\"A\"},{\"account\":{\"balance\":2},\"label\":\"B\"}]}";
        var result = run(d, input);
        assertThat(result.content()).isEqualTo("[B]/[B]");
        assertThat(result.trace().get(0).reasons()).hasSize(3);
        list.set("filter", group("any"));
        assertThat(run(d, input).content()).isEqualTo("[A,B]/[A,B]");
        first(d).set("content", content("unused"));
        assertThat(run(d, "{\"p1\":0,\"p7\":\"bad\"}").content()).isEqualTo("unused");
    }
    @Test void invalidReferencesTokensAndUnsafePathsFailBeforeExecution() throws Exception {
        ObjectNode d = draft();
        first(d).set("content", content("{{x}}"));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("未绑定");
        first(d).set("content", content("{{x}", binding("x", "param", "1", "NUMBER", "plain", 0)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("双花括号");
        first(d).set("content", content("{{x}}", binding("x", "param", "1", "NUMBER", "plain", 0), binding("x", "param", "1", "NUMBER", "plain", 0)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("重复token");
        first(d).set("content", content("{{x}}", binding("x", "list", "missing", "STRING", "plain", 0)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("列表引用失效");
        first(d).set("content", content("{{x}}", binding("x", "param", "999", "NUMBER", "plain", 0)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("参数失效");
        first(d).set("content", content("{{x}}", binding("x", "param", "1", "STRING", "plain", 0)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("声明类型");
        first(d).set("content", content("{{x}}", binding("x", "field", "balance", "NUMBER", "plain", 0)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("只能用于列表");
        first(d).set("content", content("text"));
        d.withArray("lists").add(list("7", group("all"), content("{{x}}", binding("x", "field", "account.__proto__", "STRING", "plain", 0))));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("不安全");
    }
    @Test void envelopesRequireIdenticalWorkspaceSnapshotAndIgnoreLegacyBlocks() throws Exception {
        ObjectNode d = draft(); ObjectNode w = MAPPER.createObjectNode(); w.set("ruleTemplate", d.deepCopy());
        w.putArray("oldNodes").add("不会执行");
        var root = engine.envelope(d, w, "10", 1L, params());
        BlocklyJsonValidator validator = new BlocklyJsonValidator(MAPPER);
        var validation = validator.validateStored(MAPPER.writeValueAsString(root), 1L, params(), BlocklyValidationMode.ENABLE);
        assertThat(validation.isValid()).isTrue();
        assertThat(validator.extractReferencedParamCounts(MAPPER.writeValueAsString(root))).containsEntry(1L, 1L);
        w.with("ruleTemplate").put("templateId", "20");
        assertThatThrownBy(() -> engine.envelope(d, w, "10", 1L, params())).isInstanceOf(BizException.class).hasMessageContaining("完全一致");
    }
    @Test void envelopeNormalizesDatePatternBeforeConsistencyComparison() {
        ObjectNode d = draft();
        first(d).set("content", content("{{t}}", binding("t", "param", "4", "TIME", "date", 0)
                .put("datePattern", "yyyy年MM月dd日")));
        ObjectNode w = MAPPER.createObjectNode();
        w.set("ruleTemplate", d.deepCopy());
        assertThatCode(() -> engine.envelope(d, w, "10", 1L, params())).doesNotThrowAnyException();

        ObjectNode empty = draft();
        first(empty).set("content", content("{{t}}", binding("t", "param", "4", "TIME", "date", 0)
                .put("datePattern", "")));
        ObjectNode missing = empty.deepCopy();
        ((ObjectNode) missing.path("versions").get(0).path("content").path("bindings").get(0))
                .remove("datePattern");
        w.set("ruleTemplate", missing);
        assertThatCode(() -> engine.envelope(empty, w, "10", 1L, params())).doesNotThrowAnyException();
    }
    @Test void envelopeTreatsMissingCalculationDefaultsAsHistoricalValues() throws Exception {
        ObjectNode rule = draft();
        ObjectNode condition = (ObjectNode) first(rule).path("condition").path("rules").get(0);
        condition.put("negate", false);
        condition.putArray("calculations");
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "plain", 0);
        amount.putArray("calculations");
        first(rule).set("content", content("{{amount}}", amount));

        ObjectNode workspaceRule = rule.deepCopy();
        ObjectNode workspaceCondition =
                (ObjectNode) first(workspaceRule).path("condition").path("rules").get(0);
        workspaceCondition.remove("negate");
        workspaceCondition.remove("calculations");
        ((ObjectNode) first(workspaceRule).path("content").path("bindings").get(0))
                .remove("calculations");
        ObjectNode workspace = MAPPER.createObjectNode();
        workspace.set("ruleTemplate", workspaceRule);

        assertThatCode(() -> engine.envelope(rule, workspace, "10", 1L, params()))
                .doesNotThrowAnyException();
        assertThat(run(workspaceRule, "{\"p1\":0}").matchedId()).isEqualTo("v1");
    }
    @Test void envelopeTreatsMissingCurrentSideAsLeft() throws Exception {
        ObjectNode rule = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "plain", 0);
        amount.putArray("calculations").add(calculation("add", "ADD", "1"));
        first(rule).set("content", content("{{amount}}", amount));

        ObjectNode workspaceRule = rule.deepCopy();
        ((ObjectNode) first(workspaceRule).path("content").path("bindings").get(0)
                .path("calculations").get(0)).put("currentSide", "left");
        ObjectNode workspace = MAPPER.createObjectNode();
        workspace.set("ruleTemplate", workspaceRule);

        assertThatCode(() -> engine.envelope(rule, workspace, "10", 1L, params()))
                .doesNotThrowAnyException();
        assertThat(run(rule, "{\"p1\":2}").content()).isEqualTo("3");
    }
    @Test void calculationValidationRejectsZeroAndNonNumberUsage() throws Exception {
        ObjectNode d = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "plain", 0);
        amount.putArray("calculations").add(calculation("divide", "DIVIDE", "0"));
        first(d).set("content", content("{{amount}}", amount));
        assertThat(run(d, "{\"p1\":10}").errors()).singleElement().asString()
                .contains("除数不能为0");

        amount = binding("text", "param", "5", "STRING", "plain", 0);
        amount.putArray("calculations").add(calculation("add", "ADD", "1"));
        first(d).set("content", content("{{text}}", amount));
        assertThat(run(d, "{\"p1\":0,\"p5\":\"a\"}").errors()).singleElement().asString()
                .contains("数值计算只能用于数值类型");

        amount = binding("amount", "param", "1", "NUMBER", "plain", 0);
        amount.putArray("calculations")
                .add(calculation("side", "ADD", "1").put("currentSide", "middle"));
        first(d).set("content", content("{{amount}}", amount));
        assertThat(run(d, "{\"p1\":1}").errors()).singleElement().asString()
                .contains("计算位置必须为left或right");
    }
    @Test void boundedInputsBranchesItemsAndOutputs() throws Exception {
        ObjectNode d = draft();
        for (int i = 1; i <= 100; i++) d.withArray("versions").add(branch("v"+(i+1), group("all", condition("c"+(i+1), "param", "1", "NUMBER", "eq", "0")), content("x")));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("100");
        d = draft(); first(d).set("content", content("a".repeat(RuleTemplateEngine.MAX_BYTES)));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("1MiB");
        d = draft(); d.withArray("lists").add(list("6", group("all"), content("x")));
        first(d).set("content", content("{{l}}", binding("l", "list", "list1", "STRING", "plain", 0)));
        var values = MAPPER.createObjectNode().put("p1", 0); var array = values.putArray("p6"); for(int i=0;i<1001;i++) array.add(i);
        assertThat(run(d, MAPPER.writeValueAsString(values)).errors()).singleElement().asString().contains("1000");
        d = draft(); first(d).set("content", content("{{x}}".repeat(20), binding("x", "param", "5", "STRING", "plain", 0)));
        var result = run(d, MAPPER.writeValueAsString(Map.of("p1",0,"p5","x".repeat(60000))));
        assertThat(result.content()).isEmpty(); assertThat(result.errors()).singleElement().asString().contains("输出");
    }
    @Test void stringsFallbackAndPrimitiveStringLists() throws Exception {
        ObjectNode d = draft();
        first(d).set("condition", group("all", condition("c1", "param", "5", "STRING", "contains", "abc")));
        first(d).set("content", content("{{n}}", binding("n", "param", "2", "NUMBER", "plain", 0).put("fallback", "missing")));
        assertThat(run(d, "{\"p5\":\"xabcx\"}").content()).isEqualTo("missing");
        assertThat(run(d, "{\"p5\":\"xabcx\",\"p2\":0}").content()).isEqualTo("0");
        d.withArray("lists").add(list("8", group("all"), content("{{x}}", binding("x", "field", "$value", "STRING", "plain", 0))));
        first(d).set("content", content("{{l}}", binding("l", "list", "list1", "STRING", "plain", 0)));
        assertThat(run(d, "{\"p5\":\"abc\",\"p8\":[\"a\",\"\",\"b\"]}").content()).isEqualTo("[a,b]");
    }
    @Test void defaultIdsNestingAndDeletedParametersAreValidated() throws Exception {
        ObjectNode d = draft(); d.remove("fallback");
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("fallback");
        d = draft(); ((ObjectNode)d.path("fallback")).put("id", "v1");
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("ID重复");
        d = draft(); ((ObjectNode)d.path("fallback")).set("condition", group("all"));
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("不能有条件");
        d = draft(); ((ObjectNode)first(d).path("condition").path("rules").get(0)).putArray("rules");
        assertThat(run(d, "{}").errors()).singleElement().asString().contains("不支持嵌套");
        var definitions = params(); definitions.get(1L).setDeleted(1);
        assertThat(engine.render(draft(),1L,definitions,Map.of()).errors()).singleElement().asString().contains("参数失效");
        definitions.get(1L).setDeleted(0); definitions.get(1L).setSceneId(2L);
        assertThat(engine.render(draft(),1L,definitions,Map.of()).errors()).singleElement().asString().contains("不属于当前场景");
    }
    @Test void skippedBranchesDoNotReadValuesAndArrayOperatorsRejectComparison() throws Exception {
        ObjectNode d = draft();
        d.withArray("versions").add(branch("v2",group("all",condition("c2","param","2","NUMBER","eq","1")),content("second")));
        var result = run(d,"{\"p1\":0,\"p2\":\"invalid-unused-value\"}");
        assertThat(result.errors()).isEmpty(); assertThat(result.matchedId()).isEqualTo("v1");
        first(d).set("condition",group("all",condition("c1","param","6","NUMBER_ARRAY","eq","[]")));
        assertThat(run(d,"{}").errors()).singleElement().asString().contains("运算符与类型");
    }
}
