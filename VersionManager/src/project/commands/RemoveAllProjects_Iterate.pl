$[/myProject/perlHeader]

adminLogin();

my $artifacts = $ec->findObjects("artifact", {
    filter => [
        {
            "operator" => "equals",
            "propertyName" => "groupId",
            "operand1" => "VersionedProjects"
        },
        {
            "operator" => "isNull",
            "propertyName" => "ec_versioning_problem"
        }
    ]
});

foreach my $artifact($artifacts->findnodes("//artifact")) {
    my $name = $artifact->findvalue("description")->value();
    $ec->createJobStep({
        jobStepName => $name,
        subproject => "$[/myProject/projectName]",
        subprocedure => "RemoveProject",
        actualParameter => [{
            actualParameterName => "project_name",
            value => $name
        }]
    });
}
