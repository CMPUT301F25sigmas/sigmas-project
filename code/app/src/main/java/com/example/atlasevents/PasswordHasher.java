package com.example.atlasevents;

import java.util.ArrayList;

public class PasswordHasher {
    public PasswordHasher(){}

    public String passHash(String password){
        char[] chars = password.toCharArray();
        StringBuilder hashed = new StringBuilder();
        for(char letter:chars){
            hashed.append((char)(letter + 1));
        }
        return hashed.toString();
    }
}
