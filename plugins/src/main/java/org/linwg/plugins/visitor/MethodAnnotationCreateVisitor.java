package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 如果方法不存在此注解，返回此注解创建访问器，并主动创建值数组，访问器执行访问数组时，主动
 * 访问visitEnd，是数组注解添加注解值
 */
public class MethodAnnotationCreateVisitor extends AnnotationVisitor {
    private String annotationValue;

    public MethodAnnotationCreateVisitor(String annotationValue, AnnotationVisitor av) {
        super(Opcodes.ASM5, av);
        this.annotationValue = annotationValue;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor annotationVisitor = super.visitArray(name);
        ArrayAnnotationVisitor visitor = new ArrayAnnotationVisitor(annotationValue, annotationVisitor);
        visitor.visitEnd();
        return visitor;
    }

    @Override
    public void visitEnd() {
        av.visit(null, annotationValue);
        super.visitEnd();
    }
}
