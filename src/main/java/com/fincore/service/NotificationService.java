package com.fincore.service;

import com.fincore.model.NotificationRecord;
import com.fincore.model.User;
import com.fincore.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public NotificationRecord createNotification(User user, String title, String message, String type) {
        NotificationRecord record = new NotificationRecord(user, title, message, type);
        return notificationRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> getRecentNotifications(Long userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<NotificationRecord> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (NotificationRecord n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
    }
}
