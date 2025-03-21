package net.vortexdevelopment.plugin.vinject.project;

public class ProjectSettings {
    private String groupId = "com.example";
    private String artifactId = "myproject";
    private boolean includeApiModule = false;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public boolean isIncludeApiModule() { return includeApiModule; }
    public void setIncludeApiModule(boolean includeApiModule) { this.includeApiModule = includeApiModule; }
}

