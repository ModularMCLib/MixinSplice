package com.modularmc.mixinsplice.api.transformation;

import java.util.List;
import java.util.function.UnaryOperator;

/** 为注入器的注解快照提供类型化的修改操作。每次修改返回新快照。 */
public record InjectionAnnotation(AnnotationView view) {

    public InjectionAnnotation withMethods(List<String> selectors) {
        return new InjectionAnnotation(view.withValue("method", selectors));
    }

    public InjectionAnnotation withRequire(int count) {
        return new InjectionAnnotation(view.withValue("require", count));
    }

    public InjectionAnnotation withExpect(int count) {
        return new InjectionAnnotation(view.withValue("expect", count));
    }

    public InjectionAnnotation withAllow(int count) {
        return new InjectionAnnotation(view.withValue("allow", count));
    }

    /** 按声明顺序更新已有注入点，保留接收方注解的单值或数组形式。 */
    public InjectionAnnotation mapAt(UnaryOperator<Point> edit) {
        return new InjectionAnnotation(map(view, "at", annotation -> edit.apply(new Point(annotation)).view));
    }

    /** 按声明顺序更新已有选择范围。 */
    public InjectionAnnotation mapSlices(UnaryOperator<Slice> edit) {
        return new InjectionAnnotation(map(view, "slice", annotation -> edit.apply(new Slice(annotation)).view));
    }

    public record Point(AnnotationView view) {

        public String type() {
            return (String) view.values().get("value");
        }

        public int ordinal() {
            return (int) view.values().getOrDefault("ordinal", -1);
        }

        public Point withOrdinal(int ordinal) {
            return new Point(view.withValue("ordinal", ordinal));
        }

        public Point withTarget(String target) {
            return new Point(view.withValue("target", target));
        }
    }

    public record Slice(AnnotationView view) {

        public String id() {
            return (String) view.values().getOrDefault("id", "");
        }

        public Slice withFrom(Point point) {
            return new Slice(view.withValue("from", point.view));
        }

        public Slice withTo(Point point) {
            return new Slice(view.withValue("to", point.view));
        }
    }

    private static AnnotationView map(AnnotationView view, String name, UnaryOperator<AnnotationView> edit) {
        return switch (view.values().get(name)) {
            case AnnotationView annotation -> view.withValue(name, edit.apply(annotation));
            case List<?> annotations -> view.withValue(name, annotations.stream().map(AnnotationView.class::cast).map(edit).toList());
            case null -> view;
            default -> throw new IllegalArgumentException("无效的注入器注解属性 " + name);
        };
    }
}
