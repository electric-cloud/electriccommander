<?php

require_once 'startup.php';

$projectName = getGet("projectName");

if (   getPostData("formId") == "commitVersionedProject"
    && getPostData("action") == ecgettext("OK")) {
    $comment = getPostData("comment");
    if ($comment == "") {
        Error::formError(ecgettext("A commit comment is required."), "comment");
    }

    $updateManager = new UpdateManager;
    $update = $updateManager->createUpdate("runProcedure");
    $update->addItem("projectName", "/plugins/@PLUGIN_KEY@/project");
    $update->addItem("procedureName", "CommitProjectChanges");
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
        Error::report("Commit job '$jobName' did not complete after $limit seconds.",
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

$name = new Name(null, true);
$name->setInitialValue($projectName);
$formElements = array(
    ecgettext("Name:"),              "projectName",   $name,
    ecgettext("Comment:"),           "comment",       new Entry,
);

$formArgs = array(
    "id"            => "commitVersionedProject",
    "modifyName"    => "projectName",
    "postArgs"      => array(),
    "elements"      => $formElements
);

$navigation = new NavInfo(array(
    "section"    => ecgettext("Projects"),
    "subsection" => ecgettext("Versioning")));

$page = new Page(
    ecgettext("Commit Versioned Project"),
    new StdFrame($navigation),
    new Header(
        array(
            "id"             => "pageHeader",
            "class"          => "pageHeader",
            "title"          => ecgettext("Commit Versioned Project"),
            "actions"        => array(new ShortcutLink),
            "actionStyle"    => 3,
        )
    ),
    new SubSection(new Form($formArgs)),
    new SetFocus("comment"),
    new StdFrameEnd()
);

$page->show();

?>
