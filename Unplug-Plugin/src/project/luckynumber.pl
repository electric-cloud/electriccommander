#!ec-perl

# Begin with the HTML heading for the page.
$XHTML = "<h1>Greetings Earthling!</h1>\n";

# Fetch the current user name.
# We can safely use property expansion to do this, which is much
# faster and simpler than calling getProperty().
my $userName = '$[/myUser/userName]';

# It's good practice to assume that any content we fetch externally
# may contain special HTML characters that should be escaped before
# being sent to the browser.  Use the CGI::escapeHTML() function.
my $u = CGI::escapeHTML($userName);

# Fetch a random number.
my $r = rand();

# Add the rest of the text, including the random number itself,
# in the normal fashion (no special characters, so no need to
# escape this text (although it couldn't really hurt, either)).
$XHTML .= "$u, your random number for today is $r\n";

# Done.
# NB: This code is eval'd -- so do NOT call exit()!  Doing so will
# result in a premature exit of the entire plugin, not just this
# block of code.
