package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TransformClassVisitor extends ClassVisitor {
    private boolean isTarget = false;
    private boolean isInterface;
    private AnnotationValueGetterVisitor annotationValueGetterVisitor;
    private String serviceAnnotation;
    private String methodAnnotation;

    public TransformClassVisitor(ClassVisitor cv, String serviceAnnotation, String methodAnnotation) {
        super(Opcodes.ASM5, cv);
        this.serviceAnnotation = serviceAnnotation;
        this.methodAnnotation = methodAnnotation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        String targetClassName = serviceAnnotation.replace(".", "/");
        AnnotationVisitor annotationVisitor = super.visitAnnotation(desc, visible);
        if (isInterface && desc.contains(targetClassName)) {
            isTarget = true;
            annotationValueGetterVisitor = new AnnotationValueGetterVisitor(annotationVisitor);
            return annotationValueGetterVisitor;
        }
        return annotationVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        if (isTarget) {
            String annotationValue = annotationValueGetterVisitor.getAnnotationValue();
            return new TransformMethodVisitor(annotationValue, Opcodes.ASM4, methodVisitor,methodAnnotation);
        }
        return methodVisitor;
    }
}
