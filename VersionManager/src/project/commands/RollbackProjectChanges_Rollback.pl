$[/myProject/perlHeader]

use File::Compare;

my $name = getProperty("/myParent/project_name");
my $rollback_version = getProperty("/myParent/version");
my $comment = "Rolled back to v$rollback_version - " . getProperty("/myParent/comment");
my $key = getProperty("/projects/$name/ec_versioning_artifact");
my $current_version = getProperty("/artifacts/VersionedProjects:$key/ec_current_version");
my $new_version = $current_version + 1;

# Retrieve the rollback version and import it.
my $path = "$ws/$id/rollback_version";
mkpath($path);
$ec->retrieveArtifactVersions({
    artifactVersionName => "VersionedProjects:$key:$rollback_version",
    toDirectory => $path
});
$ec->import("$path/project.xml", {
    force => 1,
    path => "/projects/$name"
});

# Increment the version property and publish the current version as an artifact.
$ec->publishArtifactVersion({
    artifactName => "VersionedProjects:$key",
    version => $new_version,
    fromDirectory => $path,
    description => $comment
});
my $createTime = getProperty("/artifactVersions/VersionedProjects:$key:$new_version/createTime");

# Set status properties.
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_current_version", $new_version);
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_current_version_comment", $comment);
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_current_version_time", $createTime);
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_pending_changes", 0);

# Reset the project ID in case it changed on import.
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_project_id", getProjectId($name));
