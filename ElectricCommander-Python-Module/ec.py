"""
ec.py -- ElectricCommander Python module / class.

Disclaimer:
This software is released AS IS with no warranties expressed or implied.
This software is not supported by Electric Cloud. If you have any questions
or issues, please contact the entity from whom you retrieved this software.
You may modify this software as you see fit as long as this disclaimer
statement is included unmodified. If you would like to add additional
disclaimers, please add them to an "Additional Disclaimers" section
below.

Usage:

from ec import ElectricCommander

cmdr = ElectricCommander()
xml = cmdr.getProperty(dict(propertyName="/server/myprop"))
... process the xml result how you normally would ...

# Login is special since it needs to update the active session in the
# object. Failed login raises an exception.
try:
    xml = cmdr.login("user1", "pass1")
except Exception as inst:
    print(inst)

# You can create multiple ElectricCommander objects to connect to multiple
# servers.
cmdr2 = ElectricCommander("server2", user="user1")
... issue requests on the cmdr2 object ...


# Here's how you can issue a findObjects request for projects whose names
# begin with "D" or "E"

print(cmdr.findObjects(dict(
            objectType = "project",
            filter = dict(
                operator = "or",
                filter = [
                    dict(
                        propertyName = "projectName",
                        operator = "like",
                        operand1 = "D%"),
                    dict(
                        propertyName = "projectName",
                        operator = "like",
                        operand1 = "E%")]))))

# Here's how you can issue a batch request, in parallel mode.
print(cmdr.httpPost(cmdr.makeEnvelope(
        cmdr.createRequest("getProperty", dict(propertyName="/server/myprop"))
        + cmdr.createRequest("getServerStatus"), "parallel")))

# Here's how to set a job property from within a job step
from os import environ
print(cmdr.setProperty(dict(
    propertyName = "/myJob/prop1",
    value = "5",
    jobStepId = environ["COMMANDER_JOBSTEPID"])))
"""

import httplib2
import socket
import re
import xml.dom.minidom
from os import environ
import os

# Share an Http object across all ElectricCommander instances. So if a user
# creates multiple ElectricCommander objects for the same server/port,
# the one Http object's connection cache will enable us to use one socket.
gHttpHandle = httplib2.Http(disable_ssl_certificate_validation=True)
gFirstUserKey = "__FIRSTUSER"


class ElectricCommanderException(Exception):
    pass


class ElectricCommander(object):
    # Constructor
    def __init__(self, server=None, port=None, secure=True, user=None,
                 timeout=180):
        self.requestCounter = 0
        self.server = server
        self.port = port
        self.user = user
        self.secure = secure
        self.timeout = timeout
        self.sessionId = None

        # Read the session file
        self._readSessionFile()

        # Set the url for talking to Commander based on the given args
        # and environment.
        self._configureUrls()

        # Figure out which session-id we're actually going to use for
        # this EC object.
        if ("COMMANDER_SESSIONID" in environ
                and len(environ["COMMANDER_SESSIONID"]) > 0):
            self.sessionId = environ["COMMANDER_SESSIONID"]
        elif (self.url in self.sessionMap):
            sessionsForUrl = self.sessionMap[self.url]
            if (self.user is None):
                self.user = sessionsForUrl[gFirstUserKey]
            if (self.user in sessionsForUrl):
                self.sessionId = sessionsForUrl[self.user]

    ############################################################
    # __getattr__
    # 
    # Magic function that essentially converts calls like:
    #     cmdr.setProperty(dict(propertyName="prop1", value="val1"))
    # into
    #     cmdr.issueRequest("setProperty",
    #           dict(propertyName="prop1", value="val1"))
    ############################################################
    def __getattr__(self, requestName):
        return lambda args=None: self.issueRequest(requestName, args or {})

    ############################################################
    # issueRequest
    # 
    # Send a request to the Commander server.
    #
    # Arguments:
    #     See createRequest.
    ############################################################
    def issueRequest(self, requestName, params=None):
        params = params or {}
        return self.httpPost(self.makeEnvelope(
            self.createRequest(requestName, params)))

    ############################################################
    # login
    # 
    # Issues a login request to the server. Upon success, it
    # stores the returned session in this object's context for
    # use in subsequent api calls.
    #
    # Arguments:
    #     userName
    #     password
    ############################################################
    def login(self, userName, password):
        result = self.issueRequest("login", dict(
            userName=userName,
            password=password))
        doc = xml.dom.minidom.parseString(result)
        responseNodes = doc.firstChild.getElementsByTagName("response")
        if (len(responseNodes) == 0):
            responseNodes = doc.firstChild.getElementsByTagName("error")
        responseNode = responseNodes[0]
        if (responseNode.nodeName == "error"):
            code = responseNode.getElementsByTagName("code")[0]
            message = responseNode.getElementsByTagName("message")[0]
            raise ElectricCommanderException("Login failed [{}]: {}".format(
                _getText(code),
                _getText(message)))

        # Good response!
        self.user = userName
        self.sessionId = _getText(
            responseNode.getElementsByTagName("sessionId")[0])
        return result

    ############################################################
    # httpPost
    # 
    #     Send a string request as post data to the Commander server.
    #     This method is mostly used internally, but module users
    #     may use it to send batch requests to Commander.
    ############################################################
    def httpPost(self, reqData):
        try:
            resp, content = gHttpHandle.request(self.url, "POST",
                                                str.encode(reqData))
            return content.decode("utf8")
        except socket.error as inst:
            raise ElectricCommanderException(inst)

    ############################################################
    # createRequest
    # 
    #     Create a Commander request (xml string) for the given request
    #     name and parameters.
    #
    # Arguments:
    #     requestName - Name of request (e.g. getProperty)
    #     params - dictionary whose keys are child element names
    #         (e.g. propertyName) and whose value is the value of the child
    #         element (e.g. "prop1"). For api calls that take complex
    #         arguments (e.g. findObjects), the value may be a dictionary
    #         (so the containing dictionary entry key becomes a non-leaf
    #         node in the request). Similarly, if the value of the child
    #         element is a list, it means this element should be emitted
    #         once for each value (e.g. "groupName" parameters in createUser).
    ############################################################
    def createRequest(self, requestName, params=None):
        # Create a document that looks something like this:
        # <request requestId="py-123"><myRequest>...
        params = params or {}
        doc = xml.dom.minidom.Document()
        requestNode = doc.createElement("request")
        doc.appendChild(requestNode)
        self.requestCounter += 1
        requestNode.setAttribute("requestId",
                                 "py-{}".format(self.requestCounter))
        apiNode = doc.createElement(requestName)
        requestNode.appendChild(apiNode)
        _addRequestParameters(apiNode, params)
        return requestNode.toxml()

    ############################################################
    # makeEnvelope
    # 
    #     Wrap one or more API request element strings in a <requests> element
    #     and create a well-formed XML string with that as the root element.
    #
    # Arguments:
    #     requests - xml string of one or more <request> elements.
    #     mode - (optional) request mode, applicable if this is a batch
    #            of multiple requests:
    #            single - execute the batch as a single transaction.
    #            serial - execute the batch one request at a time, but in
    #                     separate transactions.
    #            parallel - execute the batch in parallel in separate
    #                     transactions.
    ############################################################
    def makeEnvelope(self, requests, mode=None):
        result = '''<?xml version="1.0" encoding="UTF-8"?>
<requests xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:noNamespaceSchemaLocation="commander.xsd" version="2.0"
    timeout="{}"'''.format(self.timeout)

        if (self.sessionId is not None and len(self.sessionId) > 0):
            result += ' sessionId="{}"'.format(self.sessionId)

        if (mode is not None and len(mode) > 0):
            result += ' mode="{}"'.format(mode)

        result += '''>
{}
</requests>'''.format(requests)
        return str(result)

    ###########################################################################
    ############### Everything below this point is private   ##################
    ############### and users should not call these methods  ##################
    ############### directly.                                ##################
    ###########################################################################

    def _configureUrls(self):
        # This method is mostly transcribed from configureUrls in
        # ElectricCommander.pm.

        # The server information comes from several sources, in order of
        # priority:
        #   - constructor args via server, port, --secure flags
        #   - COMMANDER_SERVER environment variable
        #   - <default> entry in the session file
        #   - 'localhost'

        # First initialize self.server to the env var if it exists and
        # the server arg wasn't provided.
        if (self.server is None and "COMMANDER_SERVER" in environ):
            self.server = environ["COMMANDER_SERVER"]

        defaultServer = 'localhost'
        defaultPort = (("COMMANDER_PORT" in environ)
                       and environ["COMMANDER_PORT"] or 8000)
        defaultSecurePort = (("COMMANDER_HTTPS_PORT" in environ)
                             and environ["COMMANDER_HTTPS_PORT"] or 8443)
        defaultSecure = self.secure
        defaultUrl = self.defaultUrl
        if (self.server is None and defaultUrl is not None
                and len(defaultUrl) > 0):
            # server arg wasn't provided nor is it in the env, but we have a
            # defaultUrl from the session file. Update relevant 'default'
            # vars accordingly.
            m = re.search('http(s?)://(.*):(.*)/', defaultUrl)
            defaultSecure = m.group(1) == "s"
            defaultServer = m.group(2)
            if (defaultSecure):
                defaultSecurePort = m.group(3)
            else:
                defaultPort = m.group(3)
        self.defaultServer = defaultServer

        # Ok, so in the end, do we want secure or not? If we're using the
        # default session, defaultSecure is set to whatever it says. Otherwise
        # it's set to whatever was provided to the constructor arg. That
        # in turn defaults to True, so if the user didn't specify a value
        # for the 'secure' parameter, and we're not using the default
        # session, we'll indeed turn on 'secure'.

        self.secure = defaultSecure

        if (self.server is None):
            # server arg wasn't provided nor is it in the env. defaultServer
            # is currently set from the session file, else localhost. So
            # we have achieved our desired precedence order.
            self.server = defaultServer

        # At this point, self.server is set to *something*.
        # If server was configured with host:port, split the port out

        i = self.server.find(':')
        if i > -1:
            self.port = self.server[i + 1:]
            self.server = self.server[:i]

        if (self.port is None):
            # No port specified in 'server' arg either. Default the
            # port to 8000/8443 depending on if we're doing http/https.
            self.port = self.secure and defaultSecurePort or defaultPort

        # Finally, construct the url.
        self.url = "{}://{}:{}/commanderRequest".format(
            (self.secure and "https" or "http"), self.server, self.port)

    def _readSessionFile(self):
        self.sessionMap = {}
        sessionFile = _getSessionFilePath()
        if (not os.path.exists(sessionFile)):
            self.defaultUser = ''
            self.defaultUrl = None
            return

        doc = xml.dom.minidom.parse(sessionFile)
        sessionsNode = doc.firstChild
        for sessionNode in sessionsNode.getElementsByTagName("session"):
            url = _getText(sessionNode.getElementsByTagName("url")[0])
            user = _getText(sessionNode.getElementsByTagName("user")[0])
            sessionId = _getText(sessionNode.getElementsByTagName(
                "sessionId")[0])
            if (url not in self.sessionMap):
                self.sessionMap[url] = {}
            self.sessionMap[url][user] = sessionId
            if (gFirstUserKey not in self.sessionMap[url]):
                self.sessionMap[url][gFirstUserKey] = user

        # Look for the default session info now.
        for defaultNode in sessionsNode.getElementsByTagName("default"):
            self.defaultUrl = _getText(
                defaultNode.getElementsByTagName("url")[0])
            self.defaultUser = _getText(
                defaultNode.getElementsByTagName("user")[0])


def _addRequestParameters(parentElt, params):
    doc = parentElt.ownerDocument

    # Iterate over each key/value pair.  The value may be a string, list, or
    # dictionary.  If a dictionary, then the contents are emitted as
    # subelements of $key.  If a list, then multiple $key elements are
    # emitted, with the contents of each array interpreted recursively.
    #
    # This logic is largely a translation of the addParameters subroutine
    # in ElectricCommander.pm
    for key, value in params.items():
        if (isinstance(value, dict)):
            if (len(value) > 0):
                newChild = doc.createElement(key)
                parentElt.appendChild(newChild)
                _addRequestParameters(newChild, value)
        elif (isinstance(value, list)):
            # Add a key element with child v for each element of 'value'
            for v in value:
                newChild = doc.createElement(key)
                parentElt.appendChild(newChild)
                if (isinstance(v, dict)):
                    _addRequestParameters(newChild, v)
                else:
                    # Should be some kind of scalar type (string, int, etc.)
                    # Create the element with value being 'v' coerced to a
                    # string.
                    valNode = doc.createTextNode(str(v))
                    newChild.appendChild(valNode)
        else:
            # Should be some kind of scalar type (string, int, etc.)
            # Create the element with value being 'value' coerced to a string.
            newChild = doc.createElement(key)
            parentElt.appendChild(newChild)
            valNode = doc.createTextNode(str(value))
            newChild.appendChild(valNode)


######################################################################
# _getSessionFilePath
#
#     Compute the location of the session file.  Examines the
#     COMMANDER_SESSIONFILE environment variable and a set of platform
#     dependent default locations. Returns the first file that exists, or the
#     default location if no file was found at any of the locations.
######################################################################

def _getSessionFilePath():
    # If a location is explicitly specified, use that.
    if ("COMMANDER_SESSIONFILE" in environ
            and len(environ["COMMANDER_SESSIONFILE"]) > 0):
        return environ["COMMANDER_SESSIONFILE"]

    if (os.name == "nt" and "USERPROFILE" in environ):
        # On Windows, check for the file in the expected location first
        appData = os.path.join(environ["USERPROFILE"], 'Local Settings',
                               'Application Data')
        default = os.path.join(appData, 'Electric Cloud', 'ElectricCommander',
                               '.ecsession')
        if (os.path.exists(default)):
            return default

        # If it wasn't there, check the old default
        backup = os.path.join(appData, 'ElectricCommander', '.ecsession')
        if (os.path.exists(backup)):
            return backup

        # No file found, so use the normal default. 
        return default

    if ("HOME" in environ and len(environ["HOME"]) > 0):
        # Put the file in the user's home directory. (Unix)
        return os.path.join(environ["HOME"], '/.ecsession')

    # None of the normal environment variables are defined, so use the
    # working directory
    return os.path.join('.', '.ecsession')


# Get the text value of a node.
def _getText(node):
    rc = []
    for textNode in node.childNodes:
        if textNode.nodeType == node.TEXT_NODE or textNode.nodeType == node.CDATA_SECTION_NODE:
            rc.append(textNode.data)
    return ''.join(rc)