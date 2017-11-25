package com.bills.billslib.Core;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Contracts.Interfaces.ILogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by michaelvalershtein on 09/09/2017.
 */

public class BillsLog {
    private static Map<UUID, ILogger> mSessionIdToLogger;

    public static void AddNewSession(final UUID sessionId, ILogger logger){
        if(null == mSessionIdToLogger){
            mSessionIdToLogger = new HashMap<>();
        }
        if(mSessionIdToLogger.containsKey(sessionId)){
            throw new ExceptionInInitializerError("Failed to add new session. The session already exist: " + sessionId);
        }
        mSessionIdToLogger.put(sessionId, logger);
    }

    public static void Log(final UUID sessionId, LogLevel logLevel, String message, LogsDestination logsDestination, String tag){
        if(mSessionIdToLogger == null || mSessionIdToLogger.isEmpty()){
            throw new ExceptionInInitializerError("BillsLog was not initialized or empty.");
        }
        else if(!mSessionIdToLogger.containsKey(sessionId)){
            throw new ExceptionInInitializerError("mSessionIdToLogger doesn't contain sessionId " + sessionId);
        }
        mSessionIdToLogger.get(sessionId).Log(tag, logLevel, message, logsDestination);
    }

    public static void UninitCommonSession(final UUID sessionId, String myFirebaseLogPath){
        mSessionIdToLogger.get(sessionId).UninitCommonSession(myFirebaseLogPath);
    }
}
