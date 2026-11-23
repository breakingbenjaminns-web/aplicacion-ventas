package com.iglesia.caja;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CajaApplication {

	public static void main(String[] args) {
		if (args.length > 0 && ("--crear-respaldo".equals(args[0]) || "--restaurar-respaldo".equals(args[0]))) {
			if (ejecutarRespaldo(args) != 0) {
				System.exit(1);
			}
			return;
		}
		SpringApplication.run(CajaApplication.class, args);
	}

	static int ejecutarRespaldo(String[] args) {
		if (args.length != 3) {
			System.err.println("Uso: --crear-respaldo|--restaurar-respaldo <origen.db> <destino.db>");
			return 1;
		}
		try {
			RespaldoSqlite.copiar(Path.of(args[1]), Path.of(args[2]));
			System.out.println("Copia verificada creada en: " + Path.of(args[2]).toAbsolutePath().normalize());
			return 0;
		} catch (IOException | SQLException | InvalidPathException excepcion) {
			System.err.println("No se pudo crear la copia: " + excepcion.getMessage());
			return 1;
		}
	}

}
