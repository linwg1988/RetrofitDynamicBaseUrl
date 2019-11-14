package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 获取类注解值
 */
public class AnnotationValueGetterVisitor extends AnnotationVisitor {

    private String annotationValue;

    public AnnotationValueGetterVisitor(AnnotationVisitor av) {
        super(Opcodes.ASM5, av);
    }

    @Override
    public void visit(String name, Object value) {
        this.annotationValue = value.toString();
        super.visit(name, value);
    }

    public String getAnnotationValue() {
        return annotationValue;
    }
}
