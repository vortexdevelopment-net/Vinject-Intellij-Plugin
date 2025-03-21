package net.vortexdevelopment.plugin.vinject.syntax;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import net.vortexdevelopment.plugin.vinject.Plugin;
import net.vortexdevelopment.plugin.vinject.container.ClassDataManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnnotationChangeListener implements PsiTreeChangeListener {


    private void checkForAnnotationChange(PsiClass oldClass, PsiClass newClass) {

        boolean isOldClassComponent = ClassDataManager.isComponentClass(oldClass);
        boolean isNewClassComponent = ClassDataManager.isComponentClass(newClass);

        if (isOldClassComponent && !isNewClassComponent) {
            //Remove class from the beans
            //Plugin.removeBean(oldClass);
        }

        if (!isOldClassComponent && isNewClassComponent) {
            //Add class to the beans
            //Plugin.addBean(newClass);
        }
    }

    private void checkForAnnotationChange(PsiAnnotation oldAnnotation, PsiAnnotation newAnnotation) {
        if (
            (oldAnnotation.getQualifiedName().equals("net.vortexdevelopment.vinject.annotation.Component") ||
            oldAnnotation.getQualifiedName().equals("net.vortexdevelopment.vinject.annotation.Bean")) &&
            (newAnnotation.getQualifiedName().equals("net.vortexdevelopment.vinject.annotation.Component") ||
            newAnnotation.getQualifiedName().equals("net.vortexdevelopment.vinject.annotation.Bean"))
        ) {

            List<String> oldRegisterSubclasses = ClassDataManager.getClassArray(oldAnnotation, "registerSubclasses");
            List<String> newRegisterSubclasses = ClassDataManager.getClassArray(newAnnotation, "registerSubclasses");

            //Get the added or removed classes
            Set<String> addedClasses = new HashSet<>(newRegisterSubclasses);
            addedClasses.removeAll(oldRegisterSubclasses);
            Set<String> removedClasses = new HashSet<>(oldRegisterSubclasses);
            removedClasses.removeAll(newRegisterSubclasses);

            //Add the added classes to the beans
            for (String className : addedClasses) {
                //Plugin.addBean(className);
            }

            //Remove the removed classes from the beans
            for (String className : removedClasses) {
                //Plugin.removeBean(className);
            }
        }
    }

    @Override
    public void beforeChildAddition(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildRemoval(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildReplacement(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildMovement(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildrenChange(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforePropertyChange(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        if (DumbService.isDumb(Plugin.getProject())) {
            DumbService.getInstance(Plugin.getProject()).runWhenSmart(() -> {
                childrenChanged(event);
            });
            return;
        }
        ClassDataManager.processFileChange(event.getFile());
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
    }
}
