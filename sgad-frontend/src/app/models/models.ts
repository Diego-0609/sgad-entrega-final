// Los ids ahora son string: MongoDB identifica cada documento con un
// ObjectId, que en JSON viaja como una cadena hexadecimal (ej.
// "665f1a1b2c3d4e5f6a7b8c9d"), no como un número autoincremental.

// models/escuela.model.ts
export interface Escuela {
  idEscuela?: string;
  nombre: string;
  direccion: string;
  telefono: string;
}

// models/entrenador.model.ts
export interface Entrenador {
  idEntrenador?: string;
  nombre: string;
  especialidad: string;
  telefono: string;
  idEscuela: string;
}

// models/deportista.model.ts
export interface Deportista {
  idDeportista?: string;
  nombre: string;
  edad: number;
  genero: string;
  deporte: string;
  idEscuela: string;
}

// models/entrenamiento.model.ts
export interface Entrenamiento {
  idEntrenamiento?: string;
  fecha: string;
  hora: string;
  lugar: string;
  idEntrenador: string;
}

// models/asistencia.model.ts
export interface Asistencia {
  idAsistencia?: string;
  estado: string;
  idDeportista: string;
  idEntrenamiento: string;
}

// models/material.model.ts
// Material vive embebido dentro de Escuela en MongoDB (ver actividad de
// migración), pero la API sigue exponiéndolo con esta misma forma.
export interface Material {
  idMaterial?: string;
  nombre: string;
  cantidad: number;
  estado: string;
  idEscuela: string;
}

// ─── Panel del deportista ──────────────────────────────────
export interface EntrenadorVista {
  nombre: string;
  especialidad: string;
  telefono: string;
}

export interface PerfilMio {
  deportista: Deportista;
  escuela: string | null;
  entrenadores: EntrenadorVista[];
  reportesNoLeidos: number;
}

export interface EntrenamientoMio {
  idEntrenamiento: string;
  fecha: string;
  hora: string | null;
  lugar: string;
  entrenador: string | null;
  asistencia: 'presente' | 'ausente' | null;
}

export interface Semana {
  desde: string;
  hasta: string;
  total: number;
  presentes: number;
  ausentes: number;
  porcentaje: number;
}

export interface EstadisticasMias {
  resumen: {
    total: number;
    presentes: number;
    ausentes: number;
    porcentaje: number;
    racha: number;
    semanaActual: Semana;
    ultimasSemanas: Semana[];
  };
  proximo: EntrenamientoMio | null;
}

export interface Reporte {
  idReporte?: string;
  idDeportista: string;
  autor?: string;
  titulo: string;
  mensaje: string;
  calificacion?: number | null;
  fecha?: string;
  leido?: boolean;
}
