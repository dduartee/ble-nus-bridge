# Documentation Guide for Open Source Repositories

A reference guide for organizing, writing, and maintaining documentation in open source projects.

---

## 1. Core Principle

**Each fact lives in exactly one authoritative file.** Other files summarize and link to it. Never duplicate — reference.

```text
README.md (hub) ──links──→ docs/ARCHITECTURE.md (authority)
                         ──links──→ docs/INSTALL.md (authority)
                         ──links──→ docs/API.md (authority)
```

If you find yourself writing the same content in two places, one of them is wrong.

---

## 2. File Structure

```text
project-root/
├── README.md                 # Hub — always present, always current
├── LICENSE                   # Required for open source
├── CONTRIBUTING.md           # How to contribute
├── CODE_OF_CONDUCT.md        # Community standards
├── SECURITY.md               # Vulnerability reporting
├── CHANGELOG.md              # Release history
├── AGENTS.md                 # AI agent instructions (optional)
├── docs/
│   ├── ARCHITECTURE.md       # Internal design, data flow, decisions
│   ├── INSTALL.md            # Setup for all environments
│   ├── API.md                # Public API reference (if applicable)
│   ├── EXAMPLES.md           # Usage examples, workflows
│   ├── COMPARISON.md         # vs alternatives
│   ├── TROUBLESHOOTING.md    # Common problems and fixes
│   ├── FAQ.md                # Frequently asked questions
│   └── CHANGELOG.md          # (alternative: keep at root)
└── examples/                 # Code examples (if applicable)
```

### What goes where

| File | Audience | Purpose | Length target |
|------|----------|---------|---------------|
| `README.md` | Everyone | First impression, quick start, links | 100-200 lines |
| `docs/INSTALL.md` | Users | Setup instructions | 100-300 lines |
| `docs/ARCHITECTURE.md` | Contributors | How it works internally | 100-300 lines |
| `docs/API.md` | Developers | Parameter tables, return types | Varies |
| `docs/EXAMPLES.md` | Users | Practical usage patterns | 100-200 lines |
| `docs/COMPARISON.md` | Evaluators | Why this, not that | 50-150 lines |
| `docs/TROUBLESHOOTING.md` | Users having problems | Symptom → cause → fix | 50-200 lines |
| `CONTRIBUTING.md` | Contributors | How to help | 50-150 lines |
| `SECURITY.md` | Security researchers | How to report | 20-50 lines |

---

## 3. README.md Rules

The README is a **hub**, not a manual. It summarizes and links — it never details.

### Must include

1. **Title + badges** — name, license, build status, version
2. **One-liner** — what it is, in one sentence
3. **Prerequisites** — what the user needs before starting
4. **Quick Start** — minimal steps to get running (5-10 lines of commands)
5. **Tools/Features table** — brief summary with links to detailed docs
6. **Configuration** — env vars, config files (table format)
7. **Documentation index** — table linking to all docs/ files
8. **License** — one line with link

### Must NOT include

- Full installation guides (→ `docs/INSTALL.md`)
- Architecture deep dives (→ `docs/ARCHITECTURE.md`)
- Detailed API reference / parameter tables (→ `docs/API.md`)
- Full troubleshooting tables (→ `docs/TROUBLESHOOTING.md`)
- Comparison tables with competitors (→ `docs/COMPARISON.md`)
- Code examples longer than 5 lines (→ `docs/EXAMPLES.md`)
- Contributing guidelines (→ `CONTRIBUTING.md`)

### Length target: 100-200 lines

If your README exceeds 200 lines, you're explaining too much. Move content to dedicated docs.

---

## 4. Dedicated Docs Rules

Each doc in `docs/` has a **single responsibility**. When writing:

1. **Own the topic completely** — be the single source of truth
2. **Don't assume context** — link to other docs instead of repeating
3. **End with links** — point to related docs for next steps

### Doc responsibilities

| Doc | Owns | Does NOT own |
|-----|------|-------------|
| `ARCHITECTURE.md` | Data flow, design decisions, directory structure | Installation, usage examples |
| `INSTALL.md` | All setup methods, env vars, config | Architecture internals, troubleshooting details |
| `API.md` | Parameter tables, return types, error codes | Usage examples (→ EXAMPLES.md) |
| `EXAMPLES.md` | Practical workflows, CLI snippets | Parameter definitions (→ API.md) |
| `COMPARISON.md` | All competitor comparisons | Feature lists (→ README.md) |
| `TROUBLESHOOTING.md` | Symptom → cause → fix tables | Setup instructions (→ INSTALL.md) |
| `FEATURE.md` | Deep dive on a specific feature (only if complex enough to warrant its own file) | General architecture (→ ARCHITECTURE.md) |
| `INSIGHTS.md` | Design rationale, lessons learned, "why we did X" | Implementation details (→ ARCHITECTURE.md) |

---

## 5. Writing Style

### Tone

- **Direct** — no filler words, no "let me explain", no "basically"
- **Imperative** — "Run `npm install`", not "You should run `npm install`"
- **Concise** — if a sentence can be shorter, make it shorter
- **No emojis** in documentation (unless project-specific branding)

### Language

- **All user-facing text in English** — tool descriptions, error messages, log messages
- **All identifiers in English** — function names, variable names, types
- **Consistent terminology** — pick one term and use it everywhere (don't mix "repo"/"repository"/"project")

### Formatting

- **Markdown** with GitHub-flavored syntax
- **Tables** for structured data (parameters, env vars, comparisons)
- **Code blocks** with language hints (` ```bash `, ` ```json `, ` ```typescript `)
- **ASCII diagrams** for flow/architecture (not images — they rot)
- **Bullet points** for lists, **numbered lists** only for sequential steps

### What NOT to write

- "In this document, we will..." — just start
- "Note that..." — restructure so the note is obvious
- "It is important to..." — why? just state the rule
- "Please refer to..." — use a markdown link instead
- Walls of text — break into bullets, tables, or code blocks

---

## 6. Reference Format

### Linking between files

Always use relative paths. Never use absolute paths or URLs for internal docs.

```markdown
<!-- Good -->
See [Architecture](docs/ARCHITECTURE.md) for details.
See [Troubleshooting](docs/TROUBLESHOOTING.md#build-errors).

<!-- Bad -->
See [Architecture](/data/user/project/docs/ARCHITECTURE.md).
See the architecture docs.
```

### Linking to sections

Use anchor links for specific sections within a doc:

```markdown
See [Troubleshooting](docs/TROUBLESHOOTING.md#connection-errors).
```

Anchor format: lowercase, spaces → hyphens, strip special chars.
`## Connection Errors` → `#connection-errors`

### Linking from README

The README links to docs. Docs link back to README. Docs link to each other when needed.

```text
README.md ──→ docs/INSTALL.md
          ──→ docs/ARCHITECTURE.md
          ──→ docs/EXAMPLES.md

docs/INSTALL.md ──→ README.md (Quick Start)
                ──→ docs/EXAMPLES.md (usage patterns)

docs/ARCHITECTURE.md ──→ (no outbound links — it's a leaf)
```

---

## 7. Deduplication Rules

### The hierarchy

```text
README.md          — summary (2-3 sentences per topic)
docs/*.md          — full detail (one doc per topic)
AGENTS.md          — quick-scan reference for AI agents
```

### Rules

1. **README summarizes, docs explain.** If a topic has more than 3 sentences in the README, it belongs in a dedicated doc.

2. **One source of truth per fact.** The env var table lives in ONE file. Other files link to it.

3. **Code blocks are expensive.** Don't copy-paste the same config block in 3 files. Show it once, reference it from others.

4. **AGENTS.md is a cheat sheet.** It can repeat key facts for quick agent consumption, but should link to authoritative docs for details.

5. **When in doubt, delete and link.** If you're unsure whether content is duplicated, it probably is. Replace it with a link.

6. **Troubleshooting is topic-specific.** Tool-level troubleshooting goes in a dedicated TROUBLESHOOTING.md. Platform-specific troubleshooting (e.g., Termux) stays in its install doc. README can have 3-5 entries as a quick-reference triage table.

---

## 8. Open Source Requirements

### Mandatory files

| File | Purpose | Why |
|------|---------|-----|
| `README.md` | Project entry point | First thing users see |
| `LICENSE` | Legal permission | Without it, no one can use your code |
| `.gitignore` | Exclude build artifacts | Keep repo clean |

### Strongly recommended

| File | Purpose | Why |
|------|---------|-----|
| `CONTRIBUTING.md` | How to contribute | Reduces friction for contributors |
| `CODE_OF_CONDUCT.md` | Community standards | Sets expectations, protects maintainers |
| `SECURITY.md` | Vulnerability reporting | Responsible disclosure process |
| `CHANGELOG.md` | Release history | Users need to know what changed |

### Nice to have

| File | Purpose |
|------|---------|
| `.github/ISSUE_TEMPLATE/` | Structured bug reports and feature requests |
| `.github/PULL_REQUEST_TEMPLATE` | PR checklist |
| `.github/workflows/ci.yml` | Automated build + test (include a doc link checker like `lychee` to catch broken internal links) |
| `docs/FAQ.md` | Common questions |
| `docs/COMPARISON.md` | Why this over alternatives |

### Badges

Include in README:

```markdown
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Node](https://img.shields.io/badge/node-%3E%3D20-brightgreen)](package.json)
```

---

## 9. Documentation Mapping

Before writing any doc, answer: **"Who is this for?"**

| Audience | Needs | Doc |
|----------|-------|-----|
| First-time user | "How do I get this running?" | README → INSTALL.md |
| Daily user | "How do I use feature X?" | EXAMPLES.md or API.md |
| Contributor | "How is this built?" | ARCHITECTURE.md |
| Evaluator | "Why this over alternatives?" | COMPARISON.md |
| Troubled user | "It's broken, help" | TROUBLESHOOTING.md |
| AI agent | "What are the rules?" | AGENTS.md |

### The user journey

```text
1. Land on README.md
   ├── Convinced? → Quick Start (in README)
   ├── Need more detail? → docs/INSTALL.md
   └── Curious about internals? → docs/ARCHITECTURE.md

2. Using the project
   ├── How to use tool X? → docs/EXAMPLES.md
   ├── What are all parameters? → docs/API.md
   └── Something broke? → docs/TROUBLESHOOTING.md

3. Evaluating the project
   ├── Why this? → docs/COMPARISON.md
   └── What's the architecture? → docs/ARCHITECTURE.md

4. Contributing
   ├── How to help? → CONTRIBUTING.md
   ├── How it works? → docs/ARCHITECTURE.md
   └── Coding rules? → AGENTS.md
```

---

## 10. Maintenance Checklist

Before publishing or merging doc changes:

- [ ] README is under 200 lines
- [ ] Every doc has a single clear responsibility
- [ ] No content is duplicated across files (or if so, it's intentional like AGENTS.md)
- [ ] All internal links resolve (no broken links)
- [ ] All code blocks have language hints
- [ ] Tables are used for structured data (not paragraphs)
- [ ] No absolute paths in links
- [ ] No emojis (unless project branding)
- [ ] English throughout (text, identifiers, comments)
- [ ] Every doc in `docs/` is linked from README
- [ ] Troubleshooting entries are in table format (symptom → cause → fix)
- [ ] Examples are copy-pasteable (not pseudocode)

---

## 11. Anti-patterns

| Anti-pattern | Why it's bad | Fix |
|-------------|-------------|-----|
| Giant README (500+ lines) | Users can't find what they need | Split into docs/, keep README as hub |
| Copy-pasted config blocks | Updates require editing N files | Show once, link from others |
| "See above" / "As mentioned" | Fragile references, breaks on reorder | Use markdown links |
| Documentation without examples | Users learn by doing | Every API doc needs examples |
| Comments that explain what, not why | Adds noise | Comments explain why; code explains what |
| Prose where a table works | Hard to scan | Use tables for parameters, configs, comparisons |
| Docs that lag behind code | Misleading, erodes trust | Update docs in the same PR as code changes |
| Image-based diagrams | Can't be searched, edited, or versioned | Use ASCII diagrams or Mermaid |

---

## 12. Template

Use this as a starting point for new projects:

```markdown
# project-name

[![License](badge)](LICENSE)

One-liner description.

## Quick Start

```bash
git clone ...
cd ...
npm install && npm run build
```

## Tools

| Tool | Description |
|------|-------------|
| `tool_a` | What it does |
| `tool_b` | What it does |

## Configuration

| Env var | Default | Description |
|---------|---------|-------------|
| `VAR_A` | `value` | What it controls |

## Documentation

| Doc | Description |
|-----|-------------|
| [Install](docs/INSTALL.md) | Setup instructions |
| [Architecture](docs/ARCHITECTURE.md) | Internal design |
| [Examples](docs/EXAMPLES.md) | Usage patterns |
| [Comparison](docs/COMPARISON.md) | vs alternatives |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common problems |

## License

MIT
```
