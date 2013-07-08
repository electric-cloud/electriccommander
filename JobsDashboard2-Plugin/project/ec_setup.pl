if ($promoteAction eq 'promote') {
    $view->add(["Jobs Dashboard"],
               { url => 'pages/@PLUGIN_KEY@/JobsDashboardMain_run' });
} elsif ($promoteAction eq 'demote') {
    $view->remove(["Jobs Dashboard"]);
}
