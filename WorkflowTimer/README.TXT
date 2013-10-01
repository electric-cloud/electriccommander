WorkflowTimer version 0.0

WorkflowTimer is a rough cut of a means to do scheduled transitions in
workflows.  For example, if a build is not manually transitioned to
"accepted" within 24 hours, it will automatically transition to the
"deferred" state.  Or, an approver can approve a future deployment by
doing a manual approval and entering the date/time when the workflow
should automagically transition into the deployment part of the workflow.

Begin by importing the workflowTimer.export file into Commander:

 ectool import --file workflowTimer.export

The imported project contains a schedule, which drives the period timer
tick, and thus sets the resolution for the timer mechanism.  By default,
it will check, or "tick", every 3 minutes.  More often than that is
probably undesirable unless you adjust the "tick" procedure to run on
other than the local agent.  You'll also see a project-level property
named "TZ" -- set this to your local timezone. Or UTC, if you prefer.
This timezone definition is necessary because there's no guarantee that
the agent's timezone is the same as the user's timezone, and neither of
those may be the same as the Commander server's timezone.

In the project you'll find a couple of Workflow Definitions that
illustrate how it works.

Note that in order to use the "TransitionAt" procedure, you'll need
to add the Date::Manip perl library somewhere where the agent can find
it -- the easiest being to add it to your ecperl installation.  If you
installed ElectricCommander in the default locations, copy the provided
Manip.pm file to the following directory:

Windows:
 C:\Program Files\Electric Cloud\ElectricCommander\perl\site\lib\Date

Linux:
 /opt/electriccloud/electriccommander/perl/lib/site_perl/5.8.8/Date

(Of course, you don't have to use the Date::Manip library, you can modify
the "TransitionAt" code to use any library you wish to take the user's
date or time string and convert it to a standard format.  I chose the
Date::Manip package because it allows one to specify transition times
in a very human-normal fashion -- one can specify that a transition
should occur at 3:45PM and get the correct result, or more interestingly
one can specify "next Monday at 10 AM" and get the right result for that
as well.)

Finally, please note that this is a very early version of a work-in-
progress, your feedback is welcome, and please don't expect complete
upwards compatability as newer releases are published.

-Mike Westerhof
(mwesterhof@electric-cloud.com)
