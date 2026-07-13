package com.hotel.system.service;

import com.hotel.system.entity.RoomType;
import com.hotel.system.repository.RoomInventoryRepository;
import com.hotel.system.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoomService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomInventoryRepository roomInventoryRepository;

    public List<RoomType> findAllActive() {
        return roomTypeRepository.findByStatus(1);
    }

    public List<RoomType> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int quantity) {
        List<RoomType> activeRooms = roomTypeRepository.findByStatus(1);
        List<RoomType> available = new ArrayList<>();

        for (RoomType room : activeRooms) {
            List<java.time.LocalDate> dates = checkIn.datesUntil(checkOut).toList();
            boolean allAvailable = true;

            for (LocalDate date : dates) {
                var inventories = roomInventoryRepository
                        .findByRoomTypeIdAndInventoryDateBetween(room.getId(), date, date);
                if (inventories.isEmpty() || inventories.get(0).getAvailableQuantity() < quantity) {
                    allAvailable = false;
                    break;
                }
            }

            if (allAvailable) {
                available.add(room);
            }
        }

        return available;
    }
}
