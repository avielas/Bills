package com.bills.billslib.Contracts.Interfaces;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;

/**
 * Created by michaelvalershtein on 09/09/2017.
 */

public interface ILogger {
    void Log(String tag, LogLevel logLevel, String message, LogsDestination logsDestination);
}
