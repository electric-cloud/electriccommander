#############################################################################
#
#  deleteArtifactVersions -- Script to delete artifacts and caches
#  Copyright 2013 Electric-Cloud Inc.
#
#  Author: Laurent Rochette
#  Date  : 2013-Jan-30
#
# History:
#
# Date         Who        Comment 
#----------------------------------------------------------------------------
# 
#############################################################################

use strict;

use ElectricCommander;
use DateTime;
$| = 1;

# Create a single instance of the Perl access to ElectricCommander
my $ec = new ElectricCommander();

#############################################################################
#
#  Assign Commander parameters to variables
#
#############################################################################
my $artifactProperty = "doNotDeleteThisArtifact";
my $timeLimit =10;
my $executeDeletion=0;

#############################################################################
#
#  Global Variables
#
#############################################################################

#############################################################################
#
#  Main
#
#############################################################################

printf("%s artifacts older than $timeLimit days (%s).\n", 
    $executeDeletion eq "true"?"Deleting":"Reporting", 
    calculateDate($timeLimit));

my ($success, $xPath) = InvokeCommander("SuppressLog", "findObjects", 
										"artifact", {sort => [ {propertyName => "groupId",
                                                    order => "ascending"} ]});
# Loop over artifacts
my $nodeset = $xPath->find('//artifact');
foreach my $node ($nodeset->get_nodelist) {
	my $artifactName=$xPath->findvalue('artifactName', $node);
	printf("%s\n", $artifactName);

	# create filterList
	my @filterList;
	push (@filterList, {"propertyName" => 'artifactName',
                    "operator" => "equals",
                    "operand1" => $artifactName});
	push (@filterList, {"propertyName" => "createTime",
 	                    "operator" => "lessThan",
 	                  	"operand1" => calculateDate($timeLimit)});
	push (@filterList, {"propertyName" => $artifactProperty,
                    "operator" => "isNull"});
	my ($success, $xPath) = InvokeCommander("SuppressLog", "findObjects", 
											"artifactVersion",
					 						{'filter' => \@filterList});
	my $versionset = $xPath->findnodes('//artifactVersion');
	foreach my $version ($versionset->get_nodelist) {
		#print $version->findnodes_as_string("/") . "\n";
		my $versionNumber=$version->findvalue('./artifactVersionName', $node);
		if ($executeDeletion eq "true") {
			 my ($success, $xPath) = InvokeCommander("SuppressLog", "deleteArtifactVersion", 
                      $versionNumber);
			printf("\tDeleting %s\n", $versionNumber);
		} else {
			printf("\t%s\n", $versionNumber);
		}

	}	
	printf("\n");
}
#############################################################################
#
#  Calculate the size of the workspace directory
#
#############################################################################
sub getDirSize {
  my $dir  = shift;
  my $size = 0;

  opendir(D,"$dir") || return 0;
  foreach my $dirContent (grep(!/^\.\.?/,readdir(D))) {
     my $st=stat("$dir/$dirContent");
     if (S_ISREG($st->mode)) {
       $size += $st->size;
     } elsif (S_ISDIR($st->mode)) {
       $size += getDirSize("$dir/$dirContent");
     }
  }
  closedir(D);
  return $size;
}

#############################################################################
#
#  Calculate the Date based on now and the number of days required by
#  the user before deleting jobs
#
#############################################################################
sub calculateDate {
    my $nbDays=shift;
    return DateTime->now()->subtract(days => $nbDays)->iso8601() . ".000Z";
}


#############################################################################
#
#  Return human readable size
#
#############################################################################
sub humanSize {
  my $size = shift();

  if ($size > 1099511627776) {    #   TB: 1024 GB
      return sprintf("%.2f TB", $size / 1099511627776);
  }
  if ($size > 1073741824) {       #   GB: 1024 MB
      return sprintf("%.2f GB", $size / 1073741824);
  }
  if ($size > 1048576) {          #   MB: 1024 KB
      return sprintf("%.2f MB", $size / 1048576);
  }
  elsif ($size > 1024) {          #   KiB: 1024 B
      return sprintf("%.2f KB", $size / 1024);
  }
                                  #   bytes
  return "$size byte" . ($size <= 1 ? "" : "s");
}


#-------------------------------------------------------------------------
#  Run an ElectricCommander function using the Perl API
#
#  Params
#       optionFlags - "AllowLog" or "SuppressLog" or "SuppressResult"
#                     combined with "IgnoreError"
#       commanderFunction
#       Variable Parameters
#           The parameters required by the ElectricCommander function
#           according to the Perl API. See the ElectricCommander
#           Help system for more information.
#               (the functions and paramenter are based on "ectool" - run it for documentation)
#
#  Returns
#       success     - 1 if no error was detected
#       xPath       - an XML::XPath object with the result.
#       errMsg      - a message string extracted from Commander on error
#
#-------------------------------------------------------------------------
sub InvokeCommander {

    my $optionFlags = shift;
    my $commanderFunction = shift;
    my $xPath;
    my $success = 1;

    my $bSuppressLog = $optionFlags =~ /SuppressLog/i;
    my $bSuppressResult = $bSuppressLog || $optionFlags =~ /SuppressResult/i;
    my $bIgnoreError = $optionFlags =~ /IgnoreError/i;

    #  Run the command
    # print "Request to Commander: $commanderFunction\n" unless ($bSuppressLog);

    $ec->abortOnError(0) if $bIgnoreError;
    $xPath = $ec->$commanderFunction(@_);
    $ec->abortOnError(1) if $bIgnoreError;

    # Check for error return
    my $errMsg = $ec->checkAllErrors($xPath);
    if ($errMsg ne "") {

        $success = 0;
    }
    if ($xPath) {

        print "Return data from Commander:\n" .
               $xPath->findnodes_as_string("/") . "\n"
            unless $bSuppressResult;
    }

    # Return the result
    return ($success, $xPath, $errMsg);
}