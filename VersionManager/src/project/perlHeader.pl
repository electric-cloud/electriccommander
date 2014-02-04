use strict;
use warnings;
use ElectricCommander;
use File::Path;

my $ec = new ElectricCommander({debug => 0});

my $id = $::ENV{'COMMANDER_JOBSTEPID'};
my $ws = $::ENV{'COMMANDER_WORKSPACE'};

sub error($) {
    my ($msg) = @_;
    $ec->setProperty("summary", $msg);
    $ec->setProperty("/myParent/failureReason", $msg);
    exit 1;
}

sub warning($) {
    my ($msg) = @_;
    $ec->setProperty("summary", $msg);
    $ec->setProperty("/myJobStep/outcome", "warning");
    $ec->setProperty("/myParent/failureReason", $msg);
    exit 0;
}

sub getProperty($) {
    my ($prop) = @_;
    return $ec->getProperty($prop)->findvalue("//value")->string_value;
}

sub generateKey() {
    return $ec->incrementProperty("/plugins/@PLUGIN_KEY@/project/artifactCounter")->findvalue("//value")->string_value;
}

sub getProjectId($) {
    my ($name) = @_;
    return $ec->getProject($name)->findvalue("//projectId")->string_value;
} 

sub getProjectName($) {
    my ($id) = @_;
    return $ec->getObjects({objectId => "project-$id"})->findvalue("//projectName")->string_value;
}
