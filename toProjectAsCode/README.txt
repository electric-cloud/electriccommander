This Github project allows you to version an Electric Commander project according to the mechanisms described in the ProjectAsCode Github project.

Github project files
--------------------
toProjectAsCode.pl
- Converts a Electric Commander project export into files needed to implement the ProjectAsCode methodology
project/ec_setup.pl
- File taken from ProjectActCode to manage the versioning of the Electric Commander project
README.txt
- This file

Instruction for Use
-------------------
1. Export your project to project.xml: 'ectool export /<path on Commander server>/project.xml --path /projects/<projectName> --excludeJobs true --relocatable true --withNotifiers true'
2. Copy project.xml to the directory containing toProjectAsCode.pl
3. Run this perl script with ec-perl (other perl may work) 'ec-perl toProjectAsCode.pl'
4. Create a plugin project: 'ec-perl <path to createPlugin.pl> CGITemplate <path to plugin directory> <author> "Other" <project name>'
5. Copy the 'project' directory from step #3 to your plugin directory
6. Edit the build.xml and other plugin files
7. Login to a Commander server
8. 'ant build deploy'