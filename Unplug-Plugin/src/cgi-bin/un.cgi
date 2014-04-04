#!/bin/sh

exec "$COMMANDER_HOME/bin/ec-perl" -x "$0" "${@}"

#!perl

use strict;
use ElectricCommander;
use ElectricCommander::Util;
use XML::Parser;
use XML::XPath;
use URI::Escape;
use CGI;

use utf8;
ElectricCommander::initEncodings();

my $HTML = '';
my $XHTML = '';

# Can't get the CGI lib to fetch POSTDATA, so do it ourselves:
my $raw;
read(STDIN, $raw, $ENV{'CONTENT_LENGTH'});

# Create the CGI object we'll need later on.
my $cgi = new CGI;

# Extract the property value from the XML posted to our STDIN.
my $xp = new XML::XPath($raw);
my $v = $xp->findvalue('/responses/response[@requestId="v"]/property/value');

# Use some hueristics to try to guess file content.
my $c = ($v =~ m|^(.*?)$|ms)[0];

if ($c=~ m|^\s*\<.*xhtml.*\>|i) {
    # XHTML, pass through literally
    $XHTML = $v;

} elsif ($c=~ m|^\s*\<.*html.*\>|i) {
    # HTML, pass through literally
    $HTML = $v;

} elsif ($c=~ m|^\s*\#\!.*perl|i) {
    # Send standard output to the standard error stream
    # (which is normally the Apache log file for a plugin)
    open(TMPOUT, ">&STDOUT");
    open(STDOUT, '>&STDERR');
    # Compile and execute the Perl code
    eval "$v";
    # Test for any error messages from the eval
    if ($@) {
        $HTML = "<h1>Error</h1><p/><pre>" . 
                 CGI::escapeHTML($@) . "\n</pre>";
        $XHTML = '';
    }
    # Put standard output back the correct way
    open(STDOUT, ">&TMPOUT");

} elsif ($v) {
    # Normal (plain) text was provided, wrap in <pre> tags.
    # (Note: the leading space here is required for MS IE6)
    $HTML = " <pre>" . CGI::escapeHTML($v) . "</pre>\n";
}

# If XHTML, then test for well-formed-ness.
if ($XHTML) {
    my $p = XML::Parser->new(ErrorContext=>2);
    eval { $p->parse("<body>$XHTML</body>"); };
    if ($@) {
        $HTML = "<h1>Warning</h1><p/><pre>XHTML is not well-formed: " .
                 CGI::escapeHTML($@) . "\n</pre><hr>$XHTML";
        $XHTML = '';
    }
}

# Test for an empty document (usually unintended).
$HTML = "<h1>Warning</h1><p/><pre>Empty HTML document.\n</pre>"
    unless (($XHTML) || ($HTML));

# Print correct HTTP header, then the content, and the closing tags.
if ($HTML) {
    print $cgi->header('-type'=>'text/html', '-encoding'=>'UTF-8', getNoCache());
    print "<html><body>$HTML</body></html>\n";
} else {
    print $cgi->header('-type'=>'application/xhtml+xml', '-encoding'=>'UTF-8', getNoCache());
    print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    print "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"";
    print " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
    print "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">";
    print "<body>$XHTML</body></html>\n";
}

exit(0);
