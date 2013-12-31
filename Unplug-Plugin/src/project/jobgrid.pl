#!ec-perl

# We need to use the EC API
use ElectricCommander;

# Define the size of the grid
my $nCols = 20;
my $nRows = 10;

# Query Commander for a list of jobs
my $ec = new ElectricCommander();
my $xp = $ec->getJobs({sortKey => 'createTime', sortOrder => 'descending',
		       maxResults => ($nCols * $nRows)});

# We got back an XML structure, process it so that we get a list
# of all of the <job> nodes in that structure
my @jobNodes = $xp->findnodes('//job');

# Start by defining the table
$XHTML .= "<table border=\"0\" cellspacing=\"1\">\n";

# Iterate over each row in the table
my $i = 0;
foreach my $r (1..$nRows) {

    # Start the row, and lock down the height
    $XHTML .= "<tr height=\"44px\">";

    # Now iterate over each cell in the row
    foreach my $c (1..$nCols) {

	# Fetch the XML node corresponding to this cell
	my $n = $jobNodes[$i++];

	# Handle the corner case where the grid is larger than the number
	# of jobs that exist in the system -- unlikely, but possible
	next unless($n);

	# We have a node -- extract some values from the XML
	my $ji = $xp->findvalue('jobId', $n);   # 8443
	my $js = $xp->findvalue('status', $n);  # completed
	my $jo = $xp->findvalue('outcome', $n); # success
	my $jn = CGI::escapeHTML($xp->findvalue('jobName', $n)); # arbitrary text!

	# Construct a hyperlink reference to this job's job details page for use later
	my $joblink = "<a href=\"/commander/link/jobDetails/jobs/$ji\" title=\"$jn\">";

	# Construct a URL reference to an image suitable for this job (use the
	# icons provided by ElectricCommander for this)
	my $icon = '/commander/lib/images/icn16px_';
	$icon .= 'running_' if ($js eq 'running');
	$icon .= $jo . '.gif';

	# Create an image tag using the URL we just built (if appropriate)
	my $v = "<img alt=\"$jo\" src=\"$icon\"/>" if ($js eq 'running' || $jo ne 'success');

	# Based on the job outcome, select the correct background color for this cell
	my $color = '';
	$color = 'bgcolor="red"'    if ($jo eq 'error');
	$color = 'bgcolor="green"'  if ($jo eq 'success');
	$color = 'bgcolor="yellow"' if ($jo eq 'warning');

	# Now $XHTML .= the cell tag and content (note that the content is itself a table,
	# so that we can control where the job number text and the icon appear)
	$XHTML .= "<td width=\"48px\" align=\"center\" $color>";
	$XHTML .= $joblink;
	$XHTML .= '<table border="0" align="center"><tr height="16px"><td align="center">';
	$XHTML .= $v;
	$XHTML .= '</td></tr><tr><td align="center">';
	$XHTML .= $ji;
	$XHTML .= '</td></tr></table></a></td>';
	
	# That's the end of this cell, on to the next
    }

    # That's the end of this row, on to the next
    $XHTML .= "</tr>\n";
}

# The table is complete, close the tag
$XHTML .= "</table>\n";

# Add some text at the bottom to identify the page.  Usually one might put this
# at the top of the page, but the whole point of this plugin prototype is to
# maximise the number of jobs displayed without having to scroll or deal with
# pagination, so it makes more sense to put the header at the bottom!
$XHTML .= "<center><h1>Job Status Grid</h1></center>\n";

# Done
# NB: This code is eval'd -- so do NOT call exit()!  Doing so will
# exit the entire plugin, not just this block of code, resulting in
# premature termination of the entire plugin.
