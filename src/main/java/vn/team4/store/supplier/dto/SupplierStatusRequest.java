package vn.team4.store.supplier.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupplierStatusRequest(
        @NotNull(message = "Trạng thái hoạt động là bắt buộc")
        @JsonProperty("isActive")
        @JsonDeserialize(using = StrictBooleanDeserializer.class)
        Boolean isActive) {
}
