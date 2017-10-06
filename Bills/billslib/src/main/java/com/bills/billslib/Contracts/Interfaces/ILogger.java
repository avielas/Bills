package com.bills.billslib.Contracts.Interfaces;

import com.bills.billslib.Contracts.Enums.LogLevel;

/**
 * Created by michaelvalershtein on 09/09/2017.
 */

public interface ILogger {
    public void Log(String tag, LogLevel logLevel, String message);
}
