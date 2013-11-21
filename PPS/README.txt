                                   PPS
                                   ---

Properties are cool.  Property Sheets are way cool.  Together, they can be
used to create almost any sort of complex data structure you wish.  And there
are some other neat tricks -- for instance, if you overload the description
field of the property to define ancillary information, such as data type,
there's simply no limit to what you can model with properties.

Nevertheless, breaking apart a complex data structure in one step, only to
end up reassembling that same data structure later on in a subsequent step
seems to be less efficient than it needs to be.  If there's no need for the
individual data items to be in separate properties, well, then why not just
store the entire data structure in a single property and be done with it?

The pps export file is a project that illustrates how to do exactly that with
any type of Perl data, including lists.  Or hashes. Or lists of hashes.  Or
hashes of hashes of lists of hashes.  Or scalars.  Pretty much anything.

Here's how it works:

0) Import the project export file into your Commander instance.

1) Insert the following text at the top of your ec-perl command block in
the step where you intend to read/write a data structure to/from a property:

	$[/projects/pps/pps]

This will pull in a short fragment of perl that defines a couple of helper
functions: ppsSet() and ppsGet().

2) In order to save your data structure to a property, inside your perl code
simply call the ppsSet() subroutine, passing in a reference (perl pointer)
to the data you want to save into a property.  Optionally, pass in a
property path to define where to save the data (the default property is at
the /myJob level, so if you want your data to persist between separate jobs
in a workflow, for example, you would usually specify a property at the
/myWorkflow level).  The reference can be a reference to a simple scalar if
you wish, or it can be a reference to a complex perl data structure (see the
perl manual section on "perldsc").

Now, at some point in the future, you can pull that data back out in another
step.  Just repeat (1) from above, and instead of calling ppsSet(), you call
the ppsGet() function.  It returns a reference (pointer) to the data you saved.
Copy it out, or manipulate it in place.  Then if you wish, save it again for
later use in your procedure, workflow, or whatever.

The project includes a couple of examples you can refer to for help.
