package net.vortexdevelopment.plugin.vinject.build;

import com.intellij.build.events.MessageEvent;
import com.intellij.build.issue.BuildIssue;
import com.intellij.compiler.progress.BuildIssueContributor;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.util.InspectionValidator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import net.vortexdevelopment.plugin.vinject.container.ClassDataManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VInjectInspectionValidator extends InspectionValidator {
    protected VInjectInspectionValidator() {
        super("vinject.component.validator", "Validates component elements and classes", "VInject");
        System.out.println("VInjectInspectionValidator created");
    }

    @Override
    public boolean isAvailableOnScope(@NotNull CompileScope scope) {
        System.out.println("isAvailableOnScope: " + scope.toString());
        return true;
    }

    @Override
    public Collection<VirtualFile> getFilesToProcess(Project project, CompileContext context) {
        return Arrays.stream(context.getCompileScope().getFiles(JavaFileType.INSTANCE, true)).filter(virtualFile -> {
            //Check if the file is a component class
            if (virtualFile instanceof PsiJavaFile psiJavaFile) {
                for (PsiClass psiClass : psiJavaFile.getClasses()) {
                    if (ClassDataManager.isComponentClass(psiClass) || ClassDataManager.isClassProvided(psiClass)) {
                        return true;
                    }
                }
            }
            return false;
        }).collect(Collectors.toSet());
    }
}
