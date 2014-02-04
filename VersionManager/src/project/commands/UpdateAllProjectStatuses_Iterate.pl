$[/myProject/perlHeader]

my $artifacts = $ec->findObjects("artifact", {
    filter => [
        {
            "operator" => "equals",
            "propertyName" => "groupId",
            "operand1" => "VersionedProjects"
        }
    ]
});

foreach my $artifact($artifacts->findnodes("//artifact")) {
    my $name = $artifact->findvalue("description")->value();

    # Make sure the stored project name and ID are in sync.  If the name is out
    # of sync (i.e. it was renamed), use the ID to reset the name.  If the ID is
    # out of sync (e.g. upgrade to 5.0 changes with UUID changes, use the name
    # to reset the ID.
    $ec->abortOnError(0);
    my $key = $artifact->findvalue("artifactKey")->value();
    my $id = getProperty("/artifacts/VersionedProjects:$key/ec_project_id");
    my $nameForId = $ec->getObjects({objectId => "project-$id"})->findvalue("//projectName")->value();
    my $idForName = $ec->getProject($name)->findvalue("//projectId")->value();
    $ec->abortOnError(1);
    if ($nameForId ne "" && $nameForId ne $name) {
        $ec->setProperty("/artifacts/VersionedProjects:$key/description", $nameForId);
        $name = $nameForId;
    } elsif ($idForName ne "" && $idForName ne $id) {
        $ec->setProperty("/artifacts/VersionedProjects:$key/ec_project_id", $idForName);
        $id = $idForName;
    }
    
	# If both the name and ID don't resolve to an object, we have to assume the
	# project was deleted.
    if ($nameForId eq "" && $idForName eq "") {
        my $message = "Neither project name nor project id for artifact "
            . "'VersionedProjects:$key' resolve to an existing project";
        print "WARNING: $message\n";
        $ec->setProperty("/artifacts/VersionedProjects:$key/ec_versioning_problem", $message);
        next;
    }

	# If the project thinks it's being versioned by another artifact, something
	# is wrong.
    $ec->abortOnError(0);
	my $projectKey = getProperty("/projects/$name/ec_versioning_artifact");
    $ec->abortOnError(1);
    if ($key ne $projectKey) {
        my $message = "Project '$name' is versioned by 'VersionedProjects:$projectKey' "
            . "but 'VersionedProjects:$key' is also pointing to it";
        print "WARNING: $message\n";
        $ec->setProperty("/artifacts/VersionedProjects:$key/ec_versioning_problem", $message);
        next;
    }

    $ec->deleteProperty("/artifacts/VersionedProjects:$key/ec_versioning_problem");
	$ec->createJobStep({
		jobStepName => $name,
		subproject => "$[/myProject/projectName]",
		subprocedure => "UpdateProjectStatus",
		actualParameter => [{
			actualParameterName => "project_name",
			value => $name
		}]
	});
}
