package com.iglesia.caja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class RespaldoSqlite {

	private RespaldoSqlite() {
	}

	static void copiar(Path origen, Path destino) throws IOException, SQLException {
		Path fuente = origen.toAbsolutePath().normalize();
		Path salida = destino.toAbsolutePath().normalize();
		if (!Files.isRegularFile(fuente) || Files.size(fuente) == 0) {
			throw new IOException("La base de origen no existe o esta vacia: " + fuente);
		}
		if (Files.exists(salida, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("El archivo de destino ya existe: " + salida);
		}
		for (String sufijo : new String[] { "-wal", "-shm", "-journal" }) {
			if (Files.exists(Path.of(salida + sufijo), LinkOption.NOFOLLOW_LINKS)) {
				throw new IOException("Existe un archivo auxiliar del destino: " + salida + sufijo);
			}
		}

		Path temporal = Files.createTempDirectory(salida.getParent(), ".caja-respaldo-");
		Path copia = temporal.resolve("copia.db");
		try {
			try (Connection conexion = abrirLectura(fuente);
					PreparedStatement sentencia = conexion.prepareStatement("VACUUM INTO ?")) {
				sentencia.setString(1, copia.toString());
				sentencia.execute();
			}
			verificarIntegridad(copia);
			Files.move(copia, salida);
		} finally {
			Files.deleteIfExists(copia);
			Files.deleteIfExists(temporal);
		}
	}

	private static Connection abrirLectura(Path archivo) throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:" + archivo.toUri().toASCIIString() + "?mode=ro");
	}

	private static void verificarIntegridad(Path archivo) throws SQLException {
		try (Connection conexion = abrirLectura(archivo);
				Statement sentencia = conexion.createStatement();
				ResultSet resultado = sentencia.executeQuery("PRAGMA integrity_check")) {
			if (!resultado.next() || !"ok".equals(resultado.getString(1)) || resultado.next()) {
				throw new SQLException("La copia no supero la comprobacion de integridad");
			}
		}
	}
}