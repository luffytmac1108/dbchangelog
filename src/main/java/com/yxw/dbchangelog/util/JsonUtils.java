package com.yxw.dbchangelog.util;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class JsonUtils {

    @Getter
    public final static ObjectMapper objectMapper;

    static {
        objectMapper = genObjectMapper();
    }

    public final static String objectMapperBeanName = "objectMapper";

    @SneakyThrows
    public static String toJson(Object data) {
        if (data == null) {
            return "";
        }
        if (data instanceof String) {
            return (String) data;
        }
        if (data instanceof Number) {
            return data.toString();
        }
        // 基础数据类型，直接转了
        if (data.getClass().isPrimitive()) {
            return data.toString();
        }
        return getObjectMapper().writeValueAsString(data);
    }

    /**
     * json 转对象
     *
     * @param json          json数据
     * @param typeReference 要转换出的类型
     * @return t
     */
    @SneakyThrows
    public static <T> T jsonToObject(String json, TypeReference<T> typeReference) {
        if (!jsonStrCheck(json)) {
            return null;
        }
        return getObjectMapper().readValue(json, typeReference);
    }

    /**
     * 只有单层嵌套jsonString的情况下，把数据转成对象
     * 如 {aa:"{bb:{cc:123}}"}  并不支持 {aa:"{bb:"{cc:123}"}"} 的情况
     */
    @SneakyThrows
    public static <T> T nestJsonSimpleToObject(String json, Class<T> type) {
        ObjectNode jsonNode = (ObjectNode) getObjectMapper().readTree(json);
        Field[] fields = ReflectUtil.getFields(type);
        for (Field field : fields) {
            String name = field.getName();
            JsonNode node = jsonNode.get(name);
            // 如果有 jsonString 则换成 jsonNode 对象
            JsonNode value = convertJsonType(node, field);
            if (value != null) {
                jsonNode.set(name, value);
            }
        }
        return jsonToObject(toJson(jsonNode), type);
    }

    @SneakyThrows
    private static JsonNode convertJsonType(JsonNode node, Field field) {
        if (node == null) {
            return null;
        }
        if (field.getType().isEnum()) {
            return null;
        }

        if (field.getType() != String.class && node.isTextual()) {
            return getObjectMapper().readTree(node.textValue());
        }
        return null;
    }


    @SneakyThrows
    public static Map<String, Object> objectToMap(Object data) {
        if (data == null) {
            return null;
        }
        String json = toJson(data);
        return jsonToObject(json, new TypeReference<Map<String, Object>>() {
        });
    }

    @SneakyThrows
    public static <T> T jsonToObject(String json, Class<T> type) {
        if (!jsonStrCheck(json)) {
            return null;
        }

        return getObjectMapper().readValue(json, type);
    }

    @SneakyThrows
    public static Object jsonToObject(String json, Type type) {
        if (!jsonStrCheck(json)) {
            return null;
        }
        ObjectMapper mapper = getObjectMapper();
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        return getObjectMapper().readValue(json, javaType);
    }

    @SneakyThrows
    public static <T> T jsonToObject(InputStream inputStream, Class<T> type) {
        return getObjectMapper().readValue(inputStream, type);
    }

    /**
     * 把 key->obj 转成 key->string obj到str用的是 json来转换的
     */
    public static Map<String, String> objectMapToStringMap(Map<String, Object> map) {
        if (Objects.isNull(map)) {
            return new HashMap<>();
        }
        if (map.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        map.forEach((key, valueObj) -> {
            if (valueObj instanceof String) {
                result.put(key, (String) valueObj);
                return;
            }
            result.put(key, toJson(valueObj));
        });
        return result;
    }

    private static boolean jsonStrCheck(String json) {
        if (json == null || StringUtils.isEmpty(json) || "null".equals(json)) {
            return false;
        }
        return true;
    }

    private static synchronized ObjectMapper genObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * 将数组转换为逗号分隔的字符串
     * 例如: ["1","2"] 转换为 "1,2"
     *
     * @param arrayStr JSON数组字符串
     * @return 逗号分隔的字符串
     */
    public static String convertArrayToString(List<String> arrayStr) {
        try {
            if (Objects.isNull(arrayStr)) {
                return "";
            }

            JSONArray jsonArray = JSONUtil.parseArray(arrayStr);
            return jsonArray.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

        } catch (Exception e) {
            log.error("转换数组字符串出错: {}", arrayStr, e);
            return "";
        }
    }

}