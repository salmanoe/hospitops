package id.co.hospitops.room.adapter.web;
import jakarta.validation.constraints.Min;
public record UpdateRoomRequest(@Min(1) int floor, String notes) {}
