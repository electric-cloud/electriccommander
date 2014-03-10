<?php

//-----------------------------------------------------------------------------
// viewPropertySheet.php --
//
// View the contents of any property sheet.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved
//-----------------------------------------------------------------------------

require_once 'startup.php';
Html::includeCss("lib/styles/Header.css");

$path = getGet("path");

Data::$queries["genericProperties"] =     array(
            "request"      => "getProperties",
            "constantArgs" => array("path", $path, "recurse", 0, 'expand', 0),
            "result"       => "propertySheet");

$page = new Page(
    "Properties",
    new StdFrame(new NavInfo()),
    new Header(
    array(
            "id"      => "pageHeader",
            "class"   => "pageHeader",
            "title"   => ecgettext("Properties"),
            "title2"   => $path
        )
    ),
    new SubSection(
        new PropertySection(
            array(
                "id"         => "properties",
                "title"      => ecgettext("Properties"),
                "type"       => "generic",
                "expandable" => 0
            )
        )
    ),
    new StdFrameEnd()
);

$page->show();

?>
