$[/myProject/perlHeader]

my $name = getProperty("/myParent/project_name");

$ec->abortOnError(0);

my $code = $ec->getProject($name)->findvalue("//code")->value();
if ($code ne "NoSuchProject") {
    error("Project '$name' already exists");
}

if (getProperty("/projects/$name/ec_versioning_artifact") ne "") {
    error("Project '$name' is already being versioned");
}

$ec->abortOnError(1);
