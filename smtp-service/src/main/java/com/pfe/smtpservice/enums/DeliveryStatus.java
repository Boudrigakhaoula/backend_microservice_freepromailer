package com.pfe.smtpservice.enums;
/**
 * Inspiré de Postal : types de delivery
 * (Sent, SoftFail, HardFail, Held, Bounced, Processed, Error)
 */
public enum DeliveryStatus {
    SENT,
    SOFT_FAIL,
    HARD_FAIL,
    HELD,
    BOUNCED,
    ERROR
}
