#############################################################################
#
#  deleteJobs -- Script to delete jobs and workspaces
#  Copyright 2013 Electric-Cloud Inc.
#
#  Author: Laurent Rochette
#  Date  : 2013-Jan-23
#
# History:
#
# Date         Who        Comment 
#----------------------------------------------------------------------------
# 
#############################################################################

use strict;

use ElectricCommander;
use File::Path;
use File::stat;
use Fcntl ':mode';
use DateTime;
$| = 1;

# Create a single instance of the Perl access to ElectricCommander
my $ec = new ElectricCommander();

#############################################################################
#
#  Assign Commander parameters to variables
#
#############################################################################
my $jobProperty = "doNotDeleteThisJob";
my $timeLimit =90;
my $executeDeletion=0;

my $abortJobs="false";
my $errorJobs="true";
my $warningJobs="true";
my $goodJobs ="true";

#############################################################################
#
#  Global Variables
#
#############################################################################
my $version="0.0.1";
my $totalWksSize=0;          # Size of workspace files
my $totalNbJobs=0;           # Number of jobs to delete potentially
my $totalNbSteps=0;          # Number of steps to evaluate DB size
my $DBStepSize=10240;        # Step is about 10K in DB

#############################################################################
#
#  Main
#
#############################################################################

printf("%s jobs older than $timeLimit days (%s).\n", 
    $executeDeletion eq "true"?"Deleting":"Reporting", 
    calculateDate($timeLimit));

# create filterList
my @filterList;
push (@filterList, {"propertyName" => "status",
                    "operator" => "equals",
                    "operand1" => "completed"});
push (@filterList, {"propertyName" => "finish",
                    "operator" => "lessThan",
                    "operand1" => calculateDate($timeLimit)});
push (@filterList, {"propertyName" => $jobProperty,
                    "operator" => "isNull"});

if ($abortJobs eq "true") {
  push (@filterList, {"propertyName" => "abortStatus",
                      "operator" => "isNotNull"});
} else {
  push (@filterList, {"propertyName" => "abortStatus",
                      "operator" => "isNull"});
}

if ( ($errorJobs eq "true") || ($warningJobs eq "true") 
  || ($goodJobs eq "true")) {

  my @choices;
  push (@choices, "'error'")   if ($errorJobs   eq "true");
  push (@choices, "'warning'") if ($warningJobs eq "true");
  push (@choices, "'success'") if ($goodJobs    eq "true");
  push (@filterList, {"propertyName" => "outcome",
                       "operator" => "in",
                       "operand1" => join(',', @choices) });
} else {
  printf("You need to select at least of condition for Error, Warning or Successful Jobs!\n");
  exit 1;
}

my ($success, $xPath) = InvokeCommander("SuppressLog", "findObjects", "job",
                                        {filter => \@filterList ,
#                                         sort => [ {propertyName => "finish",
#                                                    order => "ascending"} ]
                                                    });

print "Search Status:\t$success\n";

# Check for the OS Type
my $osIsWindows = $^O =~ /MSWin/;

# Loop over all returned jobs
my $nodeset = $xPath->find('//job');
foreach my $node ($nodeset->get_nodelist) {
        $totalNbJobs++;
        my $wksSize;

        my $jobId = $xPath->findvalue('jobId', $node);
        my $jobName = $xPath->findvalue('jobName', $node);

        print "Job: $jobName\n";

        #
        # Find Abort Status and outcome
        printf("  Aborted: %s\n", $xPath->findvalue('abortStatus', $node)?"true":"false");
        printf("  Outcome: %s\n", $xPath->findvalue('outcome', $node));
        #
        # Find number of steps for the jobs
        my ($success, $xPath) = InvokeCommander("SuppressLog", "findJobSteps", 
                      {'jobId' => $jobId});
        my $nbSteps=scalar($xPath->findnodes('//object')->get_nodelist);
        $totalNbSteps += $nbSteps;
        printf("  Job steps: \t\t%d\n", $nbSteps);

        #  Find the workspaces (there may be more than one if some steps
        #  were configured to use a different workspace)
        my ($success, $xPath) = InvokeCommander("SuppressLog", "getJobInfo",
                                                 $jobId);
        my $wsNodeset = $xPath->find('//job/workspace');
        foreach my $wsNode ($wsNodeset->get_nodelist) {
            my $workspace;
            if ($osIsWindows) {
                $workspace = $xPath->findvalue('./winUNC', $wsNode);
                $workspace =~ s'/'\\'g;
            } else {
                $workspace = $xPath->findvalue('./unix', $wsNode);
            }

            print "  Workspace: \t\t$workspace\n" if ($workspace ne "");

            $wksSize = getDirSize($workspace);
            printf ("    Size: \t\t%s\n", humanSize($wksSize));
            $totalWksSize += $wksSize;
            if ($executeDeletion eq "true") {
                rmtree ([$workspace])  ;
                print "    Deleting Workspace\n";
            }
        }

        # Delete the job

        if ($executeDeletion eq "true") {
            InvokeCommander("SuppressLog", "deleteJob", $jobId) ;
            print "  Deleting Job\n\n";
        } 
}

printf("SUMMARY\n");
printf("Total number of jobs:  %d\n", $totalNbJobs);
printf("Total File size:       %s\n", humanSize($totalWksSize));
printf("Total number of steps: %d\n", $totalNbSteps);
printf("Total Database size:   %s\n", humanSize($totalNbSteps * $DBStepSize));

# Set Job properties if running inside a step
if ($ENV{COMMANDER_JOBSTEPID}) {
  $ec->setProperty("/myJob/numberOfJobs", $totalNbJobs);
  $ec->setProperty("/myJob/diskSpace", $totalWksSize);
  $ec->setProperty("/myJob/numbernumberOfSteps", $totalNbSteps);
}
exit(0);


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