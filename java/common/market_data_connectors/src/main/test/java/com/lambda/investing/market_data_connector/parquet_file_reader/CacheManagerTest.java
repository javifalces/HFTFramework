package com.lambda.investing.market_data_connector.parquet_file_reader;

import com.lambda.investing.market_data_connector.parquet_file_reader.CacheManager;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class CacheManagerTest {


    CacheManager getDummyCacheManager() {
        return getDummyCacheManagerCustomDate(new Date(2021, 1, 1));
    }

    CacheManager getDummyCacheManagerCustomDate(Date date) {
//        Date date = new Date(2021,1,1);
        Date startDateTotal = new Date(2021, 1, 1, 9, 0, 0);
        Date endDateTotal = new Date(2021, 2, 1, 17, 0, 0);
        List<String> depthFiles = new ArrayList<>();
        depthFiles.add("test1");
        depthFiles.add("test2");
        List<String> tradeFiles = new ArrayList<>();
        tradeFiles.add("test3");
        tradeFiles.add("test4");
        CacheManager cacheManager = new CacheManager(date, startDateTotal, endDateTotal, depthFiles, tradeFiles);
        return cacheManager;
    }

    @Test
    public void testCacheNameUUID_equals() {
        CacheManager cacheManager = getDummyCacheManager();
        CacheManager cacheManager1 = getDummyCacheManager();
        assertEquals(cacheManager.getUUID(), cacheManager1.getUUID());

        assertEquals("6151dcec-9f0e-3883-8281-1aede323ff69", cacheManager1.getUUID());
    }


    @Test
    public void testCacheNameUUID_not_equals() {
        CacheManager cacheManager1 = getDummyCacheManager();
        CacheManager cacheManager2 = getDummyCacheManagerCustomDate(new Date(2021, 1, 2));
        assertNotEquals(cacheManager2.getUUID(), cacheManager1.getUUID());

        Date date = new Date(2021, 1, 1);
        Date startDateTotal = new Date(2021, 1, 1, 9, 0, 0);
        Date endDateTotal = new Date(2021, 2, 1, 17, 0, 0);
        List<String> depthFiles = new ArrayList<>();
        depthFiles.add("test1");
        depthFiles.add("test2");
        List<String> tradeFiles = new ArrayList<>();
        tradeFiles.add("test3");
        tradeFiles.add("test4");
        CacheManager cacheManagerFirst = new CacheManager(date, startDateTotal, endDateTotal, depthFiles, tradeFiles);
        assertEquals(cacheManagerFirst.getUUID(), cacheManager1.getUUID());


        List<String> depthFiles2 = new ArrayList<>();
        depthFiles.add("test1z");
        depthFiles.add("test2z");
        List<String> tradeFiles2 = new ArrayList<>();
        tradeFiles.add("test3z");
        tradeFiles.add("test4z");
        CacheManager cacheManagerSecond = new CacheManager(date, startDateTotal, endDateTotal, depthFiles2, tradeFiles2);
        assertNotEquals(cacheManagerFirst.getUUID(), cacheManagerSecond.getUUID());


    }
}