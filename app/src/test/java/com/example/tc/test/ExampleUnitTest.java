package com.example.tc.test;

import com.tc.mapdemo.*;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testT() {

        double long_1 = 121.532177;
        double lat_1 = 31.216304;

        double long_2 = 121.532167 ;
        double lat_2 = 31.216299;

        System.out.println(Utils.GetDistance(long_1, lat_1, long_2, lat_2) > 200);
    }

}