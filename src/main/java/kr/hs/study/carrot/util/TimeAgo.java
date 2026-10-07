package kr.hs.study.carrot.util;

import java.time.Duration;
import java.time.LocalDateTime;

// 템플릿에서 T(kr.hs.study.carrot.util.TimeAgo).of(createdAt) 로 호출
public final class TimeAgo {

    private TimeAgo() {
    }

    public static String of(LocalDateTime time) {
        if (time == null) return "";
        long minutes = Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        if (minutes < 60 * 24) return minutes / 60 + "시간 전";
        if (minutes < 60 * 24 * 30) return minutes / (60 * 24) + "일 전";
        return time.toLocalDate().toString();
    }
}
