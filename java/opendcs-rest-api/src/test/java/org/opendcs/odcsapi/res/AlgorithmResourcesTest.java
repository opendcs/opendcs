/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import decodes.cwms.validation.ScreeningAlgorithm;
import decodes.sql.DbKey;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.ScriptType;
import decodes.tsdb.compedit.AlgorithmInList;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiAlgoParm;
import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiAlgorithmRef;
import org.opendcs.odcsapi.beans.ApiAlgorithmScript;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AlgorithmResourcesTest
{
	@Test
	void testMapRef()
	{
		String dataScreening = "DataScreening";
		String exec = ScreeningAlgorithm.class.getName();
		String description = "this is a unit test algorithm";
		AlgorithmInList algorithmInList = new AlgorithmInList(DbKey.createDbKey(1000L), dataScreening,
				exec, 1, description);
		ApiAlgorithmRef map = AlgorithmResources.map(algorithmInList);
		assertEquals(1000L, map.getAlgorithmId(), "Algorithm ID should be 1000L");
		assertEquals(exec, map.getExecClass(), "Exec class should match");
		assertEquals(dataScreening, map.getAlgorithmName(), "Algorithm name should be DataScreening");
		assertEquals(description, map.getDescription(), "Description should match");
		assertEquals(1, map.getNumCompsUsing(), "Number of comps using should be 1");
	}

	@Test
	void testMapApiAlgorithm()
	{
		String dataScreening = "DataScreening";
		String exec = ScreeningAlgorithm.class.getName();
		String description = "this is a unit test algorithm";
		ApiAlgorithm apiAlgorithm = new ApiAlgorithm();
		apiAlgorithm.setAlgorithmId(1000L);
		apiAlgorithm.setDescription(description);
		apiAlgorithm.setNumCompsUsing(1);
		apiAlgorithm.setName(dataScreening);
		List<ApiAlgoParm> params = new ArrayList<>();
		ApiAlgoParm param = new ApiAlgoParm();
		param.setParmType("String");
		param.setRoleName("site");
		params.add(param);
		apiAlgorithm.setParms(params);
		List<ApiAlgorithmScript> scripts = new ArrayList<>();
		ApiAlgorithmScript script = new ApiAlgorithmScript();
		script.setScriptType(ScriptType.PY_Init.getDbChar());
		String placeholderScriptText = "placeholder";
		script.setText(placeholderScriptText);
		scripts.add(script);
		apiAlgorithm.setAlgoScripts(scripts);
		Properties properties = new Properties();
		properties.setProperty("key", "value");
		apiAlgorithm.setProps(properties);
		apiAlgorithm.setExecClass(exec);
		DbCompAlgorithm map = AlgorithmResources.map(apiAlgorithm);

		assertEquals(1000, map.getId().getValue(), "Algorithm ID should match");
		assertEquals(dataScreening, map.getName(), "Algorithm name should match");
		assertEquals(exec, map.getExecClass(), "Exec class should match");
		assertEquals(description, map.getComment(), "Description should match");
		assertEquals(1, map.getNumCompsUsing(), "Number of comps using should match");
		assertEquals(apiAlgorithm.getProps(), map.getProperties(), "Properties should match");
		DbAlgoParm mapParam = map.getParms().next();
		assertEquals("String", mapParam.getParmType(), "Algorithm param type should match");
		assertEquals("site", mapParam.getRoleName(), "Algorithm param role should match");
		DbCompAlgorithmScript mapScript = map.getScripts().iterator().next();
		assertEquals(placeholderScriptText, mapScript.getText(), "Scripts should match");
		assertEquals(ScriptType.PY_Init, mapScript.getScriptType(), "Scripts should match");
	}

	@Test
	void testMapDbCompAlgorithm()
	{
		String dataScreening = "DataScreening";
		String exec = ScreeningAlgorithm.class.getName();
		String description = "this is a unit test algorithm";
		DbCompAlgorithm apiAlgorithm = new DbCompAlgorithm(DbKey.createDbKey(1000L), dataScreening, exec, description);
		apiAlgorithm.setNumCompsUsing(1);
		apiAlgorithm.setProperty("key", "value");
		apiAlgorithm.addParm(new DbAlgoParm("site", "String"));
		DbCompAlgorithmScript script = new DbCompAlgorithmScript(apiAlgorithm, ScriptType.PY_TimeSlice);
		String placeholderScriptText = "placeholder";
		script.addToText(placeholderScriptText);
		apiAlgorithm.putScript(script);
		ApiAlgorithm map = AlgorithmResources.map(apiAlgorithm);

		assertEquals(1000, map.getAlgorithmId().longValue(), "Algorithm ID should match");
		assertEquals(dataScreening, map.getName(), "Algorithm name should match");
		assertEquals(exec, map.getExecClass(), "Exec class should match");
		assertEquals(description, map.getDescription(), "Description should match");
		assertEquals(1, map.getNumCompsUsing(), "Number of comps using should match");
		assertEquals(apiAlgorithm.getProperties(), map.getProps(), "Properties should match");
		assertEquals("String", map.getParms().get(0).getParmType(), "Algorithm param type should match");
		assertEquals("site", map.getParms().get(0).getRoleName(), "Algorithm param role should match");
		assertEquals(placeholderScriptText, map.getAlgoScripts().get(0).getText(), "Scripts should match");
		assertEquals(ScriptType.PY_TimeSlice.getDbChar(), map.getAlgoScripts().get(0).getScriptType(), "Scripts should match");
	}
}
