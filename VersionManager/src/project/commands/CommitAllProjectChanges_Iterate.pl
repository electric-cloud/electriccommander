$[/myProject/perlHeader]

my $comment = getProperty("/myParent/comment");

my $artifacts = $ec->findObjects("artifact", {
    filter => [
        {
            "operator" => "equals",
            "propertyName" => "groupId",
            "operand1" => "VersionedProjects"
        },
        {
            "operator" => "equals",
            "propertyName" => "ec_pending_changes",
            "operand1" => "1"
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
        subprocedure => "CommitProjectChanges",
        actualParameter => [
        {
            actualParameterName => "project_name",
            value => $name
        },
        {
            actualParameterName => "comment",
            value => $comment
        }]
    });
}
