package $PACKAGE$;

import net.vortexdevelopment.vinject.annotation.Root;
import net.vortexdevelopment.vinject.annotation.TemplateDependency;
import net.vortexdevelopment.vortexcore.VortexPlugin;

@Root(
        packageName = "$PACKAGE$",
        createInstance = false,
        templateDependencies = {
                @TemplateDependency(groupId = "net.vortexdevelopment", artifactId = "VortexCore", version = "1.0.0-SNAPSHOT")
        }
)
public final class $CLASS_NAME$ extends VortexPlugin {

    @Override
    public void onPreComponentLoad() {

    }

    @Override
    public void onPluginLoad() {

    }

    @Override
    protected void onPluginEnable() {
    }

    @Override
    protected void onPluginDisable() {

    }
}
