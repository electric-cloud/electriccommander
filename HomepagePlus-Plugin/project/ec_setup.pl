use ElectricCommander();
my $ec = new ElectricCommander();
#my $promoteAction = 'demote';

if ($promoteAction eq 'promote') {

    # add the homepage tab
    my $hpp = <<EOF;
<view>
<base>Default</base>
<tab><label>Home</label><show>0</show></tab>
<tab>
    <label>Home</label>
    <position>1</position>
    <tab>
        <label>Overview</label>
        <url>pages/HomepagePlus/homepage</url>
        <show>1</show>
    </tab>
    <tab>
        <label>CI Dashboard</label>
        <url>pages/EC-CIManager/configure</url>
        <show>1</show>
    </tab>
    <tab>
        <label>Workflow Dashboard</label>
        <url>pages/WorkflowDashboard/WorkflowDashboardMain_run</url>
        <show>1</show>
    </tab>
    <tab>
        <label>Jobs Dashboard</label>
        <url>pages/JobsDashboard/JobsDashboardMain_run</url>
        <show>1</show>
    </tab>
    <tab>
        <label>EC Homepage</label>
        <url>pages/EC-Homepage/homepage</url>
        <show>1</show>
    </tab>
</tab>
</view>
EOF
    # Add sample configurations
    $ec->setProperty("/server/ec_ui/availableViews/HomepagePlus", {value=>"$hpp"});
    $ec->setProperty("/server/ec_ui/defaultView", {value=>"HomepagePlus"});
    $ec->setProperty("/myUser/userSettings/workflowConfigurations/Sample B-T-R/project", {value=>"EC-Examples"});
    $ec->setProperty("/myUser/userSettings/workflowConfigurations/Sample B-T-R/wf", {value=>"Build-Test-Release"});
    $ec->setProperty("/myUser/userSettings/workflowConfigurations/Sample B-T-R/start", {value=>"Start"});
    $ec->setProperty("/myUser/userSettings/workflowConfigurations/Sample WF Approvals/project", {value=>"EC-Examples"});
    $ec->setProperty("/myUser/userSettings/workflowConfigurations/Sample WF Approvals/wf", {value=>"Build Workflow With Approvals"});
    $ec->setProperty("/myUser/userSettings/workflowConfigurations/Sample WF Approvals/start", {value=>"Check Prerequisites"});
}
elsif ($promoteAction eq 'demote') {
    $ec->deleteProperty("/server/ec_ui/availableViews/HomepagePlus");
    $ec->setProperty("/server/ec_ui/defaultView", {value=>"Default"});
    $ec->deleteProperty("/myUser/userSettings/workflowConfigurations/Sample B-T-R");
    $ec->deleteProperty("/myUser/userSettings/workflowConfigurations/Sample WF Approvals");
}
