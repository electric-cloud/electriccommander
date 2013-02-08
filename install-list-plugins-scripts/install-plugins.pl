# Usage: ec-perl install-plugins.pl
# Install and promote all available plugins from the Electric-Cloud catalog site
# Creates an uninstaller for all installed plugins, chmod +x uninstall.pl

# To execute script from Cygwin on a machine with Electric Commander installed
# uncomment the following 3 steps to enable the remaining steps below behave as if run from ec-perl
##!/bin/sh
#exec "c:/Program Files/Electric Cloud/ElectricCommander/bin/ec-perl" -x "`cygpath -m "$0"`" "${@}"
#!perl
use strict;
use ElectricCommander ();
$| = 1;
my $ec = new ElectricCommander->new();

use XML::Simple;
use Data::Dumper;
use LWP::Simple;

# Installed plugins
my @installedPlugins;
for my $installedPluginXml ($ec->getPlugins()->find("//pluginKey")->get_nodelist) {
	my $installedPlugin = $installedPluginXml->string_value();
	push(@installedPlugins,$installedPluginXml->string_value());
	};

# Available plugins
my $data = new XML::Simple->XMLin(get("http://plugins.electric-cloud.com/catalog/catalog.xml"));
my $index=0;
my @availablePlugins;
while ($data->{plugin}->[$index]->{pluginKey}) {
	push (@availablePlugins,$data->{plugin}->[$index]->{pluginKey});
	$index++;
	}

# Compare available plugins to installed plugins
# Install and promote missing plugins
my $pluginInstalled = 0;
for my $availablePlugin (@availablePlugins) {
	#print "Available: $availablePlugin ________________________________\n";
	for my $installedPlugin (@installedPlugins) {
		#print $installedPlugin,"\n";
		# Compare one of the available plugins to all the installed plugins
		if ($availablePlugin eq $installedPlugin) {
			#print $installedPlugin," installed\n";
			$pluginInstalled = 1; # Installed plugin matched to the available plugin
			last; # Don't bother checking if an installed plugin has been found
			}
		}
		# If available plugin isn't installed, install it.
		if ($pluginInstalled != 1) {
			# Bug in the current (10/7/11) plugin catalog; should be EC-SendEmail
			if ($availablePlugin eq 'EC-Sendmail') {
				# NOP
				} else {
				print "Installing $availablePlugin\n";
				#$ec->installPlugin("http://plugins.electric-cloud.com/catalog/$availablePlugin.jar")
				#or die "Could not install $availablePlugin.\n";
				# Get full name/version of plugin.  Assume only one version installed.
				my $promoteName = 
					$ec->getPlugins->find("//projectName[../pluginKey = '$availablePlugin']")->string_value;
				print "Promoting $promoteName.\n";
				#$ec->promotePlugin("$promoteName")
				#or die "Failed to promote $availablePlugin.\n";
				}
			}
	$pluginInstalled = 0;
	}

# Uninstaller
# The following creates an uninstaller perl script.  The plugins to be installed are
# listed in the __END__ section of the installer.  This list can be modified so only
# the desired plugins are uninstalled.
open (UNINSTALLER, ">uninstaller.pl") or die "I couldn't create uninstaller.pl";
while (<DATA>) {
	print UNINSTALLER $_;
}
print UNINSTALLER "\n\n__END__\n";
for my $installedPluginXml ($ec->getPlugins()->find("//projectName")->get_nodelist) {
	my $installedPlugin = $installedPluginXml->string_value();
	print UNINSTALLER $installedPlugin,"\n";
	};
close UNINSTALLER;


__END__
# Uninstall plugins
#
# To execute script from Cygwin on a machine with Electric Commander installed
# uncomment the following 3 steps to enable the remaining steps below behave as if run from ec-perl
#!/bin/sh
#exec "c:/Program Files/Electric Cloud/ElectricCommander/bin/ec-perl" -x "`cygpath -m "$0"`" "${@}"
##!perl
use strict;
use ElectricCommander ();
$| = 1;
my $ec = new ElectricCommander->new();

while (<DATA>) {
  	print "Uninstalling $_\n";
	$ec->uninstallPlugin($_);
}
