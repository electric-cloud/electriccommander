$[/myProject/perlHeader]

my $name = getProperty("/myParent/project_name");
my $version = getProperty("/myParent/version");

$ec->abortOnError(0);

my $code = $ec->getProject($name)->findvalue("//code")->value();
if ($code eq "NoSuchProject") {
    error("Project '$name' doesn't exist");
}

my $key = getProperty("/projects/$name/ec_versioning_artifact");
if ($key eq "") {
    error("Project '$name' is not being versioned");
}

$code = $ec->getArtifact("VersionedProjects:$key")->findvalue("//code")->value();
if ($code eq "NoSuchArtifact") {
    error("Versioning artifact for project '$name' doesn't exist");
}

$code = $ec->getArtifactVersion("VersionedProjects:$key:$version")
    ->findvalue("//code")->value();
if ($code eq "NoSuchArtifactVersion") {
    error("No record of version $version for project '$name'");
}

if (getProperty("/artifacts/VersionedProjects:$key/ec_current_version") eq $version) {
    error("The rollback version number is the same as the current version number");
}

if (getProperty("/artifacts/VersionedProjects:$key/ec_pending_changes") eq "1") {
    error("Project '$name' has uncommitted pending changes");
}

$ec->abortOnError(1);
