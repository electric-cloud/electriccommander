use strict;
use warnings;
use ElectricCommander;
use File::Path;

my $ec = new ElectricCommander({debug => 0});

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
    return $ec->getProperty($prop)->findvalue("//value")->value();
}

sub generateKey() {
	my $artifacts = $ec->findObjects("artifact", {
		filter => [
			{
				"operator" => "equals",
				"propertyName" => "groupId",
				"operand1" => "VersionedProjects"
			},
		],
		sort => [
			{
				"propertyName" => "artifactKey",
				"order" => "descending"
			}
		],
		numObjects => 1,
		maxIds => 1
	});
	my $lastKey = $artifacts->findvalue("//artifactKey")->value();
	return ($lastKey eq '') ? 1 : $lastKey + 1;
}

sub getProjectId($) {
    my ($name) = @_;
    return $ec->getProject($name)->findvalue("//projectId")->value();
} 

sub getProjectName($) {
    my ($id) = @_;
    return $ec->getObjects({objectId => "project-$id"})->findvalue("//projectName")->value();
}

sub adminLogin() {
    my $password = $ec->getFullCredential("admin", {value => "password"})
		->findvalue("//password")->value();
	if ($password eq "") {
		error("The admin user's password has not yet been stored");
	}
	$ec->login("admin", $password);
}

my $id = $::ENV{'COMMANDER_JOBSTEPID'};
my $ws = $::ENV{'COMMANDER_WORKSPACE'};
my $repository = getProperty("/myProject/settings/repository");
