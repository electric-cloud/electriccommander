$[/myProject/perlHeader]

use File::Compare;

my $name = getProperty("/myParent/project_name");
my $key = getProperty("/projects/$name/ec_versioning_artifact");

# Compare the live version and the currently committed version.
my $live_path = "$ws/$id/live_version";
mkpath($live_path);
$ec->export("$live_path/project.xml", {
    path => "/projects/$name",
    relocatable => 1,
    excludeJobs => 1
});
my $current_path = "$ws/$id/current_version";
mkpath($current_path);
my $current_version = getProperty("/artifacts/VersionedProjects:$key/ec_current_version");
$ec->retrieveArtifactVersions({
    artifactVersionName => "VersionedProjects:$key:$current_version",
    toDirectory => $current_path
});

my $pending = compare(
    "$live_path/project.xml",
    "$current_path/project.xml"
);

# Set status properties.
$ec->setProperty("/artifacts/VersionedProjects:$key/ec_pending_changes", $pending);
