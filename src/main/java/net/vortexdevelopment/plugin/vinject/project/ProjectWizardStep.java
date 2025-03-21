package net.vortexdevelopment.plugin.vinject.project;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.AbstractNewProjectWizardStep;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.observable.properties.PropertyGraph;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.Row;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.vortexdevelopment.plugin.vinject.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ProjectWizardStep extends AbstractNewProjectWizardStep {
    public static final Key<Boolean> INCLUDE_API_MODULE_KEY = Key.create("INCLUDE_API_MODULE");
    public static final Key<String> GROUP_ID_KEY = Key.create("GROUP_ID");
    public static final Key<String> ARTIFACT_ID_KEY = Key.create("ARTIFACT_ID");

    // Properties for the fields
    private final GraphProperty<String> locationProperty;
    private final GraphProperty<String> groupIdProperty;
    private final GraphProperty<String> artifactIdProperty;

    private JdkComboBox jdkComboBox;
    private final ProjectSdksModel sdksModel = new ProjectSdksModel();
    private WizardContext context;
    JBTextField locationField = new JBTextField("", 30);


    public ProjectWizardStep(@NotNull WizardContext context) {
        super(new NewProjectWizardStep() {
            @Override
            public @NotNull WizardContext getContext() {
                return context;
            }

            @Override
            public @NotNull PropertyGraph getPropertyGraph() {
                return new PropertyGraph();
            }

            @Override
            public @NotNull Keywords getKeywords() {
                return new Keywords();
            }

            @Override
            public @NotNull UserDataHolder getData() {
                return context;
            }
        });
        this.context = context;
        // Initialize properties
        groupIdProperty = getPropertyGraph().property("com.example");
        artifactIdProperty = getPropertyGraph().property("untitled");
        locationProperty = getPropertyGraph().property(System.getProperty("user.home"));

        // Add API module by default
        context.putUserData(INCLUDE_API_MODULE_KEY, true);

        // Initialize SDK model
        sdksModel.reset(context.getProject());

        // Sync properties with context
        updateContext();
    }

    @Override
    public void setupUI(@NotNull Panel builder) {
        builder.group("Project", true, new Function1<Panel, Unit>() {

            @Override
            public Unit invoke(Panel panel) {

                panel.row("Group ID:", new Function1<Row, Unit>() {
                    @Override
                    public Unit invoke(Row row) {
                        JBTextField textField = new JBTextField("com.example", 20);
                        row.cell(textField).onChanged(jbTextField -> {
                            // Validate Group ID
                            String groupId = jbTextField.getText();
                            if (!isValidGroupId(groupId)) {
                                // Handle invalid Group ID (you could show a message or disable a button)
                                // Example: set a red border on the text field (or show a message)
                                jbTextField.setBackground(JBColor.RED);
                            } else {
                                jbTextField.setBackground(JBColor.WHITE); // reset to default
                            }
                            groupIdProperty.set(groupId);
                            context.putUserData(GROUP_ID_KEY, groupId);
                            return Unit.INSTANCE;
                        });
                        return Unit.INSTANCE;
                    }
                });

                panel.row("Artifact ID:", new Function1<Row, Unit>() {
                    @Override
                    public Unit invoke(Row row) {
                        JBTextField textField = new JBTextField("untitled", 20);
                        row.cell(textField).onChanged(jbTextField -> {
                            // Validate Artifact ID
                            String artifactId = jbTextField.getText();
                            if (!isValidArtifactId(artifactId)) {
                                // Handle invalid Artifact ID
                                jbTextField.setBackground(JBColor.RED);
                            } else {
                                jbTextField.setBackground(JBColor.WHITE); // reset to default
                            }
                            artifactIdProperty.set(artifactId);
                            context.putUserData(ARTIFACT_ID_KEY, artifactId);
                            return Unit.INSTANCE;
                        });
                        return Unit.INSTANCE;
                    }
                });

                // Add this checkbox to your setupUI method:
                panel.row("Include API Module:", (Row row) -> {
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setSelected(true);
                    row.cell(checkBox)
                            .onChanged(selected -> {
                                context.putUserData(INCLUDE_API_MODULE_KEY, selected.isSelected());
                                return Unit.INSTANCE;
                            });
                    return Unit.INSTANCE;
                });

                //Location
                panel.row("Location:", new Function1<Row, Unit>() {
                    @Override
                    public Unit invoke(Row row) {
                        // Location text field
                        JBTextField locationField = new JBTextField();
                        row.cell(locationField);

                        // Create a file chooser button
                        JButton fileChooserButton = new JButton("Choose Location");

                        fileChooserButton.addActionListener(e -> {
                            // Create a FileChooserDescriptor (to select directories only)
                            FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
                            descriptor.setTitle("Select Location");

                            // Use IntelliJ's FileChooser to open the file picker dialog
                            VirtualFile selectedFile = FileChooser.chooseFile(descriptor, null, null);

                            if (selectedFile != null) {
                                // Set the selected directory path in the location field
                                locationField.setText(selectedFile.getPath());
                                locationProperty.set(selectedFile.getPath());
                                context.setProjectFileDirectory(new File(selectedFile.getPath(), artifactIdProperty.get()).toPath(), true);
                            }
                        });

                        row.cell(fileChooserButton);
                        return Unit.INSTANCE;
                    }
                });

                return Unit.INSTANCE;
            }
        });
    }

    private boolean isValidGroupId(String groupId) {
        // Define your validation logic for Group ID (e.g., regex or length check)
        return groupId != null && groupId.matches("[a-z]+\\.[a-z]+");
    }

    private boolean isValidArtifactId(String artifactId) {
        // Define your validation logic for Artifact ID
        return artifactId != null && !artifactId.trim().isEmpty();
    }

    private void onProjectNameChanged(String text) {
        context.setProjectName(text);
        updateArtifactId(text);
        updateLocation(text);
    }

    private void onLocationChanged(String text) {
        context.setProjectFileDirectory(Path.of(text), true);
    }

    private boolean isSuitableSdkType(SdkTypeId sdkTypeId) {
        return sdkTypeId.getName().contains("Java");
    }

    private void updateArtifactId(String projectName) {
        if (projectName != null && !projectName.isEmpty()) {
            String sanitized = projectName.toLowerCase().replaceAll("[^a-z0-9]", "");
            artifactIdProperty.set(sanitized);
            context.putUserData(ARTIFACT_ID_KEY, sanitized);
        }
    }

    private void updateLocation(String projectName) {
        if (projectName != null && !projectName.isEmpty()) {
            String newLocation = System.getProperty("user.home") + "/" + projectName;
            locationProperty.set(newLocation);
            context.setProjectFileDirectory(Path.of(newLocation), true);
        }
    }

    private void updateContext() {
        context.setProjectFileDirectory(Path.of(locationProperty.get()), true);
        context.putUserData(GROUP_ID_KEY, groupIdProperty.get());
        context.putUserData(ARTIFACT_ID_KEY, artifactIdProperty.get());
    }

    @Override
    public void setupProject(@NotNull Project project) {


        String groupId = context.getUserData(ProjectWizardStep.GROUP_ID_KEY);
        String artifactId = context.getUserData(ProjectWizardStep.ARTIFACT_ID_KEY);
        String groupIdLower = groupId.toLowerCase(Locale.ENGLISH);
        boolean includeApiModule = Boolean.TRUE.equals(context.getUserData(ProjectWizardStep.INCLUDE_API_MODULE_KEY));
        File baseDir = new File(locationProperty.get(), artifactId);


        //Create base directory if it doesn't exist
        if (!baseDir.exists()) {
            System.out.println("Creating directory: " + baseDir);
            baseDir.mkdirs();
        }
        System.out.println("Base directory: " + baseDir);

        try {
            String pluginYml = new String(Plugin.class.getResourceAsStream("/projectWizard/multi/plugin.yml").readAllBytes(), StandardCharsets.UTF_8);
            pluginYml = pluginYml.replace("$ARTIFACT_ID$", artifactId)
                    .replace("$MAIN$", groupIdLower + "." + artifactId.toLowerCase(Locale.ENGLISH) + "." + artifactId);

            // Generate project
            if (includeApiModule) {
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

        // After creating the project
//        SwingUtilities.invokeLater(() -> {
//            // Request focus back to IDE
//            Component parent = SwingUtilities.getWindowAncestor(locationField);
//            if (parent != null) {
//                parent.requestFocus();
//            }
//
//            // Use IntelliJ's APIs to open the project
//            ProjectUtil.openOrImport(baseDir.getAbsolutePath(), project, true);
//        });
    }
}