push (@::gMatchers,
{
id => "LinuxDir",	
	pattern => q{(total \d+)},
	action => q{			
		diagnostic("$1", "info",1,1);
		setProperty("summary", "$1");}
},
{
	id => "WindowsDir",
	pattern => q{(\d+ File\(s\))},
	action => q{
		diagnostic("$1", "info",-4,1);
		setProperty("summary", "$1");}
},);
