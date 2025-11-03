package com.example.atlasevents;

import org.junit.Test;

public class PasswordHasherTest {
    @Test
    public void hashPass() {
        String password = "mypassword";
        PasswordHasher passwordHasher = new PasswordHasher();
        assert(passwordHasher.passHash(password).equals("nzqbttxpse"));
    }

}
