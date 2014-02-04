<?php

require_once 'startup.php';

if (   getPostData("formId") == "addExistingProject"
    && getPostData("projectName")
    && getPostData("action") == ecgettext("OK")) {
    $projectName = getPostData("projectName");
    $updateManager = new UpdateManager;
    $update = $updateManager->createUpdate("runProcedure");
    $update->addItem("projectName", "/plugins/@PLUGIN_KEY@/project");
    $update->addItem("procedureName", "AddProject");
    $update->addItem(
        "actualParameter",
        array(
            "actualParameterName" => "project_name",
            "value" => $projectName,
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
        Error::report("Add job '$jobName' did not complete after $limit seconds.",
            "Please browse to the job details page to inspect why the job hasn't completed.");
    }
    $outcome = $result->get("outcome");
    if ($outcome == "error") {
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

Data::$queries["getPickerProjects"] =  array(
            "request"      => "findObjects",
            'searchName'   => 'getProjects',
            "numObjects"   => 10000,
            "constantArgs"         => array("sort",
                    array("propertyName", "projectName",
                          "order", "ascending")),
            'constantFilters'   => array(array( "operator", "and",
                "filter", array(
                    array(
                        'propertyName' => 'ec_versioning_artifact',
                        'operator'     => 'isNull'),
                    array(
                        'propertyName' => 'pluginName',
                        'operator'     => 'isNull'),
                        ))),
            "queryClass"  => "searchQuery",
            "result"       => 'project',
            "dataIn"       => 'searchDataIn');

$projectPicker = new Selector(null, null, "getPickerProjects", "projectName");

$formElements = array(
    ecgettext("Project:"), "projectName", $projectPicker
);

$formArgs = array(
    "id"            => "addExistingProject",
    "modifyName"    => "projectName",
    "postArgs"      => array(),
    "elements"      => $formElements
);

$navigation = new NavInfo(array(
    "section"    => ecgettext("Projects"),
    "subsection" => ecgettext("Versioning")));

$page = new Page(
    ecgettext("Add Existing Project"),
    new StdFrame($navigation),
    new Header(
        array(
            "id"             => "pageHeader",
            "class"          => "pageHeader",
            "title"          => ecgettext("Add Existing Project"),
            "actions"        => array(new ShortcutLink),
            "actionStyle"    => 3,
        )
    ),
    new SubSection(new Form($formArgs)),
    new SetFocus("projectName"),
    new StdFrameEnd()
);

$page->show();

?>
