package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import kr.hs.study.carrot.dto.request.ProductDescriptionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiProductDescriptionServiceTest {

    private final ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_DEEP_STUBS);

    @Test
    void 입력한_정보만_AI에_전달하고_비어있는_항목은_생략한다() {
        // 메시지에 상품명/상태/특징이 있고 "추가 정보" 줄이 없을 때만 초안이 반환되도록 스텁
        when(builder.defaultSystem(anyString()).build().prompt()
                .user(contains("상품명: 아이패드 에어 5\n상품 상태: 거의 새것\n특징: 64GB / 스페이스그레이 / 박스 있음\n</입력>"))
                .call().content()).thenReturn("  초안입니다  ");

        var service = new AiProductDescriptionService(builder);
        var response = service.generate(
                new ProductDescriptionRequest("아이패드 에어 5", "거의 새것", "64GB / 스페이스그레이 / 박스 있음", " "));

        assertThat(response.description()).isEqualTo("초안입니다");
    }

    @Test
    void AI_호출이_실패하면_502로_변환한다() {
        when(builder.defaultSystem(anyString()).build().prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("api key missing"));

        var service = new AiProductDescriptionService(builder);

        assertThatThrownBy(() -> service.generate(new ProductDescriptionRequest("아이패드", "새것", null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
