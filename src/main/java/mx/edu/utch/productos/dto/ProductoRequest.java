package mx.edu.utch.productos.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear o actualizar un producto, con sus reglas de validación.
 */
@Schema(description = "Datos para crear o actualizar un producto")
public record ProductoRequest(

        @Schema(description = "Código SKU: 3 letras mayúsculas, guion y 4 dígitos", example = "ELE-0001")
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[A-Z]{3}-\\d{4}$", message = "El código debe tener el formato AAA-0000 (ej. ELE-0001)")
        String codigo,

        @Schema(description = "Nombre comercial", example = "Multímetro digital")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres")
        String nombre,

        @Schema(description = "Descripción opcional", example = "Multímetro con medición de voltaje AC/DC")
        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String descripcion,

        @Schema(description = "Categoría del producto", example = "Electrónica")
        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 40, message = "La categoría no puede exceder 40 caracteres")
        String categoria,

        @Schema(description = "Precio unitario en MXN", example = "499.90")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        @DecimalMax(value = "999999.99", message = "El precio no puede exceder 999,999.99")
        @Digits(integer = 6, fraction = 2, message = "El precio admite máximo 6 enteros y 2 decimales")
        BigDecimal precio,

        @Schema(description = "Unidades en existencia", example = "25")
        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 100000, message = "El stock no puede exceder 100,000 unidades")
        Integer stock,

        @Schema(description = "Indica si el producto está activo; por defecto true", example = "true")
        Boolean activo) {
}
