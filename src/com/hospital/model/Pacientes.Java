// 2. CLASE PRINCIPAL Y DE GESTIÓN
public class GestionPacientes {
    private List<Paciente> listaPacientes;

    public GestionPacientes() {
        this.listaPacientes = new ArrayList<>();
        cargarDatosPrueba(); // Carga datos iniciales para probar
    }

    // Carga los datos de prueba
    private void cargarDatosPrueba() {
        listaPacientes.add(new Paciente("PAC001", "12345678", "Juan", "Pérez", 30, "987654321", "O+", "Alergia a la penicilina"));
        listaPacientes.add(new Paciente("PAC002", "87654321", "Maria", "Gómez", 25, "912345678", "A-", "Sin antecedentes"));
    }

    // Registrar un nuevo paciente validando su DNI
    public boolean registrarPaciente(Paciente nuevo) {
        if (buscarPorDni(nuevo.getDni()) != null) {
            System.out.println("❌ Error: Ya existe un paciente con el DNI " + nuevo.getDni());
            return false;
        }
        listaPacientes.add(nuevo);
        System.out.println("✅ Paciente registrado con éxito.");
        return true;
    }

    // Buscar paciente por su DNI
    public Paciente buscarPorDni(String dni) {
        for (Paciente p : listaPacientes) {
            if (p.getDni().equalsIgnoreCase(dni)) {
                return p;
            }
        }
        return null;
    }

    // Listar a cada paciente
    public void listarPacientes() {
        if (listaPacientes.isEmpty()) {
            System.out.println("No hay pacientes registrados.");
            return;
        }
        System.out.println("\n--- LISTA DE PACIENTES ---");
        for (Paciente p : listaPacientes) {
            System.out.println(p);
        }
    }

    // Eliminar paciente por su DNI
    public boolean eliminarPaciente(String dni) {
        Paciente p = buscarPorDni(dni);
        if (p != null) {
            listaPacientes.remove(p);
            System.out.println("✅ Paciente eliminado correctamente.");
            return true;
        }
        System.out.println("❌ No se encontró ningún paciente con el DNI proporcionado.");
        return false;
    }
    // MÉTODO MAIN (EJECUCIÓN DEL MENÚ)
    public static void main(String[] args) {
        GestionPacientes gestion = new GestionPacientes();
        Scanner scanner = new Scanner(System.in);
        int opcion = 0;

        do {
            System.out.println("\n=== SISTEMA DE GESTIÓN DE PACIENTES ===");
            System.out.println("1. Registrar nuevo paciente");
            System.out.println("2. Buscar paciente por DNI");
            System.out.println("3. Listar todos los pacientes");
            System.out.println("4. Eliminar paciente");
            System.out.println("5. Salir");
            System.out.print("Seleccione una opción: ");

            try {
                opcion = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                opcion = 0;
            }

            switch (opcion) {
                case 1:
                    System.out.print("ID: ");
                    String id = scanner.nextLine();
                    System.out.print("DNI: ");
                    String dni = scanner.nextLine();
                    System.out.print("Nombre: ");
                    String nombre = scanner.nextLine();
                    System.out.print("Apellido: ");
                    String apellido = scanner.nextLine();
                    System.out.print("Edad: ");
                    int edad = 0;
                    try {
                        edad = Integer.parseInt(scanner.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Edad inválida, se registrará 0 por defecto.");
                    }
                    System.out.print("Teléfono: ");
                    String tel = scanner.nextLine();
                    System.out.print("Tipo de Sangre: ");
                    String sangre = scanner.nextLine();
                    System.out.print("Historial/Observaciones: ");
                    String historial = scanner.nextLine();

                    Paciente nuevo = new Paciente(id, dni, nombre, apellido, edad, tel, sangre, historial);
                    gestion.registrarPaciente(nuevo);
                    break;

                case 2:
                    System.out.print("Ingrese DNI a buscar: ");
                    String dniBusqueda = scanner.nextLine();
                    Paciente encontrado = gestion.buscarPorDni(dniBusqueda);
                    if (encontrado != null) {
                        System.out.println("\nPaciente encontrado:\n" + encontrado);
                    } else {
                        System.out.println("❌ Paciente no encontrado.");
                    }
                    break;

                case 3:
                    gestion.listarPacientes();
                    break;

                case 4:
                    System.out.print("Ingrese DNI a eliminar: ");
                    String dniEliminar = scanner.nextLine();
                    gestion.eliminarPaciente(dniEliminar);
                    break;

                case 5:
                    System.out.println("¡Gracias por usar el sistema!");
                    break;

                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        } while (opcion != 5);

        scanner.close();
    }
}
