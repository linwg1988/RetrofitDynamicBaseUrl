package org.linwg.plugins


import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DynamicBaseUrlPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("dynamicBaseUrlConfig",DynamicBaseUrlConfigExtension)
        project.extensions.getByType(AppExtension).registerTransform(new DynamicBaseUrlTransform(project))
        System.out.println("DynamicBaseUrlPlugin Apply Success")
    }
}