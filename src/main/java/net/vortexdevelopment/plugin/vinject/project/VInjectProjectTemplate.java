package net.vortexdevelopment.plugin.vinject.project;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.ide.util.projectWizard.EmptyModuleBuilder;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class VInjectProjectTemplate implements ProjectTemplate {
    private final ProjectGenerator generator = new ProjectGenerator();
    private ProjectWizardStep settingsStep;

    @NotNull
    @Override
    public String getName() {
        return generator.getName();
    }

    @Nullable
    @Override
    public String getDescription() {
        return generator.getDescription();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return generator.getLogo();
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        return new EmptyModuleBuilder() {
            @Override
            public ModuleType<?> getModuleType() {
                return ModuleTypeManager.getInstance().findByID("JAVA_MODULE");
            }

            @Override
            public void setupRootModel(@NotNull ModifiableRootModel rootModel) {
                // The actual project generation will happen in the ProjectGenerator
                if (settingsStep != null) {
                    String path = rootModel.getProject().getBasePath();
                    if (path != null) {
//                        generator.generateProject(
//                                rootModel.getProject(),
//                                rootModel.getProject().getProjectFile().getParent(),
//                                settingsStep.getSettings(),
//                                rootModel.getModule()
//                        );
                    }
                }
            }
        };
    }

    @Override
    public @Nullable ValidationInfo validateSettings() {
        return null;
    }
}
