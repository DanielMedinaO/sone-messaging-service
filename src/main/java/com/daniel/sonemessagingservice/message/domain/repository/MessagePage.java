package com.daniel.sonemessagingservice.message.domain.repository;

import java.util.List;

import com.daniel.sonemessagingservice.message.domain.model.Message;

public record MessagePage(List<Message> content, int page, int size, long totalElements, int totalPages) {
}
