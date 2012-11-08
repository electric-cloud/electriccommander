my $unView = 
'<view>
  <base>Default</base>
  <tab>
    <label>Home</label>
    <tab>
      <label>Un</label>
      <position>3</position>
      <url>pages/unplug/un_run</url>
    </tab>
  </tab>
</view>';

my $vDesc = 'Content to be displayed by the @PLUGIN_KEY@ plugin page';
my $vValue = '$' . '[/plugins/@PLUGIN_KEY@/project/v_example1]';

if ($promoteAction eq 'promote') {
    my $ec = new ElectricCommander();
    $ec->abortOnError(0);
    $ec->createProperty("/server/@PLUGIN_KEY@/v",
			{description=>$vDesc, value=>$vValue});
    $ec->abortOnError(1);
    $ec->setProperty("/server/ec_ui/availableViews/unView",
		     {description=>'Unplug View', value=>$unView});

} elsif ($promoteAction eq 'demote') {
    $batch->deleteProperty("/server/ec_ui/availableViews/unView");
}
