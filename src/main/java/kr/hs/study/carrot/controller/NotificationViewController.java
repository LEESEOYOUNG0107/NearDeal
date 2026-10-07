package kr.hs.study.carrot.controller;

import kr.hs.study.carrot.dto.response.NotificationResponse;
import kr.hs.study.carrot.entity.NotificationType;
import kr.hs.study.carrot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class NotificationViewController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String list(@RequestAttribute("uid") Long uid, Model model) {
        model.addAttribute("notices", notificationService.list(uid, false));
        return "notifications";
    }

    // ponytail: 읽음 처리와 이동을 GET 한 번으로 처리(링크 클릭). 부작용이 있는 GET 이라 CSRF에 약함. 로그인 도입 시 POST 폼으로 변경
    @GetMapping("/notifications/{id}/go")
    public String go(@PathVariable Long id, @RequestAttribute("uid") Long uid) {
        NotificationResponse n = notificationService.markRead(id, uid);
        if (n.type() == NotificationType.CHAT_MESSAGE && n.roomId() != null) return "redirect:/chat/" + n.roomId();
        if (n.productId() != null) return "redirect:/products/" + n.productId();
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(@RequestAttribute("uid") Long uid) {
        notificationService.markAllRead(uid);
        return "redirect:/notifications";
    }
}
