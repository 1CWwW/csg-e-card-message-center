package com.csg.ecard.messagecenter.module.template.rule;

import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** 规则测试使用的通用参数与JSON构造器。 */
public final class RuleFixtures {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private RuleFixtures() { }
    public static ObjectNode ref(String source, String key, String type) {
        return MAPPER.createObjectNode().put("source", source).put("key", key).put("type", type);
    }
    public static ObjectNode condition(String id, String source, String key, String type, String op, String value) {
        ObjectNode r = MAPPER.createObjectNode().put("id", id).put("operator", op);
        r.set("left", ref(source, key, type));
        r.set("right", MAPPER.createObjectNode().put("source", "literal").put("value", value));
        return r;
    }
    public static ObjectNode binding(String token, String source, String key, String type, String format, int decimals) {
        ObjectNode b = ref(source, key, type);
        return b.put("token", token).put("format", format).put("decimals", decimals).put("fallback", "");
    }
    public static ObjectNode calculation(String id, String operator, String value) {
        ObjectNode calculation = MAPPER.createObjectNode().put("id", id).put("operator", operator);
        calculation.set("right", MAPPER.createObjectNode().put("source", "literal").put("value", value));
        return calculation;
    }
    public static ObjectNode calculationReference(String id, String operator, String paramId) {
        ObjectNode calculation = MAPPER.createObjectNode().put("id", id).put("operator", operator);
        ObjectNode right = MAPPER.createObjectNode().put("source", "reference");
        right.set("reference", ref("param", paramId, "NUMBER"));
        calculation.set("right", right);
        return calculation;
    }
    public static ObjectNode content(String text, ObjectNode... bindings) {
        ObjectNode c = MAPPER.createObjectNode().put("text", text);
        var a = c.putArray("bindings");
        for (ObjectNode binding : bindings) a.add(binding);
        return c;
    }
    public static ObjectNode group(String mode, ObjectNode... rules) {
        ObjectNode g = MAPPER.createObjectNode().put("mode", mode);
        var a = g.putArray("rules");
        for (ObjectNode rule : rules) a.add(rule);
        return g;
    }
    public static ObjectNode branch(String id, ObjectNode group, ObjectNode content) {
        ObjectNode v = MAPPER.createObjectNode().put("id", id).put("name", id);
        v.set("condition", group); v.set("content", content); return v;
    }
    public static ObjectNode draft() {
        ObjectNode d = MAPPER.createObjectNode().put("editorType", "RULE_VERSIONS").put("schemaVersion", 1)
                .put("templateId", "10").put("sceneId", "1");
        d.putArray("versions").add(branch("v1", group("all", condition("c1", "param", "1", "NUMBER", "gte", "0")), content("selected")));
        ObjectNode f = MAPPER.createObjectNode().put("id", "default").put("name", "默认");
        f.set("content", content("default")); d.set("fallback", f); d.putArray("lists"); return d;
    }
    public static Map<Long, MsgSceneParam> params() {
        Map<Long, MsgSceneParam> p = new LinkedHashMap<>();
        String[] types = {"NUMBER", "NUMBER", "BOOLEAN", "TIME", "STRING", "NUMBER_ARRAY", "OBJECT_ARRAY", "STRING_ARRAY"};
        for (int i = 0; i < types.length; i++) {
            MsgSceneParam v = new MsgSceneParam(); v.setId((long) i + 1); v.setSceneId(1L);
            v.setParamName("p" + (i + 1)); v.setParamType(types[i]); v.setDeleted(0); v.setIsRequired(0); p.put(v.getId(), v);
        }
        return p;
    }
    public static ObjectNode list(String paramId, ObjectNode filter, ObjectNode content) {
        ObjectNode l = MAPPER.createObjectNode().put("id", "list1").put("name", "通用列表").put("paramId", paramId)
                .put("separator", ",").put("prefix", "[").put("suffix", "]");
        l.set("filter", filter); l.set("content", content); return l;
    }
}
