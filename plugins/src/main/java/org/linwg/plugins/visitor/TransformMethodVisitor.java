package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 要注入注解的方法访问器
 */
public class TransformMethodVisitor extends MethodVisitor {

    private boolean hasAnnotation = false;
    private boolean callForCreate = false;
    private String annotationValue;
    private String methodAnnotation;

    public TransformMethodVisitor(String annotationValue, MethodVisitor mv, String methodAnnotation) {
        super( Opcodes.ASM4, mv);
        this.annotationValue = annotationValue;
        this.methodAnnotation = methodAnnotation.replace(".", "/");
    }

    @Override
    public void visitEnd() {
        if (!hasAnnotation) {
            callForCreate = true;
            this.visitAnnotation("L" + methodAnnotation + ";", true);
        }
        super.visitEnd();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor annotationVisitor = super.visitAnnotation(desc, visible);
        if (desc.contains(methodAnnotation)) {
            hasAnnotation = true;
            if (callForCreate) {
                MethodAnnotationCreateVisitor av = new MethodAnnotationCreateVisitor(annotationValue, annotationVisitor);
                av.visitArray("value");
                return av;
            } else {
                return new MethodAnnotationVisitor(annotationValue, annotationVisitor);
            }
        }
        return annotationVisitor;
    }
}
