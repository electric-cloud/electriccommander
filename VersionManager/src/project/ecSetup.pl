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
