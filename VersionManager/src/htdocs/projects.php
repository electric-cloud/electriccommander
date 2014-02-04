<?php

require_once 'startup.php';
require_once 'ObjectDisplayDefinitions.php';
Html::includeCss("styles/JobStatus.css");

$filterNameName = PagedTable::getFilterNameName();
$filterName = 'versionedProjectsSearch';
$_GET[$filterNameName] = $filterName;
$objectType = "artifact";

$currentUrl = "plugins/@PLUGIN_KEY@/projects.php";
$createUrl  = "plugins/@PLUGIN_KEY@/createProject.php";
$addUrl     = "plugins/@PLUGIN_KEY@/addProject.php";

class VersionedProjectLastCommit extends Renderer {
    function html($record) {
        $projectName = $record->get("artifactKey");
        Data::$queries[1] = array(
            "request"      => "getProperties",
            "constantArgs" => array(
                "path", "/artifacts/VersionedProjects:$projectName"),
            "result"       => "propertySheet"
        );
        $response = QueryManager::handleQueryNow(1);
        $version = $response->get(
            '//property[propertyName="ec_current_version"]/value');
        $comment = $response->get(
            '//property[propertyName="ec_current_version_comment"]/value');
        $time = DateTimeRenderer::formatText($response->get(
            '//property[propertyName="ec_current_version_time"]/value'));
        return "$time [v$version] - $comment";
    }
}

class VersionedProjectStatus extends Renderer {
    function html($record) {
        $projectName = $record->get("artifactKey");
        Data::$queries[1] = array(
            "request"      => "getProperties",
            "constantArgs" => array(
                "path", "/artifacts/VersionedProjects:$projectName"),
            "result"       => "propertySheet"
        );
        $response = QueryManager::handleQueryNow(1);
        $problem = $response->get(
            '//property[propertyName="ec_versioning_problem"]/value');
        $pending = $response->get(
            '//property[propertyName="ec_pending_changes"]/value');
        if ($problem) {
            $icon = "error";
            $message = $problem;
        } else if ($pending) {
            $icon = "warning";
            $message = "Uncommitted changes";
        } else {
            $icon = "success";
            //$message = "All changes committed";
        }

        return "<table class=\"jobStatus\" cellspacing=\"0\">"
        . "<tr><td><img src=\"".$_SESSION["commanderBaseUrl"]."lib/images/icn16"
        . "px_$icon.gif\" alt=\"$icon\" /></td><td class=\"jobStatus_text\">"
        . Html::text($message) . "</td></tr></table>";
    }
}

class CommitLink {
    function html($record) {
        $projectName = $record->get("description");
        $key = $record->get("artifactKey");
        Data::$queries[1] = array(
            "request"      => "getProperties",
            "constantArgs" => array(
                "path", "/artifacts/VersionedProjects:$key"),
            "result"       => "propertySheet"
        );
        $response = QueryManager::handleQueryNow(1);
        $problem = $response->get(
            '//property[propertyName="ec_versioning_problem"]/value');
        if ($problem) {
            return "";
        }
        $pending = $response->get(
            '//property[propertyName="ec_pending_changes"]/value');
        if ($pending) {
            $commitUrl  = "plugins/@PLUGIN_KEY@/commitProjectChanges.php?projectName=$projectName";
            Data::$links["commitChanges"] = array(
                "text"         => ecgettext("Commit"),
                "base"         => $commitUrl);
            $link = new Link("commitChanges");
            return $link->html($record);
        }
        return "";
    }
}

class RollbackOrDiscardLink {
    function html($record) {
        $projectName = $record->get("description");
        $key = $record->get("artifactKey");
        Data::$queries[1] = array(
            "request"      => "getProperties",
            "constantArgs" => array(
                "path", "/artifacts/VersionedProjects:$key"),
            "result"       => "propertySheet"
        );
        $response = QueryManager::handleQueryNow(1);
        $problem = $response->get(
            '//property[propertyName="ec_versioning_problem"]/value');
        if ($problem) {
            return "";
        }
        $version = $response->get(
            '//property[propertyName="ec_current_version"]/value');
        $pending = $response->get(
            '//property[propertyName="ec_pending_changes"]/value');

        $rollbackUrl  = "plugins/@PLUGIN_KEY@/rollbackProjectChanges.php?projectName=$projectName";
        $discardUrl  = "plugins/@PLUGIN_KEY@/discardProjectChanges.php";
        Data::$links["rollbackChanges"] = array(
            "text"         => ecgettext("Rollback"),
            "base"         => $rollbackUrl);
        Data::$links["discardChanges"] = array(
            "text"    => ecgettext("Discard"),
            "base"    => $discardUrl . "?projectName=%%1",
            "args"    => array(),
            "confirm" => array(
                "heading" => ecgettext("Discard uncommitted changes to project \"%%1\"?"),
                "details" => ecgettext("WARNING: This cannot be undone."),
                "buttons" => array(
                    ecgettext("OK") => "%s",
                    ecgettext("Cancel") => ""),
                "args" => array("description")));

        if ($pending) {
            $link = new Link("discardChanges");
            return $link->html($record);
        } elseif ($version > 1) {
            $link = new Link("rollbackChanges");
            return $link->html($record);
        }
        return "";
    }
}

class DeleteLink {
    function html($record) {
        $projectName = $record->get("description");
        $key = $record->get("artifactKey");
        Data::$queries[1] = array(
            "request"      => "getProperties",
            "constantArgs" => array(
                "path", "/artifacts/VersionedProjects:$key"),
            "result"       => "propertySheet"
        );
        $response = QueryManager::handleQueryNow(1);
        $problem = $response->get(
            '//property[propertyName="ec_versioning_problem"]/value');
        if ($problem) {
            return "";
        }
        $deleteUrl  = "plugins/@PLUGIN_KEY@/deleteProject.php";
        Data::$links["deleteVersionedProject"] = array(
			"text"    => ecgettext("Delete"),
			"base"    => $deleteUrl . "?projectName=%%1&artifactKey=%%2",
			"args"    => array(),
			"confirm" => array(
				"heading" => ecgettext("Delete project \"%%1\"?"),
				"details" => ecgettext("Click OK to delete the project and all versioning history. "
					. "WARNING: This cannot be undone."),
				"buttons" => array(
					ecgettext("OK") => "%s",
					ecgettext("Cancel") => ""),
				"args" => array("description", "artifactKey")));
		$link = new Link("deleteVersionedProject");
		return $link->html($record);
    }
}



Data::$links["createVersionedProject"] = array(
            "text"    => ecgettext("Create New Project"),
            "base"    => $createUrl,
            "args"    => array());

Data::$links["addExistingProject"] = array(
            "text"    => ecgettext("Add Existing Project"),
            "base"    => $addUrl,
            "args"    => array());

$columns = array(
            array('label' => ecgettext("Project")), "description",
            array('label' => ecgettext("Last Commit")), new VersionedProjectLastCommit,
            array('label' => ecgettext("Status")), new VersionedProjectStatus,
            ecgettext("Actions"), new ActionList(array(
                new CommitLink,
                new RollbackOrDiscardLink,
                new DeleteLink)),
        );

$filter = array(array(
    "operator" => "and",
    "filter" => array(
        array(
            'propertyName' => 'groupId',
            'operator'     => 'equals',
            'operand1'     => 'VersionedProjects'))));

$defaultSort = array(array(
    'propertyName' => 'description',
    'order'        => 'ascending'));

list($searchResults, $ssfSummary, $rssUrl, $ssf) = PagedTable::buildPagedTable(
    array(
         'objectType' => $objectType,
         'filterName' => $filterName,
         'resultsPage' => $currentUrl,
         'defaultSort' => $defaultSort,
         'defaultFilter' => $filter,
         'columns' => $columns
    )
);

$pageTitle = ecgettext("Versioned Projects");

$navigation = new NavInfo(array(
    "section"    => ecgettext("Projects"),
    "subsection" => ecgettext("Versioning")));

$page = new Page(
    $pageTitle,
    new StdFrame($navigation),
    new Header(
        array(
            "id"      => "pageHeader",
            "class"   => "pageHeader",
            'rssLink' => $rssUrl,
            "title"   => $pageTitle,
            "actions" =>
            array(new ShortcutLink(array('currentUrl' => $currentUrl))),
            "actionStyle" => 3,
        )
    ),
    new SubSection(
        new Header(
            array(
                "id"        => "resultsHeader",
                "class"     => "mediumHeader",
                "titleLink" => $ssfSummary,
                "actions" =>
                array("createVersionedProject", "addExistingProject")
            )
        ),
        $searchResults
    ),
    new StdFrameEnd()
);
$page->show();

?>
