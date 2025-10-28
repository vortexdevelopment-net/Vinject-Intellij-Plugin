package net.vortexdevelopment.plugin.vinject.container;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiTypeElement;
import net.vortexdevelopment.plugin.vinject.Plugin;
import net.vortexdevelopment.plugin.vinject.templates.TemplateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassDataManager {

    public static final Set<String> COMPONENT_ANNOTATIONS = ConcurrentHashMap.newKeySet();
    private static Map<String, ClassData> classData = new ConcurrentHashMap<>();

    static {
        // Pre-register the standard annotations
        COMPONENT_ANNOTATIONS.add("net.vortexdevelopment.vinject.annotation.Root");
        COMPONENT_ANNOTATIONS.add("net.vortexdevelopment.vinject.annotation.Registry");
        COMPONENT_ANNOTATIONS.add("net.vortexdevelopment.vinject.annotation.Service");
        COMPONENT_ANNOTATIONS.add("net.vortexdevelopment.vinject.annotation.Component");
        COMPONENT_ANNOTATIONS.add("net.vortexdevelopment.vinject.annotation.Repository");
        COMPONENT_ANNOTATIONS.add("net.vortexdevelopment.vinject.annotation.Api");
    }

    /**
     * Process all annotations in a file and register new component annotations
     */
    public static void processFileChange(@org.jetbrains.annotations.Nullable PsiFile psiFile) {
        if (psiFile == null || psiFile.getVirtualFile() == null) return;

        if (DumbService.isDumb(psiFile.getProject())) {
            //Queue the file for processing when not in dumb mode
            DumbService.getInstance(psiFile.getProject()).runWhenSmart(() -> {
                processFileChange(psiFile);
            });
            return;
        }

        if (psiFile instanceof PsiJavaFile psiJavaFile) {
            PsiClass[] classes = psiJavaFile.getClasses();
            for (PsiClass psi : classes) {
                // Process Registry annotations to discover new component annotations
                PsiAnnotation registryAnnotation = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.Registry");
                if (registryAnnotation != null) {
                    List<String> newAnnotations = getClassArray(registryAnnotation, "annotation");
                    for (String annotation : newAnnotations) {
                        registerComponentAnnotation(annotation);
                    }
                }

                // Process Root annotations
                PsiAnnotation rootAnnotation = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.Root");
                if (rootAnnotation != null) {
                    ClassData classData = new ClassData(psi, rootAnnotation);
                    List<String> componentAnnotations = getClassArray(rootAnnotation, "componentAnnotations");
                    for (String annotation : componentAnnotations) {
                        registerComponentAnnotation(annotation);
                    }
                    addClassData(psi, classData);

                    PsiAnnotation[] registerTemplateAnnotations = ClassDataManager.getAnnotationArray(rootAnnotation, "templateDependencies");
                    for (PsiAnnotation registerTemplateAnnotation : registerTemplateAnnotations) {
                        //print values artifactId, groupId, version
                        String artifactId = registerTemplateAnnotation.findAttributeValue("artifactId").getText();
                        String groupId = registerTemplateAnnotation.findAttributeValue("groupId").getText();
                        String version = registerTemplateAnnotation.findAttributeValue("version").getText();

                        //Check classpath for the dependency
                        Project project = Plugin.getProject();
                        VirtualFile dependencyRoot = getDependencyRoot(project, groupId, artifactId, version);
                        if (dependencyRoot != null) {
                            String rawPath = dependencyRoot.getPath();
                            String absolutePath = rawPath.replace(".jar!/", ".jar");
                            //Open the jar file and search for the vinject.templates directory to load templates
                            TemplateManager.getInstance().registerTemplateFromJar(absolutePath);
                        }
                    }

                    //Check for the @RegisterTemplate annotation array
                    continue;
                }

                // Process RegisterTemplate annotations
                PsiAnnotation registerTemplateAnnotation = psi.getAnnotation("net.vortexdevelopment.vinject.annotation.RegisterTemplate");
                if (registerTemplateAnnotation != null) {
                    String annotationFqcn = registerTemplateAnnotation.findAttributeValue("annotationFqcn").getText();
                    String resource = registerTemplateAnnotation.findAttributeValue("resource").getText();
                    String name = registerTemplateAnnotation.findAttributeValue("name").getText();
                    TemplateManager.getInstance().registerTemplateFromFile(resource, name, annotationFqcn);
                    continue;
                }

                // Check for any component annotation
                boolean isComponent = false;
                for (String annotationFqn : COMPONENT_ANNOTATIONS) {
                    PsiAnnotation annotation = psi.getAnnotation(annotationFqn);
                    if (annotation != null) {
                        ClassData classData = new ClassData(psi, annotation);
                        addClassData(psi, classData);
                        isComponent = true;
                        break;
                    }
                }

                // If not a component, remove from tracking
                if (!isComponent) {
                    removeClassData(psi);
                }
            }
        }
    }

    /**
     * Get the virtual directory for a dependency in the classpath
     * @param project Current project
     * @param groupId Maven/Gradle group ID
     * @param artifactId Maven/Gradle artifact ID
     * @param version Version of the dependency
     * @return VirtualFile representing the dependency root, or null if not found
     */
    private static VirtualFile getDependencyRoot(Project project, String groupId, String artifactId, String version) {
        // Clean up quoted values if present
        groupId = groupId.replaceAll("\"", "");
        artifactId = artifactId.replaceAll("\"", "");
        version = version.replaceAll("\"", "");

        String dependencyPath = (groupId.replace('.', '/') + "/" + artifactId + "/" + version).toLowerCase(Locale.ENGLISH);

        // Search in all project libraries
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            VirtualFile[] roots = rootManager.orderEntries().getAllLibrariesAndSdkClassesRoots();

            for (VirtualFile libraryRoot : roots) {
                String path = libraryRoot.getPath().toLowerCase(Locale.ENGLISH);

                // Match by Maven coordinates
                if (path.contains(dependencyPath)) {

                    // If it's a JAR file, get the JAR root
                    if (path.endsWith(".jar")) {
                        try {
                            com.intellij.openapi.vfs.JarFileSystem jarFileSystem =
                                    com.intellij.openapi.vfs.JarFileSystem.getInstance();
                            return jarFileSystem.getJarRootForLocalFile(libraryRoot);
                        } catch (Exception e) {
                            // Failed to access JAR
                        }
                    }
                    // If it's a directory, return it directly
                    return libraryRoot;
                }
            }
        }
        return null;
    }

    /**
     * Registers a new component annotation and creates a template for it
     *
     * @param fqn Fully qualified name of the annotation
     * @return true if the annotation was newly added, false if it already existed
     */
    public static boolean registerComponentAnnotation(String fqn) {
        return COMPONENT_ANNOTATIONS.add(fqn);
    }

    /**
     * Removes a component annotation
     *
     * @param fqn Fully qualified name of the annotation
     * @return true if the annotation was removed, false if it didn't exist
     */
    public static boolean unregisterComponentAnnotation(String fqn) {
        return COMPONENT_ANNOTATIONS.remove(fqn);
    }
    public static void addClassData(PsiClass psiClass, ClassData data) {
        classData.put(psiClass.getQualifiedName(), data);
    }

    public static ClassData getClassData(PsiClass psiClass) {
        return classData.get(psiClass.getQualifiedName());
    }

    public static void removeClassData(PsiClass psiClass) {
        classData.remove(psiClass.getQualifiedName());
    }

    public static Set<String> getComponentAnnotations() {
        return COMPONENT_ANNOTATIONS;
    }

    public static boolean isComponentClass(PsiClass psiClass) {
        for (String annotation : COMPONENT_ANNOTATIONS) {
            if (psiClass.getAnnotation(annotation) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isComponentAnnotation(@NotNull PsiAnnotation annotation) {
        for (String componentAnnotation : COMPONENT_ANNOTATIONS) {
            if (Objects.equals(annotation.getQualifiedName(), componentAnnotation)) {
                return true;
            }
        }
        return false;
    }

    public static PsiAnnotation[] getAnnotationArray(PsiAnnotation annotation, String propertyName) {
        PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            // Check if the attribute name is 'registerSubclasses'
            if (propertyName.equals(attribute.getName())) {
                PsiAnnotationMemberValue value = attribute.getValue();
                if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
                    //Array value
                    // Iterate over the array elements (class references)
                    PsiAnnotationMemberValue[] values = arrayValue.getInitializers();
                    PsiAnnotation[] annotations = new PsiAnnotation[values.length];
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] instanceof PsiAnnotation annotationValue) {
                            annotations[i] = annotationValue;
                        }
                    }
                    return annotations;
                }
            }
        }
        return new PsiAnnotation[0];
    }

    public static List<String> getClassArray(PsiAnnotation annotation, String propertyName) {
        List<String> subClasses = new ArrayList<>();
        PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            // Check if the attribute name is 'registerSubclasses'
            if (propertyName.equals(attribute.getName())) {
                PsiAnnotationMemberValue value = attribute.getValue();
                if (value instanceof PsiClassObjectAccessExpression objectValue) {
                    //Single value
                    subClasses.add(objectValue.getOperand().getType().getCanonicalText());
                }
                if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
                    //Array value
                    // Iterate over the array elements (class references)
                    PsiAnnotationMemberValue[] arrayElements = arrayValue.getInitializers();
                    for (PsiAnnotationMemberValue arrayElement : arrayElements) {
                        if (arrayElement instanceof PsiClassObjectAccessExpression classExpr) {
                            // Extract the class from the PsiClassObjectAccessExpression
                            PsiTypeElement typeElement = classExpr.getOperand();
                            String className = typeElement.getType().getCanonicalText();
                            subClasses.add(className);
                        }
                    }
                }
            }
        }
        return subClasses;
    }

    public static boolean isClassProvided(@Nullable PsiClass psiClass) {
        if (psiClass == null || psiClass.getQualifiedName() == null) {
            return false;
        }
        return classData.containsKey(psiClass.getQualifiedName()) || classData.values().stream().anyMatch(classData -> classData.isClassProvided(psiClass));
    }
}
