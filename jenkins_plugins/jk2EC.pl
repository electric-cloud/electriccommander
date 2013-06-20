#! ec-perl

#############################################################################
#
#  jk2EC -- Script to import a Jenkins procedure ino Electric Commander
#  Copyright 2013 Electric-Cloud Inc.
#
#  Author: Laurent Rochette
#  Date  : 2013-Jan-07
#
# History:
#
# Date         Who        Comment 
#----------------------------------------------------------------------------
# Jan 11, 2013 lrochette  v0.0.2: add Scheduler (thanks to Erik for the code)
# Jan 15, 2013 lrochette  v0.0.3: add Maven steps
# Jan 28, 2013 lrochette  v0.0.4: add option to read file instead of URL
#                                 Rearchitectured the code
#                                 add Email notifier
#############################################################################

#
#  TODO
#		publisher hudson.plugins.postbuildtask.PostbuildTask

use strict;
use ElectricCommander;
use XML::XPath;
use XML::XPath::XMLParser;
require LWP::UserAgent;

$|=1;

#############################################################################
#
#  Global Variables
#
#############################################################################
my $version="0.0.4";
my $DEBUG=0;						# 1: error message 
									# 3: everything including progresses
									# 7: Return xpath value
my $JKBUILD="";						# Name of the JK build to import into EC

my $JKSERVER="jenkins";					# Jenkins Server Name
my $JKPORT=8080;					# Jenkins Default Port
my $JKUSER="";
my $JKPASSWORD="";
my $jkXML;							# result returned by Jenkins XML API
my $JKFILE="";

my $ECSERVER="ecmaster";			# Jenkins Server Name
my $ECPORT="";						# Jenkins Default Port
my $ECUSER="admin";
my $ECPASSWORD="changeme";
my $ECPROCEDURE="";
my $ECPROJECT="";
my $ECARTKEY="";
my $ECARTGROUP="";

my $ec=new ElectricCommander();		# my EC instance
my $xpath;							# Used to get resulot back from EC
my $res;
my $errMsg;
my $errCode;
my $code;							# error code for EC API
my $counter;						# Loop index
my $url;
my $ua;
my $req;

#############################################################################
#
#  Usage
#
#############################################################################
sub usage {

    print "
Usage: jk2EC [options] BUILD
       jk2EC [-v] [-h]

DESCRIPTION
-----------
    Import a Jenkins build with with paramters and steps into Electric Commander. 
     
OPTIONS
-------

   -jkserver    server       Jenkins server from which to import
   -jkport      port         Jenkins Port (Default: 8080)
   -jkuser      User         Jenkins server user
   -jkpassword  Password     Jenkins server password

   -ecserver    Server       EC server to import to
   -ecuser      User         EC server username
   -ecpassword  Password     EC server password
   -ecproject   Project      EC project to store the Jenkins build (Default: Jenkins build)
   -ecprocedure Procedure    EC procedure to store Jenkins steps and parameters (Default: Jenkins build)
   -ecartifactkey Key        EC artifact Key
   -ecartifactgroup Grp      EC artifact Group

   -d level                  Debug Mode
   -v                        Output the version string
   -h                        Print this message
";

}

###############################################
# debugMsg
#
# print a message if debug level permits
#
# args
#   lvl  - the debug level for this message
#   msg  - the message to show
#
###############################################
sub debugMsg {
    my ($lvl, $msg) = @_;
    if ($DEBUG && ($lvl <= $DEBUG))  {
        print "$msg\n";
    }
}

# ParseRanges
#
# Process a single field from a cron-style schedule specification and populate
# an array indicating which values are "turned on" by the spec.
#
# Takes three arguments:  the low end of the range; the high end; and the
# spec itself.
#
# Returns an array of zero and one values.

sub ParseRanges {
    my($RangeLow,$RangeHigh,$spec) = @_;
    my @result;

    for (my $i = $RangeLow ; $i <= $RangeHigh ; ++$i) {
        $result[$i] = 0;
    }
    foreach my $part (split(',',$spec)) {
        my $start = $RangeLow;
        my $end   = $RangeHigh;
        my($val,$step) = split('/', $part);
        my($low,$high) = split('-', $val);

        if (!defined $step && !defined $high) {
            # Specific minute given.

            if ($low eq "*") {
                for (my $i = $RangeLow; $i <= $RangeHigh; ++$i) {
                    $result[$i] = 1;
                }
            } else {
                $result[$low] = 1;
            }
            next;
        }

        if (!defined $step) {
            $step = 1;
        }

        if (defined $high) {
            $start = $low;
            $end   = $high;

            if ($start < $RangeLow) {
                $start = $RangeLow;
            }
            if ($end > $RangeHigh) {
                $end = $RangeHigh;
            }
        }

        for (my $i = $start ; $i <= $end ; $i += $step) {
            $result[$i] = 1;
        }
    }
    return @result;
}

sub FirstNonzero {
    my($values,$start) = @_;
    for (my $i = $start ; $i < scalar @{$values} ; ++$i) {
        if (${$values}[$i] != 0) {
            return $i;
        }
    }
    return -1;
}

sub CountNonzeros {
    my($values) = @_;
    my $count = 0;
    for (my $i = 0 ; $i < scalar @{$values} ; ++$i) {
        ++$count if (${$values}[$i] != 0);
    }
    return $count;
}

sub FindInterval {
    my($values) = @_;

    my $first = FirstNonzero(\@{$values},0);
    return (0,-1,-1) if ($first == -1);

    my @tmp  = @{$values};
    my $next = FirstNonzero(\@tmp,$first + 1);
    return (0,-1,-2) if ($next == -1);

    my $step = $next - $first;
    my $last;

    for ($last = $first ; $last < scalar @tmp ; $last += $step) {
        if ($tmp[$last] != 1) {
            return (0,-1,-3);
        }
        $tmp[$last] = 0;
    }

    my $finished = (CountNonzeros(\@tmp) == 0);

    return ($finished,$step,$first,$last-1);
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
#		errCode     - the code part <code> of the XML
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
    my $errCode=$xPath->findvalue('//code',)->value();
    if ($errMsg ne "") {
        $success = 0;
    }
    if ($xPath) {
        print "Return data from Commander:\n" .
               $xPath->findnodes_as_string("/") . "\n"
            unless $bSuppressResult;
    }

    # Return the result
    return ($success, $xPath, $errMsg, $errCode);
}
#==============================================================================
# Main
#==============================================================================


#==============================================================================
# Parsing options
#==============================================================================

while (@ARGV) {
	$_ = shift(@ARGV);
	if ( /^-h/ || /^-H/ || /^-help/ ) {
		&usage;
		exit(0);
	} elsif ( /^-d/ || /^-debug/) {
		$_=shift(@ARGV);
		$DEBUG=$_;
		debugMsg(5, "DEBUG Mode ON");
	} elsif ( /^-version/ || /^-v$/ ) {
		printf("jk2EC v%s\n", $version);
		printf("Copyright 2013 Electric-Cloud Inc.\n");
		exit(0);
	} 
	#
	# Hidden option to replace URL by File
	#
	elsif ( /^-f/ || /^-file/) {
		$_=shift(@ARGV);
		if ($DEBUG) {
			$JKFILE = $_;
		} else {
			printf("jk2EC -f option requires DEBUG mode!\n");
			exit(1);
		}
	}
	#
	# Jenkins Options
	#
	elsif (/^-jkserver$/) {
		$_=shift(@ARGV);
		$JKSERVER = $_;
	} elsif (/^-jkport$/) {
		$_=shift(@ARGV);
		$JKPORT = $_;
	}  elsif (/^-jkuser$/) {
		$_=shift(@ARGV);
		$JKUSER = $_;
	}  elsif (/^-jkpassword$/) {
		$_=shift(@ARGV);
		$JKPASSWORD = $_;
	}
	#
	# EC Options
	#
	elsif (/^-ecserver$/) {
		$_=shift(@ARGV);
		$ECSERVER = $_;
	} elsif (/^-ecport$/) {
		$_=shift(@ARGV);
		$ECPORT = $_;
	}  elsif (/^-ecuser$/) {
		$_=shift(@ARGV);
		$ECUSER = $_;
	}  elsif (/^-ecpassword$/) {
		$_=shift(@ARGV);
		$ECPASSWORD = $_;
	} elsif (/^-ecproject$/) {
		$_=shift(@ARGV);
		$ECPROJECT = $_;
	} elsif (/^-ecprocedure$/) {
		$_=shift(@ARGV);
		$ECPROCEDURE = $_;
	} elsif (/^-ecartifactkey$/) {
		$_=shift(@ARGV);
		$ECARTKEY = $_;
	} elsif (/^-ecartifactgroup$/) {
		$_=shift(@ARGV);
		$ECARTGROUP = $_;
	}
	#
	# Error management
	#
	elsif (/^-/) {
		print "ERROR - unknown option '$_'\n";
		&usage;
		exit(1);
	} else {   
		$JKBUILD=$_;# Nothing for now
	}
}

#
# Verifying Parameters
#
if (($JKBUILD eq "") && ($JKFILE eq "")) {
	print "ERROR - no Jenkins build to import!\n";
	&usage;
	exit(1);		
}

if ($JKFILE ne "") {
	$JKBUILD = "Jenkins";
}

$ECPROJECT=$JKBUILD   if ($ECPROJECT   eq "");
$ECPROCEDURE=$JKBUILD if ($ECPROCEDURE eq "");
$ECARTGROUP=$JKBUILD  if ($ECARTGROUP  eq "");
$ECARTKEY=$JKBUILD    if ($ECARTKEY    eq "");

#
# Check Connection to EC
#
$xpath=$ec->login($ECUSER, $ECPASSWORD, {'server'=>$ECSERVER});
debugMsg(5, $xpath->findnodes_as_string("/"));
$code=$xpath->findvalue('//code',)->value();
if ($code ne "") {
	printf("Cannot log on the EC Server %s\n", $ECSERVER);
	printf("Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
	exit(1);
}

if ($JKFILE eq "") {
	#
	# Check Connection to Jenkins
	#

	if ("$JKSERVER" eq "") {
		printf("Jenkins server needs to be passed on as an option!\n");
		&usage;
		exit(1);
	}

	$url= "http://$JKSERVER:$JKPORT/job/$JKBUILD/config.xml";
	$req =  HTTP::Request->new(GET => $url);

	if ("$JKUSER" ne "") {
	  $req->authorization_basic($JKUSER, $JKPASSWORD);
	}
	$ua = LWP::UserAgent->new;
	$res=$ua->request($req);

	# Check the outcome of the response
	if ($res->is_success) {
		$jkXML=XML::XPath->new(xml => $res->decoded_content);
	} else {
	  printf("Could not connect to Jenkins server %s:%s configuration page\n", $JKSERVER, $JKPORT);
	  print $res->status_line, "\n";
	  exit 1;
	}
} else {
	# Simply load the file passed as argument
	$jkXML=XML::XPath->new(filename => $JKFILE);
}

#
# Create EC Project
#
my $jkDescription=$jkXML->findvalue("/project/description");
($res, $xpath,$errMsg, $errCode)=InvokeCommander("SuppressLog IgnoreError", 'createProject', $ECPROJECT, 
					{'description'=>"Created automatically by jk2EC script." . $jkDescription});
if ($res == 1) {
	printf("Project $ECPROJECT created successfully\n");
} else {
	if ($errCode eq "DuplicateProjectName") {
		printf("Project $ECPROJECT already exists\n");
	} else {
		printf("ERROR creating project %s\n", $ECPROJECT);
		printf("Code:%s. Message:%s\n", $errCode,$errMsg);
		exit(2);
	}
}

#
# Create Procedure
#
($res, $xpath,$errMsg, $errCode)=InvokeCommander("SuppressLog IgnoreError", 'createProcedure', $ECPROJECT, $ECPROCEDURE, 
						 {'description'=>"Procedure created automatically by jk2EC script."});
if ($res == 1) {
	printf("  Procedure $ECPROCEDURE in project $ECPROJECT created\n");
} else {
	if ($errCode eq "DuplicateProcedureName") {
		printf("  Procedure $ECPROCEDURE in project $ECPROJECT already exists\n");
	} else {
		printf("ERROR creating procedure %s in project %s\n", $ECPROCEDURE, $ECPROJECT);
		printf("Code:%s. Message:%s\n", $errCode,$errMsg);
		exit(2);
	}

}

#
# Create Parameters
#
my @jkParams = $jkXML->findnodes("/*/properties/*/parameterDefinitions/*");
foreach my $jkParam (@jkParams) {
	my $paramName=$jkParam->findvalue("./name");
	my $paramDesc=$jkParam->findvalue("./description");
	my $paramDefaultValue=$jkParam->findvalue("./defaultParameterValue/value");

	# Entry field
	printf("    Creating Parameter %s\n", $paramName);
	if ($jkParam->getName() eq "hudson.model.StringParameterDefinition") {
		($res, $xpath,$errMsg, $errCode)=InvokeCommander("SuppressLog IgnoreError", 'createFormalParameter',
												$ECPROJECT, "$ECPROCEDURE", $paramName,
												{'description'=>$paramDesc,
												 'defaultValue'=>$paramDefaultValue,
												 'type'=>'entry'
												 }
											);
	} elsif ($jkParam->getName() eq "hudson.model.BooleanParameterDefinition") {
		($res, $xpath,$errMsg, $errCode)=InvokeCommander("SuppressLog IgnoreError", 'createFormalParameter',
												$ECPROJECT, "$ECPROCEDURE", $paramName,
												{'description'=>$paramDesc,
												 'defaultValue'=>$paramDefaultValue,
												 'type'=>'checkbox'
												 }
											);

		$ec->setProperty("/projects/$ECPROJECT/procedures/$ECPROCEDURE/ec_customEditorData/parameters/$paramName/checkedValue", 1);
		$ec->setProperty("/projects/$ECPROJECT/procedures/$ECPROCEDURE/ec_customEditorData/parameters/$paramName/uncheckedValue", 0);
		$ec->setProperty("/projects/$ECPROJECT/procedures/$ECPROCEDURE/ec_customEditorData/parameters/$paramName/initiallyChecked", ($paramDefaultValue eq "true")?1:0);
	} elsif ($jkParam->getName() eq "hudson.model.ChoiceParameterDefinition") {
		($res, $xpath,$errMsg, $errCode)=InvokeCommander("SuppressLog IgnoreError", 'createFormalParameter',
												$ECPROJECT, "$ECPROCEDURE", $paramName,
												{'description'=>$paramDesc,
												 'defaultValue'=>$paramDefaultValue,
												 'type'=>'dropdown'
												 }
											);
		my @jkOptions=$jkXML->findnodes("/*/property/parameterDefinition/choice");
		$counter=1;
		foreach my $jkOption (@jkOptions) {
			my $optValue=$jkOption->string_value("");
			$ec->setProperty("/projects/$ECPROJECT/procedures/$ECPROCEDURE/ec_customEditorData/parameters/$paramName/options/option".$counter."/text", $optValue);
			$ec->setProperty("/projects/$ECPROJECT/procedures/$ECPROCEDURE/ec_customEditorData/parameters/$paramName/options/option".$counter."/value", $optValue);
			$counter++;				
		}
		$ec->setProperty("/projects/$ECPROJECT/procedures/$ECPROCEDURE/ec_customEditorData/parameters/$paramName/options/optionCount", $counter-1);
	} else {
		printf("WARNING: Jenkins parameter type %s not yet supported. Import manually\n",
				$jkParam->getName());
	
	}
}

#
# To be removed when all call are going through InvokeCommander
#
$ec->abortOnError(0);

#
# Schedule triggered builds
#
my $unconvertable = 0;
my $convertable = 0;
my $start=0;
my $end=0;

$counter=1;
foreach my $jkTrigger ($jkXML->findnodes("/project/triggers/*")) {
	#
	# Time trigger
	#
	if ( ($jkTrigger->getName() eq "hudson.triggers.TimerTrigger") ||
	 	($jkTrigger->getName() eq "hudson.triggers.SCMTrigger") ) {
		my $ecSchedule=$jkTrigger->findvalue("./spec");
		my @values=split(/\s+/, $ecSchedule);
	    my @minutes;
	    my @hours;

	    if (scalar @values == 0) {
	        next;
	    }

	    if (scalar @values != 5) {
	        $unconvertable++;
	        next;
	    }
	    $convertable++;

	    @minutes = ParseRanges(0,59,$values[0]);
	    @hours   = ParseRanges(0,23,$values[1]);
	    my @dom     = ParseRanges(1,31,$values[2]);
	    my @months  = ParseRanges(1,12,$values[3]);
	    my @dow     = ParseRanges(0, 7,$values[4]);
	    my @dayname = ("Sunday", "Monday", "Tuesday",
	                "Wednesday", "Thursday", "Friday", "Saturday");

	    # Days of the week allows both 0 and 7 for Sunday; let's consolidate those.
	    $dow[0] |= $dow[7];
	    $dow[7]  = 0;

	    my %counts;
	    $counts{"dom"}    = CountNonzeros(\@dom);
	    $counts{"dow"}    = CountNonzeros(\@dow);
	    $counts{"months"} = CountNonzeros(\@months);
	    $counts{"hours"}  = CountNonzeros(\@hours);
	    $counts{"minutes"}= CountNonzeros(\@minutes);

	    my @minInt  = FindInterval(\@minutes);
	    my @hourInt = FindInterval(\@hours);

	    if (($counts{"dom"} != 31 && $counts{"dow"} != 7) 
	        || $counts{"months"} != 12
	        || ($minInt[0] == 0 && $hourInt[0] == 0)) {
	        # Cron allows to specify both days of the month AND days of the week;
	        # the schedule will only trigger if BOTH conditions are met.
	        #
	        # Cron also allows you to specify the months during which a schedule
	        # will be active.
	        #
	        # Finally, cron allows you to specify just an arbitrary set of times
	        # at which the schedule will fire.
	        #
	        # Commander does not have a way (or at least, no convenient way) to
	        # express these scenarios.

	        $unconvertable++;
	        print STDERR "WARNING: unable to convert schedule '$_'\n";
	        next;
	    }

	    my $scheduleDays = "";
	    if ($counts{"dow"} != 7) {
	        $scheduleDays = "\n<weekDays>";
	        for (my $i = 0; $i < 7; ++$i) {
	            if ($dow[$i] == 1) {
	                $scheduleDays .= "$dayname[$i] ";
	            }
	        }
	        $scheduleDays .= "</weekDays>";
	    } elsif ($counts{"dom"} != 31) {
	        $scheduleDays = "\n<monthDays>";
	        for (my $i = 0; $i < 31; ++$i) {
	            if ($dom[$i] == 1) {
	                $scheduleDays .= sprintf("%d ", $i);
	            }
	        }
	        $scheduleDays .= "</monthDays>";
	    }

	    my %intervals;

	    if ($minInt[0]) {
	        # We'll make one schedule for each range of hours that we find.  If
	        # there are no ranges, then we'll end up with one schedule for each
	        # specified hour.

	        my $start = FirstNonzero(\@hours,0);
	        while ($start != -1) {
	            $end = $start;
	            while ($end < scalar @hours && $hours[$end] == 1) {
	                ++$end;
	            }

	            my $startTime = sprintf("%02d:%02d", $start, $minInt[2]);
	            my $stopTime  = "";

	            if ($end < 24) {
	                $stopTime = sprintf("%02d:%02d",
	                                    $end % 24, 
	                                    $minInt[3] % 60);
	            } else {
	                $stopTime = "23:59";
	            }
	            my $ecScheduleName="minuteSchedule".$counter++;

	            $xpath=$ec->createSchedule($ECPROJECT, $ecScheduleName,
	            				{'description'=>"Created automatically by jk2EC script.",
	            				 'interval'=>$minInt[1], 'intervalUnits'=>'minutes',
	            				 'projectName'=>$ECPROJECT, 'procedureName'=>$ECPROCEDURE,
	            				 'startTime'=>$startTime, 'stopTime'=>$stopTime});
				debugMsg(5,$xpath->findnodes_as_string("/"));
				$code=$xpath->findvalue('//code',)->value();
				if ($code ne "") {
					printf("  ERROR creating 'Minute' Schedule  %s\n", $ecScheduleName);
					printf("  Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
					exit(2);
				} 
				debugMsg(3, "  Schedule $ecScheduleName created");
	            # Find the start of the next range.

	            $start = FirstNonzero(\@hours,$end + 1);
	        }
	    } else {
	        # We'll make one schedule for each minute that we find, repeating after
	        # some number of hours.

	        my $min = FirstNonzero(\@minutes,0);
	        while ($min != -1) {
	            my $startTime = sprintf("%02d:%02d", $hourInt[2], $min);
            	my $stopTime  = sprintf("%02d:%02d", $hourInt[3], $min);

	            my $ecScheduleName="hourlySchedule".$counter++;
	            $xpath=$ec->createSchedule($ECPROJECT, $ecScheduleName,
	            				{'description'=>"Created automatically by jk2EC script.",
	            				 'interval'=>$hourInt[1], 'intervalUnits'=>'hours',
	            				 'projectName'=>$ECPROJECT, 'procedureName'=>$ECPROCEDURE,
	            				 'startTime'=>$startTime, 'stopTime'=>$stopTime});
				debugMsg(5,$xpath->findnodes_as_string("/"));
				$code=$xpath->findvalue('//code',)->value();
				if ($code ne "") {
					printf("  ERROR creating 'Hour' Schedule  %s\n", $ecScheduleName);
					printf("  Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
					exit(2);
				} 
				debugMsg(3, "  Schedule $ecScheduleName created");
	            # Find the start of the next range.

	            $min = FirstNonzero(\@minutes,$min + 1);
	        }
	    }
		if ($unconvertable > 0 || $convertable != 1) {		
			printf("WARNING: Unable to convert Schedule! Please contact support\n",
				$jkTrigger->getName());
		}
	}
	#
	# Other Triggers
	# NOT YET SUPPORTED
	#
	else {
		printf("WARNING: Jenkins trigger %s not yet supported. Please contact Support\n",
				$jkTrigger->getName());
	}

}

#
# Create Step for each builder
#
$counter=1;
foreach my $jkBuilder ($jkXML->findnodes("/project/builders/*")) {
	my $ecStepName="Step".$counter++;
	my %ecStepOptions=();	
	#
	# Linux or Windows Shell
	# Extract <command>
	#
	if ( ($jkBuilder->getName() eq "hudson.tasks.Shell" )    ||
		 ($jkBuilder->getName() eq "hudson.tasks.BatchFile") ) {
		my $ecCmd=$jkBuilder->findvalue("./command");
		$xpath=$ec->createStep($ECPROJECT, $ECPROCEDURE, "shell".$ecStepName,
						{'command'=>$ecCmd});
		debugMsg(5,$xpath->findnodes_as_string("/"));
		$code=$xpath->findvalue('//code',)->value();
		if ($code ne "") {
			printf("  ERROR creating Shell step %s\n", $ecStepName);
			printf("  Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
			exit(2);
		} 
		debugMsg(3, "    Step shell$ecStepName created");
	} 
	#
	# Ant Target
	#  exTract <targets>
	#  call EC-Ant plugin
	elsif ($jkBuilder->getName() eq "hudson.tasks.Ant" ) {
		my $ecTarget=$jkBuilder->findvalue("./targets");
		$xpath=$ec->createStep($ECPROJECT, $ECPROCEDURE, "ant".$ecStepName,
						{'subproject'=>"/plugins/EC-Ant/project",
						 'subprocedure'=>"runAnt",
						 'actualParameter'=> [{'actualParameterName'=>'target',
						 					   'value'=>$ecTarget}]
						 });
		debugMsg(5,$xpath->findnodes_as_string("/"));
		$code=$xpath->findvalue('//code',)->value();
		if ($code ne "") {
			printf("  ERROR creating Ant step %s\n", $ecStepName);
			printf("  Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
			exit(2);
		}
		debugMsg(3, "    Step ant$ecStepName created");
	}
		#
	# Maven Target
	#  exTract <targets>
	#  call EC-Maven plugin
	elsif ($jkBuilder->getName() eq "hudson.tasks.Maven" ) {
		my $ecTarget=$jkBuilder->findvalue("./targets");
		my $mavProperties=$jkBuilder->findvalue("./properties");
		my $ecAddOptions="";
		if ($mavProperties) {
			foreach my $option (split (" ", $mavProperties)) {
				my ($var,$val)=split('=', $option);
				$ecAddOptions .= "-D$var=$val ";
			}
		}
		$xpath=$ec->createStep($ECPROJECT, $ECPROCEDURE, "maven".$ecStepName,
						{'subproject'=>"/plugins/EC-Maven/project",
						 'subprocedure'=>"runMaven",
						 'actualParameter'=> [{'actualParameterName'=>'mavenCommand', 'value'=>$ecTarget},
						 					  {'actualParameterName'=>'workingdirectory', 'value'=>'.'},
						 					  {'actualParameterName'=>'additionalOptions', 'value'=>$ecAddOptions}
						 					  ]
						 });
		debugMsg(5,$xpath->findnodes_as_string("/"));
		$code=$xpath->findvalue('//code',)->value();
		if ($code ne "") {
			printf("  ERROR creating Maven step %s\n", $ecStepName);
			printf("  Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
			exit(2);
		}
		debugMsg(3, "    Step maven$ecStepName created");
	}

	#
	# Other Step types
	# NOT YET SUPPORTED
	#
	else {
		printf("WARNING: Jenkins Builder type %s not yet supported. Please contact Support\n",
				$jkBuilder->getName());
	}
}


#
# Jenkins publishers
# => Artifact
# => Mailer
foreach my $jkPublisher ($jkXML->findnodes("/project/publishers/*")) {
	my $ecStepName="artifactStep".$counter++;
	#
	# Artifact Archiver
	#
	if ($jkPublisher->getName() eq "hudson.tasks.ArtifactArchiver" ) {
		#
		# Create Artifact
		#
		$xpath=$ec->getArtifact($ECARTGROUP.":".$ECARTKEY);
		$code=$xpath->findvalue('//code',)->value();
		if ($code ne "") {
			if ($code eq "NoSuchArtifact") {
				debugMsg(3, "  Artifact $ECARTGROUP:$ECARTKEY does not exist yet! Creating it");
				$xpath=$ec->createArtifact($ECARTGROUP, $ECARTKEY,
					 	{'description'=>"Created automatically by jk2EC script"});
				$code=$xpath->findvalue('//code',)->value();
				if ($code ne "") {
					printf("ERROR creating artifact %s:%s\n", $ECARTGROUP, $ECARTKEY);
					printf("Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
					exit(2);
				} else {
					debugMsg(3, "  Artifact $ECARTGROUP:$ECARTKEY created successfully!");
				}			
			} else {
				printf("ERROR accessing artifact %s:%s\n", $ECARTGROUP, $ECARTKEY);
				printf("Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
				exit(2);
			}

		} else {
			debugMsg(3, "Artifact $ECARTGROUP:$ECARTKEY already exists");
		}

		#
		# Publish Artifact Version
		#
		my $ecIncludePatterns=$jkPublisher->findvalue("./artifacts");
		my $ecExcludePatterns=$jkPublisher->findvalue("./excludes");
		$xpath=$ec->createStep($ECPROJECT, $ECPROCEDURE, $ecStepName,
						{'subproject'=>"/plugins/EC-Artifact/project",
						 'subprocedure'=>"Publish",
						 'actualParameter'=> [{'actualParameterName'=>'artifactName',           'value'=>$ECARTGROUP.":".$ECARTKEY},
						 					  {'actualParameterName'=>'artifactVersionVersion', 'value'=>'1.0.0-$[jobId]'},
						 					  {'actualParameterName'=>'repositoryName',         'value'=>'default'},
						 					  {'actualParameterName'=>'includePatterns',        'value'=>$ecIncludePatterns},
						 					  {'actualParameterName'=>'excludePatterns',        'value'=>$ecExcludePatterns}
						 					 ]
						 });
		debugMsg(5,$xpath->findnodes_as_string("/"));
		$code=$xpath->findvalue('//code',)->value();
		if ($code ne "") {
			printf("  ERROR creating Artifact step %s\n", $ecStepName);
			printf("  Returned Code is %s:%s\n", $code, $xpath->findvalue('//message'));
			exit(2);
		}
		debugMsg(3, "    Step $ecStepName created");
	} 
	#
	# Mailer
	#
	elsif ($jkPublisher->getName() eq "hudson.tasks.Mailer") {
		my $jkMailerDest=$jkPublisher->findvalue("./recipients");
		($res, $xpath,$errMsg, $errCode)=InvokeCommander("SuppressLog IgnoreError", 'createEmailNotifier',
						"jenkinsNotifier", {'destinations' => $jkMailerDest,
	        								'eventType'    => "onCompletion",
       										'projectName' => $ECPROJECT, 'procedureName' => $ECPROCEDURE,
						'formattingTemplate' => "Subject: Job completion Notification: Job: $[/myJob/jobName] $[/myEvent/type]
Job: $[/myJob/jobName] $[/myEvent/type] at $[/myEvent/time]",}); 
		if ($res == 1) {
			printf("  Email notifier created\n");
		} else {
			{
				printf("ERROR creating email notifier\n");
				printf("Code:%s. Message:%s\n", $errCode,$errMsg);
				exit(2);
			}

		}

	} 
	#
	# Other Publisher types
	# NOT YET SUPPORTED
	#
	else {
		printf("WARNING: Jenkins publisher %s not yet supported. Please contact Support\n",
				$jkPublisher->getName());
	}
}
exit (0);

