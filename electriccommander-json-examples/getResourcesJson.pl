# Copyright (c) 2012 Nikhil Vaze
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# Demonstrates how to parse json output from the ElectricCommander server
# Will only work with Commander servers v4.2 and greater

use strict;
use warnings;
use ElectricCommander;

# Turn off buffering
$| = 1;

#------------------------------------------------------------------------------
# populateResourceHash
#
#       
# Arguments:
#    resources                   - An array ref of resource objects
#    resourcesHash               - Key = resource name, value= hash containing a resource object  
#------------------------------------------------------------------------------
sub populateResourceHash($$) {

  my $resources = shift;
  my $resourcesHash = shift;

  # Iterate through all the resources
  foreach my $resource (@$resources) {
    
    # Add the resource object to the hash, keyed on resource name.
    $resourcesHash->{$resource->{'resourceName'}} = $resource;
    
  }
}

# Create an ElectricCommander object to communicate with a server.
my $ec = new ElectricCommander({format=>"json"});

# Request the plugins directory property    
my $pluginsDir = $ec->getProperty("/server/settings/pluginsDirectory")->{'responses'}->[0]->{'property'}->{'value'};

# Print the value out to console
print "plugins directory is: $pluginsDir\n";
# Intended output: plugins directory is: /opt/electriccloud/electriccommander/plugins

# Issue request and store response (array ref of resources)
my $result = $ec->getResources()->{'responses'}->[0]->{'resource'};

# Create a resources hash that will hold all the resource objects returned by the server keyed by resource name
my %resourcesHash;

# Populate the hash
populateResourceHash($result,\%resourcesHash);

# Examples of retrieving from data from hash
print 'The hostname for the local resource is:' . $resourcesHash{'local'}->{'hostName'} . "\n";
print 'Is the local resource alive? ' . $resourcesHash{'local'}->{'agentState'}->{'alive'} . "\n";

# Intended output 
# The hostname for the local resource is:localhost
# Is the local resource alive? 1

## Learning more
## Use Data::Dumper; to print out result from server
## This will give you a feel for what the server has returned and how to parse it.
## You can also try adding debug=>1 to the ElectricCommander object