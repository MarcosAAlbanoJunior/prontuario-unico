import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { ptBR } from './i18n/pt-BR';

describe('App', () => {
  it('shows the app name as the main heading', () => {
    render(<App />);

    expect(screen.getByRole('heading', { level: 1, name: ptBR.app.name })).toBeInTheDocument();
  });

  it('always shows the medical disclaimer in the footer', () => {
    render(<App />);

    expect(screen.getByRole('contentinfo')).toHaveTextContent(ptBR.app.disclaimer);
  });
});
