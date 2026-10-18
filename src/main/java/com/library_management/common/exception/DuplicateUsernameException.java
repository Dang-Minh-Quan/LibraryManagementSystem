package com.library_management.common.exception;

public class DuplicateUsernameException extends BadRequestException {
    public DuplicateUsernameException() { super("Username already exists"); }
}
