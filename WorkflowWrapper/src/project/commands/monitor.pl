use strict;
use warnings;
use ElectricCommander;
use JSON;

my $ec = new ElectricCommander;

sub getPropertyValue($);
sub expandWorkflow($$);
sub setHover($);

my $project = getPropertyValue('/myParent/definition_project_name');
my $workflow = getPropertyValue('/myParent/workflow_name');

my $interval = 5;
my $completed = 0;
my $lastDetails = "";
my $lastOutcome = "";
my $lastActiveState = "";

do {
    my $json = JSON->new->allow_nonref;
    my $expanded = expandWorkflow($project, $workflow);
    my $details = $json->pretty->canonical->encode($expanded);

    # Only bother setting the details property if it's changed.
    if ($details ne $lastDetails) {
        $ec->setProperty("/myParent/workflow_details", $details);
        $lastDetails = $details;
        setHover($expanded);
    }

    # Only bother setting the outcome property if the compositeJobOutcome has changed.
    if ($lastOutcome ne $expanded->{"compositeJobOutcome"}) {
        $ec->setProperty("outcome", $expanded->{"compositeJobOutcome"});
        $lastOutcome = $expanded->{"compositeJobOutcome"};
        setHover($expanded);
    }

    # Only bother setting the active state property if it's changed.
    if ($lastActiveState ne $expanded->{"activeState"}) {
        $ec->setProperty("/myParent/workflow_active_state", $expanded->{"activeState"});
        $ec->setProperty("/myJob/ec_job_progress_status", $expanded->{"activeState"});
        $lastActiveState = $expanded->{"activeState"};
        setHover($expanded);
    }

    $completed = $expanded->{"completed"};

    # If we're monitoring the workflow, keep sleeping for the specified interval
    # until we the workflow is completed.
    if (!$completed) {
        sleep $interval;
    }
} while (!$completed);

# Helper method to extract the value of a property.

sub getPropertyValue($)
{
    my ($name) = @_;
    return $ec->getProperty($name)->findvalue("//value")->value();
}

# Main helper method to expand a workflow and return the JSON representing the
# workflow, its states and transitions, and its subworkflows and subjobs.  Called
# recursively to fetch subworkflow information.

sub expandWorkflow($$)
{
    my ($project, $workflow) = @_;

    # Fields from the "get" server responses that are safe to ignore.
    my %exclude = (
        "actualParameters", 1,
        "combinedStatus", 1,
        "createTime", 1,
        "description", 1,
        "lastModifiedBy", 1,
        "modifyTime", 1,
        "owner", 1,
        "propertySheetId", 1,
        "workspace", 1
    );

    my %workflowData;

    # Save all the workflow's metadata.
    my $result = $ec->getWorkflow($project, $workflow);
    foreach my $workflowElement($result->findnodes("/responses/response/workflow/*")) {
        my $name = $workflowElement->getName();
        my $value = $workflowElement->string_value;
        if (!defined($exclude{$name}) && $value ne "") {
            $workflowData{$name} = $value;
        }
    }

    # Save the "compositeJobOutcome" of the workflow, which is composed using the outcomes
    # of all subjobs/subworkflows.
    my $compositeJobOutcome = "success";

    # Save information for each of the workflow's states.
    my @statesData = ();
    my $stateIndex = 0;
    $result = $ec->getStates($project, $workflow);
    foreach my $state($result->findnodes("/responses/response/state")) {
        my %stateData;
        my $stateName = $state->findvalue("stateName")->string_value;

        # Save all the state's metadata, and expand subjob/subworkflow elements
        # if they exist.
        foreach my $stateElement($state->findnodes("*")) {
            my $name = $stateElement->getName();
            my $value = $stateElement->string_value;
            if (!defined($exclude{$name}) && $value ne "") {
                $stateData{$name} = $value;
                my $subprocessOutcome = "";

                if ($name eq "subworkflow") {
                    # Account for different formats for the <subworkflow> element.  In
                    # either case, call expandWorkflow recursively on the subworkflow.
                    if ($value =~ m#/projects/(.*)/workflows/(.*)#) {
                        $stateData{"subworkflowDetail"} = expandWorkflow($1, $2);
                    } else {
                        $stateData{"subworkflowDetail"} = expandWorkflow($1, $value);
                    }
                    $subprocessOutcome = $stateData{"subworkflowDetail"}
                        {"compositeJobOutcome"};
                } elsif ($name eq "subjob") {
                    # Use the "getJobInfo" API call and save the job information.
                    my %jobData;
                    my $jobInfo = $ec->getJobInfo($value);
                    foreach my $jobElement($jobInfo->findnodes("/responses/response/job/*")) {
                        my $name = $jobElement->getName();
                        my $value = $jobElement->string_value;
                        if (!defined($exclude{$name}) && $value ne "") {
                            $jobData{$name} = $value;
                        }
                    }
                    $stateData{"subjobDetail"} = \%jobData;
                    $subprocessOutcome = $jobInfo->findvalue("//outcome")->string_value;
                }
                if ($subprocessOutcome ne "" && $compositeJobOutcome ne "error") {
                    if ($compositeJobOutcome eq "success"
                        && $subprocessOutcome ne "success") {
                        $compositeJobOutcome = $subprocessOutcome;
                    } elsif ($compositeJobOutcome eq "warning"
                        && $subprocessOutcome eq "error") {
                        $compositeJobOutcome = $subprocessOutcome;
                    }
                }
            }
        }

        # Save information for each of the state's transitions.
        my @transitionsData = ();
        my $transitionIndex = 0;
        $result = $ec->getTransitions($project, $workflow, $stateName);
        foreach my $transition($result->findnodes("/responses/response/transition")) {
            my %transitionData;
            my $transitionName = $transition->findvalue("transitionName")->string_value;
            foreach my $transitionElement($transition->findnodes("*")) {
                my $name = $transitionElement->getName();
                my $value = $transitionElement->string_value;
                if (!defined($exclude{$name}) && $value ne "") {
                    $transitionData{$name} = $value;
                }
            }
            $transitionsData[$transitionIndex] = \%transitionData;
            $transitionIndex++;
        }
        $stateData{'transition'} = \@transitionsData;
        $statesData[$stateIndex] = \%stateData;
        $stateIndex++;
    }
    $workflowData{'compositeJobOutcome'} = $compositeJobOutcome;
    $workflowData{'state'} = \@statesData;

    # Check the last log entry to see if the workflow was automatically or manually
    # completed and save it.  If manual, also save the user who completed it.
    if ($workflowData{'completed'}) {
        my $workflowId = $workflowData{'workflowId'};
        my $result = $ec->findObjects('logEntry', {
            filter =>
            [
                {
                    propertyName => "severity",
                    operator => "equals",
                    operand1 => "INFO"
                },
                {
                    propertyName => "message",
                    operator => "contains",
                    operand1 => "has completed the workflow"
                },
                {
                    operator => "equals",
                    propertyName => "container",
                    operand1 => "workflow-$workflowId"
                }
            ],
            maxIds => 1,
            numObjects => 1
        });
        if (scalar($result->findnodes("//logEntry"))) {
            $workflowData{'completedAutomatically'} = 0;
            my $message = $result->findvalue("//message")->string_value;
            $message =~ m/User \'(.*)\' has completed the workflow/;
            $workflowData{'completedByUser'} = $1;
        } else {
            $workflowData{'completedAutomatically'} = 1;
        }
    }

    return \%workflowData;
}

# Helper method to set the hover popup for the wrapper job shown in the CI dashboard.

sub setHover($) {
    my ($details) = @_;
    my $workflowName = $details->{"workflowName"};
    my $activeState = $details->{"activeState"};
    my $compositeJobOutcome = $details->{"compositeJobOutcome"};
    my $html = qq(
    <html>
        <div style="border:1px dotted black;padding:5px">
            Workflow:
            <a href="/commander/link/workflowDetails/projects/$project/workflows/$workflowName"
               target="_blank">$workflowName</a><br />
            Active state: $activeState<br />
            Composite job outcome: $compositeJobOutcome<br />
        </div>
    </html>);
    $ec->setProperty("/myJob/ec_job_description", $html);
}
