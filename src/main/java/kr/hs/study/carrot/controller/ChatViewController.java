package kr.hs.study.carrot.controller;

import kr.hs.study.carrot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatViewController {

    private final ChatService chatService;

    @GetMapping("/chat")
    public String list(@RequestAttribute("uid") Long uid, Model model) {
        model.addAttribute("rooms", chatService.rooms(uid));
        return "chat-list";
    }

    @PostMapping("/chat/start/{productId}")
    public String start(@PathVariable Long productId, @RequestAttribute("uid") Long uid) {
        return "redirect:/chat/" + chatService.openRoom(productId, uid);
    }

    @GetMapping("/chat/{roomId}")
    public String room(@PathVariable Long roomId, @RequestAttribute("uid") Long uid, Model model) {
        model.addAttribute("room", chatService.room(roomId, uid));
        return "chat-room";
    }

    @PostMapping("/chat/{roomId}/messages")
    public String send(@PathVariable Long roomId, @RequestParam String content, @RequestAttribute("uid") Long uid) {
        chatService.send(roomId, uid, content);
        return "redirect:/chat/" + roomId;
    }
}
