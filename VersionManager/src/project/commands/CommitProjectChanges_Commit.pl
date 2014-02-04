$[/myProject/perlHeader]

use File::Compare;

my $name = getProperty("/myParent/project_name");
my $comment = getProperty("/myParent/comment");
my $schedule = getProperty("/myJob/scheduleName");
if ($schedule ne "") {
    $comment .= " [$schedule]";
} else {
    my $user = getProperty("/myJob/launchedByUser");
    $comment .= " [$user]";
}
my $key = getProperty("/projects/$name/ec_versioning_artifact");
my $current_version = getProperty("/artifacts/VersionedProjects:$key/ec_current_version");
my $new_version = $current_version + 1;

my $path = "$ws/$id/live_version";
mkpath($path);
$ec->export("$path/project.xml", {
    path => "/projects/$name",
    relocatable => 1,
    excludeJobs => 1
});
$ec->publishArtifactVersion({
    artifactName => "VersionedProjects:$key",
    version => $new_version,
    fromDirectory => $path,
    description => $comment
});
my $createTime = getProperty("/artifactVersions/VersionedProjects:$key:$new_version/createTime");

# Set status properties.
my $base = "/artifacts/VersionedProjects:$key";
$ec->setProperty("$base/ec_current_version", $new_version);
$ec->setProperty("$base/ec_current_version_comment", $comment);
$ec->setProperty("$base/ec_current_version_time", $createTime);
$ec->setProperty("$base/ec_pending_changes", 0);
