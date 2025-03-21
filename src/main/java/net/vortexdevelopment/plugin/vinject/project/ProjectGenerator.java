package net.vortexdevelopment.plugin.vinject.project;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.ide.util.projectWizard.EmptyModuleBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGeneratorBase;
import com.intellij.platform.ProjectTemplate;
import net.vortexdevelopment.plugin.vinject.Plugin;
import net.vortexdevelopment.plugin.vinject.utils.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

public class ProjectGenerator extends DirectoryProjectGeneratorBase<ProjectSettings> implements ProjectTemplate {

    @Override
    public @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return "Create a new VInject Minecraft plugin project";
    }

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return "Create VInject Minecraft Plugin";
    }

    @Override
    public Icon getIcon() {
        return PluginIcons.PLUGIN_ICON;
    }

    @Override
    public @NotNull AbstractModuleBuilder createModuleBuilder() {
        return new EmptyModuleBuilder() {
            @Override
            public ModuleType<?> getModuleType() {
                return ModuleTypeManager.getInstance().findByID("JAVA_MODULE");
            }

            @Override
            public void setupRootModel(@NotNull ModifiableRootModel rootModel) {
                // Do nothing, as we'll handle file creation in generateProject
            }
        };
    }

    @Override
    public @Nullable ValidationInfo validateSettings() {
        return null;
    }

    @Override
    public @Nullable Icon getLogo() {
        return PluginIcons.PLUGIN_ICON;
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull ProjectSettings settings, @NotNull Module module) {

        //wait for smart mode
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).waitForSmartMode();
        }

        String groupId = settings.getGroupId();
        String artifactId = settings.getArtifactId();
        String groupIdLower = groupId.toLowerCase(Locale.ENGLISH);


        try {
            String pluginYml = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/plugin.yml").readAllBytes(), StandardCharsets.UTF_8);
            pluginYml = pluginYml.replace("$ARTIFACT_ID$", artifactId)
                    .replace("$MAIN$", groupIdLower + "." + artifactId.toLowerCase(Locale.ENGLISH) + "." + artifactId);

            // Generate project
            if (settings.isIncludeApiModule()) {
                // Generate project with API module


                //Load templates from resources/projectWizard/multi/ - api.pom.xml, main.pom.xml, parent.pom.xml
                String parentPom = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/parent.pom.xml").readAllBytes(), StandardCharsets.UTF_8);
                String apiPom = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/api.pom.xml").readAllBytes(), StandardCharsets.UTF_8);
                String mainPom = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/main.pom.xml").readAllBytes(), StandardCharsets.UTF_8);

                //Replace placeholders in the templates
                parentPom = parentPom.replace("$GROUP_ID$", groupId).replace("$ARTIFACT_ID$", artifactId);
                apiPom = apiPom.replace("$GROUP_ID$", groupId).replace("$ARTIFACT_ID$", artifactId);
                mainPom = mainPom.replace("$GROUP_ID$", groupId)
                        .replace("$ARTIFACT_ID$", artifactId)
                        .replace("$GROUP_ID_SLASHES$", groupIdLower.replace(".", "/"))
                        .replace("$ARTIFACT_ID_LOWER$", artifactId.toLowerCase(Locale.ENGLISH));

                String apiClass = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/ApiClass.java").readAllBytes(), StandardCharsets.UTF_8);
                apiClass = apiClass.replace("$PACKAGE$", groupIdLower + "." + artifactId.toLowerCase(Locale.ENGLISH) + ".api")

                        .replace("$CLASS_NAME$", artifactId + "Api");

                String pluginClass = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/PluginClass.java").readAllBytes(), StandardCharsets.UTF_8);
                pluginClass = pluginClass.replace("$PACKAGE$", groupIdLower + "." + artifactId.toLowerCase(Locale.ENGLISH))
                        .replace("$CLASS_NAME$", artifactId);




                //Create directories for the project
                File base = new File(baseDir.getPath());

                //Write parent pom.xml
                File parentPomFile = new File(base, "pom.xml");
                parentPomFile.createNewFile();
                Files.write(parentPomFile.toPath(), parentPom.getBytes(StandardCharsets.UTF_8));

                //Create Artifactid-API directory
                //Create Artifactid-Plugin directory
                File apiDir = new File(base, artifactId + "-API");
                File pluginDir = new File(base, artifactId + "-Plugin");

                //Create directories
                apiDir.mkdirs();
                pluginDir.mkdirs();

                //Create API module files
                File apiPomFile = new File(apiDir, "pom.xml");
                apiPomFile.createNewFile();
                Files.write(apiPomFile.toPath(), apiPom.getBytes(StandardCharsets.UTF_8));

                //Create src/main/java directory for api
                File apiSrcMainJava = new File(apiDir, "src/main/java");
                apiSrcMainJava.mkdirs();

                //Create groupIdLower.artifactIdLower.api package
                File apiPackage = new File(apiSrcMainJava, groupIdLower + "/" + artifactId.toLowerCase(Locale.ENGLISH) + "/api");
                apiPackage.mkdirs();

                //Create java api file with ArtifactIdApi.java
                File apiFile = new File(apiPackage, artifactId + "Api.java");
                apiFile.createNewFile();
                Files.write(apiFile.toPath(), apiClass.getBytes(StandardCharsets.UTF_8));
                //Api done ---

                //Create Plugin module files
                File mainPomFile = new File(pluginDir, "pom.xml");
                mainPomFile.createNewFile();
                Files.write(mainPomFile.toPath(), mainPom.getBytes(StandardCharsets.UTF_8));

                //Create src/main/java directory for plugin
                File pluginSrcMainJava = new File(pluginDir, "src/main/java");
                pluginSrcMainJava.mkdirs();

                //Create groupIdLower.artifactIdLower package
                File pluginPackage = new File(pluginSrcMainJava, groupIdLower + "/" + artifactId.toLowerCase(Locale.ENGLISH));
                pluginPackage.mkdirs();

                //Create java main file with Main.java
                File pluginFile = new File(pluginPackage, artifactId + ".java");
                pluginFile.createNewFile();
                //Write plugin class to file
                Files.write(pluginFile.toPath(), pluginClass.getBytes(StandardCharsets.UTF_8));

                //Create resources directory
                File resources = new File(pluginDir, "src/main/resources");
                resources.mkdirs();

                //Copy plugin.yml
                File pluginYmlFile = new File(resources, "plugin.yml");
                pluginYmlFile.createNewFile();
                Files.write(pluginYmlFile.toPath(), pluginYml.getBytes(StandardCharsets.UTF_8));

            } else {
                // Generate project without API module
                //Load templates from resources/projectWizard/single/ - pom.xml, main.java
                String pom = new String(Plugin.class.getResourceAsStream("/projectWizard/single/main.pom.xml").readAllBytes(), StandardCharsets.UTF_8);
                pom = pom.replace("$GROUP_ID$", groupId)
                        .replace("$ARTIFACT_ID$", artifactId)
                        .replace("$GROUP_ID_SLASHES$", groupIdLower.replace(".", "/"))
                        .replace("$ARTIFACT_ID_LOWER$", artifactId.toLowerCase(Locale.ENGLISH));

                //Create directories for the project
                File base = new File(baseDir.getPath());

                //write pom.xml
                File pomFile = new File(base, "pom.xml");
                pomFile.createNewFile();
                Files.write(pomFile.toPath(), pom.getBytes(StandardCharsets.UTF_8));

                //Create src/main/java directory
                File srcMainJava = new File(base, "src/main/java");
                srcMainJava.mkdirs();

                //Create groupIdLower.artifactIdLower package
                File pluginPackage = new File(srcMainJava, groupIdLower + "/" + artifactId.toLowerCase(Locale.ENGLISH));
                pluginPackage.mkdirs();

                //Load main.java template
                String pluginClass = new String(Plugin.class.getResourceAsStream("/projectWizard/single/PluginClass.java").readAllBytes(), StandardCharsets.UTF_8);
                pluginClass = pluginClass.replace("$PACKAGE$", groupIdLower + "." + artifactId.toLowerCase(Locale.ENGLISH))
                        .replace("$CLASS_NAME$", artifactId);

                //Create java main file with pluginClass
                File pluginFile = new File(pluginPackage, artifactId + ".java");
                pluginFile.createNewFile();
                //Write plugin class to file
                Files.write(pluginFile.toPath(), pluginClass.getBytes(StandardCharsets.UTF_8));

                //Create resources directory
                File resources = new File(base, "src/main/resources");
                resources.mkdirs();

                //Copy plugin.yml
                File pluginYmlFile = new File(resources, "plugin.yml");
                pluginYmlFile.createNewFile();
                Files.write(pluginYmlFile.toPath(), pluginYml.getBytes(StandardCharsets.UTF_8));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
