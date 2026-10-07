package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.Favorite;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.repository.FavoriteRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock ProductRepository productRepository;
    @Mock NotificationService notificationService;
    @InjectMocks FavoriteService favoriteService;

    private Product product() {
        return Product.builder().sellerId(1L).title("아이폰").price(1000L)
                .category(Category.DIGITAL).description("설명").region("서울").build();
    }

    @Test
    void 이미_찜한_상품을_또_찜하면_409이고_알림도_가지_않는다() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(favoriteRepository.existsByUserIdAndProductId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.add(10L, 2L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(favoriteRepository, never()).save(any());
        verify(notificationService, never()).onFavorited(any(), any());
    }

    @Test
    void 찜하면_저장되고_판매자에게_알림_처리가_호출된다() {
        Product product = product();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteRepository.existsByUserIdAndProductId(2L, 10L)).thenReturn(false, true);
        when(favoriteRepository.countByProductId(10L)).thenReturn(1L);

        var status = favoriteService.add(10L, 2L);

        verify(favoriteRepository).save(any(Favorite.class));
        verify(notificationService).onFavorited(product, 2L);
        assertThat(status.favoriteCount()).isEqualTo(1L);
        assertThat(status.favorited()).isTrue();
    }

    @Test
    void 찜하지_않은_상품의_찜_취소는_404() {
        when(favoriteRepository.findByUserIdAndProductId(2L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.remove(10L, 2L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
