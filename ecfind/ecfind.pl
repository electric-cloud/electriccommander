#!/usr/bin/env ec-perl
# An ec-perl utility to manage security and general configuration of 
# various Commander objects (projects, artifacts, and resources).
#
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

use strict;
use utf8;
use XML::XPath;
use ElectricCommander;
use Data::Dumper;
use File::Path;
use Term::ReadKey;
use Getopt::Long;

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

$::version = '0.3b';
$::debug = 0;
$| = 1;

# Define some vars used by GetOptions (because of use strict)
my $server;
my $user;
my $pw;
my $help;

my $object;
my $maxIds = 10000;
my $query;
my @selects;
my @sorts;

my $actionFail;
my $actionPrint;
my @actionSetP;
my @actionDeleteP;
my @actionEditP;
my @actionExec;
my @actionPerl;

# Now use the GetOptions API to parse the command line
my $result = GetOptions (
    "server=s"          => \$server,
    "user=s"            => \$user,
    "pw=s"              => \$pw,
    "help"              => \$help,
    "debug+"            => \$::debug,
    "object=s"          => \$object,
    "query=s"           => \$query,
    "select=s"          => \@selects,
    "sort=s"            => \@sorts,
    "maxIds=i"          => \$maxIds,
    "fail-if=s"         => \$actionFail,
    "print=s"		=> \$actionPrint,
    "setProperty=s"     => \@actionSetP,
    "editProperty=s"    => \@actionEditP,
    "deleteProperty=s"  => \@actionDeleteP,
    "exec=s"            => \@actionExec,
    "perl=s"            => \@actionPerl,
    );

if ($actionPrint) {
    $actionPrint = lc($actionPrint);
    $help++ if (($actionPrint ne 'id') &&
		($actionPrint ne 'xml') &&
		($actionPrint ne 'count') &&
		($actionPrint ne 'name'));
}

if ($actionFail) {
    $actionFail = lc($actionFail);
    $help++ if (($actionFail ne 'none') &&
		($actionFail ne 'any'));
}    

if ($help || (! $object) || (! $result)) {
    printHelp();
    print "(version $::version)\n";
    die "\n";
}

# Default action is to print the id
$actionPrint = 'id' unless (($actionPrint) ||
			    (@actionExec) ||
			    (@actionPerl) ||
			    (@actionSetP) ||
			    (@actionEditP) ||
			    (@actionDeleteP));

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

# Establish our connection to Commander
my $ec = ElectricCommander->new($server);
die "Error: Unable to establish connection to Commander server $server\n" unless ($ec);

# Login, if requested
if ($user) {
    unless ($pw) {
	print "$user password: ";
	eval { ReadMode( 'noecho' ); };
	chomp($pw = ($@ ? <STDIN> : ReadLine(0)));
	eval {ReadMode( 'normal' )};
	print "\n";
    }
    $ec->login($user, $pw);
    $ec->saveSessionFile();
}

# We do not wish to abort on errors at this point any longer
#$ec->abortOnError(0);

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Process the query argument and build the findObjects arglist

my %a = ();

# Set the max number of objects we'll be fetching
$a{'maxIds'} = $maxIds;

# Set the number of complete objects we need -- either none, or maxIds, based
# on the action the user has specified us to take on each selected object
my $full = (($actionPrint eq 'xml') ||
	    ($actionPrint eq 'name') ||
	    (@actionExec) ||
	    (@actionPerl));
$a{'numObjects'} = ($full) ? $maxIds : 0;

# Test for a special query string syntaxes
if ($query eq '-') {
    $query = '';
    while(<STDIN>) {
	chomp;
	$query .= $_;
    }
} elsif ($query=~m/^@(.*)$/) {
    my $xp = $ec->getProperty($1);
    $query = $xp ? $xp->find('/responses/response/property/value')->string_value() : undef;
}

# If a query was provided, process it and add it.  Note that a side effect
# of the query is that any properties used in the query are added to the list
# of properties to be returned in the full object.
if ($query) {
    my ($f, @s) = mkfilter($query);
    $a{'filter'} = [ $f ];
    push @selects, @s;
}

# Add the select list, but only if we are generating XML output
# (nothing else uses it, yet)
if ($actionPrint eq 'xml') {
    foreach my $s (@selects) {
	push @{$a{'select'}}, {"propertyName" => $s};
    }
}

# Add the sort list
foreach my $s (@sorts) {
    my $sdir = 'ascending';
    if ($s =~ m/^\-(.*)$/) {
	$s = $1;
	$sdir = 'descending';
    } elsif ($s =~ m/^\+(.*)$/) {
	$s = $1;
    }
    push @{$a{'sort'}}, {"propertyName" => $s, 'order' => $sdir};
};

print "a: " . Dumper(\%a) . "\n" if ($::debug);

# Now run the findObjects query itself
my $xp = $ec->findObjects($object, \%a);

print "findObjects:\n" . $xp->findnodes_as_string('/') . "\n" if ($::debug);

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Do something with the results

my $count = 0;

print "<?xml version=\"1.0\"?>\n<objects>\n" if ($actionPrint eq 'xml');

my $xquery = $full ? '/responses/response/object' : '/responses/response/objectId';
my $objectNodeSet = $xp->find($xquery);
foreach my $o ($objectNodeSet->get_nodelist()) {

    $count++;

    # All objects have an object Id -- fetch that appropriately
    my $objectId;
    if ($full) {
	$objectId = $xp->find('./objectId', $o)->string_value();
    } else {
	$objectId = $o->string_value();
    }
    print "objectId: $objectId\n" if ($::debug);
    print "$objectId\n" if ($actionPrint eq 'id');

    # Print the XML representation of the object, if requested
    print '    ' . XML::XPath::XMLParser::as_string($o) . "\n"
	if ($actionPrint eq 'xml');

    # Iterate over the object type and extract "interesting" properties,
    # storing them in a hash.  Currently these include intrinsics that
    # end in either "Name" or "Id", or contain the string "container".
    if ($full) {
	my %e = ();
#	my $nodelist = $xp->find('./' . $object . '/*', $o);
	my $nodelist = $xp->find('./*/*', $o);
	foreach my $node ($nodelist->get_nodelist()) {
	    my $n = $node->getName();
	    if (($n =~ m|Id$|) || ($n =~ m|Name$|) || ($n =~ m|container|)) {
		$e{$n} = $node->string_value();
		print "  $n: $e{$n}\n" if ($::debug);
	    }
	}
	$e{'objectId'} =  $objectId;

	my $objectName = $e{$object . 'Name'};
	# Special case -- some objects are unusual in terms of naming
	unless ($objectName) {
	    if (($object eq 'procedureStep') || ($object eq 'jobStep')) {
		$objectName = $e{'stepName'};
	    } elsif ($object eq 'emailConfig') {
		$objectName = $e{'configName'};
	    } elsif ($object eq 'emailNotifier') {
		$objectName = $e{'notifierName'};
	    } elsif ($object eq 'logEntry') {
		$objectName = $e{'logEntryId'};
	    } else {
		$objectName = '**unknown**';
	    }
	}

        # Print the object name if requested
	print "object name: $objectName\n" if ($::debug);
	print "$objectName\n" if ($actionPrint eq 'name');

        # In preparation for external commands, set environment variables based
        # on the saved "interesting" properties found earlier.
	foreach (keys %ENV) { delete $ENV{$_} if ($_ =~ m/^ECFIND_/); };
	foreach (keys %e) { $ENV{'ECFIND_' . $_} = $e{$_}; };

        # Handle an arbitrary external command execution
	foreach my $a (@actionExec) {
	    system($a);
	}

	foreach my $a (@actionPerl) {
	    eval($a);
	    die "Error: $@\neval($a)\n" if ($@);
	}

    }

    # Special actions (that do not require the full object) here:

    foreach my $a (@actionSetP) {
	if ($a =~ m/^\s*(.*?)\s*\=\s*(.*?)\s*$/) {
	    die "Error: property name must be provided in setProperty action: \"$a\"\n" unless ($1);
	    my $pn = $1;
	    my $v = $2;
	    $ec->setProperty($pn, $v, {'objectId' => $objectId}) unless ($::debug);
	    print "setProperty '$pn' '$v' --objectId '$objectId'\n" if ($::debug);
	} else {
	    die "Error: syntax error in setProperty action: \"$a\"\n";
	}
    }

    foreach my $a (@actionEditP) {
	if ($a =~ m/^\s*(.*?)\s*\=\~(.*?)$/) {
	    die "Error: property name must be provided in editProperty action: \"$a\"\n" unless ($1);
	    my $pn = $1;
	    my $e = $2;
	    $ec->abortOnError(0);
	    my $x = $ec->getProperty($pn, {'objectId' => $objectId,
					   'expand' => 'true'});
	    $ec->abortOnError(1);
	    my $v = $x ? $x->find('/responses/response/property/value')->string_value() : undef;
	    print "getProperty '$pn' --objectId '$objectId' returns: '$v'\n" if ($::debug);
	    eval '$v =~ ' . $e;
	    die "Error processing regular expression: $@\n" if ($@);
	    $ec->setProperty($pn, $v, {'objectId' => $objectId}) unless ($::debug);
	    print "setProperty '$pn' '$v' --objectId '$objectId'\n" if ($::debug);
	} else {
	    die "Error: syntax error in editProperty action: \"$a\"\n";
	}
    }

    foreach my $a (@actionDeleteP) {
	print "deleteProperty '$a' --objectId '$objectId'\n" if ($::debug);
	$ec->deleteProperty($a, {'objectId' => $objectId}) unless ($::debug);
    }

}

print "</objects>\n" if ($actionPrint eq 'xml');

print "$count\n" if ($actionPrint eq 'count');

my $status = 0;
$status = 1 if (($actionFail eq 'none') && ($count == 0));
$status = 1 if (($actionFail eq 'any') && ($count > 0));

exit($status);

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#



#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
#                          Query parser below                               #
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

# Main entrypoint: given a query, it returns the generated filter structure,
# and a list of properties referenced by the query (for use in "selects")
sub mkfilter($) {
    $::query = shift;
    $::p = 0;
    my @f = getExpr();
    while ($::p<length($::query) && (substr($::query,$::p,1) =~ /\s/)) {
	$::p++;
    }
    syntaxError("expected end of query") unless ($::p==length($::query));
    return(@f);
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub getExpr() {
    my %otab = (
	'and'			=> [1,-1], # boolean, arbitrary list
	'or'			=> [1,-1], # boolean, arbitrary list
	'not'			=> [1, 1], # boolean, unary
	'between'		=> [0, 2], # property, two operands
	'contains'		=> [0, 1], # property, one operand
	'equals'		=> [0, 1],
	'greaterOrEqual'	=> [0, 1],
	'greaterThan'		=> [0, 1],
	'in'			=> [0, 1],
	'lessOrEqual'		=> [0, 1],
	'lessThan'		=> [0, 1],
	'like'			=> [0, 1],
	'notEqual'		=> [0, 1],
	'notLike'		=> [0, 1],
	'isNotNull'		=> [0, 0], # property, no operands
	'isNull'		=> [0, 0],
	);
    my %filter = ();
    my %selects = ();
    my $op = getNextTokenD();
    syntaxError("\"$op\": unrecognized operator") unless defined($otab{$op});
    my $t = getNextTokenD();
    syntaxError("\"$t\": expected open parenthesis") unless ($t eq '(');
    $filter{'operator'} = $op;
    my ($is_boolean, $n) = @{$otab{$op}};
    if ($is_boolean) {
	print "Expecting boolean filter, with $n operands.\n" if ($::debug > 1);
	my @filterList = ();
	do {
	    my ($f, @s) = getExpr();
	    push @filterList, $f;
	    foreach (@s) { $selects{$_}++; };
	} while (($t = getNextTokenD()) eq ',');
	print "Obtained boolean filter list: " . Dumper(\@filterList) . "\n" if ($::debug > 1);
	$filter{'filter'} = \@filterList;
    } else {
	print "Expecting property filter, with $n operands.\n" if ($::debug > 1);
	my $pn = getNextTokenD();
	$filter{'propertyName'} = $pn;
	$selects{$pn}++;
	if ($n > 0) {
	    $t = getNextTokenD();
	    syntaxError("\"$t\": expected comma") unless ($t eq ',');
	    $filter{'operand1'} = getNextTokenD();
	}
	if ($n > 1) {
	    $t = getNextTokenD();
	    syntaxError("\"$t\": expected comma") unless ($t eq ',');
	    $filter{'operand2'} = getNextTokenD();
	}
	$t = getNextTokenD();
    }
    syntaxError("\"$t\": expected close parenthesis") unless ($t eq ')');
    print "Returning: " . Dumper(\%filter) . "Selecting: " .
	join(', ', keys(%selects)) . "\n\n" if ($::debug > 1);
    return (\%filter, keys(%selects));
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub getNextTokenD() {
    my $t = getNextToken();
    printf("p=%3d c=%1s t=%s\n",$::p,substr($::query,$::p,1),$t) if ($::debug > 2);
    return $t;
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# A subroutine that returns the next token from the query line.
# The query is assumed to be in a global named "$::query" and
# the current position in the string is held in "$::p"
sub getNextToken() {

    my $token = '';

    # start by removing leading whitespace
    while ($::p<length($::query) && (substr($::query,$::p,1) =~ /\s/)) {
	$::p++;
    }

    # ok, we have a non-space character, handle special case where the first
    # character we encounter is a special character.
    my $c = substr($::query,$::p,1);
    if ($c =~ /[\,\(\)]/) {
	$::p++;
	return $c;
    }

    # we have a normal token, so read it in.  Special handling is embedded
    # for handling of quoted strings.
    while ($::p<length($::query)) {
	$c = substr($::query,$::p,1);
        if ($c eq '"') {
	    $::p++;
	    while (($::p<length($::query)) &&
		   (($c=substr($::query,$::p++,1)) ne '"')) {
		$token .= $c;
	    }
        } elsif ($c eq '\'') {
	    $::p++;
	    while (($::p<length($::query)) &&
		   (($c=substr($::query,$::p++,1)) ne '\'')) {
		$token .= $c;
	    }
	} elsif ($c =~ /\s/) {
	    $::p++;
	    return $token;
	} elsif ($c =~ /[\,\(\)]/) {
	    return $token;
	} else {
	    $token .= $c;
	    $::p++;
	}
    }

    # if we fall through to here, we ran off the end of the string.
    return undef;
}
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub syntaxError {
    my $e = shift;
    die "Syntax Error: $e\n  '$::query'\n   " . ' ' x $::p . "^\n";
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub printHelp {
    my $helptext = <<END_OF_TEXT;
Usage: $0 [--server <host>] [--user <username>] [--pw <password>]
  general:
   --object <commander-object-type-name>
   [--query <query-expression> | @<property-path> | -]
   [--select <property>[,<property...]]
   [--sort <property>[,<property...]]
   [--maxIds <n>]
  actions:
   [--print id | xml | name | count]
   [--fail-if none | any]
   [--exec <command-or-script>]...
   [--setProperty <property>=<value>]...
   [--editProperty <property>=~s/<old>/<new>/[g]]...
   [--deleteProperty <property>]...
  debugging: [--debug]
  help: [--help]

Query Syntax:
  A query takes the form of a function call.  For example, the following query
will search for the project named "Default":

equals("projectName","Default")

  Certain functions ("and", "or", "not") take other functions as arguments,
allowing one to express complicated queries.  The following query will search
for all projects where the name contains "EC" and the property "ec-visibility"
is set to the value "all":

and(contains("projectName","EC"),equals("ec-visibility","all"))

Functions can be nested arbitrarily deeply.  Property names can refer to either
intrinsic properties, or properties added to the top-level of the object in
question (more specifically, one cannot search for properties contained in
property sheets on objects).  Both property names and values should be quoted
in a query.

Valid functions include:

    logical
 and(<function()>,<function()>,...)
 or(<function()>,<function()>,...)
 not(<function()>)

    comparison
 equals("propertyName","value")
 notEqual("propertyName","value")
 greaterThan("propertyName","value")
 greaterOrEqual("propertyName","value")
 lessThan("propertyName","value")
 lessOrEqual("propertyName","value")

    patterns
 like("propertyName","value")
 contains("propertyName","value")
 notLike("propertyName","value")

    existence
 isNotNull("propertyName")
 isNull("propertyName")

    ranges
 between("propertyName","lowerValue","upperValue")
  
More detail on each function can be found in the ElectricCommander online
help, under the help topic for the findObjects API.

Because query strings can become quite unwieldy to pass in via the command
line (due to length as well as the need to protect strings and quotes from
the shell being used), alternate means to specify the query are provided.
If the query string is simple the dash "-" character, the query is read from
STDIN.  If the query string starts with the special character "@", the
remainder of the query string is taken to be a property path, and the query
will be read from the specified property.

Note that in all cases, a query can cross multiple lines, which can be helpful
in order to make complicated queries more legible.  (The ability to have the
query string span multiple lines when passed in on the command line will be
limited by the particular shell being used, which in practice means that one
would typically use this syntax when reading the query in from STDIN or from
a property.)

Actions:
  Of course, it's not useful to search for things unless one wants to do
something with the results.

 --print id | xml | name | count

By default, the utility will simply print the ElectricCommander object IDs
for the objects found.  This is particularly useful when the query is being
run from a script that intends to perform other operations with the objects
found (most ectool commands have a syntax that accepts the object ID as a
valid way to identify the object).  This default is the same as specifying
"--print id" for the action.

For humans, often the object name is more useful.  Specifying "--print name"
will result in the object's name being displayed.  Note, however, that unlike
the object ID, the object name may not be sufficient to unambiguously identify
the object in question.  For example, it would be common to find many steps
named "setup" in any system with many procedures.

Another option, particularly useful with PowerShell, is to print the results
in the form of a complete XML document.  THe "--print xml" accomplishes this.

Finally, sometimes one merely wants to know how many items are found by the
specified query.  The "--print count" option accomplishes this goal.

 --fail-if none | any

Closely related to the "count" option are the the "--fail-if" options.  These
are intended for use in scripting where one merely needs to make a simple
binary decision based on the query results.  Specifying "--fail-if none"
causes the utility to return a failure error code (1) if there are no results
returned by the query.  The "--fail-if any" option results in a failure code
if there are any results returned.

The "--fail-if" option can be combined with other actions as required, although
doing so may make it difficult for the calling script to know the exact reason
for a failure exit code.

 --exec <command-or-script>...

If one needs to perform more interesting work with each object that the query
locates, the "--exec" option can be used to execute an external script or
program for each object.  Multiple external scripts or programs can be
specified, if desired, and if so they are executed in the order specified on
the command line.

When the script or program executes, it can find selected bits of information
about the object in the environment variables.  Each object will at least
have the "ECFIND_ojectId" variable set to the object ID that was returned by
the query.  Depending on the object type, additional "ECFIND_" variables may
be set.  More specifically, all intrinsics where the name ends in "Name" or
"Id", or where the name contains the string "container" will be placed into
the environment.  This should be sufficient for the executed script to
uniquely identify the object without needing to use the object ID.  For
example, a query for step objects will result in at least the following
environment variables being set to valid values:

  ECFIND_objectId
  ECFIND_projectName
  ECFIND_procedureName
  ECFIND_stepName

A common reason to execute an external script for each located object is
because one wishes to make changes -- which many times can be expressed in
terms of manipulating properties, either intrisic or custom.  In order to
facilitate this in as efficient a manner as possible, a set of "property"
operations can be specified:

 --setProperty <property>=<value>...

The "--setProperty" action permits one to set one or more properties on
each object found to a specific value.  As usual with setProperty operations,
if the property does not exist, it will be created.  Note that the property
in question can be an intrinsic, as well as a custom. It does not necessarily
have to be at the top level of the object either.  (In fact, it does not have
to be on the property at all.)

 --deleteProperty <property>...

If one can create properties, it stands to reason that one should be easily
able to remove properties.  The "--deleteProperty" action allows one to
delete custom properties.  It is not an error to attempt to delete a property
that does not exist.

 --editProperty <property>=~s/<old>/<new>/[g]...

Finally, an advanced action is the "--editProperty" operation.  This action
functions like the "--setProperty" operation, except that it sets the new
value based on the application of a Perl regex substitution operation on the
original value (if the property did not originally exist on the object, its
value is taken to be the null string).  This functionality allows for global
search and replace operations to be performed.

Examples:

The following simple query prints a list of all of the projects:

$0 --object project

The same, but this time print the project names:

$0 --object project --print name

It might be nice to sort it, and make sure we print a LOT of them:

$0 --object project --sort projectName --maxId 1000000 --print name

Limit the query to projects names that contain the string "EC":

$0 --object project --query "contains('projectName','EC')" --print name

A more complicated query, that prints the names of projects where the name
contains the string "EC", and either the custom property "ec_visibility" does
not exist or has the value "all", and is not a plugin (i.e. the intrinsic
property "pluginName" does not exist.. Use the maxId option to make sure we
search all projects, and sort the names. Line wrapping is for legibility:

$0 --object property --maxId 1000000 --query
 "and(contains('projectName','EC'), 
      or(isNull('ec_visibility'),equals('ec_visibility','all'))
      isNull('pluginName'))" --sort projectName --print name

Find all projects that contain the string "EC" in the name, and print
out the full ElectricCommander path to the project:

$0 --object project --query "contains('projectName','example')"
  --exec "echo /projects/\%ECFIND_projectName%"

Finally, the following example is a query that finds all steps in any project
that end in the string "lib", where the command block (an intrinsic property)
contains the string "workdir".  For each step that matches, print the unique
object id of the step, and then change the string "workdir" in the command
block to "working_directory", and append a string (an arbitrary log message)
to the custom property named "Log" on the step:

$0 --object procedureStep --query
 "and(like('projectName','\%lib'), contains('command','workdir'))"
 --print id
 --editProperty "command=~s/workdir/working_directory/g"
 --editProperty "Log=~s/\$/Bulk update for directory name change - MJW\\n/"

END_OF_TEXT
    print $helptext;
}
