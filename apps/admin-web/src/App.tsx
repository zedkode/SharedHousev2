import { API_VERSION } from '@sharedhouse/contracts';

const foundationAreas = [
  {
    title: 'Operations',
    description: 'Service health, releases, jobs, and incident visibility.',
  },
  {
    title: 'Commerce',
    description: 'Products, verified store transactions, and entitlements.',
  },
  {
    title: 'Privacy',
    description: 'Export and deletion requests with least-privilege access.',
  },
] as const;

export function App() {
  return (
    <div className="app-shell">
      <header className="masthead">
        <a className="brand" href="#main">
          SharedHouse
        </a>
        <span className="environment" aria-label="Environment: local development">
          Local development
        </span>
      </header>

      <main id="main" className="content">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Platform administration</p>
          <h1 id="page-title">A secure foundation for household operations.</h1>
          <p>
            This first engineering slice establishes the portal shell. Household content remains
            inaccessible until authenticated, audited role-based access is implemented.
          </p>
          <dl className="contract-status">
            <div>
              <dt>API contract</dt>
              <dd>{API_VERSION}</dd>
            </div>
            <div>
              <dt>Environment</dt>
              <dd>synthetic only</dd>
            </div>
          </dl>
        </section>

        <section aria-labelledby="foundation-title">
          <h2 id="foundation-title">Foundation areas</h2>
          <div className="area-grid">
            {foundationAreas.map((area) => (
              <article className="area-card" key={area.title}>
                <h3>{area.title}</h3>
                <p>{area.description}</p>
                <span className="status">
                  <span aria-hidden="true">○</span> Not configured
                </span>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
