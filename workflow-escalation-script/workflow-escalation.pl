# Script will output the runtime and active state for all incomplete workflows in a given project.
# if the workflow has been running longer than a user defined set of time in hours, then an
# escalation email is sent, notifyig the recipient of this.

use strict;
use Time::Local;
use Getopt::Long;
use ElectricCommander;
$| = 1;

my $project;
my $hours=10;
my $email;
my $emailconfig="gmail";
my $help;

sub usage {
    print "Usage: ec-perl escalation.pl --project <project name> --email <email address> --hours <n number of hours> --email-config\n";
    print "  --project:\t(required)Name of the project \n";
    print "  --email:\t(required)send notification to this email address\n";
    print "  --hours:\tsend notifications for workflows running n or more hours, default to 10\n";
    print "  --email-config:\tname of email configuration in commander to use, default to \"gmail\"\n";
    print "  --help:\tto see these options\n";
    close(STDOUT);
    exit(1);
}

my $ret = GetOptions ('project=s' => \$project,
            'email=s' => \$email,
            'email-config=s' => \$emailconfig,
            'hours=s' => \$hours,
            'help' => \$help);
usage() if (!$ret || !$project|| !$email ||$help);

my $ec = new ElectricCommander();
my $xPath = $ec->findObjects("workflow", {
    filter => {propertyName => "projectName",
               operator     => "equals",
               operand1     => "$project"},
    filter => {propertyName => "completed",
               operator     => "equals",
               operand1     => "0"}
    });

print "\n\n";
# my $string = $xPath->findnodes_as_string("//workflow[completed = 0]");
my $nodeset = $xPath->findnodes("//workflow[completed = 0]");
# print "Using \$xPath->findnodes->size, nodes size is " . $nodeset->size . "\n";

	my %month = ("Jan", 1, "Feb", 2, "Mar", 3, "Apr", 4, "May", 5, "Jun", 6, "July", 7, 
								"Aug", 8, "Sep", 9, "Oct", 10, "Nov", 11, "Dec", 12);
									

	my @currTime = split (/  | |:/, gmtime());
	# print "Current time (GMT): " . gmtime() . "\n";
	my $ctime = timelocal (@currTime[5], @currTime[4], @currTime[3], @currTime[2], $month{@currTime[1]}, @currTime[6]);
	# print "ctime (timelocal) =   $ctime\n";
	# print "Year =    @currTime[6]\n";
	# print "Month =   $month{@currTime[1]}\n";
	# print "Day =     @currTime[2]\n";
	# print "Hour =    @currTime[3]\n";
	# print "Minutes = @currTime[4]\n";
	# print "Seconds = @currTime[5]\n";
	# print "\n";
	
	
foreach my $node ($nodeset->get_nodelist) 
{

	# Find workflow name
	# my $workflow = $node->findnodes("//workflowName")->string_value();
	my $workflow = $node->findvalue("workflowName");

	# Find active state within the workflow
	my $activeState = $node->findnodes("activeState");

	# Find user who started the workflow
	my $launchedByUser = $node->findvalue("launchedByUser");
		
	# Find the time when the last action was taken on the workflow
	my $modifyTime = $node->findvalue("modifyTime");


	# Debug output
	print "Workflow:            $workflow\n";
	print "Recently finished:   $activeState\n";
	print "Modify time:         $modifyTime\n";
	print "Started by:          $launchedByUser\n";
	print "Email to:            $email\n";
	
	## Parse modifyTime so that we can compare with current time
	 my @t = split (/-|:|\.|T/, $modifyTime);
	# Debug Output
	 # print "Year is  : @t[0]\n";
	 # print "Month is : @t[1]\n";
	 # print "Day is   : @t[2]\n";
	 # print "Hour is  : @t[3]\n";
	 # print "Minute is: @t[4]\n";	 
	 # print "Second is: @t[5]\n";
	 # print "\n";

	# Time::Local::timelocal
	# protocol: timelocal ($sec, $min, $hrs, $day, $mon, $year);
	my $mtime = timelocal (@t[5], @t[4], @t[3], @t[2], @t[1], @t[0]);
	my $diff = $ctime - $mtime;
	
	print "******************** Last action taken $diff seconds ago *****************\n";
	
	if ($diff < 60*60*24*0.5) {
		# Less than 12 hrs have elapsed 
		print "************************* Less than 12 hrs have elapsed *******************\n";
		}
	elsif (($diff > 60*60*24*0.5) && ($diff < 60*60*24*1)) {
		# Less than 24 hrs have elapsed 
		print "************ More than 12 hrs but less than 24 hrs have elapsed ***********\n";
	}
	elsif (($diff > 60*60*24*1) && ($diff < 60*60*24*2)) {
		# More than 24 hrs have elapsed 
		print "*********** More than 24 hrs but less than 48 hrs have elapsed ************\n";
	}
	elsif (($diff > 60*60*24*2) && ($diff < 60*60*24*7)) {
		# More than 48hrs but less than 1 week have elapsed 
		print "*********** More than 48 hrs but less than 1 week have elapsed ************\n";
	}
	else {
		# More than 1 week have elapsed
		print "******************** More  than 1 week have elapsed ***********************\n";

	}

	# my $userRequestedDiff = $ec->getProperty ("/myJob/Age");
	
	my $userRequestedDiff = $hours * 60 * 60;
	# print "\$userRequestedDiff = $userRequestedDiff\n";
	if ($diff > $userRequestedDiff) {
		print "Workflow has been running for more than $hours hours, sending escalation email\n";
		emailUser($ec, $workflow, $activeState, $userRequestedDiff, $diff, $launchedByUser, $project, $email);
	}
	print "\n\n";
	
}

sub emailUser ($) 
{
	# my $ec = $_[0];
	# my $workflow = $_[1];
	# my $activeState = $_[2];
	# my $userRequestedDiff = $_[3];
	# my $diff = $_[4];
	# my $launchedByUser = $_[5];
	# my $nextAssignedUser = $_[6];
	# my $nextAssignedGroup = $_[7];
	my ($ec, $workflow, $activeState, $userRequestedDiff, $diff, $launchedByUser, $project, $email) = @_;

	my $diffHours = $diff / 3600;    # divided by 60 seconds and again by 60 minutes or divide by 3600
	$diffHours = sprintf "%2.0f", $diffHours;
		
	my $hostname = $ec->getProperty("/server/hostName")->findvalue('//value');
	# Send email using "sendEmail" API call.
        
	$ec->sendEmail({
				configName => "gmail",
				subject => "[ESCALATION]: ElectricCommander workflow \"$workflow\"needs your attention",
				to => $email,
				html => qq{
							<html>
							<body><font face="arial" color="black">
							<p>
							The following ElectricCommander workflow needs your attention. <br>
							This workflows has been waiting for a transition for $diffHours hrs.
							<p>
							Workflow: <A HREF="$hostname/commander/link/workflowDetails/projects/$project/workflows/$workflow">$workflow</A>
							\t$hostname/commander/link/workflowDetails/projects/$project/workflows/$workflow
							<p>
							Recently finished state: $activeState<br>
							<p>
							This workflow was started by: $launchedByUser<br>
							<p>
							</font>
							<hr>
							<font size=-1><i>This message was automatically sent by ElectricCommander.</i></font>
							</font>
							</body>
							</html>
						},
					});   # end of sendEmail

}
