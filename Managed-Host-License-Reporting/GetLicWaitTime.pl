use ElectricCommander;
$cmdr = ElectricCommander->new({
server      =>  "awdifk3vm-1",     
port        =>  "8000",       
securePort  =>  "8443",

});

$cmdr->login("admin", "admin");


my @filterList;
push (@filterList, {"propertyName" => "start",
                        "operator" => "greaterOrEqual",
                        "operand1" => "$ARGV[0]T00:00:00.000Z"});
push (@filterList, {"propertyName" => "finish",
                        "operator" => "lessOrEqual",
                        "operand1" => "$ARGV[0]T23:59:59.999Z"});
my $result = $cmdr->findObjects('job',
  {filter => [
    { operator => 'and',
      filter => \@filterList,
    }
  ]}
);
#print "result = " . $result-> findnodes_as_string("/") . "\n";
$jobsNodes = $result->find('//job');
#$jobNodes = $jobs->getNodeList;
$totJobs = $jobsNodes->size();
$licWaitJobs =0;
open LICWAIT, ">c:\\$ARGV[0]" . "_licwait.txt";
print LICWAIT "Job Name, Procedure Name, Licence Wait Time, Total Wait Time, License Percent of Wait, Total Elapsed Time,Licence Percent of Elapsed\n";

foreach my$job ($jobsNodes->get_nodelist)
{
  $jobName = $job->find('jobName')->string_value;
  $procName = $job->find('procedureName')->string_value;
  $licWaitTime = $job->find('licenseWaitTime')->string_value;
  $totWaitTime = $job->find('totalWaitTime')->string_value;
  $elapsedTime = $job->find('elapsedTime')->string_value;
  
  if ( $licWaitTime > 0 )
  {
    $licWaitPercent = $licWaitTime / $totWaitTime;
	$licElapsedPercent = $licWaitTime / $elapsedTime;
	$licWaitJobs++;
    print LICWAIT "$jobName,$procName,$licWaitTime,$totWaitTime,$licWaitPercent,$elapsedTime,$licElapsedPercent\n";
  }
  
}
print LICWAIT ",,,,,,,$licWaitJobs,$totJobs\n";
close LICWAIT;