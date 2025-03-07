package com.lambda.investing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimeSeriesQueueTest {

    TimeSeriesQueue<String> stringsQueue = new TimeSeriesQueue<>(3);

    @BeforeEach
    void setUp() {
        stringsQueue.clear();
    }

    @Test
    public void testNewest() {
        stringsQueue.add("1");
        stringsQueue.add("2");
        stringsQueue.add("3");
        assertEquals("1", stringsQueue.getOldest());
        assertEquals("3", stringsQueue.getNewest());
        assertEquals("2", stringsQueue.getBeforeNewest());

        List<String> firstNewest = stringsQueue.getListFirstNewest();
        assertEquals(3, firstNewest.size());
        assertEquals("3", firstNewest.get(0));
        assertEquals("2", firstNewest.get(1));
        assertEquals("1", firstNewest.get(2));

        List<String> firstOldest = stringsQueue.getListFirstOldest();//same order as added
        assertEquals(3, firstOldest.size());
        assertEquals("1", firstOldest.get(0));
        assertEquals("2", firstOldest.get(1));
        assertEquals("3", firstOldest.get(2));


    }

    @Test
    public void testGestIndex() {
        TimeSeriesQueue<Date> dateQueue = new TimeSeriesQueue<>(3);
        //add 3 dates of 1st january 2024 00:00:00 00:00:01 00:00:02
        Date date1 = new Date(2024, 1, 1, 0, 0, 0);
        Date date2 = new Date(2024, 1, 1, 0, 0, 1);
        Date date3 = new Date(2024, 1, 1, 0, 0, 2);
        dateQueue.add(date1);
        dateQueue.add(date2);
        dateQueue.add(date3);
        assertEquals(date1, dateQueue.getOldest());

        List<Date> indexList = new ArrayList<>();
        indexList.add(date1);
        indexList.add(date2);
        indexList.add(date3);

        List<Integer> expectedIndexList = new ArrayList<>();
        expectedIndexList.add(0);
        expectedIndexList.add(1);
        expectedIndexList.add(2);
        assertEquals(expectedIndexList, dateQueue.getIndex(indexList, 100));

        List<Date> indexList2 = new ArrayList<>();
        Date date4 = new Date(2024, 1, 1, 0, 0, 0);
        Date date5 = new Date(2024, 1, 1, 0, 0, 1);
        Date date6 = new Date(2024, 1, 1, 0, 0, 4);//not found => -1

        indexList2.add(date4);
        indexList2.add(date5);
        indexList2.add(date6);
        List<Integer> expectedIndexList2 = new ArrayList<>();
        expectedIndexList2.add(0);
        expectedIndexList2.add(1);
        expectedIndexList2.add(-1);
        assertEquals(expectedIndexList2, dateQueue.getIndex(indexList2, 1100));

        indexList2 = new ArrayList<>();
        date4 = new Date(2024, 1, 1, 0, 1, 0);//not found => -1
        date5 = new Date(2024, 1, 1, 0, 0, 1);
        date6 = new Date(2024, 1, 1, 0, 0, 4);//not found => -1

        indexList2.add(date4);
        indexList2.add(date5);
        indexList2.add(date6);
        expectedIndexList2 = new ArrayList<>();
        expectedIndexList2.add(-1);
        expectedIndexList2.add(1);
        expectedIndexList2.add(-1);
        assertEquals(expectedIndexList2, dateQueue.getIndex(indexList2, 1100));


    }
}