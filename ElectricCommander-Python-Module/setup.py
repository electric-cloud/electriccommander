from distutils.core import setup
setup(name='ec',
      version='0.1.2',
      requires=['httplib2'],
      author='Sandeep Tamhankar',
      author_email='sandman@electric-cloud.com',
      url='http://www.electric-cloud.com',
      description='A Python interface to ElectricCommander, analogous to the ElectricCommander.pm Perl module.',
      license='MIT',
      long_description="""
This module provides the ElectricCommander class for communicating with an
ElectricCommander server. It supports keep-alive connections, so it's an
efficient way to issue multiple requests to the Commander server.

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


Known Limitations:
* https doesn't work on Python 3.x.
* It does not update the session file.
* It doesn't have a concept of positional arguments for api calls (except 
  login); you must always specify request parameters in a dictionary.
* It doesn't automatically tack on context arguments (e.g. jobStepId for
  property api calls when run in a job-step context).
* It doesn't have a batch api, but you can use the public methods to issue
  batch requests yourself.
* It has no retry behavior for failed requests.
* Functions from the Perl API that are more than just wrappers around issuing
  server requests are not implemented. This includes (but is not limited to)
  installPlugin, publishArtifactVersion, retrieveArtifactVersions, and a few
  others.
        """,
      py_modules=['ec'],
      )
