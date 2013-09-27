#!ec-perl

# This is a "customType" plugin -- that is, it is designed to by called
# from the Commander UI in place of the designated Commander-provided page
# (as identified by the presence of a "customType" property on the
# target object).
#
# This mechanism results in a number of distinct operations involving URLs.
#
# The first is the original "action", usually from a user clicking on a
# link, such as the "Run Now" link in the Commander UI.  The URL sent by
# the browser in this case looks something like this:
#
#  https://cmdr/commander/link/runProcedure/projects/EC-Examples/procedures
#  /Build-Test-Release%20Template%20With%20Sample%20Errors?s=Projects
#
# This URL is sent to Commander, and processed.  Upon determining that
# the identified procedure has a valid reference to a plugin in its
# "customType" property, the URL loader part of the Commander plugin
# framework loads the plugin page itself.  In the case of a CGI plugin
# (such as this one), the URL looks something like this:
#
#  https://cmndr/commander/plugins/unplug-1.5/cgi-bin/un.cgi?s=Projects
#
# We can already note a problem here -- potentially valuable information
# (in this example, project and procedure name) have been lost.  One can
# easily hard-code this detail so that the plugin knows it, but that
# limits re-use a lot.  A better way can be used if we peek at the
# environment -- it turns out that in the above case, the HTTP_REFERER
# variable is set to the original URL.  Project and procedure can be
# easily extracted from that value.
#
# The URL may include some other information encoded into the query
# portion of the URL (the part after the "?" character).  Commander
# passes the UI tab and sub-tab using parameter "s" and "ss" respectively.
# It's also possible to find procedure parameter values encoded therein
# as well.
#
# So, armed with all this information, the plugin code can display a
# standard web form to collect the rest of the information necessary to
# run the procedure in question.  Keep in mind that once the form is
# sent to the browser, this code will exit -- and the user will fill in
# the form without any involvement from the server whatsoever. In the
# case of a GWT parameter page plugin, the GWT code will communicate
# directly to Commander in order to do the desired runProcedure operation.
# In our case, a simple CGI form, we're going to have a submit button
# to click on, which is going to gather up the content of the various
# fields on the form, add them as query parameters to the URL that we
# associated with the submit button, and send that to the server -- so
# the question is, what URL would that be?
#
# The logical answer is that we would send the request to ourselves.  We
# would add some logic to the plugin code to detect when this has been
# done, so that we don't paint the form for the user again, instead we
# do a runProcedure operation.  Based on the result we get back from the
# API, we can either display a diagnostic page (in case of error), or
# we can issue a redirect to send the user's browser to the job Details
# page.
#
# In this example, we will use a pair of query parameters  -- "unProject"
# and "unProcedure" -- to store the project and procedure information
# from the original HTTP_REFERER.  We will also assume that if all of
# the required procedure parameters are present in the URL as query
# parameters, then we perform the runProcedure operation, else we pain
# the web form to gather the missing information.

# Build a base URL to ourselves (for the form's submit button)
my $url = $cgi->referer();
$url =~ m|^(.*)(/pages/.*)\?|;
my $me = CGI::escapeHTML($1 . $2);
my $jobDetails = CGI::escapeHTML("$1/link/jobDetails/jobs");

# Commander uses these to keep track of the current tab and subtab
my $s       = $cgi->url_param('s');
my $ss      = $cgi->url_param('ss');

# We use these to keep track of the procedure to run.  If either one
# is not set, then assume we can get them from the HTTP_REFERER variable.
my $unProj  = $cgi->url_param('unProject');
my $unProc  = $cgi->url_param('unProcedure');
my $action  = $cgi->url_param('unAction');
unless ($unProj && $unProc) {
    my $referer = $cgi->referer();
    $referer =~ m|/link/(.+)/projects/(.+)/procedures/(.+)\?|;
    $action = uri_unescape($1);
    $unProj = uri_unescape($2);
    $unProc = uri_unescape($3);
    # Handle the odd case where we're running stand-alone, not as a
    # parameter page plugin -- for ease in testing and demonstration.
    unless ($unProc) {
	$unProj = 'EC-Examples';
	$unProc = 'Build-Test-Release Template With Sample Errors';
    }
}

# Get the query parameters corresponding to our procedure's parameters
my $branch  = $cgi->url_param('branch');
my $linux   = $cgi->url_param('linux');
my $solaris = $cgi->url_param('solaris');
my $windows = $cgi->url_param('windows');

# The runProcedure operation can also specify some other parameters
my $runNow  = $cgi->url_param('runNow');

# Test the required parameters to see if we have them all, if not, then
# paint the form to get the data from the user.

my $e = '';
my $missing = 0;

if ($windows) {
    $e .= "windows: Invalid value: \"$windows\"\n"
	if (($windows ne 'true') && ($windows ne 'false'));
} else {
    $missing++;
}

if ($solaris) {
    $e .= "solaris: Invalid value: \"$solaris\"\n"
	if (($solaris ne 'true') && ($solaris ne 'false'));
} else {
    $missing++;
}

if ($linux) {
    $e .= "linux: Invalid value: \"$linux\"\n"
	if (($linux ne 'true') && ($linux ne 'false'));
} else {
    $missing++;
}

if ($branch) {
    $e .= "branch: Invalid value: \"$branch\"\n"
	if (($branch ne '1.1') && ($branch ne '1.2') &&
	    ($branch ne '1.3') && ($branch ne '1.4'));
} else {
    $missing++;
}

if (!($e) && !($missing) && ($runNow)) {

    # We have complete (and valid) parameter values -- call the Commander
    # runProcedure API with the information.

    my $ec = ElectricCommander->new();

    # We'll do our own error handling (important for plugins)
    $ec->abortOnError(0);

    # Attempt to launch the job
    my $xp = $ec->runProcedure($unProj,
        {'procedureName' => $unProc,
	 'actualParameter' => [
	     {'actualParameterName'=>'branch',  'value'=>$branch},
	     {'actualParameterName'=>'solaris', 'value'=>$solaris},
	     {'actualParameterName'=>'windows', 'value'=>$windows},
	     {'actualParameterName'=>'linux',   'value'=>$linux},
	     ],
	});

    # Extract the error code from the XML response from Commander
    my $code = $xp->findvalue("//code")->string_value;

    if ($code) {
	# Get the actual error message for display
	my $message = $xp->findvalue("//message")->string_value;
	$e .= $message . "\n";
	# Add some debug data
	$e .= $xp->findnodes_as_string("/") . "\n";

    } else {
	# Find the job Id (for redirect)
	my $jobId = $xp->findvalue("//jobId")->string_value;
	# Print the script block to do the redirect, and just in case the
	# won't launch, add a manual link for the user as well.
        $XHTML .= <<EOXHTML;
<script type="text/javascript">
  window.location = "$jobDetails/$jobId"
</script>
<h1>Job Launched</h1>
jobId: <a href="$jobDetails/$jobId">$jobId</a>
EOXHTML
    }
}

if ($missing || $e || !($runNow)) {

    # We are here
    #  if we have missing or incorrect parameters passed in, or
    #  if we attempted the runProcedure() but it failed, or
    #  if "runNow" was not specified.

    $XHTML .= '<h1>Launch Build</h1>';
    $XHTML .= "<p/>\n";

    # Display the error message(s), if any.
    if ($e) {
	$XHTML .= "<h3>Error</h3>\n";
	my $emsg = CGI::escapeHTML($e);
	$emsg =~ s|\n|<br/>|g;
	$XHTML .= $emsg;
    }

    # Paint the web form for the user to complete.
    # Begin by escaping the values before we display them.
    $branch  = CGI::escapeHTML($branch);
    $windows = CGI::escapeHTML($windows);
    $solaris = CGI::escapeHTML($solaris);
    $linux   = CGI::escapeHTML($linux);
    $unProj  = CGI::escapeHTML($unProj);
    $unProc  = CGI::escapeHTML($unProc);
    $action  = CGI::escapeHTML($action);
    $ss      = CGI::escapeHTML($ss);
    $s       = CGI::escapeHTML($s);

    # Build the actual form.
    $XHTML .= "<form action=\"$me\" method=\"get\">";
    $XHTML .= '<input type="hidden" name="runNow" value="1"/>';
    $XHTML .= '<input type="hidden" name="s" value="' . $s . '"/>' if ($s);
    $XHTML .= '<input type="hidden" name="ss" value="' . $ss . '"/>' if ($ss);
    $XHTML .= '<input type="hidden" name="unAction" value="' . $action . '"/>' if ($action);
    $XHTML .= '<input type="hidden" name="unProject" value="' . $unProj . '"/>' if ($unProj);
    $XHTML .= '<input type="hidden" name="unProcedure" value="' . $unProc . '"/>' if ($unProc);
    $XHTML .= "Branch:  <input type=\"text\" name=\"branch\"  value=\"$branch\"/>";
    $XHTML .= "windows: <input type=\"text\" name=\"windows\" value=\"$windows\"/>";
    $XHTML .= "solaris: <input type=\"text\" name=\"solaris\" value=\"$solaris\"/>";
    $XHTML .= "linux:   <input type=\"text\" name=\"linux\"   value=\"$linux\"/>";
    $XHTML .= "<input type=\"submit\" value=\"Submit\"/></form>";

    # Paint the rest of the page -- normally this would be help text.
    $XHTML .= "<br/><hr/><h1>Debug Information</h1>\n";

    $XHTML .= '<h2>My URL Info</h2>';
    $XHTML .= "URL: " . CGI::escapeHTML($cgi->url()) . "<br/>\n";
    $XHTML .= "Referer: " . CGI::escapeHTML($cgi->referer()) . "<br/>\n";
    $XHTML .= "Original HTTP_REFERER information: $action($unProj, $unProc)<br/>\n";
    $XHTML .= "<p/>\n";

    $XHTML .= '<h2>Environment Variables</h2>';
    foreach my $e (sort(keys(%ENV))) {
	$XHTML .= CGI::escapeHTML("$e: $ENV{$e}") . "<br/>\n";
    }
    $XHTML .= "<p/>\n";

    $XHTML .= '<h2>Query Parameters</h2>';
    my @parms = $cgi->url_param();
    foreach my $p (sort(@parms)) {
	$XHTML .= CGI::escapeHTML("$p: " . $cgi->url_param($p)) . "<br/>\n";
    }

}



#Usage: runProcedure <projectName>
#	[--procedureName <procedureName>]
#	[--actualParameter <var1>=<val1> [<var2>=<val2> ...]]
#	[--scheduleName <scheduleName>]
#	[--credentialName <credentialName>]
#	[--userName <userName>]
#	[--password <password>]
#	[--credential <credName>=<userName> [<credName>=<userName> ...]]
#	[--priority <>]
#	[--destinationProject <destinationProject>]
#	[--pollInterval <pollInterval>]
#	[--timeout <timeout>]
