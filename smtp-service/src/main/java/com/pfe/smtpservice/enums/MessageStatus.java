package com.pfe.smtpservice.enums;

/**
 * États d'un message dans la queue.
 * Inspiré de Postal : Queued → Processing → Sent/HardFail/SoftFail/Held/Bounced
 */


public enum MessageStatus {
    QUEUED,
    SENDING,
    SENT,
    FAILED,
    HELD
}
