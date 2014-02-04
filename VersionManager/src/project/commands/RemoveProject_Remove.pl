$[/myProject/perlHeader]

my $name = getProperty("/myParent/project_name");

my $key = getProperty("/projects/$name/ec_versioning_artifact");
$ec->deleteArtifact("VersionedProjects:$key");
