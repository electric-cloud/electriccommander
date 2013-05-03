if ($promoteAction eq 'promote') {
    $view->add(["Workflows Dashboard"],
               { url => 'pages/@PLUGIN_KEY@/workflowDashboardMain_run' });
} elsif ($promoteAction eq 'demote') {
    $view->remove(["Workflows Dashboard"]);
}
