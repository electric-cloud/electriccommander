my $unView = 
'<view>
  <base>Default</base>
  <tab>
    <label>Home</label>
    <tab>
      <label>Un</label>
      <position>3</position>
      <url>pages/@PLUGIN_KEY@/un_run</url>
    </tab>
  </tab>
</view>';

my $unViewAll = 
'<view>
  <base>Default</base>
  <tab>
    <label>UnTab</label>
    <position>2</position>
    <tab>
      <label>Un1</label>
      <position>1</position>
      <url>pages/@PLUGIN_KEY@/un_run1</url>
    </tab>
    <tab>
      <label>Un2</label>
      <position>2</position>
      <url>pages/@PLUGIN_KEY@/un_run2</url>
    </tab>
    <tab>
      <label>Un3</label>
      <position>3</position>
      <url>pages/@PLUGIN_KEY@/un_run3</url>
    </tab>
    <tab>
      <label>Un4</label>
      <position>4</position>
      <url>pages/@PLUGIN_KEY@/un_run4</url>
    </tab>
    <tab>
      <label>Un5</label>
      <position>5</position>
      <url>pages/@PLUGIN_KEY@/un_run5</url>
    </tab>
    <tab>
      <label>Un6</label>
      <position>6</position>
      <url>pages/@PLUGIN_KEY@/un_run6</url>
    </tab>
    <tab>
      <label>Un7</label>
      <position>7</position>
      <url>pages/@PLUGIN_KEY@/un_run7</url>
    </tab>
    <tab>
      <label>Un8</label>
      <position>8</position>
      <url>pages/@PLUGIN_KEY@/un_run8</url>
    </tab>
  </tab>
</view>';

if ($promoteAction eq 'promote') {
    # Use createProperty (and ignore errors) so that we do not overwrite existing properties
    $commander->abortOnError(0);
    $commander->createProperty("/server/@PLUGIN_KEY@/v",
	{description=>'Content to be displayed by the @PLUGIN_KEY@ plugin main page',
	 value=>'$' . '[/plugins/@PLUGIN_KEY@/project/v_example2]',
	 expandable=>'1'});
    foreach my $i (0, 1, 2, 3, 4, 5, 6, 7, 8, 9) {
	$commander->createProperty("/server/@PLUGIN_KEY@/v$i",
	    {description=>"Content to be displayed by the @PLUGIN_KEY@ plugin subpage $i",
	     value=>'$' . '[/plugins/@PLUGIN_KEY@/project/v_example' . $i . ']',
	     expandable=>'1'});
    }
    foreach my $i ('flot', 'jquery', 'unplug') {
	$commander->createProperty("/server/@PLUGIN_KEY@/lib/use-$i",
	    {description=>"XHTML fragment to pull in the $i javascript libraries and dependencies",
	     value=>'$' . '[/plugins/@PLUGIN_KEY@/project/lib/use-' . $i . ']',
	     expandable=>'1'});
    }
    # Reset error handling at this point
    $commander->abortOnError(1);
    $commander->setProperty("/server/ec_ui/availableViews/unView",
		     {description=>'Unplug View', value=>$unView});
    $commander->setProperty("/server/ec_ui/availableViews/unViewAll",
		     {description=>'Unplug View, Top-level with sub-tabs', value=>$unViewAll});
} elsif ($promoteAction eq 'demote') {
    $batch->deleteProperty("/server/ec_ui/availableViews/unView");
    $batch->deleteProperty("/server/ec_ui/availableViews/unViewAll");
}
