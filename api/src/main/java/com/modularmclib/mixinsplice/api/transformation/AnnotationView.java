package com.modularmclib.mixinsplice.api.transformation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 可独立于 ASM 使用的注解快照。描述符采用 JVM 格式。 */
public record AnnotationView(String descriptor, boolean visible, Map<String, Object> values) {

    public AnnotationView {
        if (!descriptor.startsWith("L") || !descriptor.endsWith(";")) {
            throw new IllegalArgumentException("无效的注解描述符 " + descriptor);
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((name, value) -> copy.put(name, immutableValue(value)));
        values = Collections.unmodifiableMap(copy);
    }

    public AnnotationView withValue(String name, Object value) {
        Map<String, Object> updated = new LinkedHashMap<>(values);
        updated.put(name, value);
        return new AnnotationView(descriptor, visible, updated);
    }

    private static Object immutableValue(Object value) {
        return switch (value) {
            case String text -> text;
            case Boolean flag -> flag;
            case Byte number -> number;
            case Short number -> number;
            case Integer number -> number;
            case Long number -> number;
            case Float number -> number;
            case Double number -> number;
            case Character letter -> letter;
            case ClassLiteral literal -> literal;
            case EnumConstant constant -> constant;
            case AnnotationView annotation -> annotation;
            case List<?> list -> list.stream().map(AnnotationView::immutableValue).toList();
            case null, default -> throw new IllegalArgumentException("无效的注解参数 " + value);
        };
    }

    public record ClassLiteral(String descriptor) {}

    public record EnumConstant(String descriptor, String name) {}
}
