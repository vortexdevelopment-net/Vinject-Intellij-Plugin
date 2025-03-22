package net.vortexdevelopment.plugin.vinject.syntax;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import net.vortexdevelopment.plugin.vinject.Plugin;
import net.vortexdevelopment.plugin.vinject.container.ClassDataManager;
import net.vortexdevelopment.plugin.vinject.quickfixes.BeanNonAnnotatedQuickFix;
import net.vortexdevelopment.plugin.vinject.quickfixes.BeanUsedInNonServiceClass;
import net.vortexdevelopment.plugin.vinject.quickfixes.EntityPrimitiveTypeFix;
import net.vortexdevelopment.plugin.vinject.quickfixes.InjectToNonComponentClass;
import net.vortexdevelopment.plugin.vinject.quickfixes.RemoveInjectNonComponentClass;
import net.vortexdevelopment.plugin.vinject.quickfixes.RemoveNonComponentConstructorParameters;
import net.vortexdevelopment.plugin.vinject.quickfixes.RemoveUnusedInjectField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ComponentHighlighter extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Ensure @Inject is only used in @Component classes";
    }

    @Override
    public ProblemDescriptor @Nullable [] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> descriptors = new ArrayList<>();

        //Check if the method is in a Service class
        //If any methods are not annotated with @Bean, show error
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            if (containingClass.getAnnotation("net.vortexdevelopment.vinject.annotation.Service") != null) {
                //Check if the method is annotated with @Bean
                PsiAnnotation beanAnnotation = method.getAnnotation("net.vortexdevelopment.vinject.annotation.Bean");
                if (beanAnnotation == null) {
                    //Show error
                    ProblemDescriptor descriptor = manager.createProblemDescriptor(
                            method,
                            "Method is not annotated with @Bean in a Service class",
                            true,
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                            isOnTheFly,
                            new BeanNonAnnotatedQuickFix()
                    );
                    descriptors.add(descriptor);
                }
            }
        }


        return descriptors.isEmpty() ? null : descriptors.toArray(new ProblemDescriptor[0]);
    }

    @Override
    public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass psiClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        //check classes for missing @Service annotations (Beans used in non-service classes)
        List<ProblemDescriptor> descriptors = new ArrayList<>();
        if (psiClass.getAnnotation("net.vortexdevelopment.vinject.annotation.Service") == null) {
            //get all methods in the class
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                if (method.getAnnotation("net.vortexdevelopment.vinject.annotation.Bean") != null) {
                    //Check if the method is in a Service class
                    PsiClass containingClass = method.getContainingClass();
                    if (containingClass != null) {
                        if (containingClass.getAnnotation("net.vortexdevelopment.vinject.annotation.Service") == null) {
                            //Show error
                            ProblemDescriptor descriptor = manager.createProblemDescriptor(
                                    psiClass,
                                    "Class not annotated with @Service and @Bean annotation used",
                                    true,
                                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                                    isOnTheFly,
                                    new BeanUsedInNonServiceClass()
                            );
                            descriptors.add(descriptor);
                        }
                    }
                }
            }
        }
        //Check Component classes for constructors, they only allowed to have a default contructor or a constructor with parameters that can be used as @Inject
        psiClass.getConstructors();
        for (PsiMethod constructor : psiClass.getConstructors()) {
            if (constructor.getParameterList().getParameters().length == 0) {
                continue;
            }
            if (!ClassDataManager.isComponentClass(psiClass)) {
                continue;
            }
            for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
                if (parameter.getType() instanceof PsiClassType psiClassType) { //Add primitive types
                    if (!ClassDataManager.isClassProvided(psiClassType.resolve())) {
                        //Show error
                        ProblemDescriptor descriptor = manager.createProblemDescriptor(
                                parameter,
                                "Component class constructor can only have other components as parameters",
                                true,
                                ProblemHighlightType.GENERIC_ERROR,
                                isOnTheFly,
                                new RemoveNonComponentConstructorParameters()
                        );
                        descriptors.add(descriptor);
                    }
                } else if (parameter.getType() instanceof PsiPrimitiveType psiPrimitiveType) { //Add primitive types
                    //Show error
                    ProblemDescriptor descriptor = manager.createProblemDescriptor(
                            parameter,
                            "Component class constructor can only have other components as parameters",
                            true,
                            ProblemHighlightType.GENERIC_ERROR,
                            isOnTheFly,
                            new RemoveNonComponentConstructorParameters()
                    );
                    descriptors.add(descriptor);
                }
            }
        }

        return descriptors.isEmpty() ? null : descriptors.toArray(new ProblemDescriptor[0]);
    }

    @Override
    public ProblemDescriptor @Nullable [] checkField(@NotNull PsiField field, @NotNull InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> descriptors = new ArrayList<>();
        PsiClass containingClass = field.getContainingClass();

        PsiAnnotation componentAnnotation = null;
        for (String annotation : ClassDataManager.getComponentAnnotations()) {
            if (containingClass != null && containingClass.getAnnotation(annotation) != null) {
                componentAnnotation = containingClass.getAnnotation(annotation);
                break;
            }
        }

        //The current class is a component class
        if (componentAnnotation == null) {
            // Check if the field is annotated with @Inject
            PsiAnnotation injectAnnotation = field.getAnnotation("net.vortexdevelopment.vinject.annotation.Inject");
            if (injectAnnotation != null) {
                ProblemDescriptor descriptor = manager.createProblemDescriptor(
                        field,
                        "You can only use @Inject in a Component classes",
                        true,
                        ProblemHighlightType.GENERIC_ERROR,
                        isOnTheFly,
                        new InjectToNonComponentClass(), new RemoveInjectNonComponentClass()
                );
                descriptors.add(descriptor);
            }
        } else {
            //The current class is a component class
            //Check if @Inject annotated fields have a provider

            PsiAnnotation injectAnnotation = field.getAnnotation("net.vortexdevelopment.vinject.annotation.Inject");
            if (injectAnnotation != null) {
                if (field.getType() instanceof PsiClassType psiClassType) {
                    PsiClass psiClass = psiClassType.resolve();
                    if (psiClass != null) {
                        if (!ClassDataManager.isClassProvided(psiClass)) {
                            //Show error
                            ProblemDescriptor descriptor = manager.createProblemDescriptor(
                                    field,
                                    "No Bean class found for " + psiClass.getName(),
                                    true,
                                    ProblemHighlightType.GENERIC_ERROR,
                                    isOnTheFly
                            );
                            descriptors.add(descriptor);
                        }
                    }

                    //Check if the Inject annotated field is unused
                    GlobalSearchScope scope = GlobalSearchScope.projectScope(Plugin.getProject());
                    Query<PsiReference> query = ReferencesSearch.search(field, scope);
                    if (query.findFirst() == null && !field.hasAnnotation("lombok.Getter")) {
                        ProblemDescriptor descriptor = manager.createProblemDescriptor(
                                field,
                                "Injected field is never used",
                                true,
                                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                                isOnTheFly,
                                new RemoveUnusedInjectField()
                        );
                        descriptors.add(descriptor);
                    }
                }
            }
        }

        if (containingClass != null) {
            //Check if Entity annotation is presend, and if any primitive type fields are used show an error
            PsiAnnotation entityAnnotation = containingClass.getAnnotation("net.vortexdevelopment.vinject.annotation.database.Entity");
            PsiAnnotation fieldAnnotation = field.getAnnotation("net.vortexdevelopment.vinject.annotation.database.Column");
            PsiAnnotation temporalAnnotation = field.getAnnotation("net.vortexdevelopment.vinject.annotation.database.Temporal");
            if(entityAnnotation != null) {
                if (field.getType() instanceof PsiPrimitiveType && (fieldAnnotation == null || temporalAnnotation == null)) {
                    descriptors.add(manager.createProblemDescriptor(
                            field,
                            "Primitive types are not allowed in Entity classes",
                            true,
                            ProblemHighlightType.GENERIC_ERROR,
                            isOnTheFly,
                            new EntityPrimitiveTypeFix()
                    ));
                }
            }
        }

        return descriptors.isEmpty() ? null : descriptors.toArray(new ProblemDescriptor[0]);
    }
}
