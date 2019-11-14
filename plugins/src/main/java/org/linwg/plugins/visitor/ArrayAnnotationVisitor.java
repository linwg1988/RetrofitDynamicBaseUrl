package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * value为数组的注解值访问器，判断原有的注解值是否已经存在目标值，如果没有，注入目标注解值
 */
public class ArrayAnnotationVisitor extends AnnotationVisitor {
    private String annotationValue;
    private boolean hasTypeAnnotationValue = false;

    public ArrayAnnotationVisitor(String annotationValue, AnnotationVisitor av) {
        super(Opcodes.ASM5, av);
        this.annotationValue = annotationValue;
    }

    @Override
    public void visit(String name, Object value) {
        hasTypeAnnotationValue = value.equals(annotationValue);
        super.visit(name, value);
    }

    @Override
    public void visitEnd() {
        if (!hasTypeAnnotationValue) {
            av.visit(null, annotationValue);
        }
        super.visitEnd();
    }
}
