package net.vortexdevelopment.plugin.vinject.project;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import net.vortexdevelopment.plugin.vinject.utils.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class VInjectProjectTemplatesFactory extends ProjectTemplatesFactory {
    @NotNull
    @Override
    public String[] getGroups() {
        return new String[]{"VInject Minecraft Plugin"};
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(@Nullable String group, @NotNull WizardContext context) {
        return new ProjectTemplate[]{new ProjectGenerator()};
    }

    @Override
    public @Nullable Icon getGroupIcon(String group) {
        return PluginIcons.PLUGIN_ICON;
    }
}