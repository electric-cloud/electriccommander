# Usage: ec-perl pluginProcedures.pl  <pluginName>
#
# Dumps list of currently-installed plugin procedures
# if pluginName is given,  only that plugin's procedures will
# be listed.  With no parameter, all plugins are listed.

use strict;
use ElectricCommander ();
$| = 1;

my $ec = new ElectricCommander->new();
my $pluginName = shift;
my $plugins;

if ($pluginName) {
  $plugins = $ec->getPlugin($pluginName);
} else {
  $plugins = $ec->getPlugins();
}

for my $plugin ($plugins->find('//pluginName')->get_nodelist) {
	print "Plugin: ", $plugin->string_value(), "\n";
	for my $procedure ($ec->getProcedures($plugin->string_value())->find('//procedureName')->get_nodelist) {
		print " - ", $procedure->string_value(), "\n";
	}
	print "\n";
}
