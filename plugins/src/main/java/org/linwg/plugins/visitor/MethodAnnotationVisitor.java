package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 如果方法已经存在指定注解，这访问此注解的值
 */
public class MethodAnnotationVisitor extends AnnotationVisitor {
    private String annotationValue;

    public MethodAnnotationVisitor(String annotationValue, AnnotationVisitor av) {
        super(Opcodes.ASM5, av);
        this.annotationValue = annotationValue;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor annotationVisitor = super.visitArray(name);
        return new ArrayAnnotationVisitor(annotationValue,annotationVisitor);
    }
}
