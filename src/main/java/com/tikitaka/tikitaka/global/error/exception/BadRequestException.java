package com.tikitaka.tikitaka.global.error.exception;

import com.tikitaka.tikitaka.global.error.ErrorCode;
import lombok.Getter;
import lombok.ToString;

/** Status: 400
 * Bad Request Error 반환하는 커스텀 클래스입니다 */
@Getter
@ToString
public class BadRequestException extends BusinessException {
    private String message;

    public BadRequestException(String message) {
        super(ErrorCode._BAD_REQUEST);
        this.message = message;
    }

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode);
        this.message = message;
    }

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
