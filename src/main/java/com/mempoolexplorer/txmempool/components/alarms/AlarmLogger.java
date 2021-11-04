package com.mempoolexplorer.txmempool.components.alarms;

import java.util.List;

public interface AlarmLogger {

	void addAlarm(String alarm);

	void prettyPrint();

	List<String> getAlarmList();
	

}