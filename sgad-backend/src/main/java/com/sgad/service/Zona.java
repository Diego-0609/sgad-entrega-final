package com.sgad.service;

import java.time.ZoneId;

/** La escuela está en Cartagena: "hoy" y "esta semana" se calculan con la hora de Colombia, no la del servidor. */
public final class Zona {
    public static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private Zona() {}
}
