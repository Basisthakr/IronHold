package com.Basisttha.IronHold.Exception;

public class UsernameAlreadyExists extends RuntimeException{
    public UsernameAlreadyExists(String m){
        super(m);
    }
}