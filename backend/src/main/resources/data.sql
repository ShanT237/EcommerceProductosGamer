-- Vendedores
INSERT INTO vendedores (id, nombre, correo, tipo, documento, activo) VALUES
                                                                         ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'GamerZone',    'demo@gamerzone.com', 'EMPRESA',          '900111222',  true),
                                                                         ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Carlos Pérez', 'carlos@correo.com',  'PERSONA_NATURAL',  '1094567890', true);

-- Productos
INSERT INTO productos (id, vendedor_id, nombre, descripcion, precio, stock, gama, exclusividad, eliminado) VALUES
                                                                                                               ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Teclado mecánico RGB',        'Switches rojos, layout español',   250000,  8, 'ALTA',  'NORMAL',    false),
                                                                                                               ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Mouse gamer inalámbrico',     '16000 DPI, 6 botones',             120000, 19, 'MEDIA', 'NORMAL',    false),
                                                                                                               ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Figura coleccionable limitada', 'Edición numerada de colección',   350000,  5, 'ALTA',  'EXCLUSIVO', false);

-- Usuarios (contraseñas de demo: JoseGamer2026 y AnaGamer2026)
INSERT INTO usuarios (id, nombre, correo, puntos, activo, contrasena_hash, version) VALUES
                                                              ('cccccccc-cccc-cccc-cccc-ccccccccccc1', 'Jose', 'jose.demo@correo.com', 0, true, 'am9zZS1kZW1vLXNhbHQwMQ==:GQoLXkDz7/wJTTYNgUMM/pL1Jr4n/dEYWLxBMadrm1E=', 0),
                                                              ('cccccccc-cccc-cccc-cccc-ccccccccccc2', 'Ana',  'ana.demo@correo.com',  0, true, 'YW5hLWRlbW8tc2FsdDAwMA==:kWFfaer3aTQ3sfzIEk1d8QBDmEqMju4Q4OmM2sFyjoY=', 0);

-- Compras (el stock de los productos ya descuenta estas cantidades)
INSERT INTO compras (id, usuario_id, producto_id, cantidad, fecha, precio_unitario, estado, resenada, puntos_asignados, descargado) VALUES
                                                                                                                                        ('dddddddd-dddd-dddd-dddd-ddddddddddd1', 'cccccccc-cccc-cccc-cccc-ccccccccccc1', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 2, '2026-10-19 10:00:00', 250000, 'PENDIENTE',  false, false, false),
                                                                                                                                        ('dddddddd-dddd-dddd-dddd-ddddddddddd2', 'cccccccc-cccc-cccc-cccc-ccccccccccc2', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 1, '2026-10-19 11:30:00', 120000, 'COMPLETADA', false, false, false);