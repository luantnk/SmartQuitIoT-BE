package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.entity.Slot;
import com.smartquit.smartquitiot.mapper.SlotMapper;
import com.smartquit.smartquitiot.repository.SlotRepository;
import com.smartquit.smartquitiot.service.SlotService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final SlotMapper slotMapper;

    @Override
    @Transactional
    public List<Slot> findOrCreateRange(LocalTime start, LocalTime end, int slotMinutes) {
        if (slotMinutes <= 0) throw new IllegalArgumentException("slotMinutes must be > 0");
        List<Slot> out = new ArrayList<>();
        LocalTime cur = start;

        while (true) {
            LocalTime next = cur.plusMinutes(slotMinutes);
            if (next.isAfter(end)) break;

            // tìm tồn tại, nếu không có -> tạo (catch race)
            Optional<Slot> exist = slotRepository.findByStartTimeAndEndTime(cur, next);
            if (exist.isPresent()) {
                out.add(exist.get());
            } else {
                Slot s = new Slot();
                s.setStartTime(cur);
                s.setEndTime(next);
                try {
                    s = slotRepository.save(s);
                    out.add(s);
                } catch (DataIntegrityViolationException dive) {
                    // Race: another thread created it -> fetch again
                    Slot fetched = slotRepository.findByStartTimeAndEndTime(cur, next)
                            .orElseThrow(() -> new IllegalStateException("Failed to create or fetch slot"));
                    out.add(fetched);
                }
            }

            cur = next;
        }

        return out;
    }

    @Override
    public List<Slot> listAll() {
        return slotRepository.findAll();
    }

    @Override
    public Page<SlotDTO> listAllSlots(int page, int size) {
        PageRequest  pageRequest = PageRequest.of(page, size);
        Page<Slot> slots = slotRepository.findAll(pageRequest);
        return slots.map(slotMapper::toSlotDTO);
    }
}
