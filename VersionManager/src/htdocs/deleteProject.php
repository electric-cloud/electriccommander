<?php

require_once 'startup.php';

$projectName = getGet("projectName");
$updateManager = new UpdateManager;
$update = $updateManager->createUpdate("runProcedure");
$update->addItem("projectName", "/plugins/@PLUGIN_KEY@/project");
$update->addItem("procedureName", "DeleteProject");
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
	Error::report("Job '$jobName' did not complete after $limit seconds.",
		"Please browse to the job details page to inspect why the job hasn't completed.");
}
$outcome = $result->get("outcome");
if ($outcome != "success") {
	Data::$queries["getJobFailureReason"] = array(
			"request"      => "getProperty",
			"constantArgs" => array("propertyName", "/jobs/$jobId/failureReason"),
			"result"       => "property");
	$result = QueryManager::handleQueryNow("getJobFailureReason");
	Error::report("Job '$jobName' failed: " . $result->get("value"),
		"Please browse to the job details page to inspect why the job hasn't completed.");
}

redirect();

?>
