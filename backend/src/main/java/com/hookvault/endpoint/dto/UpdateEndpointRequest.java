package com.hookvault.endpoint.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateEndpointRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Map<String, String> headers;

    private Boolean active;
}