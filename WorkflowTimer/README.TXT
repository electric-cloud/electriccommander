WorkflowTimer version 0.1

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

Finally, please note that this is a very early version of a work-in-
progress, your feedback is welcome, and please don't expect complete
upwards compatability as newer releases are published.

-Mike Westerhof
(mwesterhof@electric-cloud.com)
