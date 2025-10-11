/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.multistat;

import java.util.*;

import javax.swing.*;

public class AlarmList
	extends SpinnerListModel
{
	Vector alarmList;
	CancelledAlarmList cancelledAlarmList;

	public AlarmList()
	{
		super();
		alarmList = new Vector();
		/*
		The SpinnerListModel requires a non-empty list. Therefore we add one
		'empty' alarm at the beginning of the list which is always there.
		*/
		alarmList.add(new Alarm("", "", "", "", 0, "", true));
		setList(alarmList);
		cancelledAlarmList = new CancelledAlarmList();
	}

	/**
	 * Adds an alarm to the list. If an alarm with this module and event num
	 * is already in the list, it is replaced with the passed alarm.
	 * @param alarm the alarm.
	 */
	public synchronized void addAlarm(final Alarm alarm)
	{
		/* Don't add, if this alarm is already in the list, just replace. */
		for(int idx = 0; idx < alarmList.size(); idx++)
		{
			Alarm a = (Alarm)alarmList.get(idx);
			if (a.module.equals(alarm.module) && a.alarmNum == alarm.alarmNum)
			{
				alarm.assertionCount = a.assertionCount + 1;
				alarmList.setElementAt(alarm, idx);
				notifyGui();
				return;
			}
		}

		/* Add the alarm. */
		alarmList.add(alarm);
		if (getNumAlarms() == 1)
			setValue(alarm);
		notifyGui();
	}

	private void notifyGui()
	{
		// already in GUI thread.
		fireStateChanged();

	}

	public synchronized void cancelCurrentAlarm()
	{
		Alarm alarm = (Alarm)getValue();
		if (!alarm.isEmpty())
		{
			int idx = alarmList.indexOf(alarm);
			alarmList.remove(alarm);
			if (idx >= alarmList.size())
				setValue(alarmList.elementAt(idx-1));
			notifyGui();
			cancelledAlarmList.add(alarm);
		}
	}

	/**
	 * Cancels any alarms with matching module and number.
	 * @param module the module
	 * @param num the alarm number
	 */
	public synchronized void cancelAlarm(String module, int num)
	{
		Alarm curAlarm = (Alarm)getValue();
		for(int idx = 0; idx < alarmList.size(); idx++)
		{
			Alarm alarm = (Alarm)alarmList.get(idx);
			if (alarm.module.equals(module) && alarm.alarmNum == num)
			{
				if (curAlarm == alarm)
				{
					cancelCurrentAlarm();
				}
				else
				{
					alarmList.remove(alarm);
					notifyGui();
				}
				cancelledAlarmList.add(alarm);
				return;
			}
		}
	}

	public int getNumAlarms()
	{
		return alarmList.size() - 1;
	}

	/**
	 * Overload base class. Don't show the zero (empty) object unless the list
	 * is empty.
	 * @return Object
	 */
	public Object getPreviousValue()
	{
		Object v = getValue();
		if (getNumAlarms() > 0 && alarmList.indexOf(v) == 1)
			return v;
		else
			return super.getPreviousValue();
	}

	/**
	 * @return number of alarms for specified host.
	 */
	public synchronized int countAlarmsForSource(String source)
	{
		int count = 0;
		for(int idx = 0; idx < alarmList.size(); idx++)
		{
			Alarm alarm = (Alarm)alarmList.get(idx);
			if (alarm.source.equalsIgnoreCase(source))
				count++;
		}
		return count;
	}

}
