#!/usr/bin/env ec-perl
# An ec-perl utility to manage security and general configuration of
# various Commander objects (projects, artifacts, and resources).
#
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# NB: For proper formatting, use perltidy with options: -ce -l=100
#     (meaning use cuddled else, and line length of 100)
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

$::version = '0.4h';
$::debug   = 0;
$|         = 1;

my $doFull     = 0;
my $doSelect   = 0;
my $printTotal = 0;
my $actionFail = '';

# Define some vars used by GetOptions (because of use strict)
my $server;
my $user;
my $pw;
my $help = undef;
my $tz;
my $object;
my $maxIds = 10000;
my $query;
my @selects;
my @sorts;
my @actions;

# Now use the GetOptions API to parse the command line
my $result = GetOptions(
    "server=s"  => \$server,
    "user=s"    => \$user,
    "pw=s"      => \$pw,
    "tz=s"      => \$tz,
    "help:s"    => \$help,
    "debug+"    => \$::debug,
    "object=s"  => \$object,
    "query=s"   => \$query,
    "select=s"  => \@selects,
    "sort=s"    => \@sorts,
    "maxIds=i"  => \$maxIds,
    "fail-if=s" => sub {
        my $a = shift;
        my $v = lc(shift);
        die "$v: invalid argument\n"
          if ( ( $v ne 'none' )
            && ( $v ne 'any' ) );
        $actionFail = $v;
    },
    "print:s" => sub {
        my $a = shift;
        my $v = lc(shift);
        $v = "id" unless ($v);
        die "$v: invalid argument\n"
          if ( ( $v ne 'id' )
            && ( $v ne 'count' )
            && ( $v ne 'total' )
            && ( $v ne 'name' )
            && ( $v ne 'xml' ) );
        push @actions, [ "$a", $v ];
        $doFull = 1
          if ( ( $v eq 'name' )
            || ( $v eq 'xml' ) );
        $doSelect   = 1 if ( $v eq 'xml' );
        $printTotal = 1 if ( $v eq 'total' );
    },
    "expandString=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
    },
    "setProperty=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
    },
    "editProperty=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
    },
    "deleteProperty=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
    },
    "copyProperty=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
    },
    "exec=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
        $doFull = 1;
    },
    "perl=s" => sub {
        my $a = shift;
        my $v = shift;
        push @actions, [ "$a", $v ];
        $doFull   = 1;
        $doSelect = 1;
    },
);

if ( defined($help) ) {
    printHelp($help);
    exit 0;
}

if ( ( !$object ) || ( !$result ) ) {
    printHelp('usage');
    die "\n";
}

# Default action is to print the id
push @actions, [ 'print', 'id' ] unless ( @actions || $actionFail );

pDebug( "actions array: " . Dumper( \@actions ) );

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

# Establish our connection to Commander
$::ec = ElectricCommander->new($server);
die "Error: Unable to establish connection to Commander server $server\n"
  unless ($::ec);

# Login, if requested
if ($user) {
    unless ($pw) {
        print "$user password: ";
        eval { ReadMode('noecho'); };
        chomp( $pw = ( $@ ? <STDIN> : ReadLine(0) ) );
        eval { ReadMode('normal') };
        print "\n";
    }
    $::ec->login( $user, $pw );
    $::ec->saveSessionFile();
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Process the query argument and build the findObjects arglist

my %a = ();

# Set the max number of objects we'll be fetching
$a{'maxIds'} = $maxIds;

# Set the number of complete objects we need -- either none, or maxIds, based
# on the action the user has specified us to take on each selected object
$a{'numObjects'} = ($doFull) ? $maxIds : 0;

# Test for a special query string syntaxes
if ( $query eq '-' ) {
    $query = '';
    while (<STDIN>) {
        chomp;
        $query .= $_;
    }
} else {
    $query = expandAt($query);
}

# If a query was provided, process it and add it.  Note that a side effect
# of the query is that any properties used in the query are added to the list
# of properties to be returned in the full object.
if ($query) {
    my ( $f, @s ) = mkfilter($query);
    $a{'filter'} = [$f];
    push @selects, @s;
}

# Add the select list, but only if we are generating XML output
# (nothing else uses it, yet)
if ($doSelect) {
    foreach my $s (@selects) {
        push @{ $a{'select'} }, { "propertyName" => $s };
    }
}

# Add the sort list
foreach my $s (@sorts) {
    my $sdir = 'ascending';
    if ( $s =~ m/^\-(.*)$/ ) {
        $s    = $1;
        $sdir = 'descending';
    } elsif ( $s =~ m/^\+(.*)$/ ) {
        $s = $1;
    }
    push @{ $a{'sort'} }, { "propertyName" => $s, 'order' => $sdir };
}

pDebug( "a: " . Dumper( \%a ) );

# Now run the findObjects query itself
my $xp = $::ec->findObjects( $object, \%a );

pDebug( "findObjects:\n" . $xp->findnodes_as_string('/') );

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Do something with the results

my $counter          = 0;
my $xmlHeaderPrinted = 0;

my $xquery = $doFull ? '/responses/response/object' : '/responses/response/objectId';
my $objectNodeSet = $xp->find($xquery);
foreach my $o ( $objectNodeSet->get_nodelist() ) {

    $counter++;

    # All objects have an object Id -- fetch that appropriately
    my $objectId;
    if ($doFull) {
        $objectId = $xp->find( './objectId', $o )->string_value();
    } else {
        $objectId = $o->string_value();
    }
    pDebug("objectId: $objectId");

    my $objectName = '';
    my %e          = ();

    # Iterate over the object type and extract "interesting" properties,
    # storing them in a hash.  Currently these include intrinsics that
    # end in either "Name" or "Id", or contain the string "container".
    if ($doFull) {

        #        my $nodelist = $xp->find('./' . $object . '/*', $o);
        my $nodelist = $xp->find( './*/*', $o );
        foreach my $node ( $nodelist->get_nodelist() ) {
            my $n = $node->getName();
            if (   ( $n =~ m|Id$| )
                || ( $n =~ m|Name$| )
                || ( $n =~ m|container| ) )
            {
                $e{$n} = $node->string_value();
                pDebug("  $n: $e{$n}");
            }
        }
        $e{'objectId'} = $objectId;

        $objectName = $e{ $object . 'Name' };

        # Special case -- some objects are unusual in terms of naming
        unless ($objectName) {
            if ( ( $object eq 'procedureStep' ) || ( $object eq 'jobStep' ) ) {
                $objectName = $e{'stepName'};
            } elsif ( $object eq 'emailConfig' ) {
                $objectName = $e{'configName'};
            } elsif ( $object eq 'emailNotifier' ) {
                $objectName = $e{'notifierName'};
            } elsif ( $object eq 'logEntry' ) {
                $objectName = $e{'logEntryId'};
            } else {
                $objectName = '**unknown**';
            }
        }

        pDebug("object name: $objectName");

        # In preparation for external commands, set environment variables based
        # on the saved "interesting" properties found earlier.
        foreach ( keys %ENV ) { delete $ENV{$_} if ( $_ =~ m/^ECFIND_/ ); }
        foreach ( keys %e ) { $ENV{ 'ECFIND_' . $_ } = $e{$_}; }
    }

    for my $i ( 0 .. $#actions ) {

        my $a = $actions[$i][0];
        my $v = $actions[$i][1];
        pDebug("Action=\"$a\", Value=\"$v\"");

        if ( $a eq 'print' ) {
            if ( $v eq 'id' ) {
                print "$objectId\n";
            } elsif ( $v eq 'count' ) {
                print "$counter\n";
            } elsif ( $v eq 'name' ) {
                print "$objectName\n";
            } elsif ( $v eq 'xml' ) {
                if ( !$xmlHeaderPrinted ) {
                    print "<?xml version=\"1.0\"?>\n<objects>\n";
                    $xmlHeaderPrinted++;
                }
                print '    ' . XML::XPath::XMLParser::as_string($o) . "\n";
            }
        }

        elsif ( $a eq 'exec' ) {
            $v = expandAt($v);
            system($v);
        }

        elsif ( $a eq 'perl' ) {
            $v = expandAt($v);
            eval $v;
            die "Error: $@\neval($v)\n" if ($@);
        }

        elsif ( $a eq 'expandString' ) {
            $v = expandAt($v);
            $::ec->abortOnError(0);
            my $xp = $::ec->expandString( $v, { 'objectId' => $objectId } );
            $::ec->abortOnError(1);
            pDebug2( "expandString($v, $objectId): " . $xp->findnodes_as_string("/") );
            my $va;
            if ( $xp->exists('/responses/error/code') ) {
                $va = $xp->findvalue('/responses/error/message')->string_value;
            } else {
                $va = $xp->findvalue('/responses/response/value')->string_value;
            }
            print "$va\n";
        }

        elsif ( $a eq 'setProperty' ) {
            $v = expandAt($v);
            if ( $v =~ m/^\s*(.*?)\s*\=\s*(.*?)\s*$/ ) {
                die "Error: property name must be provided in setProperty action: \"$v\"\n"
                  unless ($1);
                my $pn = $1;
                my $va = $2;
                setP( $pn, $va, $objectId ) unless ($::debug);
                pDebug("setProperty '$pn' '$va' --objectId '$objectId'");
            } else {
                die "Error: syntax error in setProperty action: \"$v\"\n";
            }
        }

        elsif ( $a eq 'editProperty' ) {
            $v = expandAt($v);
            if ( $v =~ m/^\s*(.*?)\s*\=\~(.*?)$/ ) {
                die "Error: property name must be provided in editProperty action: \"$v\"\n"
                  unless ($1);
                my $pn = $1;
                my $e  = $2;
                $::ec->abortOnError(0);
                my $va = getP( $pn, 'true', $objectId );
                $::ec->abortOnError(1);
                my $vaOrig = $va;
                $e = '$va =~ ' . $e;
                pDebug( "Eval: " . $e );
                eval $e;
                die "Error processing regular expression: $@\n" if ($@);

                if ( $va eq $vaOrig ) {
                    pDebug("no change in value, setProperty() skipped.");
                } else {
                    setP( $pn, $va, $objectId ) unless ($::debug);
                    pDebug("setProperty '$pn' '$va' --objectId '$objectId'");
                }
            } else {
                die "Error: syntax error in editProperty action: \"$v\"\n";
            }
        }

        elsif ( $a eq 'copyProperty' ) {
            $v = expandAt($v);
            if ( $v =~ m/^\s*(.*?)\s*\=\s*(.*?)\s*$/ ) {
                die "Error: target property name must be provided in copyProperty action: \"$v\"\n"
                  unless ($1);
                "Error: origin property name must be provided in copyProperty action: \"$v\"\n"
                  unless ($2);
                my $tpn = $1;
                my $opn = $2;
                $::ec->abortOnError(0);
                my $va = getP( $opn, 'true', $objectId );
                $::ec->abortOnError(1);
                if ( defined($va) ) {
                    setP( $tpn, $va, $objectId ) unless ($::debug);
                    pDebug("setProperty '$tpn' '$va' --objectId '$objectId'");
                } else {
                    pDebug("Origin property does not exist, not copied.");
                }
            } else {
                die "Error: syntax error in copyProperty action: \"$v\"\n";
            }
        }

        elsif ( $a eq 'deleteProperty' ) {
            $v = expandAt($v);
            pDebug("deleteProperty '$v' --objectId '$objectId'");
            $::ec->abortOnError(0);
            delP( $v, $objectId ) unless ($::debug);
            $::ec->abortOnError(1);
        }
    }

}

print "</objects>\n" if ($xmlHeaderPrinted);
print "$counter\n"   if ($printTotal);

my $status = 0;
$status = 1 if ( ( $actionFail eq 'none' ) && ( $counter == 0 ) );
$status = 1 if ( ( $actionFail eq 'any' )  && ( $counter > 0 ) );

exit($status);

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
#                          Query parser below                               #
#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#

# Main entrypoint: given a query, it returns the generated filter structure,
# and a list of properties referenced by the query (for use in "selects")
sub mkfilter($) {
    $::query = shift;
    $::p     = 0;
    my @f = getExpr();
    while ( $::p < length($::query) && ( substr( $::query, $::p, 1 ) =~ /\s/ ) ) {
        $::p++;
    }
    syntaxError("expected end of query") unless ( $::p == length($::query) );
    return (@f);
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub getExpr() {
    my %otab = (
        'and'            => [ 1, -1 ],    # boolean, arbitrary list
        'or'             => [ 1, -1 ],    # boolean, arbitrary list
        'not'            => [ 1, 1 ],     # boolean, unary
        'between'        => [ 0, 2 ],     # property, two operands
        'contains'       => [ 0, 1 ],     # property, one operand
        'equals'         => [ 0, 1 ],
        'greaterOrEqual' => [ 0, 1 ],
        'greaterThan'    => [ 0, 1 ],
        'in'             => [ 0, 1 ],
        'lessOrEqual'    => [ 0, 1 ],
        'lessThan'       => [ 0, 1 ],
        'like'           => [ 0, 1 ],
        'notEqual'       => [ 0, 1 ],
        'notLike'        => [ 0, 1 ],
        'isNotNull'      => [ 0, 0 ],     # property, no operands
        'isNull'         => [ 0, 0 ],
    );
    my %stab = (
        'equal'          => 'equals',
        'eq'             => 'equals',
        'greaterorequal' => 'greaterOrEqual',
        'ge'             => 'greaterOrEqual',
        'greaterthan'    => 'greaterThan',
        'greater'        => 'greaterThan',
        'gt'             => 'greaterThan',
        'lessorequal'    => 'lessOrEqual',
        'le'             => 'lessOrEqual',
        'lessthan'       => 'lessThan',
        'lt'             => 'lessThan',
        'less'           => 'lessThan',
        'notequal'       => 'notEqual',
        'ne'             => 'notEqual',
        'notlike'        => 'notLike',
        'unlike'         => 'notLike',
        'isnotnull'      => 'isNotNull',
        'notnull'        => 'isNotNull',
        'isnull'         => 'isNull',
        'null'           => 'isNull',
    );

    my %filter  = ();
    my %selects = ();
    my $op      = getNextTokenD();
    $op = $stab{ lc($op) } if defined( $stab{ lc($op) } );
    syntaxError("\"$op\": unrecognized operator") unless defined( $otab{$op} );
    my $t = getNextTokenD();
    syntaxError("\"$t\": expected open parenthesis") unless ( $t eq '(' );
    $filter{'operator'} = $op;
    my ( $is_boolean, $n ) = @{ $otab{$op} };

    if ($is_boolean) {
        pDebug2("Expecting boolean filter, with $n operands.");
        my @filterList = ();
        do {
            my ( $f, @s ) = getExpr();
            push @filterList, $f;
            foreach (@s) { $selects{$_}++; }
        } while ( ( $t = getNextTokenD() ) eq ',' );
        pDebug2( "Obtained boolean filter list: " . Dumper( \@filterList ) );
        $filter{'filter'} = \@filterList;
    } else {
        pDebug2("Expecting property filter, with $n operands.");
        my $pn = getNextTokenD();
        $filter{'propertyName'} = $pn;
        $selects{$pn}++;
        if ( $n > 0 ) {
            $t = getNextTokenD();
            syntaxError("\"$t\": expected comma") unless ( $t eq ',' );
            $filter{'operand1'} = getNextMacroTokenD();
        }
        if ( $n > 1 ) {
            $t = getNextTokenD();
            syntaxError("\"$t\": expected comma") unless ( $t eq ',' );
            $filter{'operand2'} = getNextMacroTokenD();
        }
        $t = getNextTokenD();
    }
    syntaxError("\"$t\": expected close parenthesis") unless ( $t eq ')' );
    pDebug2( "Returning: " . Dumper( \%filter ) . "Selecting: " . join( ', ', keys(%selects) ) );
    return ( \%filter, keys(%selects) );
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub getNextMacroTokenD() {
    my $t = getNextTokenD();
    if ( $t =~ m/^#[Dd][Aa][Tt][Ee]:\s*(.*)$/ ) {
        my $value = $1;
        syntaxError("\"$t\": missing argument for macro \"DATE\"") unless ($value);
        if ( !$::DateManipIsInitialized ) {
            require Date::Manip;

            # HACK - this code is necessary for the old version of Date::Manip
            # provided with Commander 4.1.x and earlier.
            if ($tz) {
                Date::Manip::Date_Init( "TodayIsMidnight=1", "TZ=$tz" );
            } else {
                Date::Manip::Date_Init("TodayIsMidnight=1");
            }
            $::DateManipIsInitialized = 1;
        }
        my $d = Date::Manip::ParseDate($value);
        my $d = Date::Manip::Date_ConvTZ( $d, "", "UTC" );
        $value = Date::Manip::UnixDate( $d, '%Y-%m-%dT%H:%M:%S.000Z' );
        pDebug("Macro translation: \"$t\" -> \"$value\"");
        $t = $value;
    }
    return $t;
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub getNextTokenD() {
    my $t = getNextToken();
    pDebug2( sprintf( "p=%3d c=%1s t=%s", $::p, substr( $::query, $::p, 1 ), $t ) );
    return $t;
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# A subroutine that returns the next token from the query line.
# The query is assumed to be in a global named "$::query" and
# the current position in the string is held in "$::p"
sub getNextToken() {

    my $token = '';

    # start by removing leading whitespace
    while ( $::p < length($::query) && ( substr( $::query, $::p, 1 ) =~ /\s/ ) ) {
        $::p++;
    }

    # ok, we have a non-space character, handle special case where the first
    # character we encounter is a special character.
    my $c = substr( $::query, $::p, 1 );
    if ( $c =~ /[\,\(\)]/ ) {
        $::p++;
        return $c;
    }

    # we have a normal token, so read it in.  Special handling is embedded
    # for handling of quoted strings.
    while ( $::p < length($::query) ) {
        $c = substr( $::query, $::p, 1 );
        if ( $c eq '"' ) {
            $::p++;
            while (( $::p < length($::query) )
                && ( ( $c = substr( $::query, $::p++, 1 ) ) ne '"' ) )
            {
                $token .= $c;
            }
        } elsif ( $c eq '\'' ) {
            $::p++;
            while (( $::p < length($::query) )
                && ( ( $c = substr( $::query, $::p++, 1 ) ) ne '\'' ) )
            {
                $token .= $c;
            }
        } elsif ( $c =~ /\s/ ) {
            $::p++;
            return $token;
        } elsif ( $c =~ /[\,\(\)]/ ) {
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
# Utility to handle @property references in arguments

sub expandAt {
    my $a = shift;
    return $1 if ( $a =~ m/^@(@.*)$/ );
    return getP($1) if ( $a =~ m/^@(\/.*)$/ );
    if ( $a =~ m/^@(.*)$/ ) {
        my $xp = $::ec->expandString($1);
        pDebug2( "expandString($1): " . $xp->findnodes_as_string("/") );
        return $xp->findvalue('/responses/response/value')->string_value;
    }
    return $a;
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Utility function to get a property.  The objectId is used if the
# propertyName argument does not start with a forward-slash.  The value
# "undef" is returned if the property does not exist.  Note that the
# caller is responsible for calling abortOnError() as appropriate.

sub getP {
    my $pN     = shift;
    my $expand = shift;
    my $objId  = shift;

    my $v;
    my $xp;
    my %a;
    $a{'expand'} = $expand if ( $expand ne '' );
    $a{'objectId'} = $objId if ( $objId && ( !( $pN =~ m|^/| ) ) );
    $xp = $::ec->getProperty( $pN, \%a );
    pDebug2( "getP($pN): " . $xp->findnodes_as_string("/") );
    if ( $xp->exists('/responses/error/code') ) {
        $v = undef;
    } else {
        $v = $xp->findvalue('/responses/response/property/value')->string_value;
        pDebug("DEBUG: getP($pN): value=\"$v\"");
    }
    return $v;
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Utility function to delete a property.  The objectId is used if the
# propertyName argument does not start with a forward-slash.

sub delP {
    my $pN    = shift;
    my $objId = shift;

    my %a;
    $a{'objectId'} = $objId if ( $objId && ( !( $pN =~ m|^/| ) ) );
    my $xp = $::ec->deleteProperty( $pN, \%a );
    pDebug2( "delP($pN): " . $xp->findnodes_as_string("/") );
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
# Utility function to set a property.  The objectId is used if the
# propertyName argument does not start with a forward-slash.

sub setP {
    my $pN    = shift;
    my $v     = shift;
    my $objId = shift;

    my %a;
    $a{'value'} = $v;
    $a{'objectId'} = $objId if ( $objId && ( !( $pN =~ m|^/| ) ) );
    my $xp = $::ec->setProperty( $pN, \%a );
    pDebug2( "setP($pN): " . $xp->findnodes_as_string("/") );
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub pDebug {
    my $msg = shift;
    print "DEBUG: $msg\n" if ($::debug);
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub pDebug2 {
    my $msg = shift;
    print "DEBUG: $msg\n" if ( $::debug > 1 );
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub pDebug3 {
    my $msg = shift;
    print "DEBUG: $msg\n" if ( $::debug > 2 );
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub syntaxError {
    my $e = shift;
    die "Syntax Error: $e\n  '$::query'\n   " . ' ' x $::p . "^\n";
}

#---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---=---#
sub printHelp ($) {
    my $help = shift;

    my $helptext_usage = <<END_OF_TEXT_USAGE;
Usage: ecfind [--server <host>] [--user <username>] [--pw <password>]
  general:
   --object <commander-object-type-name>
   [--query <query-expression> | @</property-path-or-expansion> | -]
   [--tz <timezone>]
   [--select <property>[,<property...]]
   [--sort <property>[,<property...]]
   [--maxIds <n>]
  actions:
   [--print id | xml | name | count]
   [--fail-if none | any]
   [--expandString <string>]...
   [--exec <command-or-script>]...
   [--setProperty <property>=<value>]...
   [--copyProperty <target>=<original>]...
   [--editProperty <property>=~s/<old>/<new>/[g]]...
   [--deleteProperty <property>]...
  debugging: [--debug]
  help: [--help [usage | query | about]]
END_OF_TEXT_USAGE

    my $helptext_query = <<END_OF_TEXT_QUERY;
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

    - logical operators -
            and ( <function()>, <function()>, ... )
             or ( <function()>, <function()>, ... )
            not ( <function()> )

    - comparison operators -
         equals ( "propertyName", "value" )
       notEqual ( "propertyName", "value" )
    greaterThan ( "propertyName", "value" )
 greaterOrEqual ( "propertyName", "value" )
       lessThan ( "propertyName", "value" )
    lessOrEqual ( "propertyName", "value" )

    - pattern matching operators -
           like ( "propertyName", "value" )
       contains ( "propertyName", "value" )
        notLike ( "propertyName", "value" )

    - existence testing operators -
      isNotNull ( "propertyName")
         isNull ( "propertyName")

    - range testing operators -
        between ( "propertyName", "lowerValue", "upperValue" )
  
Many of the operators have synonyms (for example, one can use "ge" instead
of "greaterOrEqual" to save typing).  However, use of the full operator
name is encouraged.

In order to facilitate queries involving dates, a special macro, "#date:",
can be used in place of the "value" in a query.  For example:

  "#date: 12 Dec 2001 8:45 AM EST"

One can also specify relative or sloppy dates:

  "#date: last week"
  "#date: last Monday"
  "#date: today"

Note: when specifying relative dates, especially "today", be aware of the
pitfalls caused by the fact that Commander performs the comparison based
on a specific full date/time.  The macro "#date: today" actually translates
to midnight at the beginning of the current day (which matches best to
the common intent).  In contrast, the macro "#date: yesterday" actually
translates to exactly 24 hours ago.  In general, try to use more complete
date/time specifications.

Note: date conversions require knowlege of the current timezone.  Due to
limitations in the version of perl, the utility may be unable to determine
your timezone.  If you get a message indicating this, you may need to
specify your timezone on the command line using the "--tz <timezone>"
option.  Please specify the timezone in the format "CST6CDT" or "EST".

More detail on each function can be found in the ElectricCommander online
help, under the help topic for the findObjects API.
END_OF_TEXT_QUERY

    my $helptext_generic = <<END_OF_TEXT_GENERIC;

Property References and Expansion:

Because query strings and actions can become quite unwieldy to pass in via
the command line (due to length as well as the need to protect strings and
quotes from the shell being used), alternate means to specify the query and
actions are provided.

If the query string is simple the dash character, the query is read from STDIN.

If the query string starts with the special character "@", the remainder of
the query string is specially processed.  If the remainder starts with the
forward slash ("/"), then it is taken to be a full property path, and the query
will be read from the specified property.  Otherwise, the remainder is passed
through "expandString" to expand all embedded property references (such as
\$[/myUser/saved_property]).

Note that in all cases, a query can cross multiple lines, which can be helpful
in order to make complicated queries more legible.  (The ability to have the
query string span multiple lines when passed in on the command line will be
limited by the particular shell being used, which in practice means that one
would typically use this syntax when reading the query in from STDIN or from
a property.)

Certain actions also honor the "@" syntax, allowing one to store the details
of an editProperty action (for example) in a property.  The actions that permit
this form of property expansion include:

  --setProperty
  --copyProperty
  --editProperty
  --expandString
  --exec

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

The "--print count" option simply prints an incrementing counter value for
each item.  This may be useful for determining relative position in a large
amount of output, but more commonly, one merely wants to know how many items
are found by the specified query.  The "--print total" option accomplishes
this goal.

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

 --expandString <string>...

When it is desirable to have a little more control over the output, in some
cases the "--expandString" option may be useful.  The provided string is sent
to the "expandString" API in order to have all the embedded property
references (e.g. '\$[description]') expanded prior to being displayed.  Note
that this option is often of limited use, since it attempts to expand nested
property references, which may not be valid outside of the job execution
context.  Clever use of embedded javascript references can mitigate this to
some extent, but in general this option should be used very carefully and only
when the content of the properties in question is well-understood and is
defined and valid outside of the job execution context.

 --exec <command-or-script>...

If one needs to perform more interesting work with each object that the query
locates, the "--exec" option can be used to execute an external script or
program for each object.  Multiple external scripts or programs can be
specified, if desired, and if so they are executed in the order specified on
the command line.

When the script or program executes, it can find selected bits of information
about the object in the environment variables.  Each object will at least
have the "ECFIND_objectId" variable set to the object ID that was returned by
the query.  Depending on the object type, additional "ECFIND_" variables may
be set.  More specifically, all intrinsics where the name ends in "Name" or
"Id", or where the name contains the string "container" will be placed into
the environment.  This should be sufficient for the executed script to
uniquely identify the object without needing to use the object ID.  For
example, a query for procedureStep objects will result in at least the
following environment variables being set to valid values:

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
in question can be a either an intrinsic property or a custom property.

 --copyProperty <target>=<original>...

The "--copyProperty" action permits one to copy the value of an existing
property to a target property.  If the target property exists, its value
will be replaced.  Only the value is copied (the description field is
ignored).  Note that the properties involved can be either intrinsic or
custom properties.

 --deleteProperty <property>...

If one can create properties, it stands to reason that one should be easily
able to remove properties.  The "--deleteProperty" action allows one to
delete custom properties.  It is not an error to attempt to delete a property
that does not exist, but you cannot delete an intrinsic property (use the
setProperty action to set the intrinsic's value to the empty string instead).

 --editProperty <property>=~s/<old>/<new>/[g]...

Finally, an advanced action is the "--editProperty" operation.  This action
functions like the "--setProperty" operation, except that it sets the new
value based on the application of a Perl regex substitution operation on the
original value (if the property did not originally exist on the object, its
value is taken to be the null string).  This functionality allows for global
search and replace operations to be performed.

Examples:

The following simple query prints a list of all of the projects:

ecfind --object project

The same, but this time print the project names:

ecfind --object project --print name

It might be nice to sort it, and make sure we print a LOT of them:

ecfind --object project --sort projectName --maxId 1000000 --print name

Limit the query to projects names that contain the string "EC":

ecfind --object project --query "contains('projectName','EC')" --print name

A more complicated query, that prints the names of projects where the name
contains the string "EC", and either the custom property "ec_visibility" does
not exist or has the value "all", and is not a plugin (i.e. the intrinsic
property "pluginName" does not exist). Use the maxId option to make sure we
search all projects, and sort the names. Line wrapping is for legibility:

ecfind --object property --maxId 1000000 --query
 "and(contains('projectName','EC'), 
      or(isNull('ec_visibility'),equals('ec_visibility','all'))
      isNull('pluginName'))" --sort projectName --print name

Find all projects that contain the string "EC" in the name, and print
out the full ElectricCommander path to the project:

ecfind --object project --query "contains('projectName','example')"
  --exec "echo /projects/\%ECFIND_projectName%"

Finally, the following example is a query that finds all steps in any project
who's name ends in the string "lib" and where the command block (an intrinsic
property) contains the string "workdir".  For each step found, it will print
the unique object id of the step followed by the Commander path to the step
in question, make a backup copy of the command block, change the string
"workdir" in the command block to "working_directory", and finally write a
log message to the front of a custom property named "changeLog" on the step:

ecfind --object procedureStep --query
 "and(like('projectName','\%lib'), contains('command','workdir'))"
 --print id
 --expandString
  "/projects/\$[projectName]/procedures/\$[procedureName]/steps/\$[stepName]"
 --copyProperty "command_20120717=command"
 --editProperty "command=~s/workdir/working_directory/g"
 --editProperty "changeLog=~s/^/Bulk copy-replace to fix workdir name\\n/"

END_OF_TEXT_GENERIC

    my $helptext_about = <<END_OF_TEXT_ABOUT;
ecfind version $::version
END_OF_TEXT_ABOUT

    if ( $help eq 'usage' ) {
        print $helptext_usage;
    } elsif ( $help eq 'query' ) {
        print $helptext_query;
    } elsif ( $help eq 'about' ) {
        print $helptext_about;
    } else {
        print $helptext_usage;
        print "\n";
        print $helptext_query;
        print "\n";
        print $helptext_generic;
        print "\n";
        print $helptext_about;
    }
}
