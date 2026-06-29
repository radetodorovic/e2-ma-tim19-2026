package com.example.mobilnekt1.profile.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class DailyTokenPolicy {
    public static final long BASE_DAILY_TOKENS = 5;

    private DailyTokenPolicy() {
    }

    public static long rewardForLeague(long league) {
        return BASE_DAILY_TOKENS + Math.max(0, Math.min(5, league));
    }

    public static boolean canClaim(Date lastClaim, Date now, TimeZone timeZone) {
        if (lastClaim == null) return true;
        Calendar last = Calendar.getInstance(timeZone);
        last.setTime(lastClaim);
        Calendar current = Calendar.getInstance(timeZone);
        current.setTime(now);
        return last.get(Calendar.ERA) != current.get(Calendar.ERA)
                || last.get(Calendar.YEAR) != current.get(Calendar.YEAR)
                || last.get(Calendar.DAY_OF_YEAR) != current.get(Calendar.DAY_OF_YEAR);
    }
}
