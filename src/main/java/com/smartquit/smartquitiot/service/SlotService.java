package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.dto.response.SlotReseedResponse;
import com.smartquit.smartquitiot.entity.Slot;
import org.springframework.data.domain.Page;

import java.time.LocalTime;
import java.util.List;

public interface SlotService {
    List<Slot> findOrCreateRange(LocalTime start, LocalTime end, int slotMinutes, int gapMinutes);
    List<Slot> listAll();

    Page<SlotDTO> listAllSlots(int page, int size);
    
    SlotReseedResponse reseedSlots(LocalTime start, LocalTime end, int slotMinutes, int gapMinutes);
}
