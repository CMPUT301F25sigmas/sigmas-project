package com.example.atlasevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {
    @Test
    public void hashPass() {
        String password = "mypassword";
        PasswordHasher passwordHasher = new PasswordHasher();
        assertNotEquals("mypassword", passwordHasher.passHash(password));
    }

    @Test
    public void checkTest(){
        String password = "mypassword";
        PasswordHasher passwordHasher = new PasswordHasher();
        String hashed = passwordHasher.passHash(password);
        assertTrue(passwordHasher.checkPass("mypassword",hashed));
    }
}
