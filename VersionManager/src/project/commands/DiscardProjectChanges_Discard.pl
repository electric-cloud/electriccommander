$[/myProject/perlHeader]

adminLogin();

my $name = getProperty("/myParent/project_name");
my $key = getProperty("/projects/$name/ec_versioning_artifact");
my $rollback_version = getProperty("/artifacts/VersionedProjects:$key/ec_current_version");

my $path = "$ws/$id/rollback_version";
mkpath($path);
$ec->retrieveArtifactVersions({
    artifactVersionName => "VersionedProjects:$key:$rollback_version",
    toDirectory => $path,
    repository => $repository
});
$ec->import("$path/project.xml", {
    force => 1,
    path => "/projects/$name"
});

# Set status properties.
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_pending_changes", 0);

# Reset the project ID in case it changed on import.
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_project_id", getProjectId($name));
