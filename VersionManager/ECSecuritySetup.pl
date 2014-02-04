# -*- Perl -*-

# On upgrades, preserve properties that contain user data

if ($upgradeAction eq 'upgrade') {
    my $query = $commander->newBatch();
    my ($old, $new) = ("/plugins/$otherPluginName/project/",
                       "/plugins/$pluginName/project/");
    my @reqs = (
        $query->getProperty(
            $old."CustomPolicies"),
        $query->getProperty(
            $old."policyList"),
        $query->getProperty(
            $old."defaultResource"),
        $query->getProperty(
            $old."currentPolicy"),

    );
    $query->submit();

    if ($query->findvalue($reqs[0],'code') ne 'NoSuchProperty') {
        $batch->deleteProperty($new."CustomPolicies");
        $batch->clone({
            path => $old."CustomPolicies",
            cloneName => $new."CustomPolicies"
        });
    }
    if ($query->findvalue($reqs[1],'code') ne 'NoSuchProperty') {
        $batch->deleteProperty($new."policyList");
        $batch->clone({
            path => $old."policyList",
            cloneName => $new."policyList"
        });
    }
    my $resource = $query->findvalue($reqs[2],'property/value');
    if ($resource ne '') {
        $batch->setProperty($new."defaultResource", $resource);
    }
    my $policy = $query->findvalue($reqs[3],'property/value');
    if ($policy ne '') {
        $batch->setProperty($new."currentPolicy", $policy);
    }
}

# After promote events, ensure the EC-Security project principal has permission
# to create projects, resources, and workspaces.  Remove access on demote.

if ($promoteAction ne '') {
    my @objTypes = ('projects', 'resources', 'workspaces');
    my $query = $commander->newBatch();
    my $pluginName = '@PLUGIN_NAME@';
    my @reqs = map { $query->getAclEntry('user', "project: $pluginName", {
        systemObjectName => $_}) } @objTypes;
    push @reqs, $query->getProperty('/server/ec_hooks/promote');
    $query->submit();

    foreach my $type (@objTypes) {
        if ($query->findvalue(shift @reqs, 'code') ne 'NoSuchAclEntry') {
            $batch->deleteAclEntry('user', "project: $pluginName", {
                systemObjectName => $type});
        }
    }

    if ($promoteAction eq 'promote') {
        foreach my $type (@objTypes) {
            $batch->createAclEntry('user', "project: $pluginName", {
                systemObjectName => $type,
                readPrivilege => 'allow',
                modifyPrivilege => 'allow',
                changePermissionsPrivilege => 'allow' });
        }

        # Register the plugin promotion hook
        $batch->setProperty('/server/ec_hooks/promote', {
            value => q{$[/plugins/@PLUGIN_NAME@/project/scripts/promoteHook]}
        });
    } else {
        $batch->setProperty('/server/ec_hooks/promote', {
            value => ''
        });
    }
}
