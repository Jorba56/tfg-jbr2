package com.jorge.usuarios.utils;

import com.jorge.usuarios.dto.UsersAllDTO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

/**
 * Clase utilitaria encargada de generar y exportar listados de datos a formato Excel (.xlsx).
 * <p>
 * Utiliza la librería Apache POI para construir dinámicamente el documento en memoria,
 * aplicando estilos a las cabeceras y ajustando el ancho de las columnas antes de
 * inyectar el archivo directamente en el flujo de respuesta HTTP.
 * </p>
 */
public class UsuarioExcelExporter {
    private final XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private final List<UsersAllDTO> listaUsuarios;

    public UsuarioExcelExporter(List<UsersAllDTO> listaUsuarios) {
        this.listaUsuarios = listaUsuarios;
        workbook = new XSSFWorkbook();
    }

    private void escribirCabecera() {
        sheet = workbook.createSheet("Todos los Usuarios");
        Row fila = sheet.createRow(0);

        CellStyle estilo = workbook.createCellStyle();
        XSSFFont fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeight(14);
        estilo.setFont(fuente);

        // Empezamos directamente por el Nombre en la columna 0
        crearCelda(fila, 0, "Nombre de Usuario", estilo);
        crearCelda(fila, 1, "Apellido", estilo);
        crearCelda(fila, 2, "Correo Electrónico", estilo);

    }

    private void crearCelda(Row fila, int contadorColumnas, Object valor, CellStyle estilo) {
        sheet.autoSizeColumn(contadorColumnas);
        Cell celda = fila.createCell(contadorColumnas);

        if (valor != null) {
            celda.setCellValue(valor.toString());
        } else {
            celda.setCellValue("");
        }
        celda.setCellStyle(estilo);
    }

    private void escribirDatos() {
        int contadorFilas = 1;
        CellStyle estilo = workbook.createCellStyle();
        XSSFFont fuente = workbook.createFont();
        fuente.setFontHeight(12);
        estilo.setFont(fuente);

        for (UsersAllDTO usuario : listaUsuarios) {
            Row fila = sheet.createRow(contadorFilas++);
            int contadorColumnas = 0;

            // Usamos solo los campos que sabemos que tienes en tu DTO
            crearCelda(fila, contadorColumnas++, usuario.getNombreUsuario(), estilo);
            crearCelda(fila, contadorColumnas++, usuario.getApellidoUsuario(), estilo);
            crearCelda(fila, contadorColumnas, usuario.getEmailUsuario(), estilo);
        }
    }

    public void exportar(HttpServletResponse response) throws IOException {
        escribirCabecera();
        escribirDatos();

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }
}