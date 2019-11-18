package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class FindIgnoreMethodClassVisitor extends ClassVisitor {
    private boolean isTarget = false;
    private boolean isInterface;
    private String serviceAnnotation;
    private String ignoreAnnotation;
    private List<IgnoreMethodVisitor> methodVisitorList = new ArrayList<>();
    private List<String> ignoreMethod = new ArrayList<>();

    public List<String> getIgnoreMethod() {
        return ignoreMethod;
    }

    public FindIgnoreMethodClassVisitor(ClassVisitor cv, String serviceAnnotation, String ignoreAnnotation) {
        super(Opcodes.ASM5, cv);
        this.serviceAnnotation = serviceAnnotation;
        this.ignoreAnnotation = ignoreAnnotation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        String targetClassName = serviceAnnotation.replace(".", "/");
        if (isInterface && desc.contains(targetClassName)) {
            isTarget = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        if (isTarget) {
            String key = access + name + desc + signature;
            IgnoreMethodVisitor tmv = new IgnoreMethodVisitor(key, Opcodes.ASM4, methodVisitor, ignoreAnnotation);
            methodVisitorList.add(tmv);
            return tmv;
        }
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        for (int i = 0; i < methodVisitorList.size(); i++) {
            IgnoreMethodVisitor methodVisitor = methodVisitorList.get(i);
            if (methodVisitor.isDynamicIgnore()) {
                ignoreMethod.add(methodVisitor.getMethodName());
            }
        }
        super.visitEnd();
    }
}
