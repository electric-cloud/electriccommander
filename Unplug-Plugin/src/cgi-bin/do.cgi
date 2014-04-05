#!/bin/sh

exec "$COMMANDER_HOME/bin/ec-perl" -x "$0" "${@}"

#!perl

use strict;
use ElectricCommander;
use ElectricCommander::Util;
use URI::Escape;
use JSON;
use CGI;

use utf8;
ElectricCommander::initEncodings();

##############################################################################

# Establish defaults

my $err  = undef;	# Error message (undef if no error)
my $resp = undef;	# Response string to be returned
my $type = 'XML';	# Response type: "text", "XML", or "JSON"

##############################################################################

# Create global CGI Query object (will be needed by "exec" code)
$::query = new CGI;

# Extract and dispatch on the requested action
my $action = $::query->path_info();

##############################################################################

# Action: "exec" - run ec-perl CGI script from a property
if ($action =~ m|^/exec/(.+)$|) {
    my $pn = '/server/@PLUGIN_KEY@/' . $1;
    my $pv = callAPI('getProperty', 'text',
		     '/responses/response/property/value', $pn);
    if (! $err) {
	if ($pv) {
	    eval $pv;
	    # If the eval was sucessful, just return as we did an "exec"
	    exit(0) if (! $@);
	    # If we get here, there was a problem - fall through with error
	    $err = $@;
	} else {
	    $err = "Error: $pn: Empty property value to execute.";
	}
    }

##############################################################################

# Action: "japi" or "xapi" - call a Commander API and return results
} elsif ($action =~ m|^/([jx])api/(.+?)(/.*)?$|) {

    # Figure out required type (japi vs xapi)
    $type = ($1 eq 'j') ? 'JSON' : 'XML';

    # Extract the API to be called
    my $op = $2;

    # And finally the optional xpath query (applies only to xapi)
    my $xq = $3;
    $type = 'text' if ($xq);

    # Define hash and list to store arguments passed in via the query
    my @ap = ();
    my %ah = ();

    # Now iterate over all the arguments in the URL
    foreach my $pa ($::query->param()) {
	# Short form for an API with a single positional argument
	if ($pa eq 'arg') {
	    $ap[0] = $::query->param($pa);
	# Positional argument, e.g. "arg0=projX" or "arg3=checkoutStep"
	} elsif ($pa =~ m/^arg(\d+)$/) {
	    $ap[$1] = $::query->param($pa);
	# Optional or non-positional argument, e.g. "arg_maxResults=200"
	} elsif ($pa =~ m/^arg_(.+)$/) {
	    $ah{$1} = $::query->param($pa);
	}
    }

    # Parameters should be complete, assemble the arguments into a list
    my @a = @ap;
    push @a, \%ah;

    # Perform the operation.
    $resp = callAPI($op, $type,  $xq, @a);

##############################################################################

# Action: shortcut "getP" - get textual property value
} elsif ($action =~ m|^/getP(/.+)$|) {
    my $pn = $1;
    $type = 'text';
    $resp= callAPI('getProperty', 'text',
		   '/responses/response/property/value', $pn);

# Action: shortcut "getPnx" - get textual property value without expanding
} elsif ($action =~ m|^/getPnx(/.+)$|) {
    my $pn = $1;
    $type = 'text';
    $resp= callAPI('getProperty', 'text',
		   '/responses/response/property/value', $pn,
		   {'expand' => '0'});

# Action: shortcut "expand" - expand text string using expandString API
#     (TODO: This should be made to work using POST as well)
} elsif ($action =~ m|^/expand$|) {
    my $str = $::query->param('arg');
    $type = 'text';
    $resp= callAPI('expandString', 'text',
		   '/responses/response/value', $str);

##############################################################################

# Action: none of the above - oops.
} else {
    $err = "Invalid query: $action";
}

##############################################################################

# Return data to the client in the appropriate format with correct type

my $docType = 'application/XML';
$docType = 'application/JSON' if ($type eq 'JSON');
$docType = 'text/plain' if ($type eq 'text');


# If the user requested escaping, presumably they want text back
my $escapeHTML = $::query->param('escapeHTML');
if (($escapeHTML eq '1') || ($escapeHTML eq 'true')) {
    $resp = CGI::escapeHTML($resp);
    $docType = 'text/plain';
}

# Explicit document type always wins
my $responseType = $::query->param('responseType');
if ($responseType) {
    $docType = $responseType;
}

##############################################################################

# Done with the work - check if we have an error to return

if ($err) {

    # Debug: add the query objects list of parameters
    if (0) {
	my @names = $::query->param;
	$err .= "\nParams: @names\n";
	foreach my $p (@names) {
	    foreach my $pv (split('\0', $::query->param($p))) {
		$err .= "$p = $pv\n";
	    }
	}
    }

    # Return message in a format that makes sense for the doctype
    if ($docType eq 'text/HTML') {
	$resp = "<h2>Error</h2><pre>" . CGI::escapeHTML($err) . "\n</pre>";

    } elsif ($docType eq 'application/XML') {
	$resp = '<error>' . xmlQuote($err) . '</error>';

    } elsif ($docType eq 'application/JSON') {
	my $jh = {'error' => $err};
	$resp = encode_json $jh;

    } else {
	$resp = $err;
    }
}

##############################################################################

print $::query->header('-type'=>$docType,'-encoding'=>'UTF-8',getNoCache());
print $resp;
print "\n";

##############################################################################

# Done.

exit(0);

##############################################################################
#
# sub callAPI(<apiName>, <responseType>, <xquery> [, <apiArgs>...])
#
# apiName: Commander API, e.g. getProperty
# apiType: text string defining API and response type to use:
#	'JSON' - uses JSON API, returns textual JSON format,
#	'XML'  - uses XML API, by default returning textual XML
#       'text' - uses XML API and the supplied xquery string
# xquery: the xquery string used when 'text' is specified
#
# If an error is encountered, $err will contain the raw (unescaped) error
# text from Commander or from the XPath library, as the case may be.
#
sub callAPI() {
    my $apiName = shift;
    my $apiType = shift;
    my $xquery = shift;

    # String to return - XML, JSON, or value
    my $v = undef;

    # Arguments for the Commander session
    my %eh = ();
    $eh{'format'} = 'json' if ($apiType eq 'JSON');

    # Begin by opening a connection to Commander
    my $ec = undef;
    eval { $ec = ElectricCommander->new(\%eh); };
    if (! $@) {

	# Ok, that worked - now call the API
	my $xp = undef;
	eval { $xp = $ec->$apiName(@_); };
	if (! $@) {

	    # Success! Crunch the output based on requested return type
	    if ($apiType eq 'JSON') {
		$v = encode_json $xp;
	    } elsif ($xquery) {
		$v = $xp->findvalue($xquery);
	    } else {
		$v = $xp->findnodes_as_string('/');
	    }
	}
    };

    # Set error flag if something went wrong
    $err = $@ if ($@);

    # And return the requested string
    return $v;
}
#
##############################################################################
