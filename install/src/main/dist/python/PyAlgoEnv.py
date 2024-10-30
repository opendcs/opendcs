#
# This file contains canned variables and functions for the CCP PythonAlgorithm.
# The code in this method is called after instantiating the PythonInterpreter.
#
from decodes.tsdb.algo import PythonAlgorithm
from decodes.tsdb import NoSuchObjectException
from decodes.tsdb import NoValueException

def get_dcstool_home():
	ret = os.environ.get("DCSTOOL_HOME")
	if ret is None:	
		import java.lang.System as jsys
		ret = jsys.getProperty("DCSTOOL_HOME", "<DCSTOOL_HOME environment variable or java property is not set>")
	return ret
	
import sys,os

custom_packages = get_dcstool_home()+"/bin/opendcs.jar/python-packages"
if custom_packages not in sys.path:
	sys.path.append(custom_packages)
# The Java code places the running instance into a static var
# so that it is availabe to Python:
algo = PythonAlgorithm.getRunningInstance()
algo.debug1('Retrieved Running PythonAlgorithm Instance in Python')
missingValue=-9000000000000.
missingLimit=-8999999999900.

# define the algorithm parameter class
class AlgoParm:
	def __init__(self, tsid, value=missingValue, qual=0x40000000):
		self.tsid = tsid
		self.value = value
		self.qual = qual

def warning(msg):
	algo.warning(msg)

def info(msg):
	algo.info(msg)

def debug1(msg):
	algo.debug1(msg)

def debug2(msg):
	algo.debug2(msg)

def debug3(msg):
	algo.debug3(msg)

def setOutput(rolename, value):
	globals()[rolename].value = value
	if value < missingLimit:
		return
	algo.setOutput(rolename, value)

def isPresent(rolename):
	return algo.isPresent(rolename)

def isQuestionable(rolename):
	return algo.isQuestionable(rolename)

def isRejected(rolename):
	return algo.isRejected(rolename)

def isMissing(rolename):
	return isPresent(rolename) != 0

def isGoodQuality(rolename):
	return algo.isGoodQuality(rolename)

def runningAverage(rolename, duration, boundaries = '(]'):
	return algo.runningAverage(rolename, duration, boundaries)

def getAggregateCount():
	return algo.getAggregateCount()

def datchk(rolename):
	globals()[rolename].qual = algo.datchk(rolename)

def screening(rolename):
	globals()[rolename].qual = algo.screening(rolename)

def setQual(rolename, qual):
	algo.setQual(rolename, qual)
	globals()[rolename].qual = qual

def setOutputAndQual(rolename, value, qual):
	globals()[rolename].value = value
	globals()[rolename].qual = qual
	if value < missingLimit:
		return
	algo.setOutputAndQual(rolename, value, qual)

def isNew(rolename):
	return algo.isNew(rolename)

def changeSince(rolename, duration):
	return algo.changeSince(rolename, duration)

def rating(specId, *indep):
	for v in indep:
		if v < missingLimit:
			warning('Rating failed: One of the indeps is not present.')
			return missingValue
	return algo.rating(specId, indep)

def rdbrating(tabfile, indep):
	if indep < missingLimit:
		warning('RDB Rating failed: indep is not present.')
		return missingValue
	return algo.rdbrating(tabfile, indep)

def tabrating(tabfile, indep):
	if indep == missingLimit:
		warning('TAB Rating failed: indep is not present.')
		return missingValue
	return algo.tabrating(tabfile, indep)

def abortComp(msg):
	algo.abortComp(msg)

def getConnection(msg):
	return algo.tsdb.getConnection()

