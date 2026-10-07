package kr.hs.study.carrot.config;

// 채팅방별 구독 경로. 방송, 구독 검증이 같은 규칙을 쓰도록 한 곳에 둔다
public final class ChatTopic {

    private static final String PREFIX = "/topic/chat-rooms/";

    private ChatTopic() {
    }

    public static String of(Long roomId) {
        return PREFIX + roomId;
    }

    public static boolean matches(String destination) {
        return destination != null && destination.startsWith(PREFIX);
    }

    public static Long roomIdOf(String destination) {
        return Long.valueOf(destination.substring(PREFIX.length()));
    }
}
