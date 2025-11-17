package com.app.wooridooribe.service.chat;

import com.app.wooridooribe.entity.Member;

public interface ChatBotService {
    String chat(String message, Member member);
}
