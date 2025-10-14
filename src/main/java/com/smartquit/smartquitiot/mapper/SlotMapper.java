package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.entity.Slot;
import org.springframework.stereotype.Component;

@Component
public class SlotMapper {

    public SlotDTO toSlotDTO(Slot slot) {
        if(slot == null) return null;
        SlotDTO slotDTO = new SlotDTO();
        slotDTO.setId(slot.getId());
        slotDTO.setStartTime(slot.getStartTime());
        slotDTO.setEndTime(slot.getEndTime());

        return slotDTO;
    }
}
