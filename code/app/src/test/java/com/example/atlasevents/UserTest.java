package com.example.atlasevents;

import org.junit.Assert;
import org.junit.Test;

public class UserTest {
    @Test
    public void SetAndGetUserInfoTest() {
        User user = new User("name", "email", "password", "phoneNumber");
        user.setName("John");
        user.setEmail("abc@123");
        user.setPassword("abc123");
        user.setUserType("Entrant");

        Assert.assertEquals("John", user.getName());
        Assert.assertEquals("abc@123", user.getEmail());
        Assert.assertEquals("abc123", user.getPassword());
        Assert.assertEquals("Entrant", user.getUserType());
    }
    @Test
    public void EditProfileFuncTest() {
        User user = new User("name", "email", "password", "phoneNumber");
        user.editProfile("John", "abc@123", "abc123", "123456");

        Assert.assertEquals("John", user.getName());
        Assert.assertEquals("abc@123", user.getEmail());
        Assert.assertEquals("abc123", user.getPassword());
    }

}
