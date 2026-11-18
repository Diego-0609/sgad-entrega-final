// Producción (Docker): nginx sirve el frontend Y reenvía /api, /login, /logout... al backend,
// así que todo está en el MISMO origen (mismo dominio/puerto) y se usan rutas relativas.
export const environment = {
  production: true,
  // true: Angular manda el JWT en cada petición a la API (Authorization: Bearer ...).
  // false: vuelve a usar solo la cookie de sesión, como en la versión anterior.
  useJwt: true,
  backendUrl: '',
  apiUrl: '/api'
};
