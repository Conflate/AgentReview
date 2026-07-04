import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  createRepositoryProfile,
  createSession,
  generateAiSummary,
  generateReviewPacket,
  getAiSummary,
  getDashboard,
  getRepositoryProfiles,
  getReviewPackets,
  getSessions,
  importDiff,
  importEvents,
  importTestOutput,
  runRiskAnalysis,
} from './api/client.js';
import './styles.css';

const riskOrder = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const toolLabels = {
  CLAUDE_CODE: 'Claude Code',
  CODEX: 'Codex',
  CURSOR: 'Cursor',
  COPILOT: 'Copilot',
  CUSTOM: 'Custom',
};

const sampleEventsJson = JSON.stringify(
	[
		{
			eventType: 'EDIT_FILE',
			filePath: 'src/main/java/com/app/inventory/InventoryReservationService.java',
			command: null,
			summary: 'Modified inventory reservation logic',
			timestamp: '2026-06-27T20:00:00Z',
		},
    {
      eventType: 'RUN_COMMAND',
      filePath: null,
      command: './mvnw test',
      summary: 'Ran backend tests',
      timestamp: '2026-06-27T20:05:00Z',
    },
  ],
  null,
  2,
);

const sampleDiffText =
	'diff --git a/src/main/java/com/app/inventory/InventoryReservationService.java b/src/main/java/com/app/inventory/InventoryReservationService.java\n' +
	'index abc..def 100644\n' +
	'--- a/src/main/java/com/app/inventory/InventoryReservationService.java\n' +
	'+++ b/src/main/java/com/app/inventory/InventoryReservationService.java';

function App() {
  const [view, setView] = useState('dashboard');
  const [dashboard, setDashboard] = useState(null);
  const [reviewPackets, setReviewPackets] = useState([]);
  const [profiles, setProfiles] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState('');

  async function loadData({ showLoading = true } = {}) {
    if (showLoading) {
      setStatus('loading');
    }
    setError('');
    try {
      const [dashboardData, packetData, profileData, sessionData] = await Promise.all([
        getDashboard(),
        getReviewPackets(),
        getRepositoryProfiles(),
        getSessions(),
      ]);
      setDashboard(dashboardData);
      setReviewPackets(packetData);
      setProfiles(profileData);
      setSessions(sessionData);
      setStatus('ready');
    } catch (loadError) {
      setError(loadError.message);
      setStatus('error');
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">AR</div>
          <div>
            <h1>AgentReview</h1>
            <p>Review evidence</p>
          </div>
        </div>
        <nav className="nav-list" aria-label="Main navigation">
          <button
            className={`nav-item ${view === 'dashboard' ? 'active' : ''}`}
            type="button"
            onClick={() => setView('dashboard')}
          >
            Dashboard
          </button>
          <button
            className={`nav-item ${view === 'workflow' ? 'active' : ''}`}
            type="button"
            onClick={() => setView('workflow')}
          >
            Session import
          </button>
          <button
            className={`nav-item ${view === 'packets' ? 'active' : ''}`}
            type="button"
            onClick={() => setView('packets')}
          >
            Review packets
          </button>
        </nav>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Frontend uses Vite proxy for /api requests</p>
            <h2>{viewTitle(view)}</h2>
          </div>
          <button className="refresh-button" type="button" onClick={loadData}>
            Refresh
          </button>
        </header>

        {status === 'error' && <ConnectionError message={error} />}
        {status === 'loading' && <LoadingState />}
        {status === 'ready' && dashboard && view === 'dashboard' && (
          <DashboardView dashboard={dashboard} reviewPackets={reviewPackets} />
        )}
        {status === 'ready' && view === 'workflow' && (
          <SessionImportFlow
            profiles={profiles}
            sessions={sessions}
            onDataChange={() => loadData({ showLoading: false })}
          />
        )}
        {status === 'ready' && view === 'packets' && <ReviewPacketBrowser packets={reviewPackets} />}
      </main>
    </div>
  );
}

function viewTitle(view) {
  if (view === 'workflow') {
    return 'Session import';
  }
  if (view === 'packets') {
    return 'Review packets';
  }
  return 'Dashboard';
}

function DashboardView({ dashboard, reviewPackets }) {
  return (
    <>
      <MetricGrid dashboard={dashboard} />
      <section className="workspace-grid">
        <RiskDistribution packetsByRisk={dashboard.packetsByRisk} />
        <SessionsByTool sessionsByTool={dashboard.sessionsByTool} />
      </section>
      <section className="workspace-grid lower">
        <TopPolicyFlags flags={dashboard.topPolicyFlags} />
        <ReviewPacketList packets={reviewPackets} />
      </section>
    </>
  );
}

function SessionImportFlow({ profiles, sessions, onDataChange }) {
  const sessionOptions = useMemo(
    () => [...sessions].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)),
    [sessions],
  );
  const [profileForm, setProfileForm] = useState({
		repoName: 'warehouse-api',
		businessCriticality: 'HIGH',
		protectedPathPatterns:
			'src/main/java/**/auth/**\nsrc/main/java/**/inventory/**\nsrc/main/java/**/orders/**\n.github/workflows/**',
		restrictedCommandPatterns: 'rm -rf *\ncurl * | sh\ngit push --force',
		approvalRequiredAt: 'MEDIUM',
	});
  const [sessionForm, setSessionForm] = useState({
		sessionExternalId: 'codex-run-001',
		agentTool: 'CODEX',
		developer: 'David',
		repoName: 'warehouse-api',
		repositoryProfileId: '',
		branchName: 'feature/inventory-reservations',
		summary: 'Agent modified inventory reservation logic',
	});
  const [selectedSessionId, setSelectedSessionId] = useState('');
  const [eventsJson, setEventsJson] = useState(sampleEventsJson);
  const [diffText, setDiffText] = useState(sampleDiffText);
  const [testCommand, setTestCommand] = useState('./mvnw test');
  const [testOutput, setTestOutput] = useState('Tests run: 42, Failures: 0, Errors: 0, Skipped: 0 BUILD SUCCESS');
  const [result, setResult] = useState(null);
  const [workflowStatus, setWorkflowStatus] = useState('idle');
  const [workflowError, setWorkflowError] = useState('');

  async function submitProfile(event) {
    event.preventDefault();
    await runWorkflowAction(async () => {
      const profile = await createRepositoryProfile({
        repoName: profileForm.repoName,
        businessCriticality: profileForm.businessCriticality,
        protectedPathPatterns: lines(profileForm.protectedPathPatterns),
        restrictedCommandPatterns: lines(profileForm.restrictedCommandPatterns),
        approvalRequiredAt: profileForm.approvalRequiredAt,
      });
      setResult({ title: 'Repository profile created', body: `Profile #${profile.id} created for ${profile.repoName}.` });
      setSessionForm((current) => ({
        ...current,
        repositoryProfileId: String(profile.id),
        repoName: profile.repoName,
      }));
      await onDataChange();
    });
  }

  async function submitSession(event) {
    event.preventDefault();
    await runWorkflowAction(async () => {
      const session = await createSession({
        ...sessionForm,
        repositoryProfileId: sessionForm.repositoryProfileId ? Number(sessionForm.repositoryProfileId) : null,
      });
      setSelectedSessionId(String(session.id));
      setResult({ title: 'Session created', body: `Session #${session.id} created for ${session.repoName}.` });
      await onDataChange();
    });
  }

  async function submitEvidence(event) {
    event.preventDefault();
    await runWorkflowAction(async () => {
      if (!selectedSessionId) {
        throw new Error('Select or create a session before importing evidence.');
      }
      const sessionId = Number(selectedSessionId);
      let events;
      try {
        events = JSON.parse(eventsJson);
      } catch {
        throw new Error('Agent events JSON is not valid JSON.');
      }
      if (!Array.isArray(events)) {
        throw new Error('Agent events JSON must be an array of event objects.');
      }
      await importEvents(sessionId, events);
      await importDiff(sessionId, diffText);
      await importTestOutput(sessionId, {
        command: testCommand,
        output: testOutput,
      });
      const risk = await runRiskAnalysis(sessionId);
      const packet = await generateReviewPacket(sessionId);
      setResult({
        title: 'Review packet generated',
        body: `Packet #${packet.id} generated with ${risk.riskLevel} risk and ${risk.mergeReadiness} readiness.`,
        packetMarkdown: packet.packetMarkdown,
      });
      await onDataChange();
    });
  }

  async function runWorkflowAction(action) {
    setWorkflowStatus('running');
    setWorkflowError('');
    try {
      await action();
      setWorkflowStatus('ready');
    } catch (actionError) {
      setWorkflowError(actionError.message);
      setWorkflowStatus('error');
    }
  }

  return (
    <div className="flow-stack">
      {workflowStatus === 'error' && <ConnectionError message={workflowError} />}
      {result && <ResultPanel result={result} />}
      <section className="form-grid">
        <form className="panel form-panel" onSubmit={submitProfile}>
          <div className="panel-header">
            <h3>Repository profile</h3>
            <span>{profiles.length} existing</span>
          </div>
          <TextInput label="Repository" value={profileForm.repoName} onChange={(value) => setProfileForm({ ...profileForm, repoName: value })} />
          <SelectInput label="Business criticality" value={profileForm.businessCriticality} options={riskOrder} onChange={(value) => setProfileForm({ ...profileForm, businessCriticality: value })} />
          <SelectInput label="Review threshold" value={profileForm.approvalRequiredAt} options={riskOrder} onChange={(value) => setProfileForm({ ...profileForm, approvalRequiredAt: value })} />
          <TextareaInput label="Protected paths" value={profileForm.protectedPathPatterns} rows={5} onChange={(value) => setProfileForm({ ...profileForm, protectedPathPatterns: value })} />
          <TextareaInput label="Restricted commands" value={profileForm.restrictedCommandPatterns} rows={4} onChange={(value) => setProfileForm({ ...profileForm, restrictedCommandPatterns: value })} />
          <button className="primary-button" type="submit" disabled={workflowStatus === 'running'}>
            Create profile
          </button>
        </form>

        <form className="panel form-panel" onSubmit={submitSession}>
          <div className="panel-header">
            <h3>Agent session</h3>
            <span>{sessions.length} existing</span>
          </div>
          <TextInput label="External ID" value={sessionForm.sessionExternalId} onChange={(value) => setSessionForm({ ...sessionForm, sessionExternalId: value })} />
          <SelectInput label="Agent tool" value={sessionForm.agentTool} options={Object.keys(toolLabels)} onChange={(value) => setSessionForm({ ...sessionForm, agentTool: value })} />
          <TextInput label="Developer" value={sessionForm.developer} onChange={(value) => setSessionForm({ ...sessionForm, developer: value })} />
          <SelectInput
            label="Repository profile"
            value={sessionForm.repositoryProfileId}
            options={profiles.map((profile) => String(profile.id))}
            placeholder="No profile selected"
            optionLabel={(id) => {
              const profile = profiles.find((item) => String(item.id) === id);
              return profile ? `${profile.repoName} (#${profile.id})` : id;
            }}
            onChange={(value) => {
              const profile = profiles.find((item) => String(item.id) === value);
              setSessionForm({
                ...sessionForm,
                repositoryProfileId: value,
                repoName: profile?.repoName || sessionForm.repoName,
              });
            }}
          />
          <TextInput label="Repository" value={sessionForm.repoName} onChange={(value) => setSessionForm({ ...sessionForm, repoName: value })} />
          <TextInput label="Branch" value={sessionForm.branchName} onChange={(value) => setSessionForm({ ...sessionForm, branchName: value })} />
          <TextareaInput label="Summary" value={sessionForm.summary} rows={3} onChange={(value) => setSessionForm({ ...sessionForm, summary: value })} />
          <button className="primary-button" type="submit" disabled={workflowStatus === 'running'}>
            Create session
          </button>
        </form>
      </section>

      <form className="panel evidence-panel" onSubmit={submitEvidence}>
        <div className="panel-header">
          <h3>Import evidence and generate packet</h3>
          <span>{selectedSessionId ? `Session #${selectedSessionId}` : 'No session selected'}</span>
        </div>
        <SelectInput
          label="Session"
          value={selectedSessionId}
          options={sessionOptions.map((session) => String(session.id))}
          placeholder="Select a session"
          optionLabel={(id) => {
            const session = sessions.find((item) => String(item.id) === id);
            return session ? `#${session.id} ${session.repoName} ${session.branchName}` : id;
          }}
          onChange={setSelectedSessionId}
        />
        <TextareaInput label="Agent events JSON" value={eventsJson} rows={8} onChange={setEventsJson} />
        <TextareaInput label="Git diff" value={diffText} rows={6} onChange={setDiffText} />
        <TextInput label="Test command" value={testCommand} onChange={setTestCommand} />
        <TextareaInput label="Test output" value={testOutput} rows={4} onChange={setTestOutput} />
        <button className="primary-button" type="submit" disabled={!selectedSessionId || workflowStatus === 'running'}>
          Import evidence and generate review packet
        </button>
      </form>
    </div>
  );
}

function lines(value) {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean);
}

function ResultPanel({ result }) {
  return (
    <section className="notice success">
      <h3>{result.title}</h3>
      <p>{result.body}</p>
      {result.packetMarkdown && <pre className="packet-preview">{result.packetMarkdown}</pre>}
    </section>
  );
}

function TextInput({ label, value, onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function SelectInput({ label, value, options, optionLabel = (option) => option, placeholder = null, onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {(placeholder || options.length === 0) && (
          <option value="">{placeholder || 'No options'}</option>
        )}
        {options.map((option) => (
          <option key={option} value={option}>
            {optionLabel(option)}
          </option>
        ))}
      </select>
    </label>
  );
}

function TextareaInput({ label, value, rows, onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <textarea rows={rows} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function MetricGrid({ dashboard }) {
  const metrics = [
    ['Total sessions', dashboard.totalSessions],
    ['Review packets', dashboard.totalReviewPackets],
    ['High risk packets', dashboard.highRiskPackets],
    ['Blocked packets', dashboard.blockedPackets],
  ];

  return (
    <section className="metric-grid" aria-label="Dashboard metrics">
      {metrics.map(([label, value]) => (
        <div className="metric" key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </section>
  );
}

function RiskDistribution({ packetsByRisk }) {
  const total = useMemo(
    () => Object.values(packetsByRisk || {}).reduce((sum, value) => sum + value, 0),
    [packetsByRisk],
  );

  return (
    <section className="panel" id="dashboard">
      <div className="panel-header">
        <h3>Packets by risk</h3>
        <span>{total} total</span>
      </div>
      <div className="risk-bars">
        {riskOrder.map((risk) => {
          const value = packetsByRisk?.[risk] || 0;
          const width = total === 0 ? 0 : Math.max(8, Math.round((value / total) * 100));
          return (
            <div className="risk-row" key={risk}>
              <span>{risk}</span>
              <div className="bar-track">
                <div className={`bar-fill ${risk.toLowerCase()}`} style={{ width: `${width}%` }} />
              </div>
              <strong>{value}</strong>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function SessionsByTool({ sessionsByTool }) {
  return (
    <section className="panel">
      <div className="panel-header">
        <h3>Sessions by tool</h3>
      </div>
      <div className="table-like">
        {Object.entries(sessionsByTool || {}).map(([tool, count]) => (
          <div className="table-row" key={tool}>
            <span>{toolLabels[tool] || tool}</span>
            <strong>{count}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function TopPolicyFlags({ flags }) {
  return (
    <section className="panel" id="packets">
      <div className="panel-header">
        <h3>Top policy flags</h3>
      </div>
      {flags.length === 0 ? (
        <p className="empty-state">No policy flags yet.</p>
      ) : (
        <div className="flag-list">
          {flags.map((flag) => (
            <div className="flag-row" key={flag.message}>
              <span>{flag.message}</span>
              <strong>{flag.count}</strong>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function ReviewPacketList({ packets }) {
  return (
    <section className="panel">
      <div className="panel-header">
        <h3>Recent packets</h3>
        <span>{packets.length} shown</span>
      </div>
      {packets.length === 0 ? (
        <p className="empty-state">Generate a review packet from a session to see it here.</p>
      ) : (
        <div className="packet-list">
          {packets.slice(0, 6).map((packet) => (
            <article className="packet-card" key={packet.id}>
              <div>
                <span className={`risk-pill ${packet.riskLevel.toLowerCase()}`}>{packet.riskLevel}</span>
                <h4>Packet #{packet.id}</h4>
                <p>Session #{packet.sessionId}</p>
              </div>
              <strong>{packet.mergeReadiness}</strong>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function ReviewPacketBrowser({ packets }) {
  const [selectedPacketId, setSelectedPacketId] = useState(packets[0]?.id ? String(packets[0].id) : '');
  const selectedPacket = packets.find((packet) => String(packet.id) === selectedPacketId) || packets[0];
  const [summary, setSummary] = useState(null);
  const [summaryStatus, setSummaryStatus] = useState('idle');
  const [summaryError, setSummaryError] = useState('');

  useEffect(() => {
    if (!selectedPacketId && packets[0]?.id) {
      setSelectedPacketId(String(packets[0].id));
    }
  }, [packets, selectedPacketId]);

  useEffect(() => {
    let cancelled = false;

    async function loadSummary() {
      if (!selectedPacket?.id) {
        setSummary(null);
        setSummaryStatus('idle');
        setSummaryError('');
        return;
      }

      setSummary(null);
      setSummaryStatus('loading');
      setSummaryError('');
      try {
        const latestSummary = await getAiSummary(selectedPacket.id);
        if (!cancelled) {
          setSummary(latestSummary);
          setSummaryStatus('ready');
        }
      } catch (loadError) {
        if (!cancelled) {
          setSummaryStatus(loadError.status === 404 ? 'empty' : 'error');
          setSummaryError(loadError.status === 404 ? '' : loadError.message);
        }
      }
    }

    loadSummary();

    return () => {
      cancelled = true;
    };
  }, [selectedPacket?.id]);

  async function handleGenerateSummary() {
    if (!selectedPacket?.id) {
      return;
    }

    setSummaryStatus('generating');
    setSummaryError('');
    try {
      const generatedSummary = await generateAiSummary(selectedPacket.id);
      setSummary(generatedSummary);
      setSummaryStatus('ready');
    } catch (generateError) {
      setSummaryStatus('error');
      setSummaryError(generateError.message);
    }
  }

  return (
    <section className="workspace-grid packet-browser">
      <ReviewPacketList packets={packets} />
      <section className="panel">
        <div className="panel-header">
          <h3>Packet detail</h3>
          {selectedPacket && <span>#{selectedPacket.id}</span>}
        </div>
        {packets.length > 0 && (
          <SelectInput
            label="Review packet"
            value={selectedPacketId}
            options={packets.map((packet) => String(packet.id))}
            optionLabel={(id) => {
              const packet = packets.find((item) => String(item.id) === id);
              return packet ? `Packet #${packet.id} (${packet.riskLevel})` : id;
            }}
            onChange={setSelectedPacketId}
          />
        )}
        {selectedPacket ? (
          <>
            <AiSummaryPanel
              summary={summary}
              status={summaryStatus}
              error={summaryError}
              onGenerate={handleGenerateSummary}
            />
            <pre className="packet-preview">{selectedPacket.packetMarkdown}</pre>
          </>
        ) : (
          <p className="empty-state">No review packets yet.</p>
        )}
      </section>
    </section>
  );
}

function AiSummaryPanel({ summary, status, error, onGenerate }) {
  const isBusy = status === 'loading' || status === 'generating';

  return (
    <section className="ai-summary-panel">
      <div className="ai-summary-header">
        <div>
          <h4>AI reviewer summary</h4>
          <p>{summary ? `Generated by ${summary.provider}` : 'Optional reviewer-facing summary'}</p>
        </div>
        <button className="secondary-button" type="button" onClick={onGenerate} disabled={isBusy}>
          {status === 'generating' ? 'Generating...' : summary ? 'Regenerate' : 'Generate'}
        </button>
      </div>

      {status === 'loading' && <p className="empty-state">Checking for an existing summary.</p>}
      {status === 'empty' && <p className="empty-state">No summary generated yet.</p>}
      {status === 'error' && <p className="inline-error">{error}</p>}
      {summary && (
        <div className="summary-grid">
          <SummaryBlock title="Reviewer summary" body={summary.reviewerSummary} />
          <SummaryBlock title="Inspection areas" body={summary.suggestedInspectionAreas} />
          <SummaryBlock title="Risk explanation" body={summary.riskExplanation} />
        </div>
      )}
    </section>
  );
}

function SummaryBlock({ title, body }) {
  return (
    <article className="summary-block">
      <h5>{title}</h5>
      <p>{body}</p>
    </article>
  );
}

function ConnectionError({ message }) {
  return (
    <section className="notice error">
      <h3>Backend unavailable</h3>
      <p>{message}</p>
    </section>
  );
}

function LoadingState() {
  return (
    <section className="notice">
      <h3>Loading</h3>
      <p>Fetching AgentReview data.</p>
    </section>
  );
}

createRoot(document.getElementById('root')).render(<App />);
