package pe.edu.upeu.fia.planes;
import jakarta.validation.constraints.*;
public record PlanInput(@NotBlank @Size(max=150) String school,@NotBlank @Size(max=20) String version,@NotBlank @Size(max=30) String variant,@Size(max=300) String source) {}
