<?php

require_once 'startup.php';

if (   getPostData("formId") == "createVersionedProject"
    && getPostData("projectName")
    && getPostData("action") == ecgettext("OK")) {
    $projectName = getPostData("projectName");
    $updateManager = new UpdateManager;
    $update = $updateManager->createUpdate("runProcedure");
    $update->addItem("projectName", "/plugins/@PLUGIN_KEY@/project");
    $update->addItem("procedureName", "CreateProject");
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
            "actualParameterName" => "project_description",
            "value" => getPostData("description"),
        )
    );
    $update->addItem(
        "actualParameter",
        array(
            "actualParameterName" => "default_resource",
            "value" => getPostData("resourceName"),
        )
    );
    $update->addItem(
        "actualParameter",
        array(
            "actualParameterName" => "default_workspace",
            "value" => getPostData("workspaceName"),
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
        Error::report("Creation job '$jobName' did not complete after $limit seconds.",
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

    $targetUrl = nonHtmlUrl("projectDetails.php", "projectName", $projectName);
    header("Location: " . $targetUrl);
}

// Set up an editable combo-box for the user to select the default workspace
// and resource for this project.

$defaultResource = new TypeOrSelect(
    array(
        "queryRequest"   => "getResources",
        "queryResultTag" => "resourceName",
    )
);

$defaultWorkspace = new TypeOrSelect(
    array(
        "queryRequest"   => "getWorkspaces",
        "queryResultTag" => "workspaceName",
    )
);

$formElements = array(
    ecgettext("Name:"),              "projectName",   new Name,
    ecgettext("Description:"),       "description",   new HtmlTextArea,
    ecgettext("Default Resource:"),  "resourceName",  $defaultResource,
    ecgettext("Default Workspace:"), "workspaceName", $defaultWorkspace
);

$formArgs = array(
    "id"            => "createVersionedProject",
    "modifyName"    => "projectName",
    "postArgs"      => array(),
    "elements"      => $formElements
);

$navigation = new NavInfo(array(
    "section"    => ecgettext("Projects"),
    "subsection" => ecgettext("Versioning")));

$page = new Page(
    ecgettext("Create Versioned Project"),
    new StdFrame($navigation),
    new Header(
        array(
            "id"             => "pageHeader",
            "class"          => "pageHeader",
            "title"          => ecgettext("Create Versioned Project"),
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
