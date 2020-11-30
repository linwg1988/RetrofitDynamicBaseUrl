package org.linwg.plugins


import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

class DynamicBaseUrlPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("dynamicBaseUrlConfig", DynamicBaseUrlConfigExtension)
        try {
            def appExtension = project.extensions.getByType(AppExtension)
            if (appExtension != null) {
                appExtension.registerTransform(new DynamicBaseUrlTransform(project,false))
            }
            System.out.println("DynamicBaseUrlPlugin Apply On App Success!")
        } catch (UnknownDomainObjectException e) {
        }
        try {
            def libraryExtension = project.extensions.getByType(LibraryExtension)
            if (libraryExtension != null) {
                libraryExtension.registerTransform(new DynamicBaseUrlTransform(project,true))
            }
            System.out.println("DynamicBaseUrlPlugin Apply On Library Success!")
        } catch (UnknownDomainObjectException e) {

        }
    }
}