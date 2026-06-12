package id.co.hospitops.channel.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigureChannelPropertyRequest(
        @NotBlank @Size(max = 128) String externalPropertyId) {
}
