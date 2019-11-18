package org.linwg.plugins

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.linwg.plugins.visitor.FindIgnoreMethodClassVisitor
import org.linwg.plugins.visitor.TransformClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.util.concurrent.Callable
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class DynamicBaseUrlTransform extends Transform {
    Project project

    DynamicBaseUrlTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "DynamicBaseUrlTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(@NonNull TransformInvocation transformInvocation) {
        String sa = project.dynamicBaseUrlConfig.serviceAnnotation
        String ma = project.dynamicBaseUrlConfig.methodAnnotation
        String[] services = project.dynamicBaseUrlConfig.services
        String ignoreAnnotation = project.dynamicBaseUrlConfig.ignoreAnnotation
        if (sa == null || sa.length() == 0) {
            throw new RuntimeException("build.gradle property miss : dynamicBaseUrlConfig.serviceAnnotation required.")
        }
        def startTime = System.currentTimeMillis()
        Collection<TransformInput> inputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        boolean incremental = transformInvocation.incremental
        //如果不需要增量编译，先删除之前的输出
        if (!incremental && outputProvider != null) {
            outputProvider.deleteAll()
        }
        //并发编译
        WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
        //遍历inputs
        inputs.each { TransformInput input ->
            //遍历directoryInputs
            input.directoryInputs.each { DirectoryInput directoryInput ->
                waitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        handleDirectoryInput(directoryInput, outputProvider, sa, services, ma, incremental, ignoreAnnotation)
                        return null
                    }
                })

            }

            //遍历jarInputs
            input.jarInputs.each { JarInput jarInput ->
                waitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        handleJarInputs(jarInput, outputProvider, sa, project.dynamicBaseUrlConfig.ignoreJar, services, ma, incremental, ignoreAnnotation)
                        return null
                    }
                })
            }
        }
        waitableExecutor.waitForTasksWithQuickFail(true)
        def cost = (System.currentTimeMillis() - startTime) / 1000
        println "> Task :app:DynamicBaseUrlTransform transform cost ： $cost s"
    }

    /**
     * 处理文件目录下的class文件
     */
    static void handleDirectoryInput(DirectoryInput directoryInput,
                                     TransformOutputProvider outputProvider,
                                     String serviceAnnotation,
                                     String[] services,
                                     String methodAnnotation,
                                     boolean incremental,
                                     String ignoreAnnotation) {
        def dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes,
                Format.DIRECTORY)
        if (incremental) {
            processIncrementalDirectoryInput(directoryInput, dest, serviceAnnotation, services, methodAnnotation, ignoreAnnotation)
        } else {
            directoryInputTransform(directoryInput, dest, serviceAnnotation, services, methodAnnotation, ignoreAnnotation)
        }
    }

    /**
     * 获取目录下有变化的文件列表，根据文件状态，对每个文件进行操作注入
     * @param directoryInput
     * @param dest
     * @param serviceAnnotation
     * @param services
     * @param methodAnnotation
     */
    static void processIncrementalDirectoryInput(DirectoryInput directoryInput, File dest,
                                                 String serviceAnnotation,
                                                 String[] services,
                                                 String methodAnnotation,
                                                 String ignoreAnnotation) {
        FileUtils.forceMkdir(dest)
        def srcDirPath = directoryInput.getFile().getAbsolutePath()
        def destDirPath = dest.getAbsolutePath()
        def map = directoryInput.changedFiles
        map.each { Map.Entry<File, Status> entry ->
            File inputFile = entry.getKey()
            Status status = entry.getValue()
            String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath)
            File destFile = new File(destFilePath)
            switch (status) {
                case Status.NOTCHANGED:
                    //文件状态没变化 不需要修改输出文件
                    break
                case Status.ADDED:
                case Status.CHANGED:
                    //文件添加，修改 需要对文件进行拷贝
                    FileUtils.touch(destFile)
                    singleFileTransform(inputFile, destFile, services, serviceAnnotation, methodAnnotation, ignoreAnnotation)
                    break
                case Status.REMOVED:
                    //如果源文件状态是已经被删除了，那么目标文件也要删掉
                    if (destFile.exists()) {
                        FileUtils.forceDelete(destFile)
                    }
                    break
            }
        }
    }

    /**
     * 单个文件代码注入处理以及拷贝
     * @param inputFile
     * @param destFile
     * @param services
     * @param serviceAnnotation
     * @param methodAnnotation
     */
    static void singleFileTransform(File inputFile, File destFile, String[] services, String serviceAnnotation, String methodAnnotation, String ignoreAnnotation) {
        def name = inputFile.name
        if (checkClassFile(name, services)) {
            injectAnnotation(inputFile, serviceAnnotation, methodAnnotation, ignoreAnnotation)
        }
        //处理完输入文件之后，要把输出给下一个任务
        FileUtils.copyFile(inputFile, destFile)
    }

    /**
     * 非增量编译，删除目标文件后，遍历目录下所有的文件，查询需要注入代码的文件，无论是否注入，将最终文件目录拷贝到指定目录
     * @param directoryInput
     * @param dest
     * @param serviceAnnotation
     * @param services
     * @param methodAnnotation
     */
    static void directoryInputTransform(DirectoryInput directoryInput, File dest,
                                        String serviceAnnotation,
                                        String[] services,
                                        String methodAnnotation,
                                        String ignoreAnnotation) {
        //是否是目录
        if (directoryInput.file.isDirectory()) {
            //列出目录所有文件（包含子文件夹，子文件夹内文件）
            directoryInput.file.eachFileRecurse { File file ->
                def name = file.name
                if (checkClassFile(name, services)) {
                    injectAnnotation(file, serviceAnnotation, methodAnnotation, ignoreAnnotation)
                }
            }
        }
        //处理完输入文件之后，要把输出给下一个任务
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    /**
     * 对文件进行注解的注入
     * @param file
     * @param serviceAnnotation
     * @param methodAnnotation
     */
    static void injectAnnotation(File file, String serviceAnnotation, String methodAnnotation, String ignoreAnnotation) {
        ArrayList<String> method = null
        if (ignoreAnnotation != null || ignoreAnnotation.length() > 0) {
            ClassReader ignoreReader = new ClassReader(file.bytes)
            ClassWriter ignoreWriter = new ClassWriter(ignoreReader, ClassWriter.COMPUTE_MAXS)
            FindIgnoreMethodClassVisitor icv = new FindIgnoreMethodClassVisitor(ignoreWriter, serviceAnnotation, ignoreAnnotation)
            ignoreReader.accept(icv, ClassReader.EXPAND_FRAMES)
            method = icv.getIgnoreMethod()
        }
        ClassReader classReader = new ClassReader(file.bytes)
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        TransformClassVisitor cv = new TransformClassVisitor(classWriter, serviceAnnotation, methodAnnotation, method)
        classReader.accept(cv, ClassReader.EXPAND_FRAMES)
        byte[] code = classWriter.toByteArray()
        FileOutputStream fos = new FileOutputStream(
                file.parentFile.absolutePath + File.separator + file.name)
        fos.write(code)
        fos.close()
    }

    /**
     * 处理Jar中的class文件
     */
    static void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider, String serviceAnnotation,
                                boolean ignoreJar, String[] services, String methodAnnotation,
                                boolean incremental, String ignoreAnnotation) {
        def destFile = outputProvider.getContentLocation(jarInput.getFile().getAbsolutePath(),
                jarInput.contentTypes, jarInput.scopes, Format.JAR)
        if (incremental) {
            //处理增量编译，此时output的jar文件还没删除，要根据输入jar的状态来执行编译
            switch (jarInput.status) {
                case Status.NOTCHANGED:
                    //文件状态没变化 不需要修改输出文件
                    break
                case Status.CHANGED:
                    //如果源jar文件状态是已经被修改了，那么目标jar文件要删掉,然后重新拷贝
                    if (destFile.exists()) {
                        FileUtils.forceDelete(destFile)
                    }
                    processJarInputTransform(jarInput, outputProvider, serviceAnnotation, ignoreJar, services, methodAnnotation, ignoreAnnotation)
                    break
                case Status.ADDED:
                    //文件添加，需要对文件进行拷贝
                    processJarInputTransform(jarInput, outputProvider, serviceAnnotation, ignoreJar, services, methodAnnotation, ignoreAnnotation)
                    break
                case Status.REMOVED:
                    //如果源jar文件状态是已经被删除了，那么目标jar文件也要删掉
                    if (destFile.exists()) {
                        FileUtils.forceDelete(destFile)
                    }
                    break
            }
        } else {
            //不处理增量编译，直接全拷贝
            processJarInputTransform(jarInput, outputProvider, serviceAnnotation, ignoreJar, services, methodAnnotation, ignoreAnnotation)
        }
    }

    static void processJarInputTransform(JarInput jarInput, TransformOutputProvider outputProvider,
                                         String serviceAnnotation, boolean ignoreJar,
                                         String[] services, String methodAnnotation, String ignoreAnnotation) {
        //不处理增量编译，直接全拷贝
        if (jarInput.file.getAbsolutePath().endsWith(".jar") && !ignoreJar) {
            //重命名输出文件,因为可能同名,会覆盖
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()
            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
            //用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                //插桩class
                if (checkClassFile(entryName, services)) {
                    //class文件处理
                    println '----------- deal with "jar" class file <' + entryName + '> -----------'
                    jarOutputStream.putNextEntry(zipEntry)
                    def method = null
                    if (ignoreAnnotation != null || ignoreAnnotation.length() > 0) {
                        method = findIgnoreMethod(inputStream, serviceAnnotation, ignoreAnnotation)
                    }
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    TransformClassVisitor cv = new TransformClassVisitor(classWriter, serviceAnnotation, methodAnnotation, method)
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    jarOutputStream.write(code)
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }
            //结束
            jarOutputStream.close()
            jarFile.close()
            def dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        } else {
            def dest = outputProvider.getContentLocation(jarInput.getFile().getAbsolutePath(),
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(jarInput.file, dest)
        }
    }

    static List<String> findIgnoreMethod(InputStream inputStream, String serviceAnnotation, String ignoreAnnotation) {
        ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        FindIgnoreMethodClassVisitor cv = new FindIgnoreMethodClassVisitor(classWriter, serviceAnnotation, ignoreAnnotation)
        classReader.accept(cv, ClassReader.EXPAND_FRAMES)
        return cv.ignoreMethod
    }

    /**
     * 检查class文件是否需要处理
     * @param fileName
     * @return
     */
    static boolean checkClassFile(String name, String[] services) {
        if (services != null && services.length != 0) {
            for (String serviceName : services) {
                if (name.contains(serviceName)) {
                    return true
                }
            }
            return false
        }
        //只处理需要的class文件
        return (name.endsWith(".class") && !name.startsWith("R\$")
                && !"R.class".equals(name) && !"BuildConfig.class".equals(name))
    }
}
