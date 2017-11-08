package com.bills.billslib.Contracts.Interfaces;

/**
 * Created by aviel on 08/11/17.
 */

public interface IMailSender {
    /**
     * @param subject - email subject
     * @param body - email body
     * @param sender
     * @param recipients - list of recipients
     */
    void SendEmail(String subject, String body, String sender, String recipients);
}
