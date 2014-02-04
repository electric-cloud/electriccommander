<?php

require_once 'startup.php';

$projectName = getGet("projectName");

if (   getPostData("formId") == "rollbackVersionedProject"
    && getPostData("action") == ecgettext("OK")) {
    $comment = getPostData("comment");
    if ($comment == "") {
        Error::formError(ecgettext("A commit comment is required."), "comment");
    }
    $version = getPostData("version");

    $updateManager = new UpdateManager;
    $update = $updateManager->createUpdate("runProcedure");
    $update->addItem("projectName", "/plugins/@PLUGIN_KEY@/project");
    $update->addItem("procedureName", "RollbackProjectChanges");
    $update->addItem(
        "actualParameter",
        array(
            "actualParameterName" => "project_name",
            "value" => $projectName,
        )
    );
    $update->addItem(
        "actualParameter",
        array(
            "actualParameterName" => "version",
            "value" => $version,
        )
    );
    $update->addItem(
        "actualParameter",
        array(
            "actualParameterName" => "comment",
            "value" => $comment,
        )
    );
    $updateManager->handleUpdates();
    $response = $update->getResponse();
    $id = $response->get("jobId");

    Data::$queries["getJobInfo"] = array(
                "request"      => "getJobInfo",
                "constantArgs" => array("jobId", $id),
                "result"       => "job");

    $limit = 10;
    $i = 0;
    while (1) {
        $result = QueryManager::handleQueryNow("getJobInfo");
        if ($i == $limit || $result->get("status") == "completed") {
            break;
        }
        sleep(1);
        $i ++;
    }
    $jobId = $result->get("jobId");
    $jobName = $result->get("jobName");
    $status = $result->get("status");
    if ($status != "completed") {
        Error::report("Rollback job '$jobName' did not complete after $limit seconds.",
            "Please browse to the job details page to inspect why the job hasn't completed.");
    }
    $outcome = $result->get("outcome");
    if ($outcome != "success") {
        Data::$queries["getJobFailureReason"] = array(
                "request"      => "getProperty",
                "constantArgs" => array("propertyName", "/jobs/$jobId/failureReason"),
                "result"       => "property");
        $result = QueryManager::handleQueryNow("getJobFailureReason");
        Error::formError($result->get("value"), "projectName");
    }

    $targetUrl = "projects.php";
    header("Location: " . $targetUrl);
}

Data::$queries[1] = array(
	"request"      => "getProperty",
	"constantArgs" => array(
		"propertyName", "/projects/$projectName/ec_versioning_artifact"),
	"result"       => "property"
);
$response = QueryManager::handleQueryNow(1);
$artifactKey = $response->get('value');

Data::$queries["getPickerVersions"] =  array(
            "request"      => "findObjects",
            'searchName'   => 'getArtifactVersions',
            "numObjects"   => 10000,
            "constantArgs"         => array(
            "sort", array("propertyName", "version",
                          "order", "descending")),
            'constantFilters'   => array(array( "operator", "and",
                "filter", array(
                    array(
                        'propertyName' => 'groupId',
                        'operator'     => 'equals',
                        "operand1"     => 'VersionedProjects'),
                    array(
                        'propertyName' => 'artifactKey',
                        'operator'     => 'equals',
                        'operand1'     => $artifactKey),
                        ))),
            "queryClass"  => "searchQuery",
            "result"       => 'artifactVersion',
            "dataIn"       => 'searchDataIn');
$response = QueryManager::handleQueryNow('getPickerVersions');
$options = array();
$i = 0;
foreach ($response->getSiblings() as $object) {
    if ($i > 0) {
        $version = $object->get("version");
        $comment = $object->get("description");
        $createTime = DateTimeRenderer::formatText($object->get("createTime"));
        if ($comment == "") {
            $comment = "No commit comment";
        }
        $options[] = $version;
        $options[] = "v$version [$createTime] - $comment";
    }
    $i++;
}
$versionPicker = new Selector($options);

$name = new Name(null, true);
$name->setInitialValue($projectName);
$formElements = array(
    ecgettext("Name:"),              "projectName",   $name,
    ecgettext("Version:"),           "version",       $versionPicker,
    ecgettext("Comment:"),           "comment",       new Entry,
);

$formArgs = array(
    "id"            => "rollbackVersionedProject",
    "modifyName"    => "projectName",
    "postArgs"      => array(),
    "elements"      => $formElements
);

$navigation = new NavInfo(array(
    "section"    => ecgettext("Projects"),
    "subsection" => ecgettext("Versioning")));

$page = new Page(
    ecgettext("Rollback Versioned Project"),
    new StdFrame($navigation),
    new Header(
        array(
            "id"             => "pageHeader",
            "class"          => "pageHeader",
            "title"          => ecgettext("Rollback Versioned Project"),
            "actions"        => array(new ShortcutLink),
            "actionStyle"    => 3,
        )
    ),
    new SubSection(new Form($formArgs)),
    new SetFocus("version"),
    new StdFrameEnd()
);

$page->show();

?>
