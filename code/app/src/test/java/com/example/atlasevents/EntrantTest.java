package com.example.atlasevents;

import org.junit.Assert;
import org.junit.Test;

public class EntrantTest {
    @Test
    public void EntrantHashNonNullTest (){
        Entrant entrant1 = new Entrant("name", "email", "password", "phoneNumber");
        String testEmail = "email";
        int testHash = testEmail.hashCode();
        Assert.assertEquals(testHash, entrant1.hashCode());
    }

    @Test
    public void EntrantHashNullTest() {
        Entrant entrant2 = new Entrant("name", "", "password", "phoneNumber");
        Assert.assertEquals(0, entrant2.hashCode());
    }
}
