import { ptBR } from './i18n/pt-BR';

export function App() {
  return (
    <>
      <main>
        <h1>{ptBR.app.name}</h1>
        <p>{ptBR.app.underConstruction}</p>
      </main>
      <footer>
        <p>{ptBR.app.disclaimer}</p>
      </footer>
    </>
  );
}
