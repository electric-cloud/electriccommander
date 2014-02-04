$[/myProject/perlHeader]

adminLogin();

my $name = getProperty("/myParent/project_name");

$ec->abortOnError(0);

my $code = $ec->getProject($name)->findvalue("//code")->value();
if ($code eq "NoSuchProject") {
    error("Project '$name' doesn't exist");
}

if (getProperty("/projects/$name/ec_versioning_artifact") ne "") {
    error("Project '$name' is already being versioned");
}

$ec->abortOnError(1);
