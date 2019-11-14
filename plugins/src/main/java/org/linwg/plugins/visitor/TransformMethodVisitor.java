package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * 要注入注解的方法访问器
 */
public class TransformMethodVisitor extends MethodVisitor {

    private boolean hasAnnotation = false;
    private boolean callForCreate = false;
    private String annotationValue;
    private String methodAnnotation;

    public TransformMethodVisitor(String annotationValue, int api, MethodVisitor mv, String methodAnnotation) {
        super(api, mv);
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
