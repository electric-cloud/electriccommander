function getSettings() {
    this.commanderHostname = System.Gadget.Settings.read("commanderHostname");
    this.commanderPort = System.Gadget.Settings.read("commanderPort");
    this.username = System.Gadget.Settings.read("username");
    this.password = System.Gadget.Settings.read("password");
    this.jobName = System.Gadget.Settings.read("jobName");
    this.minus = System.Gadget.Settings.read("minus");
    this.numOfJobs = System.Gadget.Settings.read("numOfJobs");
    this.refreshInterval = System.Gadget.Settings.read("refreshInterval");
    this.elapsedTimeTimer = System.Gadget.Settings.read("elapsedTimeTimer");
    this.messageEnabled = System.Gadget.Settings.read("messageEnabled");
}

function load() {
    System.Gadget.onSettingsClosing = settingsClosing;

    loadSettings();

    commanderHostname.value = g_commanderHostname;
    commanderPort.value = g_commanderPort;
    username.value = g_username;
    password.value = g_password;
    jobName.value = g_jobName;
    minus.value = g_minus;
    numOfJobs.value = g_numOfJobs;
    refreshInterval.value = g_refreshInterval;
    elapsedTimeTimer.checked = g_elapsedTimerEnabled;
    messageEnabled.checked = g_messageEnabled;
}

function settingsClosing(event) {
    if(event.closeAction == event.Action.commit) {
        saveSettings();
    }
    event.cancel = false;
}

function saveSettings() {
    System.Gadget.Settings.write("commanderHostname", commanderHostname.value);
    System.Gadget.Settings.write("commanderPort", commanderPort.value);
    System.Gadget.Settings.write("username", username.value);
    System.Gadget.Settings.write("password", password.value);
    System.Gadget.Settings.write("jobName", jobName.value);
    System.Gadget.Settings.write("minus", minus.value);
    System.Gadget.Settings.write("numOfJobs", numOfJobs.options(numOfJobs.selectedIndex).value);
    System.Gadget.Settings.write("refreshInterval", refreshInterval.options(refreshInterval.selectedIndex).value);
    System.Gadget.Settings.write("elapsedTimeTimer", elapsedTimeTimer.checked);
    System.Gadget.Settings.write("messageEnabled", messageEnabled.checked);
}
