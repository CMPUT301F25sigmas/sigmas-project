package com.example.atlasevents;

import org.junit.Assert;
import org.junit.Test;

public class AdminTest {
    @Test
    public void AdminHashNonNullTest (){
        Admin admin1 = new Admin("name", "email", "password", "phoneNumber");
        String testEmail = "email";
        int testHash = testEmail.hashCode();
        Assert.assertEquals(testHash, admin1.hashCode());
    }

    @Test
    public void AdminHashNullTest() {
        Admin admin2 = new Admin("name", "", "password", "phoneNumber");
        Assert.assertEquals(0, admin2.hashCode());
    }
}
