The Jenkins plugin is an implmentation of the REST API to start and monitor 
builds on a Jenkins server.

To install the plugin, create a jar file with the following command:
jar cvf 0 jenkins.jar pages META-INF

Then load the and promote the plugin
ectool login <user> <passwd>
ectool loadPlugin  jenkins.jar
ectool promotePlugin jenkins-1.0

Contact author: 
  Laurent Rochette (lrochette@electric-cloud.com) 

Legal Jumbo
 
This module is free for use. Modify it however you see fit to better your 
experience using ElectricCommander. Share your enhancements and fixes.

This module is not officially supported by Electric Cloud. It has undergone no 
formal testing and you may run into issues that have not been uncovered in the 
limited manual testing done so far.

Electric Cloud should not be held liable for any repercusions of using this 
software.