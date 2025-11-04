package com.example.atlasevents;

import org.junit.Assert;
import org.junit.Test;

public class EntrantListTest {
    @Test
    public void addAndContainsTest(){
        EntrantList entrantList = new EntrantList();
        Entrant entrant = new Entrant();
        entrantList.addEntrant(entrant);
        Assert.assertTrue(entrantList.containsEntrant(entrant));
    }
}
