#!ec-perl

#############################################################################
#
#  Unplug and flot module interface to extract
#  Preflight Data
#############################################################################

use ElectricCommander;
use DateTime;

$XHTML = << 'ENDOFHEADER';
$[/server/@PLUGIN_KEY@/lib/use-flot]
ENDOFHEADER

my $timeLimit =  21;    # Get the last n days jobs
my $MAXJOBS   = 5000;
my %buildResult;
my %buildTime;
my %colors=(
    'success' => "green",
    'error'   => "red",
    'warning' => "orange"
    );

my $ec = new ElectricCommander({format=>'json'});

# create filterList
my @filterList;

# only finished jobs
push (@filterList, {"propertyName" => "status",
                    "operator" => "equals",
                    "operand1" => "completed"});

# older than
push (@filterList, {"propertyName" => "finish",
                    "operator" => "greaterThan",
                    "operand1" => calculateDate($timeLimit)});

my $res= $ec->findObjects("job", {maxIds => $MAXJOBS, 
                               numObjects => $MAXJOBS,
                               filter => \@filterList ,
                               sort => [ {propertyName => "finish",
                                          order => "ascending"} ]});

# Loop on job to extract outcome and create the table
                                          
foreach my $job ($res->findnodes('//job')) {
  my $name=$job->{jobName};
  my $outcome=$job->{outcome};
  my $date=$job->{finish};
  my $durationTime=$job->{elapsedTime};        # duration in ms

  $date =~ s/([\d\-]+)T.+/$1/;
  # $XHTML .= sprintf("%s: %s in %s\n", $name, $outcome, $date);
  $buildResult{$date}{$outcome} ++;
  if ($outcome eq "success") {
    $buildTime{$date} += $durationTime;
  }
}

# Create Build Time Line
$XHTML .= '<script type="text/javascript">
//<![CDATA[
$(function() {
';

#
# Generate Data points for each outcome
# 
foreach my $outcome ('success', 'error', 'warning') {
    # ccomma separated if not 1st
    $XHTML .= "var $outcome = [ ";
    my $counter=1;
    foreach my $date (sort keys %buildResult) {
        # ccomma separated if not 1st
        $XHTML .= ", " if ($counter != 1);
        $XHTML .= sprintf("[%d, %d]", $counter++, $buildResult{$date}{$outcome});
    }
    $XHTML .= " ];\n ";
}

#
# Generate Build Time Data
#
$XHTML .= 'var bldTime = [';
my $counter=1;
foreach my $date (sort keys %buildResult) {
    # ccomma separated if not 1st
    $XHTML .= ", " if ($counter != 1);
    $XHTML .= sprintf("[%d, %d]", $counter++, 
                $buildResult{$date}{'success'} ==0 ? 0: $buildTime{$date}/1000/$buildResult{$date}{'success'});
}
$XHTML .= " ];\n";

#
# Set options
#
$XHTML .= '    var options = {
        series: {
            bars: {
                align: "center", 
                barWidth: 0.7
            }
        },
        yaxes: [{position: "left"}, {position: "right"}],
        xaxis: {
            ticks:[ ';         
#
# Set Date as X-axis   
my $counter=1;
foreach my $date (sort keys %buildResult) {
    # comma separated if not 1st
    $XHTML .= ", " if ($counter != 1);
    $XHTML .= sprintf("[%d, \'%s\']", $counter++, $date);
}
$XHTML .= '        ]
        }
    };    
';

#
# Generate Graph itself
$XHTML .= '
    $.plot("#placeholder", [
                                {data: success, label: "Success", yaxis:1, bars :{ show: true, fill: 1, order: 1}, stack:true, color: "green"},
                                {data: error, label: "Failure", yaxis:1, bars :{ show: true, fill: 1, order: 2}, stack:true, color: "red"},
                                {data: warning, label: "Warning", yaxis:1, bars :{ show: true, fill: 1, order: 3}, stack:true, color: "orange"},
                                {data: bldTime, label: "Build Time (sec)", yaxis: 2, lines: { show: true}, points: {show: true}, color: "purple"}

                            ], options);
    // Add the Flot version string to the footer
    $("#footer").prepend("Flot " + $.plot.version + " &ndash; ");
});
//]]>
</script>
<div id="header">
    <h1>Job Status Chart</h1>
</div>
<div>
    <p>This is an example of charting using <A HREF="http://www.flotcharts.org/">Flot</A>, a pure Javascript plotting library.</p>
</div>
<div id="content">
    <div class="demo-container" style="
        box-sizing: border-box;
        width: 850px;
        height: 450px;
        padding: 20px 15px 15px 15px;
        margin: 15px auto 30px auto;
        border: 1px solid #ddd;
        background: #fff;
        background: linear-gradient(#f6f6f6 0, #fff 50px);
        background: -o-linear-gradient(#f6f6f6 0, #fff 50px);
        background: -ms-linear-gradient(#f6f6f6 0, #fff 50px);
        background: -moz-linear-gradient(#f6f6f6 0, #fff 50px);
        background: -webkit-linear-gradient(#f6f6f6 0, #fff 50px);
        box-shadow: 0 3px 10px rgba(0,0,0,0.15);
        -o-box-shadow: 0 3px 10px rgba(0,0,0,0.1);
        -ms-box-shadow: 0 3px 10px rgba(0,0,0,0.1);
        -moz-box-shadow: 0 3px 10px rgba(0,0,0,0.1);
        -webkit-box-shadow: 0 3px 10px rgba(0,0,0,0.1);">
        <div id="placeholder" class="demo-placeholder" style="width:100%;height:100%;font-size:14px;line-height:1.2em;"></div>
    </div>
</div>';

#
# Show a table as well
$XHTML .= '
<h2>Raw Data</h2>
<div>
    <p>The raw data associated with the graph above.</p>
</div>

<TABLE border="3"><TR><TH>Date</TH><TH>Success</TH><TH>Fail</TH><TH>Warning</TH><TH>Build Time (sec)</TH></TR>';
my %total=('succes'=>0,'error'=>0,'warning'=>0);
foreach my $date (sort keys %buildResult) {
  $total{'succes'}  += $buildResult{$date}{'success'};
  $total{'error'}   += $buildResult{$date}{'error'};
  $total{'warning'} += $buildResult{$date}{'warning'};
  $XHTML .= sprintf("<TR><TD>%s</TD><TD>%d</TD><TD>%d</TD><TD>%d</TD><TD ALIGN='right'>%d</TD></TR>\n", 
        $date, $buildResult{$date}{'success'}, $buildResult{$date}{'error'}, $buildResult{$date}{'warning'},
        $buildResult{$date}{'success'} ==0 ? "0": $buildTime{$date}/1000/$buildResult{$date}{'success'});
}
$XHTML .= sprintf("<TR><TH>Total</TH><TH>%d</TH><TH>%d</TH><TH>%d</TH><TH> - </TH></TR>\n", $total{'succes'}, $total{'error'}, $total{'warning'});

$XHTML .= '</TABLE>';

#$ec->setProperty("/projects/Default/unplug/v_debug_flot.xhtml", $XHTML);

#############################################################################
#
#  Calculate the Date based on now minus the number of days 
#
#############################################################################
sub calculateDate {
    my $nbDays=shift;
    return DateTime->now()->subtract(days => $nbDays)->iso8601() . ".000Z";
}
