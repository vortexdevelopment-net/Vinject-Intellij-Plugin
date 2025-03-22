package net.vortexdevelopment.plugin.vinject;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import net.vortexdevelopment.plugin.vinject.container.ClassDataManager;
import net.vortexdevelopment.plugin.vinject.syntax.AnnotationChangeListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Plugin implements ProjectActivity, Disposable  {


    private static final Set<String> BEAN_CLASSES = ConcurrentHashMap.newKeySet();
    private static boolean multipleRoots = false;
    private static String rootPackage = "";
    private static Project project;
    private AnnotationChangeListener annotationChangeListener;


    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        this.project = project;

        //Await indexing
        DumbService.getInstance(project).waitForSmartMode();

        //Find @Root annotated classes
        ApplicationManager.getApplication().runReadAction((Computable<List<VirtualFile>>) () -> {

            long start = System.currentTimeMillis();
            List<VirtualFile> roots = new ArrayList<>();
            ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

            projectFileIndex.iterateContent(file -> {
                if (projectFileIndex.isInSource(file) && !projectFileIndex.isInTestSourceContent(file) && !file.getPath().contains("resources") && !file.getPath().contains("target")) {
                    roots.add(file);
                }
                return true; // Continue iteration
            });

            for (VirtualFile virtualFile : roots) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                ClassDataManager.processFileChange(psiFile);

//                if (psiFile instanceof PsiJavaFile) {
//                    PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
//                    for (PsiClass psi : classes) {
//                        //Check if a component class, if so add it to the bean classes list
//                        if (isComponentClass(psi)) {
//                            BEAN_CLASSES.add(psi.getQualifiedName());
//                            if (isServiceClass(psi)) {
//                                //Register beans
//
//                                //Find all @Bean annotated methods
//                                PsiMethod[] methods = psi.getMethods();
//                                for (PsiMethod method : methods) {
//                                    if (method.getAnnotation("net.vortexdevelopment.vinject.annotation.Bean") != null) {
//                                        if (method.getReturnType() instanceof PsiClassType psiClassType) {
//                                            PsiClass psiClass = psiClassType.resolve();
//                                            if (psiClass != null) {
//                                                BEAN_CLASSES.add(psiClass.getQualifiedName());
//                                                //Check if the annotation has registerSubclasses parameter, if so register those classes as well
//                                                PsiAnnotation registerSubclassesAnnotation = method.getAnnotation("net.vortexdevelopment.vinject.annotation.Bean");
//                                                if (registerSubclassesAnnotation != null) {
//                                                    BEAN_CLASSES.addAll(getClassArray(registerSubclassesAnnotation, "registerSubclasses"));
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//
//                        PsiAnnotation rootAnnotation = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.Root");
//                        if (rootAnnotation != null) {
//                            //Add root class to Beans
//                            BEAN_CLASSES.add(psi.getQualifiedName());
//
//                            //Get annotation and get packageName parameter
//                            String packageName = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.Root").getParameterList().getAttributes()[0].getValue().getText();
//                            if (rootPackage.isEmpty()) {
//                                rootPackage = packageName.replace("\"", "");
//                            } else {
//                                //We found 2 roots, project isn't valid
//                                multipleRoots = true;
//                            }
//
//                            COMPONENT_ANNOTATIONS.addAll(getClassArray(rootAnnotation, "componentAnnotations"));
//                        }
//
//                        //Regisry annotation
//                        PsiAnnotation registryAnnotation = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.Registry");
//                        if (registryAnnotation != null && registryAnnotation.getParameterList().getAttributes().length > 0) {
//                            COMPONENT_ANNOTATIONS.addAll(getClassArray(registryAnnotation, "annotation"));
//                        }
//
//
//                        PsiAnnotation componentAnnotation = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.Component");
//                        //Register all @Component classes as well as their subclasses if any
//                        if (componentAnnotation != null && componentAnnotation.getParameterList().getAttributes().length > 0) {
//                            BEAN_CLASSES.addAll(getClassArray(componentAnnotation, "registerSubclasses"));
//                        }
//                    }
//                }
            }
            return roots;
        });

        this.annotationChangeListener = new AnnotationChangeListener();
        PsiManager.getInstance(project).addPsiTreeChangeListener(annotationChangeListener, this);

        return null;
    }

    public static Project getProject() {
        return project;
    }

    public static String getRootPackage() {
        return rootPackage;
    }

    public static void runWriteAction(Runnable runnable) {
        Application application = ApplicationManager.getApplication();
        if (!application.isWriteAccessAllowed()) return;
        application.runWriteAction(runnable);
    }

    public static void runReadAction(Runnable runnable) {
        Application application = ApplicationManager.getApplication();
        if (!application.isReadAccessAllowed()) return;
        application.runReadAction(runnable);
    }

    @Override
    public void dispose() {
        PsiManager.getInstance(project).removePsiTreeChangeListener(annotationChangeListener);
    }
}
