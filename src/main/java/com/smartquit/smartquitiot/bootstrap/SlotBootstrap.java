package com.smartquit.smartquitiot.bootstrap;

import com.smartquit.smartquitiot.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class SlotBootstrap {
    private final SlotService slotService;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${app.slots.start:07:00}")
    private String startStr;
    @Value("${app.slots.end:15:00}")
    private String endStr;
    @Value("${app.slots.minutes:30}")
    private int slotMinutes;
    @Value("${app.seed-slots.enabled:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!enabled) {
            log.info("Slot seeding disabled by config");
            return;
        }
        try {
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);
            slotService.findOrCreateRange(start, end, slotMinutes);
         //   log.info("Seeded slots {}-{} every {}m", start, end, slotMinutes);
        } catch (Exception e) {
            log.error("Failed to seed slots", e);
        }
    }
}