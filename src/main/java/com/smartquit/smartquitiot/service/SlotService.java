package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.Slot;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SlotService {
    List<Slot> findOrCreateRange(LocalTime start, LocalTime end, int slotMinutes);
    List<Slot> listAll();
}
