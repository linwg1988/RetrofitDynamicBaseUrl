package org.linwg.plugins.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * 要注入注解的方法访问器
 */
public class IgnoreMethodVisitor extends MethodVisitor {

    private String ignoreAnnotation;
    private String methodName;
    private boolean isDynamicIgnore = false;

    public IgnoreMethodVisitor(String methodName, int api, MethodVisitor mv, String ignoreAnnotation) {
        super(api, mv);
        this.methodName = methodName;
        this.ignoreAnnotation = ignoreAnnotation == null ? "" : ignoreAnnotation.replace(".", "/");
    }

    public boolean isDynamicIgnore() {
        return isDynamicIgnore;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!isDynamicIgnore) {
            isDynamicIgnore = desc.contains(ignoreAnnotation);
        }
        return super.visitAnnotation(desc, visible);
    }
}
