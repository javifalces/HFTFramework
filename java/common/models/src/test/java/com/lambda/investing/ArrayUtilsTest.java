package com.lambda.investing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilsTest {


    @Test
    void unique() {
        List<String> list = Arrays.asList("a", "b", "c", "a", "b", "c", "a", "b", "c");
        List<String> uniqueList = ArrayUtils.unique(list);
        assertEquals(3, uniqueList.size());
        assertTrue(uniqueList.contains("a"));
        assertTrue(uniqueList.contains("b"));
        assertTrue(uniqueList.contains("c"));
    }

    @Test
    void sort() {
        List<String> list = Arrays.asList("c", "b", "a");
        List<String> sortedList = ArrayUtils.sort(list);
        assertEquals(3, sortedList.size());
        assertEquals("a", sortedList.get(0));
        assertEquals("b", sortedList.get(1));
        assertEquals("c", sortedList.get(2));
    }

    @Test
    void uniqueSorted() {
        List<String> list = Arrays.asList("c", "b", "a", "c", "b", "a", "c", "b", "a");
        List<String> uniqueSortedList = ArrayUtils.uniqueSorted(list);
        assertEquals(3, uniqueSortedList.size());
        assertEquals("a", uniqueSortedList.get(0));
        assertEquals("b", uniqueSortedList.get(1));
        assertEquals("c", uniqueSortedList.get(2));
    }

    @Test
    void uniqueDate() {
        List<Date> list = Arrays.asList(new Date(2021, 1, 1), new Date(2021, 1, 2), new Date(2021, 1, 1));
        List<Date> uniqueList = ArrayUtils.uniqueDate(list, 3, false);
        assertEquals(2, uniqueList.size());

        Date referenceDate = new Date(System.currentTimeMillis());
        long diffMs = 3;
        Date referenceDatePlus3 = new Date(referenceDate.getTime() + diffMs);
        List<Date> list1 = Arrays.asList(referenceDatePlus3, referenceDate, new Date(2021, 1, 1));
        List<Date> uniqueList1 = ArrayUtils.uniqueDate(list1, diffMs, false);
        assertEquals(3, uniqueList1.size());

        //if the diff delta is greater than the difference between the dates, the dates are considered unique
        List<Date> uniqueList2 = ArrayUtils.uniqueDate(list1, diffMs + 1, false);
        assertEquals(2, uniqueList2.size());


    }

    @Test
    void uniqueSortedDate() {
    }
}