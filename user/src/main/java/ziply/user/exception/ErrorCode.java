package ziply.user.exception;

public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", "존재하지 않는 유저입니다."),
    INVALID_INPUT("INVALID_INPUT", "잘못된 입력입니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}


