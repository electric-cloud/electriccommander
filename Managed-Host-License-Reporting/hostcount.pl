use ElectricCommander;
use DateTime;

$cmdr = ElectricCommander->new({
server      =>  "awdifk3vm-1",     
port        =>  "8000",       
securePort  =>  "8443",

});

$cmdr->login("admin", "admin");
$result = $cmdr->getDatabaseConfiguration();

# find number of jobs waiting
$jobs = $cmdr-> getJobs({status=>runnable});
#print "Return data from Commander:\n" .
#    $jobs->findnodes_as_string("/") . "\n";
$jobNodes = $jobs->find('//job');
$waitingJobs = $jobNodes->size();
print "Count of jobs waiting: $waitingJobs" . "\n";


# find hosts running job steps
$resources = $cmdr->getResourceUsage();
$hostsRunning = $resources->find('//resourceName');

foreach my $host($hostsRunning->get_nodelist)
{
  $hostName = $host->string_value;
  if ( $hostName ne 'local' )
  {
    if ( exists $hosts{$hostName} )
    {
      $hosts{$hostName}++;
    }
    else
    {
      $hosts{$hostName} = 1;
    }
   }
#  print "$hostName" . "\n";
}

$hostCount = keys %hosts;
print "Managed hosts in use:$hostCount" . "\n";
my $dt   = DateTime->now;   # Stores current date and time as datetime object
my $date = $dt->ymd;   # Retrieves date as a string in 'yyyy-mm-dd' format
my $time = $dt->hms;   # Retrieves time as a string in 'hh:mm:ss' format
my $jobSteps = $cmdr->findObjects("jobStep",
  {filter => [
    { propertyName => "status",
      operator => "equals",
      operand1 => "runnable"
    }
  ]});
$jobStepNodes = $jobSteps->find('//jobStep');
$waitingJobSteps = $jobStepNodes->size();
print "Count of jobSteps waiting: $waitingJobSteps" . "\n";

open HOSTCOUNT, ">>c:\\$date" . "_hostcount.txt";
print HOSTCOUNT "$date $time,$waitingJobs,$waitingJobSteps,$hostCount\n";
close HOSTCOUNT;

  
