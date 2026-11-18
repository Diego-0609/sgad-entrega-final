// Desarrollo (ng serve): el frontend corre en :4200 y habla con el backend en :8080
export const environment = {
  production: false,
  // true: Angular manda el JWT en cada petición a la API (Authorization: Bearer ...).
  // false: vuelve a usar solo la cookie de sesión, como en la versión anterior.
  useJwt: true,
  backendUrl: 'http://localhost:8080',
  apiUrl: 'http://localhost:8080/api'
};
