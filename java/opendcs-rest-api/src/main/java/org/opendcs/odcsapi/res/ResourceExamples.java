/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.opendcs.odcsapi.res;

public final class ResourceExamples
{
	private static final String UTILITY_CLASS = "Utility Class";

	private ResourceExamples()
	{
		throw new AssertionError(UTILITY_CLASS);
	}

	static final class AlgorithmExamples
	{
		private AlgorithmExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"algorithmId\": 4,\n" +
				"  \"name\": \"\",\n" +
				"  \"execClass\": \"\",\n" +
				"  \"description\": \"\",\n" +
				"  \"props\": {\n" +
				"    \"input1_MISSING\": \"\",\n" +
				"    \"chooseHigher\": \"true\",\n" +
				"    \"upperLimit\": \"\",\n" +
				"    \"lowerLimit\": \"\",\n" +
				"    \"input2_MISSING\": \"IGNORE\"\n" +
				"  },\n" +
				"  \"parms\": {\n" +
				"    \"roleName\": \"\",\n" +
				"    \"parmType\": \"\"\n" +
				"  },\n" +
				"  \"numCompsUsing\": 1,\n" +
				"  \"algoScripts\": {}\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"name\": \"ChooseOne\",\n" +
				"  \"execClass\": \"decodes.tsdb.algo.ChooseOne\",\n" +
				"  \"description\": \"Given two inputs, output the best one: If only one is present at the time-slice, " +
				"output it. If one is outside the specified upper or lower limit (see properties) output the other. " +
				"If both are acceptable, output the first one. Useful in situations where you have redundant sensors.\",\n" +
				"  \"props\": {\n" +
				"    \"input1_MISSING\": \"IGNORE\",\n" +
				"    \"chooseHigher\": \"true\",\n" +
				"    \"upperLimit\": \"999999999999.9\",\n" +
				"    \"lowerLimit\": \"-999999999999.9\",\n" +
				"    \"input2_MISSING\": \"IGNORE\"\n" +
				"  },\n" +
				"  \"parms\": [{\n" +
				"    \"roleName\": \"input\",\n" +
				"    \"parmType\": \"i\"\n" +
				"  }," +
				"  {\n" +
				"    \"roleName\": \"output\",\n" +
				"    \"parmType\": \"o\"\n" +
				"  }],\n" +
				"  \"numCompsUsing\": 1,\n" +
				"  \"algoScripts\": {}\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"algorithmId\": 4,\n" +
				"  \"name\": \"ChooseOne\",\n" +
				"  \"execClass\": \"decodes.tsdb.algo.ChooseOne\",\n" +
				"  \"description\": \"Given two inputs, output the best one: If only one is present at the time-slice, " +
				"output it. If one is outside the specified upper or lower limit (see properties) output the other. " +
				"If both are acceptable, output the first one. Useful in situations where you have redundant sensors.\",\n" +
				"  \"props\": {\n" +
				"    \"input1_MISSING\": \"IGNORE\",\n" +
				"    \"chooseHigher\": \"true\",\n" +
				"    \"upperLimit\": \"999999999999.9\",\n" +
				"    \"lowerLimit\": \"-999999999999.9\",\n" +
				"    \"input2_MISSING\": \"IGNORE\"\n" +
				"  },\n" +
				"  \"parms\": [{\n" +
				"    \"roleName\": \"input1\",\n" +
				"    \"parmType\": \"i\"\n" +
				"  }," +
				"  {\n" +
				"    \"roleName\": \"input2\",\n" +
				"    \"parmType\": \"i\"\n" +
				"  }," +
				"  {\n" +
				"    \"roleName\": \"output\",\n" +
				"    \"parmType\": \"o\"\n" +
				"  }],\n" +
				"  \"numCompsUsing\": 1,\n" +
				"  \"algoScripts\": [{\n" +
				"    \"text\": \"#Tue May 03 11:42:01 PDT 2022\\nAlgorithmType=TIME_SLICE\\n\",\n" +
				"    \"scriptType\": \"T\"\n" +
				"  }]\n" +
				"}";
	}

	static final class AppExamples
	{
		private AppExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"appId\": 4,\n" +
				"  \"appName\": \"\",\n" +
				"  \"appType\": \"\",\n" +
				"  \"comment\": \"\",\n" +
				"  \"lastModified\": null,\n" +
				"  \"manualEditingApp\": false,\n" +
				"  \"properties\": {}\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"appName\": \"compproc\",\n" +
				"  \"appType\": \"computationprocess\",\n" +
				"  \"comment\": \"Main Computation Process\",\n" +
				"  \"lastModified\": \"2025-03-04T22:03:48.910Z\",\n" +
				"  \"manualEditingApp\": false,\n" +
				"  \"properties\": {\n" +
				"    \"fromName\": \"value\"\n" +
				"  }\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"appId\": 4,\n" +
				"  \"appName\": \"compproc\",\n" +
				"  \"appType\": \"computationprocess\",\n" +
				"  \"comment\": \"Main Computation Process\",\n" +
				"  \"lastModified\": \"2025-03-04T22:03:48.910Z\",\n" +
				"  \"manualEditingApp\": false,\n" +
				"  \"properties\": {\n" +
				"    \"fromName\": \"value\"\n" +
				"  }\n" +
				"}";
	}

	static final class ComputationExamples
	{
		private ComputationExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"computationId\": 4,\n" +
				"  \"name\": \"\",\n" +
				"  \"comment\": \"\",\n" +
				"  \"appId\": 5,\n" +
				"  \"applicationName\": \"compproc\",\n" +
				"  \"lastModified\": null,\n" +
				"  \"enabled\": false,\n" +
				"  \"effectiveStartType\": \"\",\n" +
				"  \"effectiveStartDate\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"effectiveStartInterval\": \"string\",\n" +
				"  \"effectiveEndType\": \"\",\n" +
				"  \"effectiveEndDate\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"effectiveEndInterval\": \"string\",\n" +
				"  \"algorithmId\": 24,\n" +
				"  \"algorithmName\": \"\",\n" +
				"  \"parmList\": [\n" +
				"    {\n" +
				"      \"algoParmType\": \"\",\n" +
				"      \"algoRoleName\": \"\",\n" +
				"      \"tsKey\": 1,\n" +
				"      \"dataTypeId\": 48,\n" +
				"      \"dataType\": \"\",\n" +
				"      \"interval\": \"\",\n" +
				"      \"deltaT\": 0,\n" +
				"      \"deltaTUnits\": \"\",\n" +
				"      \"unitsAbbr\": \"ft\",\n" +
				"      \"siteId\": 1,\n" +
				"      \"siteName\": \"\",\n" +
				"      \"tableSelector\": \"\",\n" +
				"      \"modelId\": 0,\n" +
				"      \"paramType\": \"\",\n" +
				"      \"duration\": \"0\",\n" +
				"      \"version\": \"\",\n" +
				"      \"ifMissing\": \"\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"props\": {\n" +
				"    \"minSamplesNeeded\": \"\",\n" +
				"    \"aggUpperBoundClosed\": \"false\",\n" +
				"    \"aggregateTimeZone\": \"\",\n" +
				"    \"average_tsname\": \"\",\n" +
				"    \"aggLowerBoundClosed\": \"false\"\n" +
				"  },\n" +
				"  \"groupId\": -1,\n" +
				"  \"groupName\": \"\"\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"computationId\": 45,\n" +
				"  \"name\": \"Daily Ave ( ... )\",\n" +
				"  \"comment\": \"Used for calculating daily average.\",\n" +
				"  \"appId\": 35,\n" +
				"  \"applicationName\": \"compproc\",\n" +
				"  \"lastModified\": null,\n" +
				"  \"enabled\": false,\n" +
				"  \"effectiveStartType\": \"Calendar\",\n" +
				"  \"effectiveStartDate\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"effectiveStartInterval\": \"string\",\n" +
				"  \"effectiveEndType\": \"No Limit\",\n" +
				"  \"effectiveEndDate\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"effectiveEndInterval\": \"string\",\n" +
				"  \"algorithmId\": 9876,\n" +
				"  \"algorithmName\": \"AverageAlgorithm\",\n" +
				"  \"parmList\": [\n" +
				"    {\n" +
				"      \"algoParmType\": \"i\",\n" +
				"      \"algoRoleName\": \"input\",\n" +
				"      \"tsKey\": 1,\n" +
				"      \"dataTypeId\": 48,\n" +
				"      \"dataType\": \"Stage\",\n" +
				"      \"interval\": \"15Minutes\",\n" +
				"      \"deltaT\": 0,\n" +
				"      \"deltaTUnits\": \"Hours\",\n" +
				"      \"unitsAbbr\": \"ft\",\n" +
				"      \"siteId\": 1,\n" +
				"      \"siteName\": \"OKVI4\",\n" +
				"      \"tableSelector\": \"string\",\n" +
				"      \"modelId\": 0,\n" +
				"      \"paramType\": \"Inst\",\n" +
				"      \"duration\": \"0\",\n" +
				"      \"version\": \"raw\",\n" +
				"      \"ifMissing\": \"string\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"props\": {\n" +
				"    \"minSamplesNeeded\": \"1\",\n" +
				"    \"aggUpperBoundClosed\": \"false\",\n" +
				"    \"aggregateTimeZone\": \"UTC\",\n" +
				"    \"average_tsname\": \"HG-Ave-Open-Open\",\n" +
				"    \"aggLowerBoundClosed\": \"false\"\n" +
				"  },\n" +
				"  \"groupId\": -1,\n" +
				"  \"groupName\": \"\"\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"computationId\": 45,\n" +
				"  \"name\": \"Daily Ave ( ... )\",\n" +
				"  \"comment\": \"Used for calculating daily average.\",\n" +
				"  \"appId\": 35,\n" +
				"  \"applicationName\": \"compproc\",\n" +
				"  \"lastModified\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"enabled\": false,\n" +
				"  \"effectiveStartType\": \"Calendar\",\n" +
				"  \"effectiveStartDate\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"effectiveStartInterval\": \"string\",\n" +
				"  \"effectiveEndType\": \"No Limit\",\n" +
				"  \"effectiveEndDate\": \"2025-03-04T22:40:47.249Z\",\n" +
				"  \"effectiveEndInterval\": \"string\",\n" +
				"  \"algorithmId\": 9876,\n" +
				"  \"algorithmName\": \"WaterFlowAlgorithm\",\n" +
				"  \"parmList\": [\n" +
				"    {\n" +
				"      \"algoParmType\": \"i\",\n" +
				"      \"algoRoleName\": \"input\",\n" +
				"      \"tsKey\": 1,\n" +
				"      \"dataTypeId\": 48,\n" +
				"      \"dataType\": \"Stage\",\n" +
				"      \"interval\": \"15Minutes\",\n" +
				"      \"deltaT\": 0,\n" +
				"      \"deltaTUnits\": \"Hours\",\n" +
				"      \"unitsAbbr\": \"ft\",\n" +
				"      \"siteId\": 1,\n" +
				"      \"siteName\": \"OKVI4\",\n" +
				"      \"tableSelector\": \"string\",\n" +
				"      \"modelId\": 0,\n" +
				"      \"paramType\": \"Inst\",\n" +
				"      \"duration\": \"0\",\n" +
				"      \"version\": \"raw\",\n" +
				"      \"ifMissing\": \"string\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"props\": {\n" +
				"    \"minSamplesNeeded\": \"1\",\n" +
				"    \"aggUpperBoundClosed\": \"false\",\n" +
				"    \"aggregateTimeZone\": \"UTC\",\n" +
				"    \"average_tsname\": \"HG-Ave-Open-Open\",\n" +
				"    \"aggLowerBoundClosed\": \"false\"\n" +
				"  },\n" +
				"  \"groupId\": 2468,\n" +
				"  \"groupName\": \"AverageGroup\"\n" +
				"}";
	}

	static final class ConfigExamples
	{
		private ConfigExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"configId\": 12,\n" +
				"  \"configSensors\": [\n" +
				"    {\n" +
				"      \"dataTypes\": {},\n" +
				"      \"properties\": {},\n" +
				"      \"recordingInterval\": 3600,\n" +
				"      \"recordingMode\": \"\",\n" +
				"      \"sensorName\": \"\",\n" +
				"      \"sensorNumber\": 1,\n" +
				"      \"timeOfFirstSample\": 0\n" +
				"    }\n" +
				"  ],\n" +
				"  \"description\": \"\",\n" +
				"  \"name\": \"\",\n" +
				"  \"numPlatforms\": 0,\n" +
				"  \"scripts\": [\n" +
				"    {\n" +
				"      \"dataOrder\": \"\",\n" +
				"      \"headerType\": \"decodes:goes\",\n" +
				"      \"formatStatements\": [\n" +
				"        {\n" +
				"          \"format\": \"\",\n" +
				"          \"label\": \"\",\n" +
				"          \"sequenceNum\": 1\n" +
				"        }\n" +
				"      ],\n" +
				"      \"name\": \"ST\",\n" +
				"      \"scriptSensors\": [\n" +
				"        {\n" +
				"          \"sensorNumber\": 1,\n" +
				"          \"unitConverter\": {\n" +
				"            \"a\": 0,\n" +
				"            \"algorithm\": \"none\",\n" +
				"            \"b\": 0,\n" +
				"            \"c\": 0,\n" +
				"            \"d\": 0,\n" +
				"            \"e\": 0,\n" +
				"            \"f\": 0,\n" +
				"            \"fromAbbr\": \"\",\n" +
				"            \"toAbbr\": \"\"\n" +
				"          }\n" +
				"        }\n" +
				"      ]\n" +
				"    }\n" +
				"  ]\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"configId\": 12,\n" +
				"  \"configSensors\": [\n" +
				"    {\n" +
				"      \"dataTypes\": {\n" +
				"        \"NL-SHEF\": \"WL\",\n" +
				"        \"SHEF-PE\": \"HG\",\n" +
				"        \"W-SHEF\": \"WL\"\n" +
				"      },\n" +
				"      \"properties\": {},\n" +
				"      \"recordingInterval\": 3600,\n" +
				"      \"recordingMode\": \"F\",\n" +
				"      \"sensorName\": \"WL\",\n" +
				"      \"sensorNumber\": 1,\n" +
				"      \"timeOfFirstSample\": 0\n" +
				"    }\n" +
				"  ],\n" +
				"  \"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n" +
				"  \"name\": \"Shef-WSC-Hydro-RVERMMAR\",\n" +
				"  \"numPlatforms\": 0,\n" +
				"  \"scripts\": [\n" +
				"    {\n" +
				"      \"dataOrder\": \"D\",\n" +
				"      \"headerType\": \"decodes:goes\",\n" +
				"      \"formatStatements\": [\n" +
				"        {\n" +
				"          \"format\": \"s(50,':',DONE),x,F(F,A,10d' ')\",\n" +
				"          \"label\": \"getlabel\",\n" +
				"          \"sequenceNum\": 0\n" +
				"        }\n" +
				"      ],\n" +
				"      \"name\": \"ST\",\n" +
				"      \"scriptSensors\": [\n" +
				"        {\n" +
				"          \"sensorNumber\": 1,\n" +
				"          \"unitConverter\": {\n" +
				"            \"a\": 0,\n" +
				"            \"algorithm\": \"none\",\n" +
				"            \"b\": 0,\n" +
				"            \"c\": 0,\n" +
				"            \"d\": 0,\n" +
				"            \"e\": 0,\n" +
				"            \"f\": 0,\n" +
				"            \"fromAbbr\": \"raw\",\n" +
				"            \"toAbbr\": \"M\"\n" +
				"          }\n" +
				"        }\n" +
				"      ]\n" +
				"    }\n" +
				"  ]\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"configId\": 12,\n" +
				"  \"configSensors\": [\n" +
				"    {\n" +
				"      \"dataTypes\": {\n" +
				"        \"NL-SHEF\": \"WL\",\n" +
				"        \"SHEF-PE\": \"HG\",\n" +
				"        \"W-SHEF\": \"WL\"\n" +
				"      },\n" +
				"      \"properties\": {},\n" +
				"      \"recordingInterval\": 3600,\n" +
				"      \"recordingMode\": \"F\",\n" +
				"      \"sensorName\": \"WL\",\n" +
				"      \"sensorNumber\": 1,\n" +
				"      \"timeOfFirstSample\": 0\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataTypes\": {\n" +
				"        \"SHEF-PE\": \"VB\"\n" +
				"      },\n" +
				"      \"properties\": {},\n" +
				"      \"recordingInterval\": 3600,\n" +
				"      \"recordingMode\": \"F\",\n" +
				"      \"sensorName\": \"VB\",\n" +
				"      \"sensorNumber\": 4,\n" +
				"      \"timeOfFirstSample\": 0\n" +
				"    }\n" +
				"  ],\n" +
				"  \"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n" +
				"  \"name\": \"Shef-WSC-Hydro-RVERMMAR\",\n" +
				"  \"numPlatforms\": 0,\n" +
				"  \"scripts\": [\n" +
				"    {\n" +
				"      \"dataOrder\": \"D\",\n" +
				"      \"headerType\": \"decodes:goes\",\n" +
				"      \"formatStatements\": [\n" +
				"        {\n" +
				"          \"format\": \"s(50,':',DONE),x,F(F,A,10d' ')\",\n" +
				"          \"label\": \"getlabel\",\n" +
				"          \"sequenceNum\": 0\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'#',getlabel),x,f(mint,a,3d' +-',1),32(w,c(N,skiphg),F(S,A,12d' +-:',1)), >GETLABEL\",\n" +
				"          \"label\": \"hg\",\n" +
				"          \"sequenceNum\": 1\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'.',getlabel),-2x,32(w,c(N,getlabel),F(S,A,12d' +-:',4)), >GETLABEL\",\n" +
				"          \"label\": \"vb\",\n" +
				"          \"sequenceNum\": 2\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'.',enddata),-2x,32(w,c(N,enddata),F(S,A,12d' +-:',4)), >GETLABEL\",\n" +
				"          \"label\": \"x-nointerval\",\n" +
				"          \"sequenceNum\": 3\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \">vb\",\n" +
				"          \"label\": \"battery\",\n" +
				"          \"sequenceNum\": 4\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"-3x,>getlabel\",\n" +
				"          \"label\": \"enddata\",\n" +
				"          \"sequenceNum\": 5\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"/\",\n" +
				"          \"label\": \"error\",\n" +
				"          \"sequenceNum\": 6\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"/,>getlabel\",\n" +
				"          \"label\": \"done\",\n" +
				"          \"sequenceNum\": 7\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"-2x,5(S(50,':',done),S(3,'HG',enddata))>getlabel\",\n" +
				"          \"label\": \"skiphg\",\n" +
				"          \"sequenceNum\": 8\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'#',getlabel),x,f(mint,a,3d' +-',6),32(w,c(N,enddata),F(S,A,12d' +-:',6)), >GETLABEL\",\n" +
				"          \"label\": \"pr\",\n" +
				"          \"sequenceNum\": 9\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \">pr\",\n" +
				"          \"label\": \"pc\",\n" +
				"          \"sequenceNum\": 10\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \">hg\",\n" +
				"          \"label\": \"hk\",\n" +
				"          \"sequenceNum\": 11\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'#',getlabel),x,f(mint,a,3d' +-',7),32(w,c(N,enddata),F(S,A,12d' +-:',7)), >GETLABEL\",\n" +
				"          \"label\": \"tx\",\n" +
				"          \"sequenceNum\": 12\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'#',getlabel),x,f(mint,a,3d' +-',8),32(w,c(N,enddata),F(S,A,12d' +-:',8)), >GETLABEL\",\n" +
				"          \"label\": \"tn\",\n" +
				"          \"sequenceNum\": 13\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'.',enddata),-2x,32(w,c(N,enddata),F(S,A,12d' +-:',4)), >GETLABEL\",\n" +
				"          \"label\": \"vb-x\",\n" +
				"          \"sequenceNum\": 14\n" +
				"        },\n" +
				"        {\n" +
				"          \"format\": \"s(12,'#',getlabel),x,f(mint,a,3d' +-',9),32(w,c(N,skiphg),F(S,A,12d' +-:',9)), >GETLABEL\",\n" +
				"          \"label\": \"hh\",\n" +
				"          \"sequenceNum\": 15\n" +
				"        }\n" +
				"      ],\n" +
				"      \"name\": \"ST\",\n" +
				"      \"scriptSensors\": [\n" +
				"        {\n" +
				"          \"sensorNumber\": 1,\n" +
				"          \"unitConverter\": {\n" +
				"            \"a\": 0,\n" +
				"            \"algorithm\": \"none\",\n" +
				"            \"b\": 0,\n" +
				"            \"c\": 0,\n" +
				"            \"d\": 0,\n" +
				"            \"e\": 0,\n" +
				"            \"f\": 0,\n" +
				"            \"fromAbbr\": \"raw\",\n" +
				"            \"toAbbr\": \"M\"\n" +
				"          }\n" +
				"        },\n" +
				"        {\n" +
				"          \"sensorNumber\": 4,\n" +
				"          \"unitConverter\": {\n" +
				"            \"a\": 0,\n" +
				"            \"algorithm\": \"none\",\n" +
				"            \"b\": 0,\n" +
				"            \"c\": 0,\n" +
				"            \"d\": 0,\n" +
				"            \"e\": 0,\n" +
				"            \"f\": 0,\n" +
				"            \"fromAbbr\": \"raw\",\n" +
				"            \"toAbbr\": \"V\"\n" +
				"          }\n" +
				"        }\n" +
				"      ]\n" +
				"    }\n" +
				"  ]\n" +
				"}";
	}

	static final class DataSourceExamples
	{
		private DataSourceExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"dataSourceId\": 10,\n" +
				"  \"groupMembers\": [\n" +
				"    {\n" +
				"      \"dataSourceId\": 4,\n" +
				"      \"dataSourceName\": \"\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"name\": \"\",\n" +
				"  \"props\": {},\n" +
				"  \"type\": \"\",\n" +
				"  \"usedBy\": 0\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"groupMembers\": [\n" +
				"    {\n" +
				"      \"dataSourceId\": 4,\n" +
				"      \"dataSourceName\": \"Cove-LRGS\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"name\": \"testgroup\",\n" +
				"  \"props\": {},\n" +
				"  \"type\": \"hotbackupgroup\",\n" +
				"  \"usedBy\": 0\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"dataSourceId\": 10,\n" +
				"  \"groupMembers\": [\n" +
				"    {\n" +
				"      \"dataSourceId\": 4,\n" +
				"      \"dataSourceName\": \"Cove-LRGS\"\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataSourceId\": 5,\n" +
				"      \"dataSourceName\": \"CDADATA-As-MBHydro\"\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataSourceId\": 7,\n" +
				"      \"dataSourceName\": \"USGS-Web\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"name\": \"testgroup\",\n" +
				"  \"props\": {\n" +
				"    \"whatevs\": \"something 567\",\n" +
				"    \"bc\": \"def\"\n" +
				"  },\n" +
				"  \"type\": \"hotbackupgroup\",\n" +
				"  \"usedBy\": 0\n" +
				"}";
	}

	static final class DecodeExamples
	{
		private DecodeExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"rawmsg\": {\n" +
				"    \"flags\": 71765,\n" +
				"    \"platformId\": \"221\",\n" +
				"    \"sequenceNum\": 25693,\n" +
				"    \"localRecvTime\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"carrierStart\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"carrierStop\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"baud\": 300,\n" +
				"    \"goodPhasePct\": 100,\n" +
				"    \"freqOffset\": 0.5,\n" +
				"    \"signalStrength\": 44.8,\n" +
				"    \"phaseNoise\": 1.97,\n" +
				"    \"xmitTime\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"momsn\": 0,\n" +
				"    \"mtmsn\": 0,\n" +
				"    \"cdrReference\": 0,\n" +
				"    \"sessionStatus\": 0,\n" +
				"    \"base64\": \"Q0UzMUQwMzAyMzEyOTEyMzQ1NUc0NSswTk4xNjFFTjIwMDAyN2JCMURBTXRBTXRBTXRBTXM6WUIgMTMuNTkgIA==\"\n" +
				"  },\n" +
				"  \"config\": {\n" +
				"    \"configId\": 12,\n" +
				"    \"name\": \"\",\n" +
				"    \"numPlatforms\": 0,\n" +
				"    \"description\": \"\",\n" +
				"    \"configSensors\": [\n" +
				"      {\n" +
				"        \"sensorNumber\": 1,\n" +
				"        \"sensorName\": \"\",\n" +
				"        \"recordingMode\": \"\",\n" +
				"        \"recordingInterval\": 3600,\n" +
				"        \"timeOfFirstSample\": 0,\n" +
				"        \"absoluteMin\": 0,\n" +
				"        \"absoluteMax\": 100,\n" +
				"        \"properties\": {},\n" +
				"        \"dataTypes\": {},\n" +
				"        \"usgsStatCode\": \"\"\n" +
				"      }\n" +
				"    ],\n" +
				"    \"scripts\": [\n" +
				"      {\n" +
				"        \"name\": \"ST\",\n" +
				"        \"dataOrder\": \"\",\n" +
				"        \"headerType\": \"decodes:goes\",\n" +
				"        \"scriptSensors\": {\n" +
				"          \"sensorNumber\": 1,\n" +
				"          \"unitConverter\": {\n" +
				"            \"ucId\": 101,\n" +
				"            \"fromAbbr\": \"\",\n" +
				"            \"toAbbr\": \"\",\n" +
				"            \"algorithm\": \"none\",\n" +
				"            \"a\": 0,\n" +
				"            \"b\": 0,\n" +
				"            \"c\": 0,\n" +
				"            \"d\": 0,\n" +
				"            \"e\": 0,\n" +
				"            \"f\": 0\n" +
				"          }\n" +
				"        },\n" +
				"        \"formatStatements\": {\n" +
				"          \"sequenceNum\": 0,\n" +
				"          \"label\": \"\",\n" +
				"          \"format\": \"\"\n" +
				"        }\n" +
				"      }\n" +
				"    ]\n" +
				"  }\n" +
				"}";

		public static final String VERBOSE = "{\n" +
				"  \"rawmsg\": {\n" +
				"    \"flags\": 71765,\n" +
				"    \"platformId\": \"221\",\n" +
				"    \"sequenceNum\": 25693,\n" +
				"    \"localRecvTime\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"carrierStart\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"carrierStop\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"baud\": 300,\n" +
				"    \"goodPhasePct\": 100,\n" +
				"    \"freqOffset\": 0.5,\n" +
				"    \"signalStrength\": 44.8,\n" +
				"    \"phaseNoise\": 1.97,\n" +
				"    \"xmitTime\": \"2025-03-04T22:54:46.693Z\",\n" +
				"    \"momsn\": 0,\n" +
				"    \"mtmsn\": 0,\n" +
				"    \"cdrReference\": 0,\n" +
				"    \"sessionStatus\": 0,\n" +
				"    \"base64\": \"Q0UzMUQwMzAyMzEyOTEyMzQ1NUc0NSswTk4xNjFFTjIwMDAyN2JCMURBTXRBTXRBTXRBTXM6WUIgMTMuNTkgIA==\"\n" +
				"  },\n" +
				"  \"config\": {\n" +
				"    \"configId\": 1,\n" +
				"    \"name\": \"Shef-WSC-Hydro-RVERMMAR\",\n" +
				"    \"numPlatforms\": 12,\n" +
				"    \"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n" +
				"    \"configSensors\": [\n" +
				"      {\n" +
				"        \"sensorNumber\": 13,\n" +
				"        \"sensorName\": \"WL\",\n" +
				"        \"recordingMode\": \"F\",\n" +
				"        \"recordingInterval\": 3600,\n" +
				"        \"timeOfFirstSample\": 0,\n" +
				"        \"absoluteMin\": 0,\n" +
				"        \"absoluteMax\": 100,\n" +
				"        \"properties\": {\n" +
				"          \"additionalProp1\": \"value1\",\n" +
				"          \"additionalProp2\": \"value2\",\n" +
				"          \"additionalProp3\": \"value3\"\n" +
				"        },\n" +
				"        \"dataTypes\": {\n" +
				"          \"NL-SHEF\": \"WL\",\n" +
				"          \"SHEF-PE\": \"HG\",\n" +
				"          \"W-SHEF\": \"WL\"\n" +
				"        },\n" +
				"        \"usgsStatCode\": \"string\"\n" +
				"      },\n" +
				"      {\n" +
				"        \"sensorNumber\": 4,\n" +
				"        \"sensorName\": \"VB\",\n" +
				"        \"recordingMode\": \"F\",\n" +
				"        \"recordingInterval\": 3600,\n" +
				"        \"timeOfFirstSample\": 0,\n" +
				"        \"absoluteMin\": 0,\n" +
				"        \"absoluteMax\": 100,\n" +
				"        \"properties\": {},\n" +
				"        \"dataTypes\": {\n" +
				"          \"SHEF-PE\": \"VB\"\n" +
				"        },\n" +
				"        \"usgsStatCode\": \"string\"\n" +
				"      }\n" +
				"    ],\n" +
				"    \"scripts\": [\n" +
				"      {\n" +
				"        \"name\": \"ST\",\n" +
				"        \"dataOrder\": \"D\",\n" +
				"        \"headerType\": \"decodes:goes\",\n" +
				"        \"scriptSensors\": [{\n" +
				"          \"sensorNumber\": 1,\n" +
				"          \"unitConverter\": {\n" +
				"            \"ucId\": 101,\n" +
				"            \"fromAbbr\": \"CFS\",\n" +
				"            \"toAbbr\": \"CMS\",\n" +
				"            \"algorithm\": \"string\",\n" +
				"            \"a\": 1,\n" +
				"            \"b\": 2,\n" +
				"            \"c\": 3,\n" +
				"            \"d\": 4,\n" +
				"            \"e\": 5,\n" +
				"            \"f\": 6\n" +
				"          }\n" +
				"        }],\n" +
				"        \"formatStatements\": [{\n" +
				"          \"sequenceNum\": 0,\n" +
				"          \"label\": \"getlabel\",\n" +
				"          \"format\": \"s(50,':',DONE),x,F(F,A,10d' ')\"\n" +
				"        },\n" +
				"        {\n" +
				"          \"sequenceNum\": 1,\n" +
				"          \"label\": \"hg\",\n" +
				"          \"format\": \"s(12,'#',getlabel),x,f(mint,a,3d' +-',1),32(w,c(N,skiphg),F(S,A,12d' +-:',1)), >GETLABEL\"\n" +
				"        },\n" +
				"        {\n" +
				"          \"sequenceNum\": 2,\n" +
				"          \"label\": \"vb\",\n" +
				"          \"format\": \"s(12,'.',getlabel),-2x,32(w,c(N,getlabel),F(S,A,12d' +-:',4)), >GETLABEL\"\n" +
				"        }]\n" +
				"      }\n" +
				"    ]\n" +
				"  }\n" +
				"}";
	}

	static final class NetlistExamples
	{
		private NetlistExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"items\": {\n" +
				"    \"\": {\n" +
				"      \"description\": \"\",\n" +
				"      \"platformName\": \"\",\n" +
				"      \"transportId\": \"\"\n" +
				"    }\n" +
				"  },\n" +
				"  \"lastModifyTime\": \"\",\n" +
				"  \"name\": \"\",\n" +
				"  \"netlistId\": 4,\n" +
				"  \"siteNameTypePref\": \"\",\n" +
				"  \"transportMediumType\": \"\"\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"items\": {\n" +
				"    \"14159500\": {\n" +
				"      \"description\": \"\",\n" +
				"      \"platformName\": \"CGRO\",\n" +
				"      \"transportId\": \"14159500\"\n" +
				"    }\n" +
				"  },\n" +
				"  \"lastModifyTime\": null,\n" +
				"  \"name\": \"USGS-Sites\",\n" +
				"  \"siteNameTypePref\": \"nwshb5\",\n" +
				"  \"transportMediumType\": \"other\"\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"items\": {\n" +
				"    \"14159500\": {\n" +
				"      \"description\": \"\",\n" +
				"      \"platformName\": \"CGRO\",\n" +
				"      \"transportId\": \"14159500\"\n" +
				"    },\n" +
				"    \"14372300\": {\n" +
				"      \"description\": \"\",\n" +
				"      \"platformName\": \"AGNO\",\n" +
				"      \"transportId\": \"14372300\"\n" +
				"    }\n" +
				"  },\n" +
				"  \"lastModifyTime\": \"2020-10-19T18:14:14.788Z[UTC]\",\n" +
				"  \"name\": \"USGS-Sites\",\n" +
				"  \"netlistId\": 4,\n" +
				"  \"siteNameTypePref\": \"nwshb5\",\n" +
				"  \"transportMediumType\": \"other\"\n" +
				"}";
	}

	static final class PlatformExamples
	{
		private PlatformExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"agency\": \"\",\n" +
				"  \"configId\": 6,\n" +
				"  \"description\": \"\",\n" +
				"  \"lastModified\": \"\",\n" +
				"  \"platformId\": 5,\n" +
				"  \"platformSensors\": [\n" +
				"    {\n" +
				"      \"max\": 120,\n" +
				"      \"min\": -40,\n" +
				"      \"sensorNum\": 1,\n" +
				"      \"sensorProps\": {},\n" +
				"      \"actualSiteId\": 5\n" +
				"    }\n" +
				"  ],\n" +
				"  \"production\": false,\n" +
				"  \"properties\": {},\n" +
				"  \"siteId\": 8,\n" +
				"  \"transportMedia\": [\n" +
				"    {\n" +
				"      \"assignedTime\": 2095,\n" +
				"      \"baud\": 0,\n" +
				"      \"channelNum\": 161,\n" +
				"      \"dataBits\": 0,\n" +
				"      \"doLogin\": false,\n" +
				"      \"mediumId\": \"\",\n" +
				"      \"mediumType\": \"\",\n" +
				"      \"parity\": \"\",\n" +
				"      \"scriptName\": \"\",\n" +
				"      \"stopBits\": 0,\n" +
				"      \"timeAdjustment\": 0,\n" +
				"      \"timezone\": \"\",\n" +
				"      \"transportInterval\": 3600,\n" +
				"      \"transportWindow\": 5\n" +
				"    }\n" +
				"  ]\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"agency\": \"CWMS\",\n" +
				"  \"configId\": 6,\n" +
				"  \"description\": \"Ball Mountain Dam, West River,VT\",\n" +
				"  \"lastModified\": \"2022-01-21T14:18:21.176Z[UTC]\",\n" +
				"  \"platformSensors\": [\n" +
				"    {\n" +
				"      \"max\": 120,\n" +
				"      \"min\": -40,\n" +
				"      \"sensorNum\": 1,\n" +
				"      \"sensorProps\": {},\n" +
				"      \"actualSiteId\": 5\n" +
				"    }\n" +
				"  ],\n" +
				"  \"production\": false,\n" +
				"  \"properties\": {},\n" +
				"  \"siteId\": 8,\n" +
				"  \"transportMedia\": [\n" +
				"    {\n" +
				"      \"assignedTime\": 2095,\n" +
				"      \"baud\": 0,\n" +
				"      \"channelNum\": 161,\n" +
				"      \"dataBits\": 0,\n" +
				"      \"doLogin\": false,\n" +
				"      \"mediumId\": \"CE31D030\",\n" +
				"      \"mediumType\": \"goes-self-timed\",\n" +
				"      \"parity\": \"U\",\n" +
				"      \"scriptName\": \"ST\",\n" +
				"      \"stopBits\": 0,\n" +
				"      \"timeAdjustment\": 0,\n" +
				"      \"timezone\": \"UTC\",\n" +
				"      \"transportInterval\": 3600,\n" +
				"      \"transportWindow\": 5\n" +
				"    }\n" +
				"  ]\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"agency\": \"CWMS\",\n" +
				"  \"configId\": 6,\n" +
				"  \"description\": \"Ball Mountain Dam, West River,VT\",\n" +
				"  \"lastModified\": \"2022-01-21T14:18:21.176Z[UTC]\",\n" +
				"  \"platformId\": 5,\n" +
				"  \"platformSensors\": [\n" +
				"    {\n" +
				"      \"max\": 120,\n" +
				"      \"min\": -40,\n" +
				"      \"sensorNum\": 1,\n" +
				"      \"sensorProps\": {}\n" +
				"    },\n" +
				"    {\n" +
				"      \"sensorNum\": 2,\n" +
				"      \"sensorProps\": {}\n" +
				"    },\n" +
				"    {\n" +
				"      \"sensorNum\": 3,\n" +
				"      \"sensorProps\": {}\n" +
				"    },\n" +
				"    {\n" +
				"      \"sensorNum\": 4,\n" +
				"      \"sensorProps\": {}\n" +
				"    },\n" +
				"    {\n" +
				"      \"actualSiteId\": 5,\n" +
				"      \"sensorNum\": 5,\n" +
				"      \"sensorProps\": {}\n" +
				"    }\n" +
				"  ],\n" +
				"  \"production\": false,\n" +
				"  \"properties\": {},\n" +
				"  \"siteId\": 8,\n" +
				"  \"transportMedia\": [\n" +
				"    {\n" +
				"      \"assignedTime\": 2095,\n" +
				"      \"baud\": 0,\n" +
				"      \"channelNum\": 161,\n" +
				"      \"dataBits\": 0,\n" +
				"      \"doLogin\": false,\n" +
				"      \"mediumId\": \"CE31D030\",\n" +
				"      \"mediumType\": \"goes-self-timed\",\n" +
				"      \"parity\": \"U\",\n" +
				"      \"scriptName\": \"ST\",\n" +
				"      \"stopBits\": 0,\n" +
				"      \"timeAdjustment\": 0,\n" +
				"      \"timezone\": \"UTC\",\n" +
				"      \"transportInterval\": 3600,\n" +
				"      \"transportWindow\": 5\n" +
				"    },\n" +
				"    {\n" +
				"      \"baud\": 0,\n" +
				"      \"channelNum\": 129,\n" +
				"      \"dataBits\": 0,\n" +
				"      \"doLogin\": false,\n" +
				"      \"mediumId\": \"CE31D030\",\n" +
				"      \"mediumType\": \"goes-random\",\n" +
				"      \"parity\": \"U\",\n" +
				"      \"scriptName\": \"RD\",\n" +
				"      \"stopBits\": 0,\n" +
				"      \"timeAdjustment\": 0,\n" +
				"      \"timezone\": \"UTC\",\n" +
				"      \"transportInterval\": 900\n" +
				"    },\n" +
				"    {\n" +
				"      \"baud\": 0,\n" +
				"      \"channelNum\": 235,\n" +
				"      \"dataBits\": 0,\n" +
				"      \"doLogin\": false,\n" +
				"      \"mediumId\": \"CE31D030\",\n" +
				"      \"mediumType\": \"goes\",\n" +
				"      \"parity\": \"U\",\n" +
				"      \"scriptName\": \"Network_Tower\",\n" +
				"      \"stopBits\": 0,\n" +
				"      \"timeAdjustment\": 0,\n" +
				"      \"timezone\": \"UTC\",\n" +
				"      \"transportInterval\": 900\n" +
				"    }\n" +
				"  ]\n" +
				"}";
	}

	static final class PresentationExamples
	{
		private PresentationExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"groupId\": 4,\n" +
				"  \"name\": \"\",\n" +
				"  \"inheritsFrom\": null,\n" +
				"  \"inheritsFromId\": null,\n" +
				"  \"lastModified\": null,\n" +
				"  \"elements\": [\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"\",\n" +
				"      \"dataTypeCode\": \"\",\n" +
				"      \"units\": \"ft\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 1000\n" +
				"    }\n" +
				"  ],\n" +
				"  \"production\": true\n" +
				"}";
		public static final String NEW = "{\n" +
				"  \"name\": \"regtest\",\n" +
				"  \"inheritsFrom\": \"CWMS-English\",\n" +
				"  \"inheritsFromId\": 152,\n" +
				"  \"lastModified\": \"2025-03-04T21:39:33.829Z\",\n" +
				"  \"elements\": [\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"CWMS\",\n" +
				"      \"dataTypeCode\": \"Elev\",\n" +
				"      \"units\": \"ft\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 500\n" +
				"    }\n" +
				"  ],\n" +
				"  \"production\": true\n" +
				"}";
		public static final String UPDATE = "{\n" +
				"  \"groupId\": 4,\n" +
				"  \"name\": \"regtest\",\n" +
				"  \"inheritsFrom\": \"CWMS-English\",\n" +
				"  \"inheritsFromId\": 152,\n" +
				"  \"lastModified\": \"2025-03-04T21:39:33.829Z\",\n" +
				"  \"elements\": [\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"CWMS\",\n" +
				"      \"dataTypeCode\": \"Elev\",\n" +
				"      \"units\": \"ft\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 10000\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"CWMS\",\n" +
				"      \"dataTypeCode\": \"Elev-Pool\",\n" +
				"      \"units\": \"ft\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 1000\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"CWMS\",\n" +
				"      \"dataTypeCode\": \"FLOW-HOLDOUT\",\n" +
				"      \"units\": \"cfs\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 15000\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"CWMS\",\n" +
				"      \"dataTypeCode\": \"FLOW-INFLOW\",\n" +
				"      \"units\": \"cfs\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 15000\n" +
				"    },\n" +
				"    {\n" +
				"      \"dataTypeStd\": \"CWMS\",\n" +
				"      \"dataTypeCode\": \"Temp\",\n" +
				"      \"units\": \"degF\",\n" +
				"      \"fractionalDigits\": 2,\n" +
				"      \"min\": 0,\n" +
				"      \"max\": 212\n" +
				"    }\n" +
				"  ],\n" +
				"  \"production\": true\n" +
				"}";
	}

	static final class ReflistExamples
	{
		private ReflistExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"reflistId\": 3,\n" +
				"  \"enumName\": \"\",\n" +
				"  \"items\": {\n" +
				"    \"standard\": {\n" +
				"      \"value\": \"\",\n" +
				"      \"description\": \"\",\n" +
				"      \"execClassName\": \"\",\n" +
				"      \"editClassName\": null,\n" +
				"      \"sortNumber\": 3\n" +
				"    }\n" +
				"  },\n" +
				"  \"defaultValue\": null,\n" +
				"  \"description\": null\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"enumName\": \"ScriptType\",\n" +
				"  \"items\": {\n" +
				"    \"standard\": {\n" +
				"      \"value\": \"standard\",\n" +
				"      \"description\": \"DECODES Format Statements and Unit Conversions\",\n" +
				"      \"execClassName\": \"DecodesScript\",\n" +
				"      \"editClassName\": null,\n" +
				"      \"sortNumber\": 3\n" +
				"    }\n" +
				"  },\n" +
				"  \"defaultValue\": null,\n" +
				"  \"description\": null\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"reflistId\": 3,\n" +
				"  \"enumName\": \"ScriptType\",\n" +
				"  \"items\": {\n" +
				"    \"standard\": {\n" +
				"      \"value\": \"standard\",\n" +
				"      \"description\": \"DECODES Format Statements and Unit Conversions\",\n" +
				"      \"execClassName\": \"DecodesScript\",\n" +
				"      \"editClassName\": null,\n" +
				"      \"sortNumber\": 3\n" +
				"    },\n" +
				"    \"nos\": {\n" +
				"      \"value\": \"nos\",\n" +
				"      \"description\": \"Hard-coded NOS data parser\",\n" +
				"      \"execClassName\": \"NOSMessageParser\",\n" +
				"      \"editClassName\": null,\n" +
				"      \"sortNumber\": 2\n" +
				"    },\n" +
				"    \"ndbc\": {\n" +
				"      \"value\": \"ndbc\",\n" +
				"      \"description\": \"National Data Buoy Center Context-Sensitive Parser\",\n" +
				"      \"execClassName\": \"NDBCMessageParser\",\n" +
				"      \"editClassName\": null,\n" +
				"      \"sortNumber\": 1\n" +
				"    }\n" +
				"  },\n" +
				"  \"defaultValue\": null,\n" +
				"  \"description\": null\n" +
				"}";
	}

	static final class RoutingExamples
	{
		private RoutingExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"routingId\": 20,\n" +
				"  \"name\": \"Test Routing\",\n" +
				"  \"dataSourceId\": 4,\n" +
				"  \"dataSourceName\": \"\",\n" +
				"  \"destinationType\": \"\",\n" +
				"  \"destinationArg\": \"\",\n" +
				"  \"enableEquations\": true,\n" +
				"  \"outputFormat\": \"\",\n" +
				"  \"outputTZ\": \"\",\n" +
				"  \"presGroupName\": \"\",\n" +
				"  \"lastModified\": null,\n" +
				"  \"since\": \"2022-06-05 00:00:00.000\",\n" +
				"  \"until\": \"2022-06-06 00:00:00.000\",\n" +
				"  \"settlingTimeDelay\": true,\n" +
				"  \"applyTimeTo\": \"\",\n" +
				"  \"ascendingTime\": true,\n" +
				"  \"platformIds\": [],\n" +
				"  \"platformNames\": [],\n" +
				"  \"netlistNames\": [],\n" +
				"  \"goesChannels\": [],\n" +
				"  \"properties\": {},\n" +
				"  \"goesSelfTimed\": true,\n" +
				"  \"goesRandom\": false,\n" +
				"  \"networkDCP\": false,\n" +
				"  \"iridium\": true,\n" +
				"  \"qualityNotifications\": false,\n" +
				"  \"goesSpacecraftCheck\": true,\n" +
				"  \"goesSpacecraftSelection\": \"East\",\n" +
				"  \"parityCheck\": true,\n" +
				"  \"paritySelection\": \"\",\n" +
				"  \"production\": true\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"routingId\": 20,\n" +
				"  \"name\": \"Test Routing\",\n" +
				"  \"dataSourceId\": 10,\n" +
				"  \"dataSourceName\": \"USGS-LRGS\",\n" +
				"  \"destinationType\": \"directory\",\n" +
				"  \"destinationArg\": \"some-directory-path\",\n" +
				"  \"enableEquations\": true,\n" +
				"  \"outputFormat\": \"emit-ascii\",\n" +
				"  \"outputTZ\": \"EST5EDT\",\n" +
				"  \"presGroupName\": \"CWMS-English\",\n" +
				"  \"lastModified\": null,\n" +
				"  \"since\": \"2022-06-05 00:00:00.000\",\n" +
				"  \"until\": \"2022-06-06 00:00:00.000\",\n" +
				"  \"settlingTimeDelay\": true,\n" +
				"  \"applyTimeTo\": \"Both\",\n" +
				"  \"ascendingTime\": true,\n" +
				"  \"platformIds\": [\n" +
				"    \"8675309\"\n" +
				"  ],\n" +
				"  \"platformNames\": [\n" +
				"    \"MROI4\"\n" +
				"  ],\n" +
				"  \"netlistNames\": [\n" +
				"    \"goes1\"\n" +
				"  ],\n" +
				"  \"goesChannels\": [\n" +
				"    123\n" +
				"  ],\n" +
				"  \"properties\": {},\n" +
				"  \"goesSelfTimed\": true,\n" +
				"  \"goesRandom\": false,\n" +
				"  \"networkDCP\": false,\n" +
				"  \"iridium\": true,\n" +
				"  \"qualityNotifications\": false,\n" +
				"  \"goesSpacecraftCheck\": true,\n" +
				"  \"goesSpacecraftSelection\": \"East\",\n" +
				"  \"parityCheck\": true,\n" +
				"  \"paritySelection\": \"Good\",\n" +
				"  \"production\": true\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"routingId\": 20,\n" +
				"  \"name\": \"Test Routing\",\n" +
				"  \"dataSourceId\": 10,\n" +
				"  \"dataSourceName\": \"USGS-LRGS\",\n" +
				"  \"destinationType\": \"directory\",\n" +
				"  \"destinationArg\": \"some-directory-path\",\n" +
				"  \"enableEquations\": true,\n" +
				"  \"outputFormat\": \"emit-ascii\",\n" +
				"  \"outputTZ\": \"EST5EDT\",\n" +
				"  \"presGroupName\": \"CWMS-English\",\n" +
				"  \"lastModified\": \"2025-03-04T21:47:37.861Z\",\n" +
				"  \"since\": \"2022-06-05 00:00:00.000\",\n" +
				"  \"until\": \"2022-06-06 00:00:00.000\",\n" +
				"  \"settlingTimeDelay\": true,\n" +
				"  \"applyTimeTo\": \"Both\",\n" +
				"  \"ascendingTime\": true,\n" +
				"  \"platformIds\": [\n" +
				"    \"123456\",\n" +
				"    \"7754681\"\n" +
				"  ],\n" +
				"  \"platformNames\": [\n" +
				"    \"MROI4\"\n" +
				"  ],\n" +
				"  \"netlistNames\": [\n" +
				"    \"goes1\"\n" +
				"  ],\n" +
				"  \"goesChannels\": [\n" +
				"    123\n" +
				"  ],\n" +
				"  \"properties\": {\n" +
				"    \"additionalProp1\": \"value1\",\n" +
				"    \"additionalProp2\": \"value2\",\n" +
				"    \"additionalProp3\": \"value3\"\n" +
				"  },\n" +
				"  \"goesSelfTimed\": true,\n" +
				"  \"goesRandom\": false,\n" +
				"  \"networkDCP\": false,\n" +
				"  \"iridium\": true,\n" +
				"  \"qualityNotifications\": false,\n" +
				"  \"goesSpacecraftCheck\": true,\n" +
				"  \"goesSpacecraftSelection\": \"East\",\n" +
				"  \"parityCheck\": true,\n" +
				"  \"paritySelection\": \"Good\",\n" +
				"  \"production\": true\n" +
				"}";
	}

	static final class ScheduleExamples
	{
		private ScheduleExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"schedEntryId\": 21,\n" +
				"  \"name\": \"\",\n" +
				"  \"appId\": 14,\n" +
				"  \"appName\": \"\",\n" +
				"  \"routingSpecId\": 9,\n" +
				"  \"routingSpecName\": \"\",\n" +
				"  \"enabled\": false,\n" +
				"  \"lastModified\": null,\n" +
				"  \"startTime\": \"2025-03-04T21:57:55.480Z\",\n" +
				"  \"timeZone\": \"\",\n" +
				"  \"runInterval\": \"15 Minutes\"\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"schedEntryId\": 21,\n" +
				"  \"name\": \"something-else\",\n" +
				"  \"appId\": 14,\n" +
				"  \"appName\": \"RoutingScheduler\",\n" +
				"  \"routingSpecId\": 9,\n" +
				"  \"routingSpecName\": \"goes1\",\n" +
				"  \"enabled\": true,\n" +
				"  \"lastModified\": null,\n" +
				"  \"startTime\": \"2025-03-04T21:57:55.480Z\",\n" +
				"  \"timeZone\": \"America/New_York\",\n" +
				"  \"runInterval\": \"15 Minutes\"\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"schedEntryId\": 21,\n" +
				"  \"name\": \"something-else\",\n" +
				"  \"appId\": 14,\n" +
				"  \"appName\": \"RoutingScheduler\",\n" +
				"  \"routingSpecId\": 9,\n" +
				"  \"routingSpecName\": \"goes1\",\n" +
				"  \"enabled\": true,\n" +
				"  \"lastModified\": \"2025-03-04T21:57:55.480Z\",\n" +
				"  \"startTime\": \"2025-03-04T21:57:55.480Z\",\n" +
				"  \"timeZone\": \"America/New_York\",\n" +
				"  \"runInterval\": \"15 Minutes\"\n" +
				"}";
	}

	static final class SiteExamples
	{
		private SiteExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"siteId\": 7,\n" +
				"  \"sitenames\": {\n" +
				"    \"CWMS\": \"\",\n" +
				"    \"NWSHB5\": \"\"\n" +
				"  },\n" +
				"  \"description\": \"\",\n" +
				"  \"latitude\": \"42.4278\",\n" +
				"  \"longitude\": \"-72.06261\",\n" +
				"  \"elevation\": 234.7,\n" +
				"  \"elevUnits\": \"\",\n" +
				"  \"nearestCity\": \"\",\n" +
				"  \"timezone\": \"\",\n" +
				"  \"state\": \"\",\n" +
				"  \"country\": \"USA\",\n" +
				"  \"region\": \"\",\n" +
				"  \"active\": true,\n" +
				"  \"locationType\": \"\",\n" +
				"  \"publicName\": \"\",\n" +
				"  \"properties\": {},\n" +
				"  \"lastModified\": null\n" +
				"}";

		public static final String NEW = "{\n" +
				"  \"siteId\": 45,\n" +
				"  \"sitenames\": {\n" +
				"    \"CWMS\": \"BFD\",\n" +
				"    \"NWSHB5\": \"BFD\"\n" +
				"  },\n" +
				"  \"description\": \"Barre Falls Dam. Ware River\",\n" +
				"  \"latitude\": \"42.4278\",\n" +
				"  \"longitude\": \"-72.06261\",\n" +
				"  \"elevation\": 234.7,\n" +
				"  \"elevUnits\": \"M\",\n" +
				"  \"nearestCity\": \"Barre Falls Dam\",\n" +
				"  \"timezone\": \"America/New_York\",\n" +
				"  \"state\": \"MA\",\n" +
				"  \"country\": \"USA\",\n" +
				"  \"region\": \"\",\n" +
				"  \"active\": true,\n" +
				"  \"locationType\": \"\",\n" +
				"  \"publicName\": \"BFD\",\n" +
				"  \"properties\": {},\n" +
				"  \"lastModified\": null\n" +
				"}";

		public static final String UPDATE = "{\n" +
				"  \"siteId\": 45,\n" +
				"  \"sitenames\": {\n" +
				"    \"CWMS\": \"BFD\",\n" +
				"    \"NWSHB5\": \"BFD\"\n" +
				"  },\n" +
				"  \"description\": \"Barre Falls Dam. Ware River\",\n" +
				"  \"latitude\": \"42.4278\",\n" +
				"  \"longitude\": \"-72.06261\",\n" +
				"  \"elevation\": 234.7,\n" +
				"  \"elevUnits\": \"M\",\n" +
				"  \"nearestCity\": \"Barre Falls Dam\",\n" +
				"  \"timezone\": \"America/New_York\",\n" +
				"  \"state\": \"MA\",\n" +
				"  \"country\": \"USA\",\n" +
				"  \"region\": \"string\",\n" +
				"  \"active\": true,\n" +
				"  \"locationType\": \"string\",\n" +
				"  \"publicName\": \"BFD\",\n" +
				"  \"properties\": {\n" +
				"    \"additionalProp1\": \"value1\",\n" +
				"    \"additionalProp2\": \"value2\",\n" +
				"    \"additionalProp3\": \"value3\"\n" +
				"  },\n" +
				"  \"lastModified\": \"2025-03-04T22:17:13.441Z\"\n" +
				"}";
	}

	static final class TsGroupExamples
	{
		private TsGroupExamples()
		{
			throw new AssertionError(UTILITY_CLASS);
		}

		public static final String BASIC = "{\n" +
				"  \"groupId\": 19,\n" +
				"  \"groupName\": \"\",\n" +
				"  \"groupType\": \"\",\n" +
				"  \"description\": \"\",\n" +
				"  \"tsIds\": [\n" +
				"    {\n" +
				"      \"uniqueString\": \"\",\n" +
				"      \"key\": 1,\n" +
				"      \"description\": \"\",\n" +
				"      \"storageUnits\": \"\",\n" +
				"      \"active\": true\n" +
				"    }\n" +
				"  ],\n" +
				"  \"includeGroups\": [\n" +
				"    {\n" +
				"      \"groupId\": 1,\n" +
				"      \"groupName\": \"\",\n" +
				"      \"groupType\": \"\",\n" +
				"      \"description\": \"\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"excludeGroups\": [\n" +
				"    {\n" +
				"      \"groupId\": 1,\n" +
				"      \"groupName\": \"\",\n" +
				"      \"groupType\": \"\",\n" +
				"      \"description\": \"\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"intersectGroups\": [\n" +
				"    {\n" +
				"      \"groupId\": 1,\n" +
				"      \"groupName\": \"\",\n" +
				"      \"groupType\": \"\",\n" +
				"      \"description\": \"\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"groupAttrs\": [\n" +
				"    \"\"\n" +
				"  ],\n" +
				"  \"groupSites\": [\n" +
				"    {\n" +
				"      \"siteId\": 2,\n" +
				"      \"sitenames\": {\n" +
				"        \"CWMS\": \"\",\n" +
				"        \"USGS\": \"\"\n" +
				"      },\n" +
				"      \"publicName\": \"\",\n" +
				"      \"description\": \"\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"groupDataTypes\": [\n" +
				"    {\n" +
				"      \"id\": 224,\n" +
				"      \"standard\": \"S\",\n" +
				"      \"code\": \"\",\n" +
				"      \"displayName\": \"\"\n" +
				"    }\n" +
				"  ]\n" +
				"}";

		public static final String VERBOSE = "{\n" +
				"  \"groupId\": 19,\n" +
				"  \"groupName\": \"Flow Data Group\",\n" +
				"  \"groupType\": \"basin\",\n" +
				"  \"description\": \"This group contains flow data for river sites.\",\n" +
				"  \"tsIds\": [\n" +
				"    {\n" +
				"      \"uniqueString\": \"OKVI4.Stage.Inst.15Minutes.0.raw\",\n" +
				"      \"key\": 1,\n" +
				"      \"description\": \"string\",\n" +
				"      \"storageUnits\": \"ft\",\n" +
				"      \"active\": true\n" +
				"    }\n" +
				"  ],\n" +
				"  \"includeGroups\": [\n" +
				"    {\n" +
				"      \"groupId\": 101,\n" +
				"      \"groupName\": \"topgroup\",\n" +
				"      \"groupType\": \"basin\",\n" +
				"      \"description\": \"Group that contains weather-related time series data.\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"excludeGroups\": [\n" +
				"    {\n" +
				"      \"groupId\": 101,\n" +
				"      \"groupName\": \"topgroup\",\n" +
				"      \"groupType\": \"basin\",\n" +
				"      \"description\": \"Group that contains weather-related time series data.\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"intersectGroups\": [\n" +
				"    {\n" +
				"      \"groupId\": 101,\n" +
				"      \"groupName\": \"topgroup\",\n" +
				"      \"groupType\": \"basin\",\n" +
				"      \"description\": \"Group that contains weather-related time series data.\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"groupAttrs\": [\n" +
				"      \"BaseLocation=TESTSITE2\",\n" +
				"      \"BaseParam=ELEV\",\n" +
				"      \"BaseVersion=DCP\",\n" +
				"      \"Duration=0\",\n" +
				"      \"Interval=1Hour\",\n" +
				"      \"ParamType=Inst\",\n" +
				"      \"SubLocation=Spillway2-Gate1\",\n" +
				"      \"SubParam=PZ1B\",\n" +
				"      \"SubVersion=Raw\",\n" +
				"      \"Version=DCP-Raw\"\n" +
				"  ],\n" +
				"  \"groupSites\": [\n" +
				"    {\n" +
				"      \"siteId\": 1,\n" +
				"      \"sitenames\": {\n" +
				"        \"CWMS\": \"OKVI4\",\n" +
				"        \"nwshb5\": \"OKVI4\"\n" +
				"      },\n" +
				"      \"publicName\": \"Barre Falls Dam\",\n" +
				"      \"description\": \"Iowa River at Oakville, IA (USGS)\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"groupDataTypes\": [\n" +
				"    {\n" +
				"      \"id\": 224,\n" +
				"      \"standard\": \"SHEF-PE\",\n" +
				"      \"code\": \"Depth-Snow\",\n" +
				"      \"displayName\": \"SHEF-PE:Depth-Snow\"\n" +
				"    }\n" +
				"  ]\n" +
				"}";
	}
}
