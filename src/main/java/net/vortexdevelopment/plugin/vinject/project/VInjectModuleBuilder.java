package net.vortexdevelopment.plugin.vinject.project;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.AbstractNewProjectWizardBuilder;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.vortexdevelopment.plugin.vinject.Plugin;
import net.vortexdevelopment.plugin.vinject.utils.PluginIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class VInjectModuleBuilder extends AbstractNewProjectWizardBuilder {

    private WizardContext context;

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) {
    }

    @Override
    protected @NotNull NewProjectWizardStep createStep(@NotNull WizardContext wizardContext) {
        this.context = wizardContext;
        return new ProjectWizardStep(wizardContext);
    }

    @Override
    public @NotNull String getDescription() {
        return "Create a new VInject Minecraft Plugin project";
    }

    @Override
    public @NotNull String getPresentableName() {
        return "VInject Minecraft Plugin";
    }

    @Override
    public @NotNull Icon getNodeIcon() {
        return PluginIcons.PLUGIN_ICON;
    }
}
