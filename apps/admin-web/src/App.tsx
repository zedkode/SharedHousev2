import { API_VERSION } from '@sharedhouse/contracts';

const foundationAreas = [
  {
    title: 'Operations & Health',
    description:
      'Service telemetry, background job queues, rate limiting, and incident monitoring.',
    status: 'Operational',
    badgeClass: 'status-badge',
  },
  {
    title: 'Commerce & Entitlements',
    description: 'Subscriptions, verified Play/App Store transactions, and billing cycles.',
    status: 'Ready for integration',
    badgeClass: 'status-badge',
  },
  {
    title: 'Privacy & Security',
    description: 'GDPR data exports, account erasure requests, and zero-knowledge session store.',
    status: 'Enforced',
    badgeClass: 'status-badge',
  },
] as const;

export function App() {
  return (
    <div className="app-shell">
      <header className="masthead">
        <a className="brand" href="#main">
          SharedHouse Admin
        </a>
        <span className="environment" aria-label="Environment: local development">
          Local Environment
        </span>
      </header>

      <main id="main" className="content">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Platform Administration</p>
          <h1 id="page-title">A secure foundation for household operations.</h1>
          <p>
            Welcome to the SharedHouse control hub. Operating with strict privacy boundaries,
            audited access policies, and enterprise-grade ledger integrity.
          </p>
          <dl className="contract-status">
            <div>
              <dt>API Contract Version</dt>
              <dd>v{API_VERSION}</dd>
            </div>
            <div>
              <dt>Ledger Isolation</dt>
              <dd>Multi-tenant strict</dd>
            </div>
            <div>
              <dt>Environment</dt>
              <dd>Local Development</dd>
            </div>
          </dl>
        </section>

        <section aria-labelledby="foundation-title">
          <h2 id="foundation-title">Platform Operations & Architecture</h2>
          <div className="area-grid">
            {foundationAreas.map((area) => (
              <article className="area-card" key={area.title}>
                <div>
                  <h3>{area.title}</h3>
                  <p>{area.description}</p>
                </div>
                <div>
                  <span className={area.badgeClass}>
                    <span aria-hidden="true">●</span> {area.status}
                  </span>
                </div>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
