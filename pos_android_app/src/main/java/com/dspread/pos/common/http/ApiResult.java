package com.dspread.pos.common.http;

// Java 11 compatible result type for API operations
public abstract class ApiResult<T> {
    
    public static final class Success<T> extends ApiResult<T> {
        private final T data;
        
        public Success(T data) {
            this.data = data;
        }
        
        public T data() {
            return data;
        }
    }
    
    public static final class Error<T> extends ApiResult<T> {
        private final String message;
        private final Integer code;
        
        public Error(String message) {
            this(message, null);
        }
        
        public Error(String message, Integer code) {
            this.message = message;
            this.code = code;
        }
        
        public String message() {
            return message;
        }
        
        public Integer code() {
            return code;
        }
    }
}