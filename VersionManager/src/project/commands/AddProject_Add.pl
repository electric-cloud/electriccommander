$[/myProject/perlHeader]

my $name = getProperty("/myParent/project_name");
my $user = getProperty("/myJob/launchedByUser");
my $comment = "Initial commit for previously existing project [$user]";
my $key = generateKey();

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
