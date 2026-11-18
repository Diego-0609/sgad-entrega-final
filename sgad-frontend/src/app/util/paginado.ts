/** Búsqueda + paginación en memoria: la tabla dibuja solo una página, aunque haya miles de registros. */
export class Paginado<T> {
  readonly tamano = 25;
  pagina = 1;
  busqueda = '';
  visibles: T[] = [];
  total = 0;
  paginas = 1;
  private todos: T[] = [];

  constructor(private texto: (item: T) => string) {}

  set(items: T[]): void {
    this.todos = items;
    this.recalcular();
  }

  buscar(q: string): void {
    this.busqueda = q;
    this.pagina = 1;
    this.recalcular();
  }

  ir(p: number): void {
    this.pagina = Math.min(Math.max(1, p), this.paginas);
    this.recalcular();
  }

  recalcular(): void {
    const q = normalizar(this.busqueda);
    const filtrados = q ? this.todos.filter(t => normalizar(this.texto(t)).includes(q)) : this.todos;
    this.total = filtrados.length;
    this.paginas = Math.max(1, Math.ceil(filtrados.length / this.tamano));
    if (this.pagina > this.paginas) this.pagina = this.paginas;
    const ini = (this.pagina - 1) * this.tamano;
    this.visibles = filtrados.slice(ini, ini + this.tamano);
  }
}

/** Minúsculas y sin tildes, para que "jose" encuentre "José". */
function normalizar(s: string): string {
  return (s ?? '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}
