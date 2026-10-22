package com.uniquindio.backend;


import com.uniquindio.backend.application.usecase.IniciarSesionUseCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest

class BackendApplicationTests {

	@Autowired
	private IniciarSesionUseCase iniciarSesionUseCase;

	@Test
	void contextLoads() {
	}

	@Test
	void losUsuariosDeDemoPuedenIniciarSesion() {
		Assertions.assertFalse(iniciarSesionUseCase.ejecutar(
				"jose.demo@correo.com", "JoseGamer2026").isBlank());
		Assertions.assertFalse(iniciarSesionUseCase.ejecutar(
				"ana.demo@correo.com", "AnaGamer2026").isBlank());
		Assertions.assertFalse(iniciarSesionUseCase.ejecutar(
				"demo@gamerzone.com", "GamerZone2026").isBlank());
	}

}