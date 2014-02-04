$[/myProject/perlHeader]

my $name = getProperty("/myParent/project_name");
my $comment = "Initial commit for new project";
my $key = generateKey();

# Create the project.
my $description = getProperty("/myParent/project_description");
my $resource = getProperty("/myParent/default_resource");
my $workspace = getProperty("/myParent/default_workspace");
$ec->createProject($name, {
    description => $description,
    resourceName => $resource,
    workspaceName => $workspace
});

# Store a pointer to the artifact on the project (before exporting so it isn't
# picked up as a diff).
$ec->setProperty("/projects/$name/ec_versioning_artifact", $key);

# Export the project and publish an artifact version for it.
my $path = "$ws/$id/first_version";
mkpath($path);
$ec->export("$path/project.xml", {
    path => "/projects/$name",
    relocatable => 1,
    excludeJobs => 1
});
$ec->publishArtifactVersion({
    artifactName => "VersionedProjects:$key",
    version => 1,
    fromDirectory => $path,
    description => $comment
});
my $createTime = getProperty("/artifactVersions/VersionedProjects:$key:1/createTime");

# Set status properties.
my $base = "/artifacts/VersionedProjects:$key";
$ec->setProperty("$base/ec_current_version", 1);
$ec->setProperty("$base/ec_current_version_comment", $comment);
$ec->setProperty("$base/ec_current_version_time", $createTime);
$ec->setProperty("$base/ec_pending_changes", 0);
$ec->setProperty("$base/ec_project_id", getProjectId($name));
$ec->setProperty("$base/description", $name);
