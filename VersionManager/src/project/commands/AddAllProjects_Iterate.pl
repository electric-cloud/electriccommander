$[/myProject/perlHeader]

adminLogin();

my $projects = $ec->findObjects("project", {
    filter => [
        {
            "operator" => "isNull",
            "propertyName" => "pluginName"
        },
        {
            "operator" => "isNull",
            "propertyName" => "ec_versioning_artifact"
        }
    ],
    sort => [
        {
            "propertyName" => "projectName",
            "order" => "ascending"
        }
    ]
});

foreach my $project($projects->findnodes("//project")) {
    my $name = $project->findvalue("projectName")->value();
	$ec->createJobStep({
		jobStepName => $name,
		subproject => "$[/myProject/projectName]",
		subprocedure => "AddProject",
		actualParameter => [{
			actualParameterName => "project_name",
			value => $name
		}]
	});
}
