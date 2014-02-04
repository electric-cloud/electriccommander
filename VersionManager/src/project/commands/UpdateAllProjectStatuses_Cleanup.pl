$[/myProject/perlHeader]
use File::Path;

my $project = getProperty("/myJob/projectName");
my $procedure = getProperty("/myJob/procedureName");
my $jobs = $ec->findObjects("job", {
    filter => [
        {
            "operator" => "equals",
            "propertyName" => "projectName",
            "operand1" => $project
        },
        {
            "operator" => "equals",
            "propertyName" => "procedureName",
            "operand1" => $procedure
        },
        {
            "operator" => "equals",
            "propertyName" => "status",
            "operand1" => "completed"
        }
    ],
    maxIds => 100,
    numObjects => 100
});

my $thisJob = getProperty("/myJob/jobName");
foreach my $job($jobs->findnodes("//job")) {
    my $name = $job->findvalue("jobName")->value();
    if ($name ne $thisJob) {
        rmtree("$ws/../$name");
        $ec->deleteJob($name);
    }
}
