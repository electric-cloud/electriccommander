# -*- Perl -*-

# After promote events, ensure the VersionManager project principal has
# permission to export/import, read projects, create artifacts and run on
# resources/workspaces. Remove access on demote.

if ($promoteAction ne '') {
    my @objTypes = ('admin', 'artifacts', 'projects', 'repositories', 'resources', 'workspaces');
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
        $batch->createAclEntry('user', "project: $pluginName", {
            systemObjectName => 'admin',
            readPrivilege => 'allow',
            modifyPrivilege => 'allow'
        });
        $batch->createAclEntry('user', "project: $pluginName", {
            systemObjectName => 'artifacts',
            readPrivilege => 'allow',
            modifyPrivilege => 'allow'
        });
        $batch->createAclEntry('user', "project: $pluginName", {
            systemObjectName => 'projects',
            readPrivilege => 'allow'
        });
        $batch->createAclEntry('user', "project: $pluginName", {
            systemObjectName => 'repositories',
            readPrivilege => 'allow'
        });
        $batch->createAclEntry('user', "project: $pluginName", {
            systemObjectName => 'resources',
            readPrivilege => 'allow',
            executePrivilege => 'allow'
        });
        $batch->createAclEntry('user', "project: $pluginName", {
            systemObjectName => 'workspaces',
            readPrivilege => 'allow',
            executePrivilege => 'allow'
        });

        # Create the 'admin' credential and set the password to the default; the user
        # will need to update it's been changed.
        $batch->createCredential($pluginName, 'admin', {
            userName => 'admin',
            password => 'changeme'
        });

        # Attach the 'admin' credential to all procedure steps.
        my $steps = $commander->findObjects('procedureStep', {
            filter => [
                {
                    "operator" => "equals",
                    "propertyName" => "projectName",
                    "operand1" => $pluginName
                },
            ]
        });
        foreach my $step($steps->findnodes("//step")) {
            my $procedureName = $step->findvalue("procedureName");
            my $stepName = $step->findvalue("stepName");
            $batch->attachCredential($pluginName, 'admin', {
                procedureName => $procedureName,
                stepName => $stepName
            });
        }

        # Register the plugin promotion hook
        $batch->setProperty('/server/ec_hooks/promote', {
            value => q{$[/plugins/@PLUGIN_NAME@/project/scripts/promoteHook]}
        });


		# Create the subtab
		$view->add(["Projects", "All"],
				   { url => 'link/projects' });
		$view->add(["Projects", "Versioned"],
				   { url => 'plugins/VersionManager/projects.php' });
    } else {
        $batch->setProperty('/server/ec_hooks/promote', {
            value => ''
        });
		# Remove the subtab
		$view->remove(["Projects", "Versioned"]);
    }
}
