package com.homesweet.notification.exception;

import com.homesweet.notification.exception.BusinessException;
import com.homesweet.notification.exception.ErrorCode;

/**
 * 알림 관련 비즈니스 예외
 *
 * @author dogyungkim
 */
public class NotificationException extends BusinessException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
