package com.Basisttha.IronHold.Exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(String m){
        super(m);
    }
}
