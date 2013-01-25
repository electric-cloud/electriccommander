/**
 * Global variables
 */
var g_countToView = 4;
var g_maxIndex = 0;
var g_currentArrayIndex = 0;    // current index in the whole builds array
var g_lastCalledArrayIndex;

// data array
var g_jobIds = [];
var g_elapsedTimes = [];
var g_running = [];

var g_refreshTime = 0;
var g_timeDiff = 0;

var g_elapsedTimerUpdateInterval = 1000;
var g_elapsedTimer;
var g_feedClicked;

var g_commanderHostname = "chronic2";
var g_commanderPort = "8000";
var g_username = "guest";
var g_password = "guest";
var g_jobName = "ecloud-%";
var g_minus = "";
var g_numOfJobs = "20";
var g_refreshInterval = "60";
var g_elapsedTimerEnabled = true;
var g_messageEnabled = true;

var L_LOADING_TEXT = "Loading...";

function loadSettings()
{
    var settings = new getSettings();

    if ($.trim(settings.commanderHostname).length) {
        g_commanderHostname =  $.trim(settings.commanderHostname);
    }
    if ($.trim(settings.commanderPort)) {
        g_commanderPort = $.trim(settings.commanderPort);
    }
    if ($.trim(settings.username)) {
        g_username = settings.username;
    }
    if ($.trim(settings.password)) {
        g_password = settings.password;
    }
    if ($.trim(settings.jobName)) {
        g_jobName = settings.jobName;
    } else {
        g_jobName = "%";
    }
    if ($.trim(settings.minus)) {
        g_minus = $.trim(settings.minus);
    } else {
        g_minus = "";
    }
    if ($.trim(settings.numOfJobs)) {
        g_numOfJobs = settings.numOfJobs;
    }
    if ($.trim(settings.refreshInterval)) {
        g_refreshInterval = settings.refreshInterval;
    }
    if ($.trim(settings.elapsedTimeTimer)) {
        g_elapsedTimerEnabled = settings.elapsedTimeTimer;
    }
    if ($.trim(settings.messageEnabled)) {
        g_messageEnabled = settings.messageEnabled;
    }

    commander.url = "http://" + g_commanderHostname + ":" + g_commanderPort;
    commander.hostname = g_commanderHostname;
    commander.username = g_username;
    commander.password = g_password;
    commander.jobName = g_jobName;
    commander.numOfJobs = g_numOfJobs;
    commander.retrieve.interval = g_refreshInterval * 1000;
}

/**
 * main function
 */
function main()
{
    System.Gadget.onShowSettings = loadSettings;
    System.Gadget.settingsUI = "settings.html";

    System.Gadget.onUndock = toggleDock;
    System.Gadget.onDock = toggleDock;

    loadSettings();

    if (!System.Gadget.docked) {
        document.body.style.height = "232px";
        document.body.style.width = "296px";
        gadgetBackground.style.height = "232px";
        gadgetBackground.style.width = "296px";
        gadgetBackground.src = "url(images/gadget.png)";
    } else {
        document.body.style.height = "173px";
        document.body.style.width = "130px";
        gadgetBackground.style.height = "173px";
        gadgetBackground.style.width = "130px";
        gadgetBackground.src = "url(images/gadget_docked.png)";
    }

    showSpinner('35%');
    document.body.focus();

    commander.data.add(loadData);
    commander.messages.add(spinnerMessage);
    commander.messages.add(messageOnUI);
    commander.run();
}

function clearViewElements() {
    for (var i = 0; i < g_countToView; i++) {
        $('#buildLink'+i).html('').attr('href', '').removeAttr('style');
        $('#buildOwner'+i).html('').removeAttr('style');
        $('#buildDate'+i).html('').removeAttr('style');
        $('#buildStatus'+i).attr('src', 'images/icn16px_blank.gif');
        g_elapsedTimes = [];
        g_running = [];
    }
}

function loadData() {
    g_maxIndex = commander.dataArray.length() - 1;
    g_refreshTime = (new Date()).getTime();
    g_timeDiff = 0;
    updateGadget();
}

function toggleDock() {
    // style
    for (var i = 0; i < g_countToView; i++) {
        if (!System.Gadget.docked) {
            $('#build'+i).css({'border-bottom' : 'dotted 1px #3b4458',
                               'height' : '44px',
                               'width' : '264px',
                               'font-size' : '16px',
                               'line-height' : '14px',
                               'overflow' : 'hidden',
                               'padding' : '6px 7px 2px 7px',
                               'border-bottom' : '0'});

            $('#buildStatus'+i).css('position', 'static');

            $('#buildLink'+i).css({'position' : 'static',
                                   'padding' : '0px 0px 4px 3px',
                                   'width' : '230px',
                                   'text-overflow' : 'ellipsis',
                                   'overflow' : 'hidden',
                                   'white-space' : 'nowrap'});

            $('#buildOwner'+i).css({'position' : 'static',
                                    'visibility' : 'visible',
                                    'height' : '14px',
                                    'width' : '130px',
                                    'font-size' : '12px',
                                    'color' : '#67788a',
                                    'line-height' : '11px',
                                    'text-overflow' : 'ellipsis',
                                    'overflow' : 'hidden',
                                    'white-space' : 'nowrap',
                                    'text-align' : 'left',
                                    'float' : 'left'});

            $('#buildDate'+i).css({'position' : 'static',
                                   'visibility' : 'visible',
                                   'height' : '14px',
                                   'width' : '120px',
                                   'font-size' : '12px',
                                   'color' : '#67788a',
                                   'line-height' : '11px',
                                   'text-align' : 'right',
                                   'overflow' : 'hidden',
                                   'float' : 'right'});
        }
        else {
            $('#build'+i).css({'border-bottom' : 'dotted 1px #3b4458',
                               'height' : '35px',
                               'width' : '121px',
                               'font-size' : '12px',
                               'line-height' : '13px',
                               'overflow' : 'hidden',
                               'padding' : '5px 2px 1px 6px'});

            $('#buildStatus'+i).css({'position' : 'absolute',
                                     'top' : '9px',
                                     'left' : '5px'});

            $('#buildLink'+i).css({'position' : 'absolute',
                                   'top' : '4px',
                                   'left' : '26px',
                                   'width' : '93px',
                                   'text-overflow' : 'ellipsis',
                                   'overflow' : 'hidden',
                                   'white-space' : 'nowrap'});

            $('#buildOwner'+i).css({'visibility' : 'hidden'});

            $('#buildDate'+i).css({'position' : 'absolute',
                                   'top' : '18px',
                                   'left' : '26px',
                                   'height' : '11px',
                                   'width' : '93px',
                                   'font-size' : '11px',
                                   'color' : '#67788a',
                                   'line-height' : '12px',
                                   'text-align' : 'center',
                                   'overflow' : 'hidden'});
        }
    }

    if (!System.Gadget.docked) {
        $('#buildHldr').css({'top' : '14px',
                             'left' : '13px',
                             'margin-right' : '14px'});

        $('#navHolder').css('top', '190px');
        if (g_messageEnabled) {
            $('#navHolder').css('left', '193px');
            $('#message').css('visibility', 'visible');
        } else {
            $('#navHolder').css('left', '106px');
            $('#message').css('visibility', 'hidden');
        }

        document.body.style.height = "232px";
        document.body.style.width = "296px";
        gadgetBackground.style.height = "232px";
        gadgetBackground.style.width = "296px";
        gadgetBackground.src = "url(images/gadget.png)";
    }
    else {
        $('#buildHldr').css({'top' : '4px',
                             'left' : '4px',
                             'margin-right' : '4px'});

        $('#navHolder').css('top', '145px');
        $('#navHolder').css('left', '25px');
        $('#message').css('visibility', 'hidden');

        document.body.style.height = "173px";
        document.body.style.width = "130px";
        gadgetBackground.style.height = "173px";
        gadgetBackground.style.width = "130px";
        gadgetBackground.src = "url(images/gadget_docked.png)";
    }
}

function updateGadget() {
    errorTextHldr.style.visibility = "hidden";
    buildHldr.style.visibility = "visible";
    navHolder.style.visibility = "visible";

    if(g_lastCalledArrayIndex) {
        g_currentArrayIndex = g_lastCalledArrayIndex;
    } else {
        g_currentArrayIndex = 0;
    }
    setNextViewItems();
}

function updateElapsedTime() {
    g_timeDiff = (new Date()).getTime() - g_refreshTime;
    for (var i = 0; i < g_countToView; i++) {
        if (g_running[i]) {
            $('#buildDate'+i).text(time(g_elapsedTimes[i] + g_timeDiff));
        }
    }
}

function setPreviousViewItems() {
    g_currentArrayIndex = g_currentArrayIndex - (g_countToView * 2);
    setNextViewItems();
}

function setNextViewItems() {
    // No data to display
    if (g_maxIndex < 0) {
        return;
    }

    // stop updateElapsedTime() timer
    window.clearInterval(g_elapsedTimer);

    g_lastCalledArrayIndex = g_currentArrayIndex;

    if (g_currentArrayIndex > g_maxIndex) {
        g_currentArrayIndex = 0;
        setNextViewItems();
        return;
    }

    if (g_currentArrayIndex < 0) {
        var countDiff = g_maxIndex%g_countToView;
        g_currentArrayIndex = (countDiff == 0 ?
                               g_maxIndex - g_countToView :
                               g_maxIndex - countDiff);
        setNextViewItems();
        return;
    }

    clearViewElements();

    for (var i = 0; i < g_countToView; i++) {

        // partial list (e.g. 9-10)
        if (g_currentArrayIndex > g_maxIndex) {
            for (var j = i; j < g_countToView; j++) {
                g_currentArrayIndex++;
            }
            break;
        }

        positionNumbers.innerText =
                ((g_currentArrayIndex + 1) - i) + " - " + (g_currentArrayIndex + 1);

        g_jobIds[i] = commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.jobId);
        g_elapsedTimes[i] = parseInt(commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.elapsedTime));
        g_running[i] = commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.status) == "running";

        $('#buildLink'+i).text(commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.jobName).replace($.trim(g_minus), ""));
        $('#buildLink'+i).attr('href', "");
        $('#buildOwner'+i).text(commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.owner));
        $('#buildStatus'+i).attr('src',
            statusIcon(commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.outcome),
                       commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.status),
                       commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.abortStatus),
                       commander.dataArray.get(g_currentArrayIndex, commander.dataArray.index.errorCode)));
        $('#buildDate'+i).text(time(g_elapsedTimes[i] + (g_running[i] ? g_timeDiff : 0)));

        g_currentArrayIndex++;
    }

    toggleDock();

    // start updateElapsedTime() timer
    if (g_elapsedTimerEnabled) {
        g_elapsedTimer = window.setInterval(updateElapsedTime, g_elapsedTimerUpdateInterval);
    }
}

function showFlyout(feedAll) {
}

function openInIE(index) {
    System.Shell.execute("https://" + commander.hostname + "/commander/jobDetails.php?jobId=" + g_jobIds[index]);
}

function showSpinner(posTop) {
    clearViewElements();
    navHolder.style.visibility = "hidden";
    buildHldr.style.visibility = "hidden";
    message.style.visibility = "hidden";
    errorTextHldr.style.visibility = "visible";
    errorTextHldr.style.top = posTop;

    errorTextDiv.innerHTML = "<p style=\"margin:0px;padding-bottom:10px;\">"
                            + "<img src=\"images/loading.gif\" />"
                            + "</p>"+L_LOADING_TEXT;
    errorTextDiv.title = L_LOADING_TEXT;
}

function spinnerMessage(message) {
    errorTextDiv.innerHTML = "<p style=\"margin:0px;padding-bottom:10px;\">"
                            + "<img src=\"images/loading.gif\" />"
                            + "</p>" + message;
    errorTextDiv.title = message;
}

function messageOnUI(msg, type) {
    message.innerText = msg;
}

function checkVisibility() {
}

function mouseWheelNavigate() {
    if(event.wheelDelta < -20) {
        setNextViewItems();
    }
    if(event.wheelDelta > 20) {
        setPreviousViewItems();
    }
}

function time(ms) {
    var t = "";

    var sec = Math.floor(ms/1000);
    ms = ms % 1000;

    var min = Math.floor(sec/60);
    sec = sec % 60;
    if (sec > 0) {
        t = sec + "s"
    }

    var hr = Math.floor(min/60);
    min = min % 60;
    if (min > 0) {
        t = min + "m " + t
    }

    var day = Math.floor(hr/24);
    hr = hr % 24;
    if (hr > 0) {
        t = hr + "h " + t
    }
    if (day > 0) {
        t = day + "d " + t
    }

    return t;
}

function statusIcon(outcome, status, abort, errorCode) {
    var icon = "";
    if (status == "running") {
        if (abort) {
            icon = "stopped";
        } else if (outcome == "success") {
            icon = "running_success";
        } else if (outcome == "warning") {
            icon = "running_warning";
        } else if (outcome == "error") {
            icon = "running_error";
        } else {
            icon = "running";
        }
    } else if (outcome == "success") {
        icon = "success";
    } else if (outcome == "warning") {
        icon = "warning";
    } else if (outcome == "error") {
        if (abort || errorCode == 'ABORTED') {
            icon = "stopped";
        } else {
            icon = "error";
        }
    } else if (outcome == "skipped") {
        icon = "skipped";
    } else {
        icon = "runnable";
    }
    return "images/icn16px_" + icon + ".gif";
}

