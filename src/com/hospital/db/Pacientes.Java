import java.time.LocalDate;

public class Paciente {
    private String idPaciente;
    private String dni;
    private String nombre;
    private String apellido;
    private int edad;
    private String telefono;
    private String tipoSangre;
    private String historialClinico;

    // Constructor completo
    public Paciente(String idPaciente, String dni, String nombre, String apellido, 
                    int edad, String telefono, String tipoSangre, String historialClinico) {
        this.idPaciente = idPaciente;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.telefono = telefono;
        this.tipoSangre = tipoSangre;
        this.historialClinico = historialClinico;
    }

    public String getIdPaciente() { return idPaciente; }
    public void setIdPaciente(String idPaciente) { this.idPaciente = idPaciente; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTipoSangre() { return tipoSangre; }
    public void setTipoSangre(String tipoSangre) { this.tipoSangre = tipoSangre; }

    public String getHistorialClinico() { return historialClinico; }
    public void setHistorialClinico(String historialClinico) { this.historialClinico = historialClinico; }

    // Representación  del paciente
    @Override
    public String toString() {
        return "ID: " + idPaciente +
               " | DNI: " + dni +
               " | Nombre: " + nombre + " " + apellido +
               " | Edad: " + edad +
               " | Teléfono: " + telefono +
               " | T. Sangre: " + tipoSangre +
               " | Obs: " + historialClinico;
    }

    // Método MAIN 
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DE LA CLASE PACIENTE ===");

        // Crear dos objetos Paciente de prueba
        Paciente p1 = new Paciente("PAC001", "12345678", "Juan", "Pérez", 30, "987654321", "O+", "Alergia a la penicilina");
        Paciente p2 = new Paciente("PAC002", "87654321", "Maria", "Gómez", 25, "912345678", "A-", "Sin antecedentes");

        // Mostrar los datos en consola usando el método toString()
        System.out.println(p1.toString());
        System.out.println(p2.toString());

        // Probar el cambio de un dato 
        System.out.println("\n...Actualizando teléfono de Juan...");
        p1.setTelefono("900111222");
        System.out.println("Nuevo teléfono de " + p1.getNombre() + ": " + p1.getTelefono());
    }
}
