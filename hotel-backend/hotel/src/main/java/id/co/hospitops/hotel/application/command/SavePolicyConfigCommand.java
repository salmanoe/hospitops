package id.co.hospitops.hotel.application.command;

import id.co.hospitops.shared.HotelId;

public record SavePolicyConfigCommand(
        HotelId hotelId,
        int taxPercent,
        String taxName,
        String invoiceHotelName,
        String invoiceAddress,
        String invoiceFooterNote
) {}
