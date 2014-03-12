my $projectName = "ProjectAsCode";

#!ec-perl
# project2code.pl
# Converts a Commander project into ProjectAsCode-ready set of files:
# - <projectName>-<stepName><.extension>
# - manifest.pl
# - project.xml.in

use ElectricCommander;
use strict;

$| = 1;

# TODO: remove old one if it exists
mkdir "project";

my $projectTemplate = "";
my $manifest = "";
my $ec = new ElectricCommander();

$manifest .= qq(\@files = \(
	['//project/propertySheet/property[propertyName="ec_setup"]/value', 'ec_setup.pl'],\n);

$projectTemplate .= qq(<?xml version="1.0" encoding="UTF-8"?>
<exportedData version="61" buildLabel="" buildVersion="4.2.0.0">
	<exportPath>/projects/\@PLUGIN_KEY@</exportPath>
	<project>
		<projectName>\@PLUGIN_KEY@</projectName>
		<description>TODO: plugin project description</description>
		<propertySheet>
			<property>
				<propertyName>ec_setup</propertyName>
				<expandable>0</expandable>
				<value>PLACEHOLDER</value>
			</property>
				<property>
				<propertyName>project_version</propertyName>
				<expandable>0</expandable>
				<value>\@PLUGIN_VERSION@</value>
			</property>
		</propertySheet>\n);

$projectTemplate .= qq(		<procedure>\n);
foreach my $procedure ($ec->getProcedures($projectName)
		->find("//procedureName")
		->get_nodelist) {
	my $procedureName = $procedure->string_value;
	$projectTemplate .= qq(			<procedureName>$procedureName</procedureName>\n);
	
	$projectTemplate .= qq(			<step>\n);
	foreach my $step ($ec->getSteps($projectName, $procedureName)
		->find("//stepName")
		->get_nodelist) {
		my $stepName = $step->string_value;
		my $command = $step->find("../command")->string_value;
		my $shell = $step->find("../shell")->string_value;
		$projectTemplate .= qq(				<stepName>$stepName</stepName>
				<command>PLACEHOLDER</command>
				<shell>$shell</shell>\n);
		# TODO: Create step file
		my $ext="";
		$ext = ".pl" if ($shell eq 'ec-perl' || $shell eq 'perl');
		my $commandFile = "$procedureName-$stepName${ext}";
		open (COMMAND, ">project/$commandFile") or die "$!\n";
		print COMMAND $command, "\n";
		close COMMAND;
		# TODO: update manifest
		$manifest .= qq(	['//project/procedure[procedureName="$procedureName"]/step[stepName="$stepName"]/command', '$procedureName-$stepName${ext}'],\n);
	} # step
	$projectTemplate .= qq(			</step>\n);
} # procedure
$projectTemplate .= qq(		</procedure>\n);

$projectTemplate .= qq(	</project>
</exportedData>\n);

$manifest .= qq(\););
open (MANIFEST, ">project/manifest.pl") or die "$!\n";
print MANIFEST $manifest, "\n";
close MANIFEST;

open (TEMPLATE, ">project/project.xml.in") or die "$!\n";
print TEMPLATE $projectTemplate, "\n";
close TEMPLATE;