package com.example.mobilnekt1.profile.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public final class DailyTokenPolicyTest {
    private final TimeZone zone = TimeZone.getTimeZone("Europe/Belgrade");

    @Test
    public void rewardIncludesLeagueBenefit() {
        assertEquals(5, DailyTokenPolicy.rewardForLeague(0));
        assertEquals(8, DailyTokenPolicy.rewardForLeague(3));
        assertEquals(10, DailyTokenPolicy.rewardForLeague(9));
    }

    @Test
    public void claimIsAllowedOnlyOnNewCalendarDay() {
        Date morning = date(2026, Calendar.JUNE, 28, 9);
        Date evening = date(2026, Calendar.JUNE, 28, 22);
        Date tomorrow = date(2026, Calendar.JUNE, 29, 1);

        assertFalse(DailyTokenPolicy.canClaim(morning, evening, zone));
        assertTrue(DailyTokenPolicy.canClaim(morning, tomorrow, zone));
        assertTrue(DailyTokenPolicy.canClaim(null, morning, zone));
    }

    private Date date(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.clear();
        calendar.set(year, month, day, hour, 0, 0);
        return calendar.getTime();
    }
}
