package pe.edu.upeu.fia.planes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocenteInput(
    @NotBlank @Size(max=250) String name,
    @Size(max=500) String course,
    @Size(max=150) String school) {}
