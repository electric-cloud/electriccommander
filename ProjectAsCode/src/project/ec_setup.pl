if ($promoteAction eq 'promote') {
    my $pluginName = '@PLUGIN_NAME@';
    my $pluginKey = '@PLUGIN_KEY@';
    my $pluginVersion = '@PLUGIN_VERSION@';

    # The purpose of a "ProjectAsCode" plugin is to develop a PROJECT so it can be checked
    # into source control and properly revisioned.  End users of these projects shouldn't
    # be aware of plugins usage on the back-end.  So we create a project by the same name
    # as the plugin key (without a version number) and update that project in-place when a
    # different version is promoted.  To achieve this, we use export and import instead of
    # delete and clone so we can preserve jobs/workflows which would otherwise be lost.

    # Use the logs directory for the temporary export file (we know it's writable by the
    # server since it writes its logs there).
    my $exportFile = $commander->getProperty("/server/Electric Cloud/dataDirectory")
        ->findvalue("//value")->value() . "/logs/$pluginName.xml";
    $commander->export($exportFile, {path => "/projects/$pluginName"});
    $commander->import($exportFile, {path => "/projects/$pluginKey", force => 1});
    unlink($exportFile);

    # Delete the ec_setup property from the user-facing project since it's irrelevant.
    $commander->deleteProperty("/projects/$pluginKey/ec_setup");

    # Store the plugin version in a property on the project so it's easy to find.
    $commander->setProperty("/projects/$pluginKey/project_version", $pluginVersion);
}
