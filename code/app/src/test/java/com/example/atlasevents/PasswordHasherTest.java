package com.example.atlasevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {
    @Test
    public void hashPass() {
        String password = "mypassword";
        PasswordHasher passwordHasher = new PasswordHasher();
        assertEquals("nzqbttxpse", passwordHasher.passHash(password));
    }

}
