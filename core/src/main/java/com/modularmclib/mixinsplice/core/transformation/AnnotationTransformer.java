package com.modularmclib.mixinsplice.core.transformation;

import com.modularmclib.mixinsplice.api.mixin.MixinSubject;
import com.modularmclib.mixinsplice.api.transformation.AnnotationTarget;
import com.modularmclib.mixinsplice.api.transformation.AnnotationTransformation;
import com.modularmclib.mixinsplice.api.transformation.AnnotationView;
import com.modularmclib.mixinsplice.api.transformation.AnnotationView.ClassLiteral;
import com.modularmclib.mixinsplice.api.transformation.AnnotationView.EnumConstant;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AnnotationTransformer {

    private final List<AnnotationTransformation> rules;

    public AnnotationTransformer(List<AnnotationTransformation> rules) {
        this.rules = List.copyOf(rules);
    }

    public void transform(MixinSubject mixin, ClassNode node) {
        AnnotationLists classAnnotations = transform(new AnnotationTarget(mixin, Optional.empty()),
                node.visibleAnnotations, node.invisibleAnnotations);
        List<AnnotationLists> methodAnnotations = node.methods.stream().map(method -> transform(
                new AnnotationTarget(mixin, Optional.of(new AnnotationTarget.Method(method.name, method.desc))),
                method.visibleAnnotations, method.invisibleAnnotations)).toList();

        node.visibleAnnotations = classAnnotations.visible;
        node.invisibleAnnotations = classAnnotations.invisible;
        for (int i = 0; i < node.methods.size(); i++) {
            MethodNode method = node.methods.get(i);
            AnnotationLists updated = methodAnnotations.get(i);
            method.visibleAnnotations = updated.visible;
            method.invisibleAnnotations = updated.invisible;
        }
    }

    private AnnotationLists transform(AnnotationTarget target, @Nullable List<AnnotationNode> visible,
                                      @Nullable List<AnnotationNode> invisible) {
        List<AnnotationView> annotations = new ArrayList<>();
        if (visible != null) {
            visible.forEach(annotation -> annotations.add(read(annotation, true)));
        }
        if (invisible != null) {
            invisible.forEach(annotation -> annotations.add(read(annotation, false)));
        }
        List<AnnotationView> snapshot = List.copyOf(annotations);
        List<AnnotationView> adjusted = snapshot;
        for (AnnotationTransformation rule : rules) {
            adjusted = List.copyOf(rule.transform(target, adjusted));
        }
        if (snapshot.equals(adjusted)) {
            return new AnnotationLists(visible, invisible);
        }

        List<AnnotationNode> nextVisible = new ArrayList<>();
        List<AnnotationNode> nextInvisible = new ArrayList<>();
        for (AnnotationView annotation : adjusted) {
            (annotation.visible() ? nextVisible : nextInvisible).add(write(annotation));
        }
        return new AnnotationLists(nextVisible, nextInvisible);
    }

    private static AnnotationView read(AnnotationNode node, boolean visible) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (node.values != null) {
            for (int i = 0; i < node.values.size(); i += 2) {
                values.put((String) node.values.get(i), readValue(node.values.get(i + 1)));
            }
        }
        return new AnnotationView(node.desc, visible, values);
    }

    private static Object readValue(Object value) {
        return switch (value) {
            case Type type -> new ClassLiteral(type.getDescriptor());
            case String[] pair -> new EnumConstant(pair[0], pair[1]);
            case AnnotationNode nested -> read(nested, false);
            case List<?> list -> list.stream().map(AnnotationTransformer::readValue).toList();
            default -> value;
        };
    }

    private static AnnotationNode write(AnnotationView view) {
        AnnotationNode node = new AnnotationNode(view.descriptor());
        view.values().forEach((name, value) -> node.visit(name, writeValue(value)));
        return node;
    }

    private static Object writeValue(Object value) {
        return switch (value) {
            case ClassLiteral literal -> Type.getType(literal.descriptor());
            case EnumConstant constant -> new String[] { constant.descriptor(), constant.name() };
            case AnnotationView nested -> write(nested);
            case List<?> list -> new ArrayList<>(list.stream().map(AnnotationTransformer::writeValue).toList());
            default -> value;
        };
    }

    private record AnnotationLists(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible) {}
}
